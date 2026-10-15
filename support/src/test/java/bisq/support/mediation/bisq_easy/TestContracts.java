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
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.profile.UserProfile;

import java.util.List;
import java.util.Optional;

// Real offers and contracts, so that tests compare deserialized copies by value as the mediator does.
final class TestContracts {
    private static final long OFFER_DATE = 1_700_000_000_000L;
    private static final long TAKE_OFFER_DATE = 1_700_000_600_000L;
    private static final long QUOTE_AMOUNT = 100_0000;
    private static final long BASE_AMOUNT = 100_000;

    private TestContracts() {
    }

    static BisqEasyContract contract(UserProfile maker, UserProfile taker, UserProfile mediator) {
        return contract(maker, taker, mediator, List.of("en"));
    }

    // Contracts built with different language codes differ only in one field of the offer. They have the same trade ID.
    static BisqEasyContract contract(UserProfile maker,
                                     UserProfile taker,
                                     UserProfile mediator,
                                     List<String> supportedLanguageCodes) {
        return contract("offer-1", maker, taker, mediator, supportedLanguageCodes);
    }

    // The trade ID is derived from the offer ID, the taker and the take offer date, so the contract of another offer is
    // the contract of another trade.
    static BisqEasyContract contractOfOffer(String offerId,
                                            UserProfile maker,
                                            UserProfile taker,
                                            UserProfile mediator) {
        return contract(offerId, maker, taker, mediator, List.of("en"));
    }

    // The offer has the fixed price M1 of Bisq Connect, and the agreed price is M3, so that the contract also carries
    // a rounded value. From PriceQuoteVOFactory.fromPrice of bisq-mobile 7f9cddce7b: the base side id is BTC, not
    // BTC [crypto], and the value is rounded to 8 significant digits.
    static BisqEasyContract contractWithFixedPriceOfBisqConnect(UserProfile maker,
                                                                UserProfile taker,
                                                                UserProfile mediator) {
        return contract("offer-1",
                maker,
                taker,
                mediator,
                List.of("en"),
                mobileFixPriceSpec(643_211_200L, 643_211_200L),
                mobileFixPriceSpec(643_211_230L, 643_211_234L));
    }

    private static BisqEasyContract contract(String offerId,
                                             UserProfile maker,
                                             UserProfile taker,
                                             UserProfile mediator,
                                             List<String> supportedLanguageCodes) {
        return contract(offerId,
                maker,
                taker,
                mediator,
                supportedLanguageCodes,
                new MarketPriceSpec(),
                new MarketPriceSpec());
    }

    private static BisqEasyContract contract(String offerId,
                                             UserProfile maker,
                                             UserProfile taker,
                                             UserProfile mediator,
                                             List<String> supportedLanguageCodes,
                                             PriceSpec offerPriceSpec,
                                             PriceSpec agreedPriceSpec) {
        BisqEasyOffer offer = new BisqEasyOffer(offerId,
                OFFER_DATE,
                maker.getNetworkId(),
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(QUOTE_AMOUNT),
                offerPriceSpec,
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                supportedLanguageCodes,
                1,
                "1.0.0",
                "2.1.13");
        return new BisqEasyContract(TAKE_OFFER_DATE,
                offer,
                taker.getNetworkId(),
                BASE_AMOUNT,
                QUOTE_AMOUNT,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediator),
                agreedPriceSpec,
                60_000_0000);
    }

    private static FixPriceSpec mobileFixPriceSpec(long value, long quoteSideAmount) {
        return new FixPriceSpec(new PriceQuote(value,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat("USD", quoteSideAmount, "USD", 4, 2)));
    }

    // Each trader's request carries its own deserialized copy of the contract.
    static BisqEasyContract protoCopy(BisqEasyContract contract) {
        try {
            return BisqEasyContract.fromProto(contract.toProto(false));
        } catch (UnresolvableProtobufException e) {
            throw new RuntimeException(e);
        }
    }
}
