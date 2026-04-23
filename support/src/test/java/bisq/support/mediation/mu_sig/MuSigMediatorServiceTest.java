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

package bisq.support.mediation.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.support.mediation.MuSigDisputeCaseDataMessage;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuSigMediatorServiceTest {
    private BannedUserService bannedUserService;

    @BeforeAll
    static void setupRes() {
        Res.setAndApplyLanguageTag("en");
    }

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<MuSigMediatorStore> persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(PersistenceClient.class), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("build/test/musig-mediator-service"));

        ChatService chatService = mock(ChatService.class);
        MuSigOpenTradeChannelService openTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(openTradeChannelService);

        UserService userService = mock(UserService.class);
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        bannedUserService = mock(BannedUserService.class);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(bannedUserService.isUserProfileBanned(any(String.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);
    }

    @Test
    void authorizeMediationRequestMatchesAndPartiesAndMediatorAreConsistent() {
        UserProfile requester = createUserProfile(1001);
        UserProfile peer = createUserProfile(1002);
        UserProfile mediator = createUserProfile(1003);
        MuSigContract contract = createContract(requester, peer, mediator, "offer-10",
                createNationalBankPayload("taker-account-10", "DE101"),
                createNationalBankPayload("maker-account-10", "DE102"));
        MuSigMediationRequest request = new MuSigMediationRequest(
                "trade-10",
                contract,
                requester,
                peer,
                List.of(),
                mediator.getNetworkId()
        );

        Optional<UserProfile> authenticatedSender = MuSigMediatorService.authorizeMediationRequest(
                request,
                bannedUserService
        );

        assertThat(authenticatedSender).containsSame(requester);
    }

    @Test
    void authorizeDisputeCasePaymentDetailsResponse_returnsCase_whenKnownSenderIsNotBanned() {
        UserProfile requester = createUserProfile(1001);
        UserProfile peer = createUserProfile(1002);
        MuSigMediationRequest request = createMediationRequest("trade-12", requester, peer);
        MuSigMediationCase mediationCase = new MuSigMediationCase(request);
        MuSigDisputeCasePaymentDetailsResponse response = new MuSigDisputeCasePaymentDetailsResponse(
                "trade-12",
                requester,
                createNationalBankPayload("taker-account-12", "DE121"),
                createNationalBankPayload("maker-account-12", "DE122")
        );

        Optional<MuSigMediationCase> authenticatedCase = MuSigMediatorService.authorizeDisputeCasePaymentDetailsResponse(
                response,
                tradeId -> tradeId.equals(mediationCase.getMuSigMediationRequest().getTradeId()) ? Optional.of(mediationCase) : Optional.empty(),
                bannedUserService
        );

        assertThat(authenticatedCase).containsSame(mediationCase);
    }

    @Test
    void authorizeDisputeCasePaymentDetailsResponse_returnsEmpty_whenSenderUserProfileIsUnknown() {
        UserProfile requester = createUserProfile(1001);
        UserProfile peer = createUserProfile(1002);
        UserProfile stranger = createUserProfile(1003);
        MuSigMediationRequest request = createMediationRequest("trade-13", requester, peer);
        MuSigMediationCase mediationCase = new MuSigMediationCase(request);
        MuSigDisputeCasePaymentDetailsResponse response = new MuSigDisputeCasePaymentDetailsResponse(
                "trade-13",
                stranger,
                createNationalBankPayload("taker-account-13", "DE131"),
                createNationalBankPayload("maker-account-13", "DE132")
        );

        Optional<MuSigMediationCase> authenticatedCase = MuSigMediatorService.authorizeDisputeCasePaymentDetailsResponse(
                response,
                tradeId -> tradeId.equals(mediationCase.getMuSigMediationRequest().getTradeId()) ? Optional.of(mediationCase) : Optional.empty(),
                bannedUserService
        );

        assertThat(authenticatedCase).isEmpty();
    }

    @Test
    void authorizeDisputeCaseDataMessage_returnsCase_whenPeerIsNotBanned() {
        UserProfile requester = createUserProfile(1001);
        UserProfile peer = createUserProfile(1002);
        MuSigMediationRequest request = createMediationRequest("trade-14", requester, peer);
        MuSigMediationCase mediationCase = new MuSigMediationCase(request);
        MuSigDisputeCaseDataMessage message = new MuSigDisputeCaseDataMessage(
                "trade-14",
                peer,
                new byte[20],
                List.of()
        );

        Optional<MuSigMediationCase> authenticatedCase = MuSigMediatorService.authorizeDisputeCaseDataMessage(
                message,
                tradeId -> tradeId.equals(mediationCase.getMuSigMediationRequest().getTradeId()) ? Optional.of(mediationCase) : Optional.empty(),
                bannedUserService
        );

        assertThat(authenticatedCase).containsSame(mediationCase);
    }

    @Test
    void authorizeDisputeCaseDataMessage_returnsEmpty_whenPeerIsBanned() {
        UserProfile requester = createUserProfile(1001);
        UserProfile peer = createUserProfile(1002);
        when(bannedUserService.isUserProfileBanned(peer)).thenReturn(true);
        MuSigMediationRequest request = createMediationRequest("trade-15", requester, peer);
        MuSigMediationCase mediationCase = new MuSigMediationCase(request);
        MuSigDisputeCaseDataMessage message = new MuSigDisputeCaseDataMessage(
                "trade-15",
                peer,
                new byte[20],
                List.of()
        );

        Optional<MuSigMediationCase> authenticatedCase = MuSigMediatorService.authorizeDisputeCaseDataMessage(
                message,
                tradeId -> tradeId.equals(mediationCase.getMuSigMediationRequest().getTradeId()) ? Optional.of(mediationCase) : Optional.empty(),
                bannedUserService
        );

        assertThat(authenticatedCase).isEmpty();
    }

    private MuSigMediationRequest createMediationRequest(String tradeId,
                                                         UserProfile requester,
                                                         UserProfile peer) {
        UserProfile mediator = createUserProfile(19000);
        return new MuSigMediationRequest(
                tradeId,
                createContract(requester, peer, mediator, "offer-" + tradeId,
                        createNationalBankPayload("taker-" + tradeId, "DE" + tradeId.substring(tradeId.length() - 2) + "1"),
                        createNationalBankPayload("maker-" + tradeId, "DE" + tradeId.substring(tradeId.length() - 2) + "2")),
                requester,
                peer,
                List.of(),
                mediator.getNetworkId()
        );
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         UserProfile mediator,
                                         String offerId,
                                         AccountPayload<?> takerPayloadForHash,
                                         AccountPayload<?> makerPayloadForHash) {
        return createContractWithMediator(maker, taker, Optional.of(mediator), offerId, takerPayloadForHash, List.of(makerPayloadForHash));
    }

    private MuSigContract createContractWithMediator(UserProfile maker,
                                                     UserProfile taker,
                                                     Optional<UserProfile> mediator,
                                                     String offerId,
                                                     AccountPayload<?> takerPayloadForHash,
                                                     List<AccountPayload<?>> makerPayloadsForOfferOptions) {
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
                mediator,
                Optional.empty(),
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
        UserProfile userProfile = new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
        return userProfile;
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
