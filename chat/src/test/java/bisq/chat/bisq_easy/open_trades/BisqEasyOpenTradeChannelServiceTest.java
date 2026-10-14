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

package bisq.chat.bisq_easy.open_trades;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyOpenTradeChannelServiceTest {

    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    private BisqEasyOpenTradeChannelService service;
    private UserProfileService userProfileService;
    private BannedUserService bannedUserService;
    private UserIdentityService userIdentityService;
    private NetworkService networkService;

    private NetworkId makerNetworkId;
    private UserProfile makerProfile;
    private BisqEasyOffer offer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void set_up() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence<BisqEasyOpenTradeChannelStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), any(), any(BisqEasyOpenTradeChannelStore.class)))
                .thenReturn(persistence);

        networkService = mock(NetworkService.class);

        UserService userService = mock(UserService.class);
        userProfileService = mock(UserProfileService.class);
        bannedUserService = mock(BannedUserService.class);
        userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);

        service = new BisqEasyOpenTradeChannelService(persistenceService, networkService, userService);

        makerNetworkId = createNetworkId(4001);
        makerProfile = createUserProfile(makerNetworkId);
        offer = createOffer(makerNetworkId);
    }

    @Test
    @DisplayName("sendTakeOfferMessage returns failed future when maker profile not found")
    void send_take_offer_message_fails_when_maker_not_found() {
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.empty());

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, result::get);
        assertTrue(ex.getCause().getMessage().contains("makerUserProfile not found"));
    }

    @Test
    @DisplayName("sendTakeOfferMessage returns failed future when maker is banned")
    void send_take_offer_message_fails_when_maker_is_banned() {
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(makerProfile)).thenReturn(true);

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, result::get);
        assertTrue(ex.getCause().getMessage().contains("Maker is banned"));
    }

    @Test
    @DisplayName("sendTakeOfferMessage returns failed future when own identity is banned")
    void send_take_offer_message_fails_when_self_is_banned() {
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(makerProfile)).thenReturn(false);

        NetworkId takerNetworkId = createNetworkId(4002);
        UserProfile takerProfile = createUserProfile(takerNetworkId);
        UserIdentity takerIdentity = createUserIdentity(takerProfile, takerNetworkId);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(takerIdentity);
        when(bannedUserService.isUserProfileBanned(takerProfile)).thenReturn(true);

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    @DisplayName("sendTakeOfferMessage creates channel and sends when not banned")
    void send_take_offer_message_creates_channel_and_sends() {
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(String.class))).thenReturn(false);
        when(bannedUserService.isRateLimitExceeding(any(String.class))).thenReturn(false);

        NetworkId takerNetworkId = createNetworkId(4002);
        UserProfile takerProfile = createUserProfile(takerNetworkId);
        UserIdentity takerIdentity = createUserIdentity(takerProfile, takerNetworkId);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(takerIdentity);

        SendMessageResult sendResult = new SendMessageResult();
        when(networkService.confidentialSend(any(), any(NetworkId.class), any())).thenReturn(CompletableFuture.completedFuture(sendResult));

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertFalse(result.isCompletedExceptionally());
        assertDoesNotThrow(() -> result.get());
    }

    private static NetworkId createNetworkId(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
    }

    private static UserProfile createUserProfile(NetworkId networkId) {
        PubKey pubKey = networkId.getPubKey();
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static UserIdentity createUserIdentity(UserProfile userProfile, NetworkId networkId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        var torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(userProfile.getId(), kp, torKeyPair, i2pKeyPair);
        Identity identity = new Identity(userProfile.getId(), networkId, keyBundle);
        return new UserIdentity(identity, userProfile);
    }

    private static BisqEasyOffer createOffer(NetworkId makerNid) {
        return new BisqEasyOffer(
                "offer-channel-test",
                System.currentTimeMillis(),
                makerNid,
                Direction.SELL,
                MARKET,
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
}
