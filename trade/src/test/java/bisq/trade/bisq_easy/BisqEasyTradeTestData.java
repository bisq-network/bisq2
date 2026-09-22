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
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real offers, contracts, trades and channels for tests of the trade chat channel binding.
 */
public final class BisqEasyTradeTestData {
    public static final long QUOTE_AMOUNT = 100_0000;
    public static final long BASE_AMOUNT = 100_000;

    private BisqEasyTradeTestData() {
    }

    public static NetworkId networkId(String keyId) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }

    public static Identity identity(String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
        return new Identity(keyId, networkId, new KeyBundle(keyId, keyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair()));
    }

    public static BisqEasyOffer offer(String id, NetworkId makerNetworkId, long quoteAmount) {
        return new BisqEasyOffer(id,
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(quoteAmount),
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

    public static BisqEasyContract contract(BisqEasyOffer offer, NetworkId takerNetworkId, Optional<UserProfile> mediator) {
        return new BisqEasyContract(System.currentTimeMillis(),
                offer,
                takerNetworkId,
                BASE_AMOUNT,
                QUOTE_AMOUNT,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                mediator,
                new MarketPriceSpec(),
                60_000_0000);
    }

    // The taker of a sell offer is the buyer.
    public static BisqEasyTrade buyerAsTakerTrade(BisqEasyContract contract, Identity takerIdentity) {
        return new BisqEasyTrade(contract,
                true,
                true,
                takerIdentity,
                contract.getOffer(),
                takerIdentity.getNetworkId(),
                contract.getMaker().getNetworkId());
    }

    // A trade in the given state, as it would be read from the store.
    public static BisqEasyTrade inState(BisqEasyTrade trade, BisqEasyTradeState state) {
        try {
            return BisqEasyTrade.fromProto(trade.toProto(false).toBuilder().setState(state.name()).build());
        } catch (UnresolvableProtobufException e) {
            throw new RuntimeException(e);
        }
    }

    public static UserProfile userProfile(NetworkId networkId) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getNetworkId()).thenReturn(networkId);
        when(userProfile.getId()).thenReturn(networkId.getId());
        return userProfile;
    }

    public static BisqEasyOpenTradeChannel channel(BisqEasyTrade trade, BisqEasyOffer offer, UserProfile peer, Optional<UserProfile> mediator) {
        UserIdentity myUserIdentity = mock(UserIdentity.class);
        when(myUserIdentity.getId()).thenReturn(trade.getMyIdentity().getNetworkId().getId());
        return BisqEasyOpenTradeChannel.create(trade.getId(), offer, myUserIdentity, Set.of(peer), mediator, false);
    }
}
