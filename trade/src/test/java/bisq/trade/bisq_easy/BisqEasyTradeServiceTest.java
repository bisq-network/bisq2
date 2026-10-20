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
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTradeServiceTest {

    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final PriceQuote PRICE_60K = PriceQuote.fromFiatPrice(60_000, "USD");
    private static Object previousUserProfileServiceInstance;

    private Persistence<?> persistence;
    private BisqEasyTradeService service;
    private NetworkId makerNid;
    private NetworkId takerNid;
    private Identity makerIdentity;
    private BisqEasyOffer offer;

    @BeforeAll
    static void stub_user_profile_service() throws Exception {
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
        when(settingsService.getNumDaysAfterRedactingTradeData()).thenReturn(new Observable<>(90));

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

        ChatService chatService = mock(ChatService.class);
        when(sp.getChatService()).thenReturn(chatService);
        BisqEasyOpenTradeChannelService openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        when(openTradeChannelService.getChannels()).thenReturn(new ObservableSet<>());
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);

        PersistenceService persistenceService = mock(PersistenceService.class);
        when(sp.getPersistenceService()).thenReturn(persistenceService);
        persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn((Persistence) persistence);

        service = new BisqEasyTradeService(sp, AppType.DESKTOP);

        makerNid = createNetworkId(1);
        takerNid = createNetworkId(2);
        makerIdentity = createIdentity("maker", makerNid);
        offer = createOffer(makerNid);
    }

    @AfterEach
    void tear_down() {
        service.shutdown().join();
    }

    @Test
    @DisplayName("findOpenTrade returns empty for non-existent trade ID")
    void find_open_trade_returns_empty_for_nonexistent_id() {
        assertThat(service.findOpenTrade("nonexistent-trade-id")).isEmpty();
    }

    @Test
    @DisplayName("wasTradeIdUsed returns false for non-existent trade ID")
    void was_trade_id_used_returns_false_for_nonexistent_id() {
        assertThat(service.wasTradeIdUsed("nonexistent-trade-id")).isFalse();
    }

    @Test
    @DisplayName("wasTradeIdUsed covers a closed and deleted trade, findOpenTrade does not")
    void was_trade_id_used_covers_closed_and_deleted_trade() {
        BisqEasyTrade trade = addTradeDirectly();
        String tradeId = trade.getId();

        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));
        assertThat(service.findOpenTrade(tradeId)).isEmpty();
        assertThat(service.wasTradeIdUsed(tradeId)).isTrue();

        service.deleteTrade(trade);
        assertThat(service.getClosedTrades()).isEmpty();
        assertThat(service.wasTradeIdUsed(tradeId)).isTrue();
    }

    @Test
    @DisplayName("closeTrade moves trade from open to closed set")
    void close_trade_moves_to_closed() {
        BisqEasyTrade trade = addTradeDirectly();

        UserProfile myProfile = createUserProfile(10);
        UserProfile peerProfile = createUserProfile(20);
        service.closeTrade(trade, myProfile, peerProfile);

        assertThat(service.findOpenTrade(trade.getId())).isEmpty();
        assertThat(service.getClosedTrades())
                .anyMatch(ct -> ct.trade().getId().equals(trade.getId()));
    }

    @Test
    @DisplayName("closeTrade removes protocol from active protocols")
    void close_trade_removes_protocol() {
        BisqEasyTrade trade = addTradeDirectly();
        String tradeId = trade.getId();
        // The service creates the protocols of the stored trades
        service.initialize().join();
        assertThat(service.findProtocol(tradeId)).isPresent();

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

    // Before, the hash code of a trade included its protocol version and its state. The creation of the protocol and
    // each change of the trade changed it, so the removal from the open trades failed without an error.
    @Test
    @DisplayName("closeTrade removes a trade which changed after it was added")
    void close_trade_removes_a_trade_which_changed() {
        BisqEasyTrade trade = addTradeDirectly();
        // The creation of the protocol sets the protocol version of the trade
        service.initialize().join();
        trade.setPaymentAccountData(Optional.of("account data"));

        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));

        assertThat(service.findOpenTrade(trade.getId())).isEmpty();
        assertThat(service.getClosedTrades()).anyMatch(ct -> ct.trade().getId().equals(trade.getId()));
        assertThat(service.wasOfferAlreadyTaken(offer, takerNid)).isFalse();
    }

    @Test
    @DisplayName("closeTrade tells the observers of the open trades that the trade is removed")
    void close_trade_notifies_the_removal() {
        BisqEasyTrade trade = addTradeDirectly();
        service.initialize().join();
        List<Object> removed = new ArrayList<>();
        service.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(BisqEasyTrade element) {
            }

            @Override
            public void onRemoved(Object element) {
                removed.add(element);
            }

            @Override
            public void onCleared() {
            }
        });

        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));

        assertThat(removed).containsExactly(trade);
    }

    @Test
    @DisplayName("deleteTrade removes a closed trade which changed after it was closed")
    void delete_trade_removes_a_closed_trade_which_changed() {
        BisqEasyTrade trade = addTradeDirectly();
        service.initialize().join();
        service.closeTrade(trade, createUserProfile(10), createUserProfile(20));
        // The redaction of trade data changes a closed trade
        trade.setPaymentAccountData(Optional.of("redacted"));

        service.deleteTrade(trade);

        assertThat(service.getClosedTrades()).noneMatch(ct -> ct.trade().getId().equals(trade.getId()));
    }

    // Earlier versions kept every closed trade also as an open trade, so their stores contain such copies
    @Test
    @DisplayName("A stored open trade which is also a closed trade is dropped at the start and gets no protocol")
    void open_copy_of_a_closed_trade_is_dropped_at_the_start() {
        BisqEasyContract contract = createContract(takerNid);
        BisqEasyTrade openCopy = createTrade(contract);
        BisqEasyTrade closedCopy = createTrade(contract);
        BisqEasyTrade openTrade = createTrade(createContract(createNetworkId(3)));
        BisqEasyTradeStore stored = new BisqEasyTradeStore(Set.of(openCopy, openTrade),
                Set.of(openCopy.getId(), openTrade.getId()),
                Set.of(new BisqEasyClosedTrade(closedCopy, createUserProfile(10), createUserProfile(20))));
        doReturn(Optional.of(stored)).when(persistence).read();

        service.readPersisted();
        service.initialize().join();

        assertThat(service.getTrades()).extracting(BisqEasyTrade::getId).containsExactly(openTrade.getId());
        assertThat(service.getClosedTrades()).hasSize(1);
        assertThat(service.wasTradeIdUsed(openCopy.getId())).isTrue();
        assertThat(service.findProtocol(openCopy.getId())).isEmpty();
        assertThat(service.findProtocol(openTrade.getId())).isPresent();
    }

    @Test
    @DisplayName("deleteTrade keeps a trade which is not closed")
    void delete_trade_keeps_open_trade() {
        BisqEasyTrade trade = addTradeDirectly();

        service.deleteTrade(trade);

        assertThat(service.getTrades()).contains(trade);
    }

    @Test
    @DisplayName("getTrades returns observable containing added trades")
    void get_trades_returns_added_trades() {
        assertThat(service.getTrades()).isEmpty();

        BisqEasyTrade trade = addTradeDirectly();
        assertThat(service.getTrades()).contains(trade);
    }

    @Test
    @DisplayName("findOpenTrade returns correct trade by ID")
    void find_open_trade_returns_correct_trade() {
        BisqEasyTrade trade = addTradeDirectly();
        Optional<BisqEasyTrade> found = service.findOpenTrade(trade.getId());
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
     * Adds a trade directly to the persistable store, bypassing the
     * full protocol creation flow (which requires deep network infrastructure).
     */
    private BisqEasyTrade addTradeDirectly() {
        BisqEasyTrade trade = createTrade(createContract(takerNid));

        service.getPersistableStore().addTrade(trade);
        return trade;
    }

    private BisqEasyContract createContract(NetworkId takerNid) {
        return new BisqEasyContract(
                System.currentTimeMillis(), offer, takerNid,
                100_000L, 600_000L,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.empty(), new FixPriceSpec(PRICE_60K), PRICE_60K.getValue());
    }

    private BisqEasyTrade createTrade(BisqEasyContract contract) {
        return new BisqEasyTrade(
                contract, false, false, makerIdentity, offer, contract.getTaker().getNetworkId(), makerNid);
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
