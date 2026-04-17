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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount;

import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.MuSigAmountComponentsController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class MuSigCreateOfferAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final MuSigCreateOfferAmountModel model;
    @Getter
    private final MuSigCreateOfferAmountView view;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;
    private final Region owner;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final MuSigAmountComponentsController muSigAmountComponentsController;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigCreateOfferAmountController(ServiceProvider serviceProvider,
                                            CreateOfferDraftWorkflow createOfferDraftWorkflow,
                                            Region owner,
                                            Consumer<Boolean> navigationButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        model = new MuSigCreateOfferAmountModel();

        muSigAmountComponentsController = new MuSigAmountComponentsController(serviceProvider, createOfferDraftWorkflow);
        view = new MuSigCreateOfferAmountView(model, this, muSigAmountComponentsController.getView().getRoot());
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        Market market = createOfferDraftWorkflow.getMarket();

        pins.add(createOfferDraftWorkflow.useRangeAmountObservable().addObserver(useRangeAmount -> {
            UIThread.run(() -> {
                model.getUseRangeAmount().set(useRangeAmount);
            });
        }));


        // model.getShouldShowWarningIcon().set(false);

     /*   if (model.getPriceQuote().get() == null && amountSelectionController.getQuote().get() != null) {
            model.getPriceQuote().set(amountSelectionController.getQuote().get());
        }


        model.getShouldShowHowToBuildReputationButton().set(model.getDisplayDirection().isSell());

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMinBaseSideAmount(),
                value -> {
                    if (model.getIsRangeAmountEnabled().get()) {
                        if (value != null && amountSelectionController.getMaxOrFixedBaseSideAmount().get() != null &&
                                value.getValue() > amountSelectionController.getMaxOrFixedBaseSideAmount().get().getValue()) {
                            amountSelectionController.setMaxOrFixedBaseSideAmount(value);
                        }
                    }
                }));
        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMaxOrFixedBaseSideAmount(),
                value -> {
                    if (value != null &&
                            model.getIsRangeAmountEnabled().get() &&
                            amountSelectionController.getMinBaseSideAmount().get() != null &&
                            value.getValue() < amountSelectionController.getMinBaseSideAmount().get().getValue()) {
                        amountSelectionController.setMinBaseSideAmount(value);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMinQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsRangeAmountEnabled().get() &&
                                amountSelectionController.getMaxOrFixedQuoteSideAmount().get() != null &&
                                value.getValue() > amountSelectionController.getMaxOrFixedQuoteSideAmount().get().getValue()) {
                            amountSelectionController.setMaxOrFixedQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                        quoteSideAmountsChanged(false);
                    }
                }));
        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMaxOrFixedQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsRangeAmountEnabled().get() &&
                                amountSelectionController.getMinQuoteSideAmount().get() != null &&
                                value.getValue() < amountSelectionController.getMinQuoteSideAmount().get().getValue()) {
                            amountSelectionController.setMinQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                        quoteSideAmountsChanged(true);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            applyAmountSpec();
            amountSelectionController.setUseRangeAmount(isRangeAmountEnabled);
        }));
        applyAmountSpec();

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getIsDefaultAmountInputBtc(), isDefaultAmountInputBtc -> {
            String quoteCode = model.getPriceQuote().get().getMarket().getQuoteCurrencyCode();
            model.getPriceTooltip().set(amountSelectionController.isDefaultAmountInputBtc()
                    ? Res.get("muSig.offer.wizard.amount.quoteSide.tooltip.fiatAmount.selectedPrice", quoteCode)
                    : Res.get("muSig.offer.wizard.amount.baseSide.tooltip.btcAmount.selectedPrice"));
        }));

        subscriptions.add(EasyBind.subscribe(model.getPriceTooltip(), priceTooltip -> {
            if (priceTooltip != null) {
                amountSelectionController.setTooltip(priceTooltip);
            }
        }));*/
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
        navigationButtonsVisibleHandler.accept(true);
        model.getIsOverlayVisible().set(false);
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        model.getPaymentMethods().setAll(paymentMethods);
    }

    public boolean validate() {
        // No errorMessage set yet... We reset the amount to a valid value in case input is invalid
        if (model.getErrorMessage().get() == null) {
            return true;
        } else {
            new Popup().invalid(model.getErrorMessage().get())
                    .owner(owner)
                    .show();
            return false;
        }
    }

    public void updateAmountSpecWithPriceSpec(PriceSpec priceSpec) {
        model.getPriceSpec().set(priceSpec);

      /*  BaseSideAmountSpec amountSpec = model.getBaseSideAmountSpec().get();
        if (amountSpec == null) {
            return;
        }
        Market market = model.getMarket();
        if (market == null) {
            log.warn("market is null at updateBaseSideAmountSpecWithPriceSpec");
            return;
        }
        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        if (priceQuote.isEmpty()) {
            log.warn("priceQuote is empty at updateBaseSideAmountSpecWithPriceSpec");
            return;
        }
        model.getPriceQuote().set(priceQuote.get());
        amountSelectionController.setQuote(priceQuote.get());

        OfferAmountUtil.updateBaseSideAmountSpecWithPriceSpec(marketPriceService, amountSpec, priceSpec, market)
                .ifPresent(baseSideAmountSpec -> model.getBaseSideAmountSpec().set(baseSideAmountSpec));*/
    }

    public void reset() {
        //amountSelectionController.reset();
        model.reset();
    }

    public ReadOnlyBooleanProperty getIsOverlayVisible() {
        return model.getIsOverlayVisible();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onSetUseRangeAmount(boolean value) {
        createOfferDraftWorkflow.setUseRangeAmount(value);
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseOverlay);
    }

    void onShowOverlay() {
        if (!model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getIsOverlayVisible().set(true);
        }
    }

    void onCloseOverlay() {
        if (model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getIsOverlayVisible().set(false);
        }
    }

    void onLearnHowToBuildReputation() {
        closeAndNavigateToHandler.accept(NavigationTarget.BUILD_REPUTATION);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }


}
