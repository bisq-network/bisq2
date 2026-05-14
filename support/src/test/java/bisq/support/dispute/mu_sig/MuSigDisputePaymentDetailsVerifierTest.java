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

package bisq.support.dispute.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import bisq.i18n.Res;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MuSigDisputePaymentDetailsVerifierTest {
    @BeforeAll
    static void setupRes() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    @DisplayName("verify returns matches when maker and taker payload hashes match")
    void verify_returns_matches_when_maker_and_taker_payload_hashes_match() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        AccountPayload<?> takerPayload = createTakerPayload();
        AccountPayload<?> makerPayload = createMakerPayload();
        MuSigContract contract = createContract(maker, taker, takerPayload, List.of(makerPayload));

        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract, takerPayload, makerPayload);

        assertThat(result.takerAccountPayloadMatches()).isTrue();
        assertThat(result.makerAccountPayloadMatches()).isTrue();
        assertThat(result.takerMismatchDetails()).isEmpty();
        assertThat(result.makerMismatchDetails()).isEmpty();
    }

    @Test
    @DisplayName("verify returns taker mismatch details when taker payload hash does not match")
    void verify_returns_taker_mismatch_details_when_taker_payload_hash_does_not_match() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        AccountPayload<?> expectedTakerPayload = createTakerPayload();
        AccountPayload<?> wrongTakerPayload = createWrongTakerPayload();
        AccountPayload<?> makerPayload = createMakerPayload();
        MuSigContract contract = createContract(maker, taker, expectedTakerPayload, List.of(makerPayload));

        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract, wrongTakerPayload, makerPayload);

        assertThat(result.takerAccountPayloadMatches()).isFalse();
        assertThat(result.makerAccountPayloadMatches()).isTrue();
        assertThat(result.takerMismatchDetails()).isPresent();
        assertThat(result.makerMismatchDetails()).isEmpty();
    }

    @Test
    @DisplayName("verify returns maker mismatch details when maker payload hash does not match")
    void verify_returns_maker_mismatch_details_when_maker_payload_hash_does_not_match() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        AccountPayload<?> takerPayload = createTakerPayload();
        AccountPayload<?> expectedMakerPayload = createMakerPayload();
        AccountPayload<?> wrongMakerPayload = createWrongMakerPayload();
        MuSigContract contract = createContract(maker, taker, takerPayload, List.of(expectedMakerPayload));

        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract, takerPayload, wrongMakerPayload);

        assertThat(result.takerAccountPayloadMatches()).isTrue();
        assertThat(result.makerAccountPayloadMatches()).isFalse();
        assertThat(result.takerMismatchDetails()).isEmpty();
        assertThat(result.makerMismatchDetails()).isPresent();
    }

    @Test
    @DisplayName("verify returns maker mismatch details when maker hash is missing")
    void verify_returns_maker_mismatch_details_when_maker_hash_is_missing() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        AccountPayload<?> takerPayload = createTakerPayload();
        AccountPayload<?> makerPayload = createMakerPayload();
        MuSigContract contract = createContract(maker, taker, takerPayload, List.of());

        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract, takerPayload, makerPayload);

        assertThat(result.takerAccountPayloadMatches()).isTrue();
        assertThat(result.makerAccountPayloadMatches()).isFalse();
        assertThat(result.takerMismatchDetails()).isEmpty();
        assertThat(result.makerMismatchDetails()).isPresent();
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         AccountPayload<?> takerPayloadForHash,
                                         List<AccountPayload<?>> makerPayloadsForOfferOptions) {
        String offerId = "offer";
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<AccountOption> accountOptions = makerPayloadsForOfferOptions.stream()
                .map(payload -> new AccountOption(
                        paymentMethod,
                        "0123456789abcdef0123456789abcdef01234567",
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(),
                        OfferOptionUtil.createSaltedAccountPayloadHash(payload, offerId)
                ))
                .toList();
        MuSigOffer offer = new MuSigOffer(
                offerId,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                accountOptions,
                "1.0.0"
        );
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        byte[] takerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(takerPayloadForHash, offerId);
        return new MuSigContract(
                System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                100_000L,
                3_500_000L,
                quoteSidePaymentMethodSpec,
                takerSaltedAccountPayloadHash,
                Optional.empty(),
                Optional.empty(),
                new MarketPriceSpec(),
                0
        );
    }

    private UserProfile createUserProfile(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private AccountPayload<?> createNationalBankPayload(String id, String accountNr) {
        return new NationalBankAccountPayload(
                id,
                "DE",
                "EUR",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                accountNr,
                Optional.empty(),
                Optional.empty()
        );
    }

    private UserProfile createMaker() {
        return createUserProfile(10_001);
    }

    private UserProfile createTaker() {
        return createUserProfile(10_002);
    }

    private AccountPayload<?> createTakerPayload() {
        return createNationalBankPayload("taker-account", "DE111");
    }

    private AccountPayload<?> createMakerPayload() {
        return createNationalBankPayload("maker-account", "DE222");
    }

    private AccountPayload<?> createWrongTakerPayload() {
        return createNationalBankPayload("wrong-taker-account", "DE333");
    }

    private AccountPayload<?> createWrongMakerPayload() {
        return createNationalBankPayload("wrong-maker-account", "DE444");
    }
}
