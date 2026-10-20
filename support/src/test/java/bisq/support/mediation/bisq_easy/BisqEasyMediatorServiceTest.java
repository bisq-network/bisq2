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

package bisq.support.mediation.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.TradeId;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
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
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BisqEasyMediatorServiceTest {

    private BisqEasyMediatorService service;
    private BisqEasyOpenTradeChannelService openTradeChannelService;
    private NetworkService networkService;
    private BannedUserService bannedUserService;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private UserIdentityService userIdentityService;
    private UserProfile mediatorProfile;
    private BisqEasyOpenTradeChannel channel;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<MediatorStore> persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(PersistenceClient.class), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("build/test/bisq-easy-mediator-service"));

        networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());

        ChatService chatService = mock(ChatService.class);
        openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);

        UserService userService = mock(UserService.class);
        userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        bannedUserService = mock(BannedUserService.class);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);

        // This node is the mediator
        UserIdentity mediatorIdentity = TestUserProfiles.createUserIdentity("mediator");
        mediatorProfile = mediatorIdentity.getUserProfile();
        when(userIdentityService.findUserIdentity(mediatorIdentity.getId())).thenReturn(Optional.of(mediatorIdentity));
        AuthorizedBondedRole mediatorRole = mock(AuthorizedBondedRole.class);
        when(mediatorRole.getBondedRoleType()).thenReturn(BondedRoleType.MEDIATOR);
        when(mediatorRole.getProfileId()).thenReturn(mediatorIdentity.getId());
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)).thenAnswer(invocation -> Stream.of(mediatorRole));

        channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getMyUserIdentity()).thenReturn(mediatorIdentity);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>());
        when(openTradeChannelService.mediatorFindOrCreatesChannel(any(), any(), any(), any(), any())).thenReturn(channel);

        service = new BisqEasyMediatorService(persistenceService, networkService, chatService, userService, bondedRolesService);
    }

    // The control case for the tests of ignored requests
    @Test
    @DisplayName("Opens a mediation case for a valid request when this node is the mediator")
    void opens_mediation_case_for_valid_request() {
        UserProfile requester = createUserProfile(4001);
        UserProfile peer = createUserProfile(4002);

        BisqEasyContract contract = createContract(requester, peer, mediatorProfile);
        BisqEasyMediationRequest request = new BisqEasyMediationRequest(
                TradeId.fromContract(contract),
                contract,
                requester,
                peer,
                List.of(),
                mediatorProfile.getNetworkId()
        );

        service.onMessage(request);

        verify(openTradeChannelService).mediatorFindOrCreatesChannel(any(), any(), any(), any(), any());
        assertThat(service.getMediationCases()).hasSize(1);
        // The first request of a trade puts the channel in mediation and tells both traders that the mediator joined
        verify(openTradeChannelService).setIsInMediation(channel, true);
        verify(networkService).confidentialSend(isA(BisqEasyMediatorsResponse.class), eq(requester.getNetworkId()), any());
        verify(networkService).confidentialSend(isA(BisqEasyMediatorsResponse.class), eq(peer.getNetworkId()), any());
        verify(openTradeChannelService, times(2)).addMediatorsResponseMessage(eq(channel), any());
    }

    @Test
    @DisplayName("Ignores mediation request from banned user")
    void ignores_mediation_request_from_banned_user() {
        UserProfile requester = createUserProfile(5001);
        UserProfile peer = createUserProfile(5002);

        when(bannedUserService.isUserProfileBanned(requester)).thenReturn(true);

        BisqEasyContract contract = createContract(requester, peer, mediatorProfile);
        BisqEasyMediationRequest request = new BisqEasyMediationRequest(
                TradeId.fromContract(contract),
                contract,
                requester,
                peer,
                List.of(),
                mediatorProfile.getNetworkId()
        );

        service.onMessage(request);

        verify(openTradeChannelService, never()).mediatorFindOrCreatesChannel(any(), any(), any(), any(), any());
        assertThat(service.getMediationCases()).isEmpty();
    }

    @Test
    @DisplayName("Ignores mediation request when this node is not the mediator")
    void ignores_mediation_request_when_this_node_is_not_the_mediator() {
        UserProfile requester = createUserProfile(6001);
        UserProfile peer = createUserProfile(6002);

        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true))
                .thenAnswer(invocation -> Stream.empty());

        BisqEasyContract contract = createContract(requester, peer, mediatorProfile);
        BisqEasyMediationRequest request = new BisqEasyMediationRequest(
                TradeId.fromContract(contract),
                contract,
                requester,
                peer,
                List.of(),
                mediatorProfile.getNetworkId()
        );

        service.onMessage(request);

        verify(openTradeChannelService, never()).mediatorFindOrCreatesChannel(any(), any(), any(), any(), any());
        assertThat(service.getMediationCases()).isEmpty();
    }

    @Test
    @DisplayName("Non-mediation messages are ignored silently")
    void non_mediation_messages_are_ignored_silently() {
        service.onMessage(mock(bisq.network.p2p.message.EnvelopePayloadMessage.class));

        verify(openTradeChannelService, never()).mediatorFindOrCreatesChannel(any(), any(), any(), any(), any());
        assertThat(service.getMediationCases()).isEmpty();
    }

    private BisqEasyContract createContract(UserProfile maker, UserProfile taker, UserProfile mediator) {
        BisqEasyOffer offer = createOffer(maker.getNetworkId());
        return new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                // 0.001 BTC for 60 EUR at a market price of 60 000 EUR
                100_000L,
                600_000L,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediator),
                new MarketPriceSpec(),
                600_000_000L
        );
    }

    private BisqEasyOffer createOffer(NetworkId makerNetworkId) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        return new BisqEasyOffer(
                "offer-test-med",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
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
}
