/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig.messages.network.handler.maker;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.MyMuSigOffersService;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.Trade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigTakeOfferRequestValidatorTest {
    private static final KeyPair TAKER_KEY_PAIR = KeyGeneration.generateDefaultEcKeyPair();
    private static final NetworkId OFFER_MAKER_NETWORK_ID = createNetworkId("offer-maker", 9997, null);
    private static final NetworkId TAKER_NETWORK_ID = createNetworkId("taker", 9999, TAKER_KEY_PAIR);
    private static final NetworkId OTHER_NETWORK_ID = createNetworkId("other", 9996, null);

    private final ContractService contractService = new ContractService(null);

    @Test
    void validRequestPasses() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));

        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateIdentity(contractService, message))
                .doesNotThrowAnyException();
    }

    @Test
    void senderNotMatchingContractTakerIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, OTHER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));

        assertRejected(message);
    }

    @Test
    void signatureFromForeignKeyIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        KeyPair attackerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        // The signature is internally consistent - hash and key match the signature - but the
        // key is not the taker's; without the binding check the maker would accept it.
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, attackerKeyPair));

        assertRejected(message);
    }

    @Test
    void tamperedSignatureIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        ContractSignatureData signatureData = sign(contract, TAKER_KEY_PAIR);
        byte[] tamperedSignature = signatureData.getSignature().clone();
        // Flip a bit in the middle of the signature bits, not the DER framing: the verifier
        // must return false on a well-formed but incorrect signature, not merely throw on a
        // malformed encoding.
        tamperedSignature[tamperedSignature.length / 2] ^= 0x01;
        ContractSignatureData tampered = new ContractSignatureData(signatureData.getContractHash(),
                tamperedSignature,
                signatureData.getPublicKey());
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, tampered);

        assertRejected(message);
    }

    @Test
    void receiverNotMatchingOfferMakerIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, OTHER_NETWORK_ID,
                sign(contract, TAKER_KEY_PAIR),
                Trade.createId(contract.getOffer().getId(),
                        contract.getTaker().getNetworkId().getId(),
                        contract.getTakeOfferDate()));

        assertRejected(message);
    }

    @Test
    void tradeIdNotDerivedFromContractIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        // The contract itself is validly signed; only the claimed trade id differs from the id
        // the maker derives, which would otherwise persist a failed trade per crafted message.
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, OFFER_MAKER_NETWORK_ID,
                sign(contract, TAKER_KEY_PAIR), "crafted-trade-id");

        assertRejected(message);
    }

    @Test
    void activatedOwnOfferPasses() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(contract.getOffer()));

        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateOffer(myOffers, message))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownOrDeactivatedOfferIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        // findActivatedOffer covers both cases: an unknown offer and a retained-but-deactivated
        // offer are equally absent from the activated set.
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id")).thenReturn(Optional.empty());

        assertOfferRejected(myOffers, message);
    }

    @Test
    void embeddedOfferDifferingFromOwnOfferIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        // Same id, different terms: the taker altered the embedded offer while keeping the id
        // of a genuine activated offer.
        MuSigOffer myRealOffer = createOffer(new BaseSideFixedAmountSpec(999_999L));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(myRealOffer));

        assertOfferRejected(myOffers, message);
    }

    private void assertOfferRejected(MyMuSigOffersService myOffers, SetupTradeMessage_A message) {
        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateOffer(myOffers, message))
                .isInstanceOf(TradeProtocolException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((TradeProtocolException) e).getTradeProtocolFailure())
                        .isEqualTo(TradeProtocolFailure.OFFER_NOT_AVAILABLE));
    }

    private void assertRejected(SetupTradeMessage_A message) {
        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateIdentity(contractService, message))
                .isInstanceOf(TradeProtocolException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((TradeProtocolException) e).getTradeProtocolFailure())
                        .isEqualTo(TradeProtocolFailure.OFFER_NOT_AVAILABLE));
    }

    private ContractSignatureData sign(MuSigContract contract, KeyPair keyPair) throws GeneralSecurityException {
        return contractService.signContract(contract, keyPair);
    }

    private static SetupTradeMessage_A createMessage(MuSigContract contract,
                                                     NetworkId sender,
                                                     ContractSignatureData contractSignatureData) {
        return createMessage(contract, sender, OFFER_MAKER_NETWORK_ID, contractSignatureData,
                Trade.createId(contract.getOffer().getId(),
                        contract.getTaker().getNetworkId().getId(),
                        contract.getTakeOfferDate()));
    }

    private static SetupTradeMessage_A createMessage(MuSigContract contract,
                                                     NetworkId sender,
                                                     NetworkId receiver,
                                                     ContractSignatureData contractSignatureData,
                                                     String tradeId) {
        return new SetupTradeMessage_A("message-id",
                tradeId,
                "1.0.0",
                sender,
                receiver,
                contract,
                contractSignatureData,
                PubKeyShares.fromProto(bisq.trade.protobuf.PubKeyShares.newBuilder()
                        .setBuyerOutputPubKeyShare(com.google.protobuf.ByteString.copyFrom(new byte[33]))
                        .setSellerOutputPubKeyShare(com.google.protobuf.ByteString.copyFrom(new byte[33]))
                        .setMultisigScriptKey(com.google.protobuf.ByteString.copyFrom(new byte[33]))
                        .build()));
    }

    private static MuSigContract createContract() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(111L));
        return new MuSigContract(1_700_000_000_000L,
                offer,
                TAKER_NETWORK_ID,
                111L,
                222L,
                nonBtcPaymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                offer.getPriceSpec(),
                0);
    }

    private static MuSigOffer createOffer(bisq.offer.amount.spec.AmountSpec amountSpec) {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        return new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.BUY,
                market,
                amountSpec,
                new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")),
                List.of(nonBtcPaymentMethodSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
    }

    private static NetworkId createNetworkId(String keyIdSuffix, int port, KeyPair keyPair) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        KeyPair pair = keyPair != null ? keyPair : KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(pair.getPublic(), "test-key-" + keyIdSuffix);
        return new NetworkId(addresses, pubKey);
    }
}
