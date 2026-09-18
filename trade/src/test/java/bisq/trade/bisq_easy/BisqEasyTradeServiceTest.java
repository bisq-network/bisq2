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

package bisq.trade.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTradeServiceTest {

    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final PriceQuote PRICE_60K = PriceQuote.fromFiatPrice(60_000, "USD");
    private static Object previousUserProfileServiceInstance;

    private BisqEasyTradeService service;
    private NetworkId makerNid;
    private NetworkId takerNid;
    private Identity makerIdentity;
    private BisqEasyOffer offer;

    @BeforeAll
    static void init_i18n() throws Exception {
        Res.setAndApplyLanguageTag("en");

        UserProfileService ups = mock(UserProfileService.class);
        when(ups.evaluateUserName(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        Field f = UserProfileService.class.getDeclaredField("instance");
        f.setAccessible(true);
        previousUserProfileServiceInstance = f.get(null);
        f.set(null, ups);
    }

    @AfterAll
    static void restore_user_profile_service() throws Exception {
        Field f = UserProfileService.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, previousUserProfileServiceInstance);
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void set_up() {
        ServiceProvider sp = mock(ServiceProvider.class);

        NetworkService networkService = mock(NetworkService.class);
        when(sp.getNetworkService()).thenReturn(networkService);
        when(networkService.getConfidentialMessageServices()).thenReturn(java.util.Set.of());

        IdentityService identityService = mock(IdentityService.class);
        when(sp.getIdentityService()).thenReturn(identityService);

        SettingsService settingsService = mock(SettingsService.class);
        when(sp.getSettingsService()).thenReturn(settingsService);
        when(settingsService.getDoAutoAddToContactList()).thenReturn(false);

        UserService userService = mock(UserService.class);
        when(sp.getUserService()).thenReturn(userService);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        ContactListService contactListService = mock(ContactListService.class);
        when(userService.getContactListService()).thenReturn(contactListService);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(sp.getBondedRolesService()).thenReturn(bondedRolesService);
        AlertService alertService = mock(AlertService.class);
        when(bondedRolesService.getAlertService()).thenReturn(alertService);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(new ObservableSet<>());

        PersistenceService persistenceService = mock(PersistenceService.class);
        when(sp.getPersistenceService()).thenReturn(persistenceService);
        Persistence<?> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn((Persistence) persistence);

        service = new BisqEasyTradeService(sp, AppType.DESKTOP);

        makerNid = createNetworkId(1);
        takerNid = createNetworkId(2);
        makerIdentity = createIdentity("maker", makerNid);
        offer = createOffer(makerNid);
    }

    @Test
    @DisplayName("findTrade returns empty for non-existent trade ID")
    void find_trade_returns_empty_for_nonexistent_id() {
        assertThat(service.findTrade("nonexistent-trade-id")).isEmpty();
    }

    @Test
    @DisplayName("tradeExists returns false for non-existent trade ID")
    void trade_exists_returns_false_for_nonexistent_id() {
        assertThat(service.tradeExists("nonexistent-trade-id")).isFalse();
    }

    @Test
    @DisplayName("closeTrade moves trade from open to closed set")
    void close_trade_moves_to_closed() {
        BisqEasyTrade trade = addTradeDirectly();

        UserProfile myProfile = createUserProfile(10);
        UserProfile peerProfile = createUserProfile(20);
        service.closeTrade(trade, myProfile, peerProfile);

        assertThat(service.findTrade(trade.getId())).isEmpty();
        assertThat(service.getClosedTrades())
                .anyMatch(ct -> ct.trade().getId().equals(trade.getId()));
    }

    @Test
    @DisplayName("closeTrade removes protocol from active protocols")
    void close_trade_removes_protocol() {
        BisqEasyTrade trade = addTradeDirectly();
        String tradeId = trade.getId();

        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));

        assertThat(service.findProtocol(tradeId)).isEmpty();
    }

    @Test
    @DisplayName("deleteTrade removes from closed trades")
    void delete_trade_removes_from_closed() {
        BisqEasyTrade trade = addTradeDirectly();
        String tradeId = trade.getId();

        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));
        assertThat(service.getClosedTrades()).isNotEmpty();

        service.deleteTrade(trade);
        assertThat(service.getClosedTrades())
                .noneMatch(ct -> ct.trade().getId().equals(tradeId));
    }

    @Test
    @DisplayName("deleteTrade on non-closed trade does not throw")
    void delete_trade_on_non_closed_does_not_throw() {
        BisqEasyTrade trade = addTradeDirectly();
        assertDoesNotThrow(() -> service.deleteTrade(trade));
    }

    @Test
    @DisplayName("getTrades returns observable containing added trades")
    void get_trades_returns_added_trades() {
        assertThat(service.getTrades()).isEmpty();

        BisqEasyTrade trade = addTradeDirectly();
        assertThat(service.getTrades()).contains(trade);
    }

    @Test
    @DisplayName("findTrade returns correct trade by ID")
    void find_trade_returns_correct_trade() {
        BisqEasyTrade trade = addTradeDirectly();
        Optional<BisqEasyTrade> found = service.findTrade(trade.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(trade.getId());
    }

    @Test
    @DisplayName("wasOfferAlreadyTaken returns true for existing offer+taker combination")
    void was_offer_already_taken_returns_true() {
        addTradeDirectly();
        assertThat(service.wasOfferAlreadyTaken(offer, takerNid)).isTrue();
    }

    @Test
    @DisplayName("wasOfferAlreadyTaken returns false for unknown taker")
    void was_offer_already_taken_returns_false_for_unknown_taker() {
        addTradeDirectly();
        NetworkId unknownTaker = createNetworkId(999);
        assertThat(service.wasOfferAlreadyTaken(offer, unknownTaker)).isFalse();
    }

    /**
     * Adds a trade directly to the persistable store via reflection, bypassing the
     * full protocol creation flow (which requires deep network infrastructure).
     */
    private BisqEasyTrade addTradeDirectly() {
        BisqEasyContract contract = new BisqEasyContract(
                System.currentTimeMillis(), offer, takerNid,
                100_000L, 600_000L,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.empty(), new FixPriceSpec(PRICE_60K), PRICE_60K.getValue());
        BisqEasyTrade trade = new BisqEasyTrade(
                contract, false, false, makerIdentity, offer, takerNid, makerNid);

        try {
            Field storeField = BisqEasyTradeService.class.getDeclaredField("persistableStore");
            storeField.setAccessible(true);
            BisqEasyTradeStore store = (BisqEasyTradeStore) storeField.get(service);
            store.addTrade(trade);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return trade;
    }

    private BisqEasyOffer createOffer(NetworkId makerNid) {
        return new BisqEasyOffer(
                "offer-lifecycle-test",
                System.currentTimeMillis(),
                makerNid,
                Direction.SELL,
                MARKET,
                new BaseSideFixedAmountSpec(100_000L),
                new FixPriceSpec(PRICE_60K),
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

    private static NetworkId createNetworkId(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        return new NetworkId(addresses, pubKey);
    }

    private static Identity createIdentity(String tag, NetworkId networkId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        var torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(tag, kp, torKeyPair, i2pKeyPair);
        return new Identity(tag, networkId, keyBundle);
    }

    private static UserProfile createUserProfile(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        NetworkId nid = new NetworkId(addresses, pubKey);
        ProofOfWork pow = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, pow, 0, nid, "", "", "1.0.0");
    }
}
