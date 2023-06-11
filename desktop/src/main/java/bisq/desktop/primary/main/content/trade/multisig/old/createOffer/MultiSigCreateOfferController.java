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

package bisq.desktop.primary.main.content.trade.multisig.old.createOffer;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.MarketSelection;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.PaymentSelection;
import bisq.desktop.primary.main.content.trade.components.ProtocolSelection;
import bisq.offer.Direction;
import bisq.offer.poc.PocOpenOfferService;
import javafx.collections.SetChangeListener;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;

@Slf4j
public class MultiSigCreateOfferController implements InitWithDataController<MultiSigCreateOfferController.InitData>, Controller {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class InitData {
        private final Market market;
        private final Direction direction;
        private final boolean showCreateOfferTab;

        public InitData(Market market, Direction direction, boolean showCreateOfferTab) {
            this.market = market;
            this.direction = direction;
            this.showCreateOfferTab = showCreateOfferTab;
        }
    }

    private final MultiSigCreateOfferModel model;
    @Getter
    private final MultiSigCreateOfferView view;
    private final PocOpenOfferService openOfferService;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final AmountPriceGroup amountPriceGroup;
    private final ProtocolSelection protocolSelection;
    private final PaymentSelection paymentSelection;
    private final SetChangeListener<Account<?, ? extends PaymentMethod<?>>> selectedBaseSideAccountsListener,
            selectedQuoteSideAccountsListener;
    private final SetChangeListener<PaymentRail> selectedBaseSidePaymentMethodsListener,
            selectedQuoteSidePaymentMethodsListener;
    private Subscription selectedMarketSubscription, directionSubscription, protocolSelectionSubscription,
            baseSideAmountSubscription, quoteSideAmountSubscription, fixPriceSubscription;

    public MultiSigCreateOfferController(DefaultApplicationService applicationService) {
        openOfferService = applicationService.getOfferService().getOpenOfferService();
        model = new MultiSigCreateOfferModel();

        marketSelection = new MarketSelection(applicationService.getSettingsService());
        directionSelection = new DirectionSelection();
        amountPriceGroup = new AmountPriceGroup(applicationService.getOracleService().getMarketPriceService());
        protocolSelection = new ProtocolSelection();
        paymentSelection = new PaymentSelection(applicationService.getAccountService());

        view = new MultiSigCreateOfferView(model, this,
                marketSelection.getRoot(),
                directionSelection.getRoot(),
                amountPriceGroup.getRoot(),
                protocolSelection.getRoot(),
                paymentSelection.getRoot());

        selectedBaseSideAccountsListener = c -> model.setAllSelectedBaseSideAccounts(paymentSelection.getSelectedBaseSideAccounts());
        selectedQuoteSideAccountsListener = c -> model.setAllSelectedQuoteSideAccounts(paymentSelection.getSelectedQuoteSideAccounts());
        selectedBaseSidePaymentMethodsListener = c -> model.setAllSelectedBaseSidePaymentMethods(paymentSelection.getSelectedBaseSidePaymentMethods());
        selectedQuoteSidePaymentMethodsListener = c -> model.setAllSelectedQuoteSidePaymentMethods(paymentSelection.getSelectedQuoteSidePaymentMethods());
    }

    @Override
    public void initWithData(InitData data) {
        marketSelection.setSelectedMarket(data.getMarket());
        directionSelection.setDirection(data.getDirection());
        model.setShowCreateOfferTab(data.isShowCreateOfferTab());
    }

    @Override
    public void onActivate() {
        model.getCreateOfferButtonVisibleProperty().set(model.getSelectedProtocolType() != null);

        selectedMarketSubscription = EasyBind.subscribe(marketSelection.selectedMarketProperty(),
                selectedMarket -> {
                    model.setSelectedMarket(selectedMarket);
                    directionSelection.setSelectedMarket(selectedMarket);
                    amountPriceGroup.setSelectedMarket(selectedMarket);
                    protocolSelection.setSelectedMarket(selectedMarket);
                    paymentSelection.setSelectedMarket(selectedMarket);
                });
        directionSubscription = EasyBind.subscribe(directionSelection.directionProperty(),
                direction -> {
                    model.setDirection(direction);
                    amountPriceGroup.setDirection(direction);
                    paymentSelection.setDirection(direction);
                });
        protocolSelectionSubscription = EasyBind.subscribe(protocolSelection.selectedProtocolType(),
                selectedProtocolType -> {
                    model.setSelectedProtocolType(selectedProtocolType);
                    paymentSelection.setSelectedProtocolType(selectedProtocolType);
                    model.getCreateOfferButtonVisibleProperty().set(selectedProtocolType != null);
                });
        baseSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.baseSideAmountProperty(),
                model::setBaseSideAmount);
        quoteSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.quoteSideAmountProperty(),
                model::setQuoteSideAmount);
        fixPriceSubscription = EasyBind.subscribe(amountPriceGroup.quoteProperty(),
                model::setFixPrice);

        paymentSelection.getSelectedBaseSideAccounts().addListener(selectedBaseSideAccountsListener);
        paymentSelection.getSelectedQuoteSideAccounts().addListener(selectedQuoteSideAccountsListener);
        paymentSelection.getSelectedBaseSidePaymentMethods().addListener(selectedBaseSidePaymentMethodsListener);
        paymentSelection.getSelectedQuoteSidePaymentMethods().addListener(selectedQuoteSidePaymentMethodsListener);

        model.setAllSelectedBaseSideAccounts(paymentSelection.getSelectedBaseSideAccounts());
        model.setAllSelectedQuoteSideAccounts(paymentSelection.getSelectedQuoteSideAccounts());
        model.setAllSelectedBaseSidePaymentMethods(paymentSelection.getSelectedBaseSidePaymentMethods());
        model.setAllSelectedQuoteSidePaymentMethods(paymentSelection.getSelectedQuoteSidePaymentMethods());
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        directionSubscription.unsubscribe();
        protocolSelectionSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        quoteSideAmountSubscription.unsubscribe();
        fixPriceSubscription.unsubscribe();

        paymentSelection.getSelectedBaseSideAccounts().removeListener(selectedBaseSideAccountsListener);
        paymentSelection.getSelectedQuoteSideAccounts().removeListener(selectedQuoteSideAccountsListener);
        paymentSelection.getSelectedBaseSidePaymentMethods().removeListener(selectedBaseSidePaymentMethodsListener);
        paymentSelection.getSelectedQuoteSidePaymentMethods().removeListener(selectedQuoteSidePaymentMethodsListener);
    }

    public void onCreateOffer() {
        openOfferService.createOffer(model.getSelectedMarket(),
                        model.getDirection(),
                        model.getBaseSideAmount(),
                        model.getFixPrice(),
                        model.getSelectedProtocolType(),
                        new ArrayList<>(model.getSelectedBaseSideAccounts()),
                        new ArrayList<>(model.getSelectedQuoteSideAccounts()),
                        new ArrayList<>(model.getSelectedBaseSidePaymentPaymentRails()),
                        new ArrayList<>(model.getSelectedQuoteSidePaymentPaymentRails()))
                .whenComplete((offer, throwable) -> {
                    if (throwable == null) {
                        model.getOfferProperty().set(offer);
                    } else {
                        //todo provide error to UI
                    }
                });
    }

    public void onPublishOffer() {
        openOfferService.publishOffer(model.getOffer());
        model.setShowCreateOfferTab(false);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_OFFER_BOOK);
    }

    public void onCancel() {
        model.setShowCreateOfferTab(false);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_OFFER_BOOK);
    }
}
