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

package bisq.offer.mu_sig.use_case.create_offer;

import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateOfferUseCaseTest {
    private final Market usdMarket = MarketRepository.getUSDBitcoinMarket();
    private final ObservableHashMap<Market, MarketPrice> marketPriceMap = new ObservableHashMap<>();
    private final Map<Market, PriceQuote> quotes = new HashMap<>();

    @Test
    void draftIsNotReadyForReviewUntilTheMarketPriceArrives() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceMap);
        when(marketPriceService.findMarketPriceQuote(any())).thenAnswer(invocation ->
                Optional.ofNullable(quotes.get(invocation.getArgument(0, Market.class))));
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenAnswer(invocation -> {
            PriceQuote quote = quotes.get(invocation.getArgument(0, Market.class));
            if (quote == null) {
                throw new IllegalStateException("No market price available");
            }
            return quote;
        });

        CreateOfferUseCase useCase = new CreateOfferUseCase(marketPriceService,
                mock(CreateOfferDraftCookieStore.class),
                mock(AccountsProvider.class));
        useCase.initialize();
        useCase.getMarketSelection().onSetMarket(usdMarket);
        useCase.getDirectionSelection().onSetDisplayDirection(Direction.BUY);

        assertFalse(useCase.isDraftReadyForReview(),
                "an unpriced draft has no amounts or price quote and must not reach review");

        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));

        assertTrue(useCase.isDraftReadyForReview(),
                "the gate must lift on its own once the market price arrives");
        assertNotNull(useCase.captureDraftSnapshot().amountSpec(),
                "a ready draft must materialize into a complete snapshot");
    }

    @Test
    void handoffIsEmptyUntilTheDraftIsReadyForReview() {
        MarketPriceService marketPriceService = marketPriceService();
        CreateOfferUseCase useCase = new CreateOfferUseCase(marketPriceService,
                mock(CreateOfferDraftCookieStore.class),
                mock(AccountsProvider.class));
        useCase.initialize();
        useCase.getMarketSelection().onSetMarket(usdMarket);
        useCase.getDirectionSelection().onSetDisplayDirection(Direction.BUY);

        assertTrue(useCase.getHandoff().isEmpty(), "an unpriced draft cannot be handed off");

        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));

        assertTrue(useCase.getHandoff().isPresent());
    }

    @Test
    void handoffBuildsTheOfferPartsFromOneSnapshot() {
        MarketPriceService marketPriceService = marketPriceService();
        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));
        PaymentMethod<?> sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        Account<?, ?> account = sepaAccount("account-1", sepa);
        AccountsProvider accountsProvider = market -> List.of(account);
        CreateOfferUseCase useCase = new CreateOfferUseCase(marketPriceService,
                mock(CreateOfferDraftCookieStore.class),
                accountsProvider);
        useCase.initialize();
        useCase.getMarketSelection().onSetMarket(usdMarket);
        useCase.getDirectionSelection().onSetDisplayDirection(Direction.BUY);
        useCase.getPaymentMethodSelection().onAddAccountByPaymentMethodEntry(Map.entry(sepa, account));

        CreateOfferUseCase.Handoff handoff = useCase.getHandoff().orElseThrow();

        assertFalse(handoff.offerId().isBlank());
        assertEquals(Direction.BUY, handoff.direction(), "on a Bitcoin-Fiat market the offer direction is the display direction");
        assertEquals(usdMarket, handoff.market());
        assertSame(handoff.snapshot().amountSpec(), handoff.amountSpec());
        assertSame(handoff.snapshot().priceSpec(), handoff.priceSpec());
        assertEquals(List.of(sepa), handoff.paymentMethods());
        CollateralOption collateralOption = OfferOptionUtil.findCollateralOption(handoff.offerOptions()).orElseThrow();
        assertEquals(MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT, collateralOption.getBuyerSecurityDeposit());
        assertEquals(MuSigOffer.DEFAULT_SELLER_SECURITY_DEPOSIT, collateralOption.getSellerSecurityDeposit());
        assertEquals(1, handoff.offerOptions().stream().filter(CollateralOption.class::isInstance).count());
        List<AccountOption> accountOptions = handoff.offerOptions().stream()
                .filter(AccountOption.class::isInstance).map(AccountOption.class::cast).toList();
        assertEquals(1, accountOptions.size());
        assertEquals(OfferOptionUtil.createdSaltedAccountId("account-1", handoff.offerId()),
                accountOptions.get(0).getSaltedAccountId());
        assertEquals(sepa, accountOptions.get(0).getPaymentMethod());

        assertNotEquals(handoff.offerId(), useCase.getHandoff().orElseThrow().offerId(),
                "every handoff is a fresh offer with its own id and salts");
    }

    @Test
    void handoffMirrorsTheDisplayDirectionOnAltcoinMarkets() {
        Market xmrMarket = MarketRepository.getXmrBtcMarket();
        DraftSnapshot snapshot = new DraftSnapshot(xmrMarket, Direction.BUY,
                new BaseSideFixedAmountSpec(1_000_000), new MarketPriceSpec(),
                ImmutableMap.of(), Optional.empty(), Optional.empty(), 0);

        CreateOfferUseCase.Handoff handoff = CreateOfferUseCase.toHandoff(snapshot, "offer-1");

        assertEquals(Direction.SELL, handoff.direction(),
                "buying the altcoin means selling Bitcoin, the offer's base currency");
        assertEquals("offer-1", handoff.offerId());
        assertTrue(handoff.paymentMethods().isEmpty());
    }

    private MarketPriceService marketPriceService() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceMap);
        when(marketPriceService.findMarketPriceQuote(any())).thenAnswer(invocation ->
                Optional.ofNullable(quotes.get(invocation.getArgument(0, Market.class))));
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenAnswer(invocation -> {
            PriceQuote quote = quotes.get(invocation.getArgument(0, Market.class));
            if (quote == null) {
                throw new IllegalStateException("No market price available");
            }
            return quote;
        });
        return marketPriceService;
    }

    private static Account<?, ?> sepaAccount(String id, PaymentMethod<?> paymentMethod) {
        SepaAccountPayload payload = new SepaAccountPayload(id + "-payload", "Alice",
                "DE89370400440532013000", "DEUTDEFF", "DE", List.of("DE"));
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(id);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        when(account.getAccountPayload()).thenReturn(payload);
        return account;
    }
}
