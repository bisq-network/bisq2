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
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelStore;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.BASE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A node of version 2.1.13 stores trades and trade channels whose offer has a fixed price of Bisq Connect. A node of
 * this version loads them, and keeps the bytes of their contracts, so the contract signatures stay valid (see
 * docs/specifications/offer/fixed-price.md).
 */
class StoredFixedPriceOfBisqConnectTest {
    // From PriceQuoteVOFactory.fromPrice of bisq-mobile 7f9cddce7b: the base side id is BTC, not BTC [crypto], and the
    // value is rounded to 8 significant digits. M1: 64,321.12 USD. M3: 64,321.1234 USD, value rounded.
    private static final FixPriceSpec MOBILE_M1 = mobileFixPriceSpec(643_211_200L, 643_211_200L);
    private static final FixPriceSpec MOBILE_M3 = mobileFixPriceSpec(643_211_230L, 643_211_234L);

    @Test
    void openAndClosedTradesWithAFixedPriceOfBisqConnectAreLoaded() throws Exception {
        Identity maker = identity("maker");
        Identity taker = identity("taker");
        BisqEasyContract openContract = contract("open-offer", maker, taker);
        BisqEasyContract closedContract = contract("closed-offer", maker, taker);
        BisqEasyTrade openTrade = buyerAsTakerTrade(openContract, taker);
        BisqEasyTrade closedTrade = sellerAsMakerTrade(closedContract, maker);
        BisqEasyTradeStore store = new BisqEasyTradeStore(Set.of(openTrade),
                Set.of(openTrade.getId(), closedTrade.getId()),
                Set.of(new BisqEasyClosedTrade(closedTrade,
                        userProfile(maker.getNetworkId()),
                        userProfile(taker.getNetworkId()))));

        BisqEasyTradeStore loaded = BisqEasyTradeStore.fromProto(
                bisq.trade.protobuf.BisqEasyTradeStore.parseFrom(store.toProto(false).toByteArray()));

        assertThat(loaded.getTrades()).hasSize(1);
        assertThat(loaded.getClosedTrades()).hasSize(1);
        assertThat(loaded.getTrades().iterator().next().getContract().serializeForHash())
                .isEqualTo(openContract.serializeForHash());
        assertThat(loaded.getClosedTrades().iterator().next().trade().getContract().serializeForHash())
                .isEqualTo(closedContract.serializeForHash());
    }

    @Test
    void tradeChannelWithAFixedPriceOfBisqConnectIsLoaded() throws Exception {
        Identity maker = identity("maker");
        Identity taker = identity("taker");
        BisqEasyContract contract = contract("offer", maker, taker);
        BisqEasyTrade trade = buyerAsTakerTrade(contract, taker);
        BisqEasyOpenTradeChannel channel = channel(trade,
                contract.getOffer(),
                userProfile(maker.getNetworkId()),
                Optional.empty());
        bisq.chat.protobuf.BisqEasyOpenTradeChannelStore storeProto =
                bisq.chat.protobuf.BisqEasyOpenTradeChannelStore.newBuilder()
                        .addChannels(channel.toProto(false))
                        .build();

        BisqEasyOpenTradeChannelStore loaded = BisqEasyOpenTradeChannelStore.fromProto(
                bisq.chat.protobuf.BisqEasyOpenTradeChannelStore.parseFrom(storeProto.toByteArray()));

        assertThat(loaded.toProto(false)).isEqualTo(storeProto);
    }

    // The offer has M1. The agreed price is M3, so that the stored contract also carries a rounded value.
    private static BisqEasyContract contract(String offerId, Identity maker, Identity taker) {
        BisqEasyOffer offer = offer(offerId, maker.getNetworkId(), QUOTE_AMOUNT, MOBILE_M1);
        return new BisqEasyContract(System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                BASE_AMOUNT,
                QUOTE_AMOUNT,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.empty(),
                MOBILE_M3,
                643_211_200L);
    }

    private static FixPriceSpec mobileFixPriceSpec(long value, long quoteSideAmount) {
        return new FixPriceSpec(new PriceQuote(value,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat("USD", quoteSideAmount, "USD", 4, 2)));
    }
}
