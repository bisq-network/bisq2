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
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.TradeId;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Anybody can send a mediator a mediation request whose contract names one profile as maker and taker, and which
 * names this profile as requester and peer. The mediator does not check the contract signatures. A mediation channel
 * needs two traders, and creating it with one profile twice threw an exception. Before StartupReplay processed each
 * replayed message on its own, this also ended the start of the support services.
 */
class BisqEasyMediationRequestPartiesTest {
    private BannedUserService bannedUserService;
    private BisqEasyOpenTradeChannelService openTradeChannelService;
    private BisqEasyMediatorService mediatorService;
    private UserIdentity mediatorIdentity;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("test"));
        PersistenceService persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);

        mediatorIdentity = createUserIdentity("mediator");
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(mediatorIdentity.getId())).thenReturn(Optional.of(mediatorIdentity));
        bannedUserService = mock(BannedUserService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(bannedUserService);

        NetworkService networkService = mock(NetworkService.class);
        openTradeChannelService = new BisqEasyOpenTradeChannelService(persistenceService, networkService, userService);
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);

        AuthorizedBondedRole mediatorRole = mock(AuthorizedBondedRole.class);
        when(mediatorRole.getBondedRoleType()).thenReturn(BondedRoleType.MEDIATOR);
        when(mediatorRole.getProfileId()).thenReturn(mediatorIdentity.getId());
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(anyBoolean()))
                .thenAnswer(invocation -> Stream.of(mediatorRole));
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);

        mediatorService = new BisqEasyMediatorService(persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService);
    }

    @Test
    void requestWhoseRequesterIsAlsoThePeerIsIgnoredWithoutException() {
        UserProfile trader = createUserIdentity("trader").getUserProfile();
        BisqEasyMediationRequest request = createRequest(trader, trader);

        assertDoesNotThrow(() -> mediatorService.onMessage(request));
        assertTrue(mediatorService.getMediationCases().isEmpty());
        assertTrue(openTradeChannelService.getChannels().isEmpty());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(request, bannedUserService, List.of()).isEmpty());
    }

    @Test
    void requestOfTwoPartiesCreatesACase() {
        UserProfile maker = createUserIdentity("maker").getUserProfile();
        UserProfile taker = createUserIdentity("taker").getUserProfile();
        BisqEasyMediationRequest request = createRequest(maker, taker);

        mediatorService.onMessage(request);

        assertTrue(mediatorService.getMediationCases().stream()
                .anyMatch(mediationCase -> mediationCase.getBisqEasyMediationRequest().equals(request)));
        assertTrue(openTradeChannelService.findChannelByTradeId(request.getTradeId()).isPresent());
    }

    private BisqEasyMediationRequest createRequest(UserProfile maker, UserProfile taker) {
        BisqEasyContract contract = new BisqEasyContract(System.currentTimeMillis(),
                createOffer(maker.getNetworkId()),
                taker.getNetworkId(),
                100_000,
                100_0000,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediatorIdentity.getUserProfile()),
                new MarketPriceSpec(),
                60_000_0000);
        return new BisqEasyMediationRequest(TradeId.fromContract(contract),
                contract,
                maker,
                taker,
                List.of(),
                mediatorIdentity.getUserProfile().getNetworkId());
    }

    private static BisqEasyOffer createOffer(NetworkId makerNetworkId) {
        return new BisqEasyOffer("offer-id",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(100_0000),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.13");
    }

    private static UserIdentity createUserIdentity(String nickName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), nickName));
        KeyBundle keyBundle = new KeyBundle(nickName, keyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair());
        UserProfile userProfile = new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
        return new UserIdentity(new Identity(nickName, networkId, keyBundle), userProfile);
    }
}
