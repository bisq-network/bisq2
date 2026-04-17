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

package bisq.offer.mu_sig.draft;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecFactory;
import bisq.offer.amount.spec.BaseSideAmountSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class CreateOfferDraftWorkflow extends OfferDraftWorkflow<CreateOfferDraft> {
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    @Delegate
    protected CreateOfferDraft createOfferDraft;

    public CreateOfferDraftWorkflow(MarketPriceService marketPriceService, SettingsService settingsService) {
        super(new CreateOfferDraft());
        this.marketPriceService = marketPriceService;
        this.settingsService = settingsService;
        createOfferDraft = offerDraft;
    }

    @Override
    protected void initialize() {
        // Default
        Market market = MarketRepository.getDefaultBtcFiatMarket();
        setMarket(market);

        setDirection(Direction.BUY);
        Boolean useBuyDirection = settingsService.getCookie().asBoolean(CookieKey.MU_SIG_CREATE_OFFER_USE_BUY_DIRECTION)
                .orElse(false);
        setDirection(useBuyDirection ? Direction.BUY : Direction.SELL);

        if (market.isBtcFiatMarket()) {
            setUseBaseCurrencyForAmountInput(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(false));
        } else {
            setUseBaseCurrencyForAmountInput(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(true));
        }

        offerDraft.setUseRangeAmount(settingsService.getCookie().asBoolean(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED)
                .orElse(false));

        // Price
        PriceQuote priceQuote = marketPriceService.findMarketPriceQuote(market).orElseThrow();
        setPriceQuote(priceQuote);
        setPriceSpec(new MarketPriceSpec());

        // Amounts
        updateDefaultTradeAmount();
        TradeAmount defaultTradeAmount = getDefaultTradeAmount();
        setFixTradeAmount(defaultTradeAmount);
        setMinTradeAmount(defaultTradeAmount);
        setMaxTradeAmount(defaultTradeAmount);
        updateAmountSpec();
    }

    @Override
    protected void reset() {

    }


    /* --------------------------------------------------------------------- */
    // Observers
    /* --------------------------------------------------------------------- */

    @Override
    protected void addObservers() {
        pin(marketObservable().addObserver(value -> {
            updateDefaultTradeAmount();
            updateFixTradeAmount();
            updateMinTradeAmount();
            updateMaxTradeAmount();
        }));

        pin(selectedAccountByPaymentMethodObservable().addObserver(() -> {
        }));

        pin(priceQuoteObservable().addObserver(value -> {
            updateDefaultTradeAmount();
            updateFixTradeAmount();
            updateMinTradeAmount();
            updateMaxTradeAmount();
        }));
        pin(priceSpecObservable().addObserver(value -> {
        }));

        pin(useBaseCurrencyForAmountInputObservable().addObserver(value -> {

        }));
        pin(useRangeAmountObservable().addObserver(value -> {
            updateAmountSpec();
        }));
        pin(defaultTradeAmountObservable().addObserver(value -> {
        }));
        pin(fixTradeAmountObservable().addObserver(value -> {
            if (!getUseRangeAmount()) {
                updateAmountSpec();
            }
        }));
        pin(minTradeAmountObservable().addObserver(value -> {
            if (getUseRangeAmount()) {
                updateAmountSpec();
            }
        }));
        pin(maxTradeAmountObservable().addObserver(value -> {
            if (getUseRangeAmount()) {
                updateAmountSpec();
            }
        }));
        pin(amountSpecObservable().addObserver(value -> {
        }));
    }


    /* --------------------------------------------------------------------- */
    // Domain methods
    /* --------------------------------------------------------------------- */

    public void setFixTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        setMaxTradeAmount(tradeAmount);
    }

    public Monetary getInputAmount(TradeAmount tradeAmount) {
        if (getUseBaseCurrencyForAmountInput()) {
            return tradeAmount.getBaseSideAmount();
        } else {
            return tradeAmount.getQuoteSideAmount();
        }
    }

    public Monetary getPassiveAmount(TradeAmount tradeAmount) {
        if (getUseBaseCurrencyForAmountInput()) {
            return tradeAmount.getQuoteSideAmount();
        } else {
            return tradeAmount.getBaseSideAmount();
        }
    }


    /* --------------------------------------------------------------------- */
    // Update methods
    /* --------------------------------------------------------------------- */

    // Depends on market, priceQuote
    private void updateDefaultTradeAmount() {
        Fiat minTradeAmountInUsd = TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD;
        Monetary defaultAmountInFiat = MarketBasedAmountConversion.usdToFiat(marketPriceService, getMarket(), minTradeAmountInUsd)
                .orElseThrow();
        TradeAmount defaultTradeAmount = toTradeAmount(defaultAmountInFiat);
        setDefaultTradeAmount(defaultTradeAmount);
    }

    private void updateFixTradeAmount() {
        Monetary inputAmount = getInputAmount(getFixTradeAmount());
        setFixTradeAmountFromInputAmount(inputAmount);
    }

    private void updateMinTradeAmount() {
        Monetary inputAmount = getInputAmount(getMinTradeAmount());
        setMinTradeAmountFromInputAmount(inputAmount);
    }

    private void updateMaxTradeAmount() {
        Monetary inputAmount = getInputAmount(getMaxTradeAmount());
        setMaxTradeAmountFromInputAmount(inputAmount);
    }

    private void updateAmountSpec() {
        //todo should we use base amount spec for non fiat as well?
        setAmountSpec(createBaseSideAmountSpec());
    }



    /* --------------------------------------------------------------------- */
    // Amount Utils
    /* --------------------------------------------------------------------- */

    private TradeAmount toTradeAmount(Monetary amount) {
        return TradeAmountConversion.toTradeAmount(getMarket(), getPriceQuote(), amount);
    }

    public BaseSideAmountSpec createBaseSideAmountSpec() {
        return AmountSpecFactory.createBaseSideAmountSpec(getUseRangeAmount(),
                getMinTradeAmount(),
                getMaxTradeAmount(),
                getFixTradeAmount());
    }



    /* --------------------------------------------------------------------- */
    // Setters
    /* --------------------------------------------------------------------- */

    public void setMarket(Market market) {
        checkNotNull(market, "Market must not be null");
        offerDraft.setMarket(market);
    }

    public void setDirection(Direction direction) {
        checkNotNull(direction, "Direction must not be null");
        offerDraft.setDirection(direction);
    }

    public void clearSelectedAccountByPaymentMethod() {
        offerDraft.clearSelectedAccountByPaymentMethod();
    }

    public void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        offerDraft.putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod);
    }


    public void setPriceQuote(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "PriceQuote must not be null");
        offerDraft.setPriceQuote(priceQuote);
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        checkNotNull(priceSpec, "PriceSpec must not be null");
        offerDraft.setPriceSpec(priceSpec);
    }

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        offerDraft.setUseBaseCurrencyForAmountInput(value);
    }

    public void setUseRangeAmount(boolean useRangeAmount) {
        offerDraft.setUseRangeAmount(useRangeAmount);
        settingsService.setCookie(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED, useRangeAmount);
    }

    public void setDefaultTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setDefaultTradeAmount(tradeAmount);
    }

    public void setFixTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setMaxTradeAmount(tradeAmount);
    }

    public void setAmountSpec(AmountSpec amountSpec) {
        checkNotNull(amountSpec, "AmountSpec must not be null");
        offerDraft.setAmountSpec(amountSpec);
    }
}
