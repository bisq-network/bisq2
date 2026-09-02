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

package bisq.offer.mu_sig.use_case.take_offer;

import bisq.account.AccountService;
import bisq.account.payment_method.PaymentRail;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.TradeAmount;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.DraftOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.PaymentMethodSelectionService;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.DefaultAccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.DefaultTakeOfferDraftCookieStore;
import bisq.offer.mu_sig.use_case.dependencies.TakeOfferDraftCookieStore;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import bisq.offer.mu_sig.use_case.take_offer.direction.TakeOfferDirectionService;
import bisq.offer.mu_sig.use_case.take_offer.market.TakeOfferMarketService;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.TakeOfferPaymentMethodService;
import bisq.offer.mu_sig.use_case.take_offer.price.TakeOfferPriceService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO
@Slf4j
public class TakeOfferUseCase extends DraftOfferUseCase {
    public static final Fiat DEFAULT_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(500, "USD");
    @Getter
    private final TakeOfferMarketService marketService;
    @Getter
    private final TakeOfferDirectionService directionService;
    @Getter
    private final TakeOfferPriceService priceService;
    @Getter
    private final TakeOfferAmountService amountService;

    private final TakeOfferDraftCookieStore cookieStore;
    @Getter
    private final TakeOfferPaymentMethodService paymentMethodService;


    /* --------------------------------------------------------------------- */
    // Construction
    /* --------------------------------------------------------------------- */

    public TakeOfferUseCase(MarketPriceService marketPriceService,
                            SettingsService settingsService,
                            AccountService accountService) {
        this(marketPriceService,
                new DefaultTakeOfferDraftCookieStore(settingsService),
                new DefaultAccountsProvider(accountService));
    }

    TakeOfferUseCase(MarketPriceService marketPriceService,
                     TakeOfferDraftCookieStore cookieStore,
                     AccountsProvider accountsProvider) {
        marketService = new TakeOfferMarketService();
        directionService = new TakeOfferDirectionService();
        priceService = new TakeOfferPriceService();
        amountService = new TakeOfferAmountService();

        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        checkNotNull(accountsProvider, "accountsProvider must not be null");
        checkNotNull(marketPriceService, "marketPriceProvider must not be null");

        PaymentMethodSelectionService paymentMethodSelectionService = new PaymentMethodSelectionService(accountsProvider);


        paymentMethodService = new TakeOfferPaymentMethodService(paymentMethodSelectionService);
    }

    private void updatePaymentMethods() {
        paymentMethodService.updatePaymentMethods(getMarket());
    }

    private PaymentRail getSelectedPaymentRail() {
        return paymentMethodService.getSelectedPaymentRail();
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void initialize() {
        throw new UnsupportedOperationException("Use initialize(MuSigOffer)");
    }

    public void initialize(MuSigOffer muSigOffer) {
    }

    /* --------------------------------------------------------------------- */
    // Amount input entry points
    /* --------------------------------------------------------------------- */

    public void setFixTradeAmountFromInputAmount(Monetary amount) {
    }

    public void setFixTradeAmountFromSliderValue(double sliderValue) {
    }


    /* --------------------------------------------------------------------- */
    // Amount conversion
    /* --------------------------------------------------------------------- */

    public Monetary toInputAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        return null;
    }

    public Monetary toPassiveAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        return null;
    }


    /* --------------------------------------------------------------------- */
    // Mutation API
    /* --------------------------------------------------------------------- */

    public void setUseBaseCurrencyForAmountInput(boolean value) {
    }

    public void setFixTradeAmount(TradeAmount tradeAmount) {
    }


    /* --------------------------------------------------------------------- */
    // Derived read model
    /* --------------------------------------------------------------------- */

    public AmountSpec getAmountSpec() {
        return amountService.getAmountSpec();
    }

    @Override
    public Market getMarket() {
        return marketService.getMarket();
    }

}
