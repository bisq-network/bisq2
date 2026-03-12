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
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
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
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
    private MuSigMediatorService service;

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

        NetworkService networkService = mock(NetworkService.class);

        ChatService chatService = mock(ChatService.class);
        MuSigOpenTradeChannelService openTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(openTradeChannelService);

        UserService userService = mock(UserService.class);
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(bannedUserService.isUserProfileBanned(any(String.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);

        service = new MuSigMediatorService(
                persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService
        );
    }

    @Test
    void verifyPaymentDetails_returnsMatchesForValidTakerAndMakerPayloadsEvenWhenReportedByTaker() throws Exception {
        UserProfile maker = createUserProfile(10001);
        UserProfile taker = createUserProfile(10002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account", "DE111");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-account", "DE222");
        MuSigContract contract = createContract(maker, taker, "offer-1", takerPayload, makerPayload);
        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                "trade-1",
                takerPayload,
                makerPayload,
                taker.getId()
        );

        Object verification = invokeVerifyPaymentDetails(contract, response, Role.TAKER);

        assertThat(invokeVerificationBoolean(verification, "takerAccountPayloadMatches")).isTrue();
        assertThat(invokeVerificationBoolean(verification, "makerAccountPayloadMatches")).isTrue();
        assertThat(invokeVerificationIssues(verification)).isEmpty();
    }

    @Test
    void processPaymentDetailsResponse_storesBothPayloads_whenResponseHashesMatch() throws Exception {
        UserProfile maker = createUserProfile(11001);
        UserProfile taker = createUserProfile(11002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-2", "DE333");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-account-2", "DE444");
        MuSigContract contract = createContract(maker, taker, "offer-2", takerPayload, makerPayload);
        String tradeId = "trade-2";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                makerPayload,
                taker.getId()
        );

        invokeProcessPaymentDetailsResponse(response);

        assertThat(mediationCase.getTakerAccountPayload().get()).containsSame(takerPayload);
        assertThat(mediationCase.getMakerAccountPayload().get()).containsSame(makerPayload);
        assertThat(mediationCase.getIssues().get()).isEmpty();
    }

    @Test
    void processPaymentDetailsResponse_storesMatchingMakerPayloadAndAddsIssue_whenTakerHashMismatches() throws Exception {
        UserProfile maker = createUserProfile(12001);
        UserProfile taker = createUserProfile(12002);
        AccountPayload<?> expectedTakerPayload = createNationalBankPayload("expected-taker-account", "DE555");
        AccountPayload<?> wrongTakerPayload = createNationalBankPayload("wrong-taker-account", "DE666");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-account-3", "DE777");
        MuSigContract contract = createContract(maker, taker, "offer-3", expectedTakerPayload, makerPayload);
        String tradeId = "trade-3";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                tradeId,
                wrongTakerPayload,
                makerPayload,
                maker.getId()
        );

        invokeProcessPaymentDetailsResponse(response);

        assertThat(mediationCase.getTakerAccountPayload().get()).isEmpty();
        assertThat(mediationCase.getMakerAccountPayload().get()).containsSame(makerPayload);
        assertThat(mediationCase.getIssues().get()).hasSize(1);
        MuSigMediationIssue issue = mediationCase.getIssues().get().getFirst();
        assertThat(issue.getType()).isEqualTo(MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
        assertThat(issue.getCausingRole()).isEqualTo(Role.MAKER);
        assertThat(issue.getDetails()).isPresent();
        assertThat(issue.getDetails().orElseThrow()).isNotBlank();
    }

    @Test
    void processPaymentDetailsResponse_storesMatchingTakerPayloadAndAddsIssue_whenMakerHashMismatches() throws Exception {
        UserProfile maker = createUserProfile(13001);
        UserProfile taker = createUserProfile(13002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-4", "DE888");
        AccountPayload<?> expectedMakerPayload = createNationalBankPayload("expected-maker-account", "DE999");
        AccountPayload<?> wrongMakerPayload = createNationalBankPayload("wrong-maker-account", "DE000");
        MuSigContract contract = createContract(maker, taker, "offer-4", takerPayload, expectedMakerPayload);
        String tradeId = "trade-4";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                wrongMakerPayload,
                taker.getId()
        );

        invokeProcessPaymentDetailsResponse(response);

        assertThat(mediationCase.getTakerAccountPayload().get()).containsSame(takerPayload);
        assertThat(mediationCase.getMakerAccountPayload().get()).isEmpty();
        assertThat(mediationCase.getIssues().get()).hasSize(1);
        MuSigMediationIssue issue = mediationCase.getIssues().get().getFirst();
        assertThat(issue.getType()).isEqualTo(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
        assertThat(issue.getCausingRole()).isEqualTo(Role.TAKER);
        assertThat(issue.getDetails()).isPresent();
        assertThat(issue.getDetails().orElseThrow()).isNotBlank();
    }

    @Test
    void processPaymentDetailsResponse_addsMakerMismatchIssue_whenMatchingOnlyNonSelectedMakerAccountOption() throws Exception {
        UserProfile maker = createUserProfile(14001);
        UserProfile taker = createUserProfile(14002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-5", "DE123");
        AccountPayload<?> makerPayloadOption1 = createNationalBankPayload("maker-account-4a", "DE124");
        AccountPayload<?> makerPayloadOption2 = createNationalBankPayload("maker-account-4b", "DE125");
        MuSigContract contract = createContract(
                maker,
                taker,
                "offer-5",
                takerPayload,
                List.of(makerPayloadOption1, makerPayloadOption2)
        );
        String tradeId = "trade-5";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                makerPayloadOption2,
                taker.getId()
        );

        invokeProcessPaymentDetailsResponse(response);

        assertThat(mediationCase.getTakerAccountPayload().get()).containsSame(takerPayload);
        assertThat(mediationCase.getMakerAccountPayload().get()).isEmpty();
        assertThat(mediationCase.getIssues().get()).hasSize(1);
        MuSigMediationIssue issue = mediationCase.getIssues().get().getFirst();
        assertThat(issue.getType()).isEqualTo(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
        assertThat(issue.getCausingRole()).isEqualTo(Role.TAKER);
    }

    @Test
    void processPaymentDetailsResponse_doesNotDuplicateIssue_whenSameMismatchReportedTwice() throws Exception {
        UserProfile maker = createUserProfile(15001);
        UserProfile taker = createUserProfile(15002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-6", "DE901");
        AccountPayload<?> expectedMakerPayload = createNationalBankPayload("expected-maker-account-2", "DE902");
        AccountPayload<?> wrongMakerPayload = createNationalBankPayload("wrong-maker-account-2", "DE903");
        MuSigContract contract = createContract(maker, taker, "offer-6", takerPayload, expectedMakerPayload);
        String tradeId = "trade-6";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                wrongMakerPayload,
                taker.getId()
        );

        invokeProcessPaymentDetailsResponse(response);
        invokeProcessPaymentDetailsResponse(response);

        assertThat(mediationCase.getIssues().get()).hasSize(1);
        MuSigMediationIssue issue = mediationCase.getIssues().get().getFirst();
        assertThat(issue.getType()).isEqualTo(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
        assertThat(issue.getCausingRole()).isEqualTo(Role.TAKER);
        assertThat(issue.getDetails()).isPresent();
        assertThat(issue.getDetails().orElseThrow()).isNotBlank();
    }

    @Test
    void processPaymentDetailsResponse_keepsSeparateIssues_whenSameMismatchReportedByDifferentRoles() throws Exception {
        UserProfile maker = createUserProfile(16001);
        UserProfile taker = createUserProfile(16002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-7", "DE911");
        AccountPayload<?> expectedMakerPayload = createNationalBankPayload("expected-maker-account-3", "DE912");
        AccountPayload<?> wrongMakerPayload = createNationalBankPayload("wrong-maker-account-3", "DE913");
        MuSigContract contract = createContract(maker, taker, "offer-7", takerPayload, expectedMakerPayload);
        String tradeId = "trade-7";
        MuSigMediationCase mediationCase = createMediationCase(tradeId, contract, maker, taker);
        service.getMediationCases().add(mediationCase);

        MuSigPaymentDetailsResponse takerResponse = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                wrongMakerPayload,
                taker.getId()
        );
        MuSigPaymentDetailsResponse makerResponse = new MuSigPaymentDetailsResponse(
                tradeId,
                takerPayload,
                wrongMakerPayload,
                maker.getId()
        );

        invokeProcessPaymentDetailsResponse(takerResponse);
        invokeProcessPaymentDetailsResponse(makerResponse);

        assertThat(mediationCase.getIssues().get()).hasSize(2);
        assertThat(mediationCase.getIssues().get())
                .extracting(MuSigMediationIssue::getType, MuSigMediationIssue::getCausingRole)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH, Role.TAKER),
                        org.assertj.core.groups.Tuple.tuple(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH, Role.MAKER)
                );
        assertThat(mediationCase.getIssues().get())
                .allMatch(issue -> issue.getDetails().isPresent() && !issue.getDetails().orElseThrow().isBlank());
    }

    @Test
    void verifyPaymentDetails_returnsTakerMismatchIssue_whenExpectedTakerHashIsMissing() throws Exception {
        UserProfile maker = createUserProfile(17001);
        UserProfile taker = createUserProfile(17002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-8", "DE921");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-account-8", "DE922");
        MuSigContract contract = createContractWithoutTakerHash(maker, taker, "offer-8", makerPayload);
        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                "trade-8",
                takerPayload,
                makerPayload,
                taker.getId()
        );

        Object verification = invokeVerifyPaymentDetails(contract, response, Role.TAKER);

        assertThat(invokeVerificationBoolean(verification, "takerAccountPayloadMatches")).isFalse();
        assertThat(invokeVerificationBoolean(verification, "makerAccountPayloadMatches")).isTrue();
        assertThat(invokeVerificationIssues(verification))
                .extracting(MuSigMediationIssue::getType)
                .containsExactly(MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
    }

    @Test
    void verifyPaymentDetails_returnsMakerMismatchIssue_whenExpectedMakerHashIsMissing() throws Exception {
        UserProfile maker = createUserProfile(18001);
        UserProfile taker = createUserProfile(18002);
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-account-9", "DE931");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-account-9", "DE932");
        MuSigContract contract = createContractWithoutMakerHash(maker, taker, "offer-9", takerPayload);
        MuSigPaymentDetailsResponse response = new MuSigPaymentDetailsResponse(
                "trade-9",
                takerPayload,
                makerPayload,
                maker.getId()
        );

        Object verification = invokeVerifyPaymentDetails(contract, response, Role.MAKER);

        assertThat(invokeVerificationBoolean(verification, "takerAccountPayloadMatches")).isTrue();
        assertThat(invokeVerificationBoolean(verification, "makerAccountPayloadMatches")).isFalse();
        assertThat(invokeVerificationIssues(verification))
                .extracting(MuSigMediationIssue::getType)
                .containsExactly(MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
    }

    private MuSigMediationCase createMediationCase(String tradeId,
                                                   MuSigContract contract,
                                                   UserProfile requester,
                                                   UserProfile peer) {
        MuSigMediationRequest request = new MuSigMediationRequest(
                tradeId,
                contract,
                requester,
                peer,
                List.of(),
                Optional.empty()
        );
        return new MuSigMediationCase(request);
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         String offerId,
                                         AccountPayload<?> takerPayloadForHash,
                                         AccountPayload<?> makerPayloadForHash) {
        return createContract(maker, taker, offerId, takerPayloadForHash, List.of(makerPayloadForHash));
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         String offerId,
                                         AccountPayload<?> takerPayloadForHash,
                                         List<AccountPayload<?>> makerPayloadsForHash) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<AccountOption> accountOptions = makerPayloadsForHash.stream()
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
                createPriceSpec(),
                0
        );
    }

    private MuSigContract createContractWithoutTakerHash(UserProfile maker,
                                                         UserProfile taker,
                                                         String offerId,
                                                         AccountPayload<?> makerPayloadForHash) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        AccountOption accountOption = new AccountOption(
                paymentMethod,
                "0123456789abcdef0123456789abcdef01234567",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                OfferOptionUtil.createSaltedAccountPayloadHash(makerPayloadForHash, offerId)
        );
        MuSigOffer offer = new MuSigOffer(
                offerId,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                List.of(accountOption),
                "1.0.0"
        );
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        byte[] makerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(makerPayloadForHash, offerId);
        return new MuSigContract(
                System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, maker.getNetworkId(), Optional.of(makerSaltedAccountPayloadHash)),
                new Party(Role.TAKER, taker.getNetworkId()),
                100_000L,
                3_500_000L,
                PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0),
                quoteSidePaymentMethodSpec,
                Optional.empty(),
                createPriceSpec(),
                0
        );
    }

    private MuSigContract createContractWithoutMakerHash(UserProfile maker,
                                                         UserProfile taker,
                                                         String offerId,
                                                         AccountPayload<?> takerPayloadForHash) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        MuSigOffer offer = new MuSigOffer(
                offerId,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                List.of(),
                "1.0.0"
        );
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        byte[] takerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(takerPayloadForHash, offerId);
        return new MuSigContract(
                System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, maker.getNetworkId()),
                new Party(Role.TAKER, taker.getNetworkId(), Optional.of(takerSaltedAccountPayloadHash)),
                100_000L,
                3_500_000L,
                PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0),
                quoteSidePaymentMethodSpec,
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

    private void invokeProcessPaymentDetailsResponse(MuSigPaymentDetailsResponse response) throws Exception {
        Method method = MuSigMediatorService.class.getDeclaredMethod("processPaymentDetailsResponse", MuSigPaymentDetailsResponse.class);
        method.setAccessible(true);
        method.invoke(service, response);
    }

    private Object invokeVerifyPaymentDetails(MuSigContract contract,
                                              MuSigPaymentDetailsResponse response,
                                              Role causingRole) throws Exception {
        Method method = MuSigMediatorService.class.getDeclaredMethod("verifyPaymentDetails",
                MuSigContract.class,
                MuSigPaymentDetailsResponse.class,
                Role.class);
        method.setAccessible(true);
        return method.invoke(service, contract, response, causingRole);
    }

    @SuppressWarnings("unchecked")
    private List<MuSigMediationIssue> invokeVerificationIssues(Object verification) throws Exception {
        Method issues = verification.getClass().getDeclaredMethod("issues");
        issues.setAccessible(true);
        return (List<MuSigMediationIssue>) issues.invoke(verification);
    }

    private boolean invokeVerificationBoolean(Object verification, String accessorName) throws Exception {
        Method accessor = verification.getClass().getDeclaredMethod(accessorName);
        accessor.setAccessible(true);
        return (boolean) accessor.invoke(verification);
    }
}
