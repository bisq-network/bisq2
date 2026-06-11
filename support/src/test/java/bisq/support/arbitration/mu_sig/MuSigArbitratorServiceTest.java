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

package bisq.support.arbitration.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.chat.ChatMessageType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuSigArbitratorServiceTest {
    private BannedUserService bannedUserService;

    @BeforeEach
    void setUp() {
        bannedUserService = mock(BannedUserService.class);
        when(bannedUserService.isUserProfileBanned(any(String.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(NetworkId.class))).thenReturn(false);
    }

    @Test
    void muSigArbitrationRequestProvidesArbitratorReceiverPublicKey() {
        UserProfile requester = createUserProfile(3051);
        UserProfile peer = createUserProfile(3052);
        UserProfile arbitrator = createUserProfile(3053);
        MuSigArbitrationRequest request = createArbitrationRequest(
                "trade-51",
                requester,
                peer,
                arbitrator,
                List.of());

        assertThat(request.getReceiverPublicKey().getEncoded())
                .isEqualTo(arbitrator.getPublicKey().getEncoded());
    }

    @Test
    void verifyArbitrationRequestAcceptsValidEmbeddedChatMessages() {
        UserProfile requester = createUserProfile(3011);
        UserProfile peer = createUserProfile(3012);
        UserProfile arbitrator = createUserProfile(3013);
        String tradeId = "trade-11";
        MuSigOpenTradeMessage chatMessage = createChatMessage(
                tradeId,
                MuSigOpenTradeChannel.createId(tradeId),
                requester,
                peer);
        MuSigArbitrationRequest request = createArbitrationRequest(
                tradeId,
                requester,
                peer,
                arbitrator,
                List.of(chatMessage));

        Optional<UserProfile> verifiedRequester = MuSigArbitratorService.verifyArbitrationRequest(
                request,
                bannedUserService
        );

        assertThat(verifiedRequester).containsSame(requester);
    }

    @Test
    void verifyArbitrationRequestRejectsEmbeddedChatMessageWithWrongTradeId() {
        UserProfile requester = createUserProfile(3021);
        UserProfile peer = createUserProfile(3022);
        UserProfile arbitrator = createUserProfile(3023);
        String tradeId = "trade-21";
        MuSigOpenTradeMessage chatMessage = createChatMessage(
                "trade-22",
                MuSigOpenTradeChannel.createId(tradeId),
                requester,
                peer);
        MuSigArbitrationRequest request = createArbitrationRequest(
                tradeId,
                requester,
                peer,
                arbitrator,
                List.of(chatMessage));

        Optional<UserProfile> verifiedRequester = MuSigArbitratorService.verifyArbitrationRequest(
                request,
                bannedUserService
        );

        assertThat(verifiedRequester).isEmpty();
    }

    @Test
    void verifyArbitrationRequestRejectsEmbeddedChatMessageWithWrongChannelId() {
        UserProfile requester = createUserProfile(3031);
        UserProfile peer = createUserProfile(3032);
        UserProfile arbitrator = createUserProfile(3033);
        String tradeId = "trade-31";
        MuSigOpenTradeMessage chatMessage = createChatMessage(
                tradeId,
                MuSigOpenTradeChannel.createId("trade-32"),
                requester,
                peer);
        MuSigArbitrationRequest request = createArbitrationRequest(
                tradeId,
                requester,
                peer,
                arbitrator,
                List.of(chatMessage));

        Optional<UserProfile> verifiedRequester = MuSigArbitratorService.verifyArbitrationRequest(
                request,
                bannedUserService
        );

        assertThat(verifiedRequester).isEmpty();
    }

    @Test
    void verifyArbitrationRequestRejectsEmbeddedChatMessageWithUnexpectedSender() {
        UserProfile requester = createUserProfile(3041);
        UserProfile peer = createUserProfile(3042);
        UserProfile arbitrator = createUserProfile(3043);
        UserProfile stranger = createUserProfile(3044);
        String tradeId = "trade-41";
        MuSigOpenTradeMessage chatMessage = createChatMessage(
                tradeId,
                MuSigOpenTradeChannel.createId(tradeId),
                stranger,
                peer);
        MuSigArbitrationRequest request = createArbitrationRequest(
                tradeId,
                requester,
                peer,
                arbitrator,
                List.of(chatMessage));

        Optional<UserProfile> verifiedRequester = MuSigArbitratorService.verifyArbitrationRequest(
                request,
                bannedUserService
        );

        assertThat(verifiedRequester).isEmpty();
    }

    @Test
    void verifyDisputeCasePaymentDetailsResponse_returnsCase_whenKnownSenderIsNotBanned() {
        UserProfile requester = createUserProfile(3061);
        UserProfile peer = createUserProfile(3062);
        UserProfile arbitrator = createUserProfile(3063);
        MuSigArbitrationRequest request = createArbitrationRequest("trade-61", requester, peer, arbitrator, List.of());
        MuSigArbitrationCase arbitrationCase = new MuSigArbitrationCase(request);
        MuSigDisputeCasePaymentDetailsResponse response = new MuSigDisputeCasePaymentDetailsResponse(
                "trade-61",
                requester.getNetworkId(),
                createNationalBankPayload("taker-account-61", "DE611"),
                createNationalBankPayload("maker-account-61", "DE612")
        );

        Optional<MuSigArbitrationCase> verifiedCase = MuSigArbitratorService.verifyDisputeCasePaymentDetailsResponse(
                response,
                tradeId -> tradeId.equals(arbitrationCase.getMuSigArbitrationRequest().getTradeId())
                        ? Optional.of(arbitrationCase)
                        : Optional.empty(),
                bannedUserService
        );

        assertThat(verifiedCase).containsSame(arbitrationCase);
    }

    @Test
    void verifyDisputeCasePaymentDetailsResponse_returnsEmpty_whenSenderNetworkIdIsUnknown() {
        UserProfile requester = createUserProfile(3071);
        UserProfile peer = createUserProfile(3072);
        UserProfile arbitrator = createUserProfile(3073);
        UserProfile stranger = createUserProfile(3074);
        MuSigArbitrationRequest request = createArbitrationRequest("trade-71", requester, peer, arbitrator, List.of());
        MuSigArbitrationCase arbitrationCase = new MuSigArbitrationCase(request);
        MuSigDisputeCasePaymentDetailsResponse response = new MuSigDisputeCasePaymentDetailsResponse(
                "trade-71",
                stranger.getNetworkId(),
                createNationalBankPayload("taker-account-71", "DE711"),
                createNationalBankPayload("maker-account-71", "DE712")
        );

        Optional<MuSigArbitrationCase> verifiedCase = MuSigArbitratorService.verifyDisputeCasePaymentDetailsResponse(
                response,
                tradeId -> tradeId.equals(arbitrationCase.getMuSigArbitrationRequest().getTradeId())
                        ? Optional.of(arbitrationCase)
                        : Optional.empty(),
                bannedUserService
        );

        assertThat(verifiedCase).isEmpty();
    }

    private MuSigArbitrationRequest createArbitrationRequest(String tradeId,
                                                             UserProfile requester,
                                                             UserProfile peer,
                                                             UserProfile arbitrator,
                                                             List<MuSigOpenTradeMessage> chatMessages) {
        MuSigContract contract = createContract(requester, peer, arbitrator, "offer-" + tradeId,
                createNationalBankPayload("taker-" + tradeId, "DE" + tradeId.substring(tradeId.length() - 2) + "1"),
                createNationalBankPayload("maker-" + tradeId, "DE" + tradeId.substring(tradeId.length() - 2) + "2"));
        return new MuSigArbitrationRequest(
                tradeId,
                contract,
                createMediationResult(contract),
                new byte[72],
                requester,
                peer,
                chatMessages,
                arbitrator.getNetworkId()
        );
    }

    private MuSigMediationResult createMediationResult(MuSigContract contract) {
        return new MuSigMediationResult(
                ContractService.getContractHash(contract),
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.NO_PAYOUT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private MuSigOpenTradeMessage createChatMessage(String tradeId,
                                                    String channelId,
                                                    UserProfile sender,
                                                    UserProfile receiver) {
        return new MuSigOpenTradeMessage(
                tradeId,
                "message-" + tradeId + "-" + sender.getId().substring(0, 6),
                channelId,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                "message",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                Set.of());
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         UserProfile arbitrator,
                                         String offerId,
                                         AccountPayload<?> takerPayloadForHash,
                                         AccountPayload<?> makerPayloadForHash) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<AccountOption> accountOptions = List.of(new AccountOption(
                paymentMethod,
                "0123456789abcdef0123456789abcdef01234567",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                OfferOptionUtil.createSaltedAccountPayloadHash(makerPayloadForHash, offerId)
        ));
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
                Optional.of(arbitrator),
                createPriceSpec(),
                0
        );
    }

    private PriceSpec createPriceSpec() {
        return new MarketPriceSpec();
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
}
