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

package bisq.desktop.primary.main.content.trade.multiSig.old.createOffer;

import bisq.account.accounts.Account;
import bisq.account.settlement.SettlementMethod;
import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.MarketSelection;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.ProtocolSelection;
import bisq.desktop.primary.main.content.trade.components.SettlementSelection;
import bisq.offer.OpenOfferService;
import bisq.offer.spec.Direction;
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
    private final OpenOfferService openOfferService;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final AmountPriceGroup amountPriceGroup;
    private final ProtocolSelection protocolSelection;
    private final SettlementSelection settlementSelection;
    private final SetChangeListener<Account<? extends SettlementMethod>> selectedBaseSideAccountsListener,
            selectedQuoteSideAccountsListener;
    private final SetChangeListener<SettlementMethod> selectedBaseSideSettlementMethodsListener,
            selectedQuoteSideSettlementMethodsListener;
    private Subscription selectedMarketSubscription, directionSubscription, protocolSelectionSubscription,
            baseSideAmountSubscription, quoteSideAmountSubscription, fixPriceSubscription;

    public MultiSigCreateOfferController(DefaultApplicationService applicationService) {
        openOfferService = applicationService.getOfferService().getOpenOfferService();
        model = new MultiSigCreateOfferModel();

        marketSelection = new MarketSelection(applicationService.getSettingsService());
        directionSelection = new DirectionSelection();
        amountPriceGroup = new AmountPriceGroup(applicationService.getOracleService().getMarketPriceService());
        protocolSelection = new ProtocolSelection();
        settlementSelection = new SettlementSelection(applicationService.getAccountService());

        view = new MultiSigCreateOfferView(model, this,
                marketSelection.getRoot(),
                directionSelection.getRoot(),
                amountPriceGroup.getRoot(),
                protocolSelection.getRoot(),
                settlementSelection.getRoot());

        selectedBaseSideAccountsListener = c -> model.setAllSelectedBaseSideAccounts(settlementSelection.getSelectedBaseSideAccounts());
        selectedQuoteSideAccountsListener = c -> model.setAllSelectedQuoteSideAccounts(settlementSelection.getSelectedQuoteSideAccounts());
        selectedBaseSideSettlementMethodsListener = c -> model.setAllSelectedBaseSideSettlementMethods(settlementSelection.getSelectedBaseSideSettlementMethods());
        selectedQuoteSideSettlementMethodsListener = c -> model.setAllSelectedQuoteSideSettlementMethods(settlementSelection.getSelectedQuoteSideSettlementMethods());
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
                    settlementSelection.setSelectedMarket(selectedMarket);
                });
        directionSubscription = EasyBind.subscribe(directionSelection.directionProperty(),
                direction -> {
                    model.setDirection(direction);
                    amountPriceGroup.setDirection(direction);
                    settlementSelection.setDirection(direction);
                });
        protocolSelectionSubscription = EasyBind.subscribe(protocolSelection.selectedProtocolType(),
                selectedProtocolType -> {
                    model.setSelectedProtocolType(selectedProtocolType);
                    settlementSelection.setSelectedProtocolType(selectedProtocolType);
                    model.getCreateOfferButtonVisibleProperty().set(selectedProtocolType != null);
                });
        baseSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.baseSideAmountProperty(),
                model::setBaseSideAmount);
        quoteSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.quoteSideAmountProperty(),
                model::setQuoteSideAmount);
        fixPriceSubscription = EasyBind.subscribe(amountPriceGroup.fixPriceProperty(),
                model::setFixPrice);

        settlementSelection.getSelectedBaseSideAccounts().addListener(selectedBaseSideAccountsListener);
        settlementSelection.getSelectedQuoteSideAccounts().addListener(selectedQuoteSideAccountsListener);
        settlementSelection.getSelectedBaseSideSettlementMethods().addListener(selectedBaseSideSettlementMethodsListener);
        settlementSelection.getSelectedQuoteSideSettlementMethods().addListener(selectedQuoteSideSettlementMethodsListener);

        model.setAllSelectedBaseSideAccounts(settlementSelection.getSelectedBaseSideAccounts());
        model.setAllSelectedQuoteSideAccounts(settlementSelection.getSelectedQuoteSideAccounts());
        model.setAllSelectedBaseSideSettlementMethods(settlementSelection.getSelectedBaseSideSettlementMethods());
        model.setAllSelectedQuoteSideSettlementMethods(settlementSelection.getSelectedQuoteSideSettlementMethods());
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        directionSubscription.unsubscribe();
        protocolSelectionSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        quoteSideAmountSubscription.unsubscribe();
        fixPriceSubscription.unsubscribe();

        settlementSelection.getSelectedBaseSideAccounts().removeListener(selectedBaseSideAccountsListener);
        settlementSelection.getSelectedQuoteSideAccounts().removeListener(selectedQuoteSideAccountsListener);
        settlementSelection.getSelectedBaseSideSettlementMethods().removeListener(selectedBaseSideSettlementMethodsListener);
        settlementSelection.getSelectedQuoteSideSettlementMethods().removeListener(selectedQuoteSideSettlementMethodsListener);
    }

    public void onCreateOffer() {
        openOfferService.createOffer(model.getSelectedMarket(),
                        model.getDirection(),
                        model.getBaseSideAmount(),
                        model.getFixPrice(),
                        model.getSelectedProtocolType(),
                        new ArrayList<>(model.getSelectedBaseSideAccounts()),
                        new ArrayList<>(model.getSelectedQuoteSideAccounts()),
                        new ArrayList<>(model.getSelectedBaseSideSettlementMethods()),
                        new ArrayList<>(model.getSelectedQuoteSideSettlementMethods()))
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
