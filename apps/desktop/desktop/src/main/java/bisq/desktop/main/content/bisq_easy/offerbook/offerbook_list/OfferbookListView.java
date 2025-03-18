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

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.bisq_easy.offerbook.BisqEasyOfferbookView;
import bisq.desktop.main.content.chat.BaseChatView;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import com.google.common.base.Joiner;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class OfferbookListView extends bisq.desktop.common.view.View<VBox, OfferbookListModel, OfferbookListController> {
    private static final double COLLAPSED_LIST_WIDTH = BisqEasyOfferbookView.COLLAPSED_LIST_WIDTH + 2; // +2 for the margin
    private static final double HEADER_HEIGHT = BaseChatView.HEADER_HEIGHT;
    private static final double LIST_CELL_HEIGHT = BisqEasyOfferbookView.LIST_CELL_HEIGHT;
    private static final long SPLITPANE_ANIMATION_DURATION = BisqEasyOfferbookView.SPLITPANE_ANIMATION_DURATION;
    private static final String ACTIVE_FILTER_CLASS = "active-filter";

    private final Label showMyOffersOnlyLabel, expandOfferListLabel, collapseOfferListLabel, offerListCollapsedIconLabel, headline;
    private final BisqTableView<OfferbookListItem> tableView;
    private final HBox showOnlyMyMessagesHBox, titleAndCollapseOfferListHBox;
    private final ImageView expandOfferListWhiteIcon, expandOfferListGreyIcon, collapseOfferListWhiteIcon, collapseOfferListGreyIcon,
            offerListGreyIcon, offerListCollapsedWhiteIcon, offerListGreenIcon, offerListExpandedWhiteIcon;
    private final DropdownMenu offerDirectionFilterMenu, paymentsFilterMenu;
    private final ListChangeListener<FiatPaymentMethod> availablePaymentsChangeListener;
    private final SetChangeListener<FiatPaymentMethod> selectedPaymentsChangeListener;
    private final CheckBox showOnlyMyMessages;
    private final VBox content;
    private DropdownBisqMenuItem buyFromOffers, sellToOffers;
    private Label offerDirectionFilterLabel, paymentsFilterLabel;
    private Subscription showOfferListExpandedPin, showBuyFromOffersPin, showMyOffersOnlyPin,
            offerListTableViewSelectionPin, activeMarketPaymentsCountPin, isCustomPaymentsSelectedPin,
            widthPropertyPin;

    OfferbookListView(OfferbookListModel model, OfferbookListController controller) {
        super(new VBox(), model, controller);

        offerListGreenIcon = ImageUtil.getImageViewById("list-view-green");
        offerListGreyIcon = ImageUtil.getImageViewById("list-view-grey");
        offerListCollapsedWhiteIcon = ImageUtil.getImageViewById("list-view-white");
        offerListExpandedWhiteIcon = ImageUtil.getImageViewById("list-view-white");
        expandOfferListWhiteIcon = ImageUtil.getImageViewById("expand-white");
        expandOfferListGreyIcon = ImageUtil.getImageViewById("expand-grey");
        collapseOfferListWhiteIcon = ImageUtil.getImageViewById("collapse-white");
        collapseOfferListGreyIcon = ImageUtil.getImageViewById("collapse-grey");

        // expanded header title
        headline = new Label(Res.get("bisqEasy.offerbook.offerList"), offerListGreenIcon);
        headline.setCursor(Cursor.HAND);
        headline.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.expandedList.tooltip")));

        collapseOfferListLabel = new Label("", collapseOfferListGreyIcon);
        collapseOfferListLabel.setCursor(Cursor.HAND);
        collapseOfferListLabel.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.expandedList.tooltip")));
        titleAndCollapseOfferListHBox = new HBox(headline, Spacer.fillHBox(), collapseOfferListLabel);
        HBox.setHgrow(titleAndCollapseOfferListHBox, Priority.ALWAYS);
        titleAndCollapseOfferListHBox.setFillHeight(true);
        titleAndCollapseOfferListHBox.setAlignment(Pos.CENTER);
        titleAndCollapseOfferListHBox.setPadding(new Insets(0, 12, 0, 12));

        // collapsed header title
        offerListCollapsedIconLabel = new Label("", offerListGreyIcon);
        offerListCollapsedIconLabel.setCursor(Cursor.HAND);
        offerListCollapsedIconLabel.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.collapsedList.tooltip")));

        expandOfferListLabel = new Label("", expandOfferListGreyIcon);
        expandOfferListLabel.setCursor(Cursor.HAND);
        expandOfferListLabel.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.collapsedList.tooltip")));

        HBox header = new HBox(titleAndCollapseOfferListHBox, offerListCollapsedIconLabel);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.getStyleClass().add("chat-header-title");
        header.setPadding(new Insets(4, 0, 0, 0));
        header.setAlignment(Pos.CENTER);

        availablePaymentsChangeListener = change -> updateMarketPaymentFilters();
        selectedPaymentsChangeListener = change -> updatePaymentsSelection();
        offerDirectionFilterMenu = createAndGetOffersDirectionFilterMenu();
        paymentsFilterMenu = createAndGetPaymentsFilterDropdownMenu();
        showMyOffersOnlyLabel = new Label(Res.get("bisqEasy.offerbook.offerList.table.filters.showMyOffersOnly"));
        showOnlyMyMessages = new CheckBox();
        showOnlyMyMessagesHBox = new HBox(5, showOnlyMyMessages, showMyOffersOnlyLabel);
        showOnlyMyMessagesHBox.getStyleClass().add("offerbook-subheader-checkbox");
        showOnlyMyMessagesHBox.setAlignment(Pos.CENTER_LEFT);

        HBox subheader = new HBox(10);
        subheader.getStyleClass().add("offer-list-subheader");
        subheader.setAlignment(Pos.CENTER_LEFT);
        subheader.getChildren().addAll(offerDirectionFilterMenu, paymentsFilterMenu, showOnlyMyMessagesHBox);

        tableView = new BisqTableView<>(model.getSortedOfferbookListItems());
        tableView.getStyleClass().add("offers-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        tableView.setFixedCellSize(LIST_CELL_HEIGHT);
        tableView.setPlaceholder(new Label());
        VBox.setVgrow(tableView, Priority.ALWAYS);
        configOffersTableView();

        HBox expandOfferListLabelBox = new HBox(expandOfferListLabel);
        VBox.setVgrow(expandOfferListLabelBox, Priority.ALWAYS);
        expandOfferListLabelBox.setAlignment(Pos.CENTER);
        expandOfferListLabelBox.setPadding(new Insets(0, 0, 60, 0));

        content = new VBox(header, Layout.hLine(), subheader, tableView, expandOfferListLabelBox);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().add(content);
    }

    @Override
    protected void onViewAttached() {
        paymentsFilterLabel.textProperty().bind(model.getPaymentFilterTitle());
        showOnlyMyMessages.selectedProperty().bindBidirectional(model.getShowMyOffersOnly());

        showOfferListExpandedPin = EasyBind.subscribe(model.getShowOfferListExpanded(), showOfferListExpanded -> {
            if (showOfferListExpanded != null) {
                tableView.setVisible(showOfferListExpanded);
                tableView.setManaged(showOfferListExpanded);
                offerDirectionFilterMenu.setVisible(showOfferListExpanded);
                offerDirectionFilterMenu.setManaged(showOfferListExpanded);
                paymentsFilterMenu.setVisible(showOfferListExpanded);
                paymentsFilterMenu.setManaged(showOfferListExpanded);
                showOnlyMyMessagesHBox.setVisible(showOfferListExpanded);
                showOnlyMyMessagesHBox.setManaged(showOfferListExpanded);
                titleAndCollapseOfferListHBox.setVisible(showOfferListExpanded);
                titleAndCollapseOfferListHBox.setManaged(showOfferListExpanded);
                expandOfferListLabel.setVisible(!showOfferListExpanded);
                expandOfferListLabel.setManaged(!showOfferListExpanded);
                offerListCollapsedIconLabel.setVisible(!showOfferListExpanded);
                offerListCollapsedIconLabel.setManaged(!showOfferListExpanded);
                if (showOfferListExpanded) {
                    expandListView();
                } else {
                    collapseListView();
                }
            }
        });

        offerListTableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                controller::onSelectOfferMessageItem);

        showBuyFromOffersPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyFromOffers -> {
            if (showBuyFromOffers != null) {
                offerDirectionFilterLabel.getStyleClass().clear();
                if (showBuyFromOffers) {
                    offerDirectionFilterLabel.setText(sellToOffers.getLabelText());
                    offerDirectionFilterLabel.getStyleClass().add("sell-to-offers");
                } else {
                    offerDirectionFilterLabel.setText(buyFromOffers.getLabelText());
                    offerDirectionFilterLabel.getStyleClass().add("buy-from-offers");
                }
            }
        });

        activeMarketPaymentsCountPin = EasyBind.subscribe(model.getActiveMarketPaymentsCount(), count -> {
            if (count.intValue() != 0) {
                if (!paymentsFilterLabel.getStyleClass().contains(ACTIVE_FILTER_CLASS)) {
                    paymentsFilterLabel.getStyleClass().add(ACTIVE_FILTER_CLASS);
                }
            } else {
                paymentsFilterLabel.getStyleClass().remove(ACTIVE_FILTER_CLASS);
            }
        });

        isCustomPaymentsSelectedPin = EasyBind.subscribe(model.getIsCustomPaymentsSelected(),
                isSelected -> updatePaymentsSelection());

        showMyOffersOnlyPin = EasyBind.subscribe(model.getShowMyOffersOnly(), showMyOffers -> {
            if (showMyOffers) {
                if (!showMyOffersOnlyLabel.getStyleClass().contains(ACTIVE_FILTER_CLASS)) {
                    showMyOffersOnlyLabel.getStyleClass().add(ACTIVE_FILTER_CLASS);
                }
            } else {
                showMyOffersOnlyLabel.getStyleClass().remove(ACTIVE_FILTER_CLASS);
            }
        });

        widthPropertyPin = EasyBind.subscribe(root.widthProperty(), widthProperty -> {
            if (widthProperty != null && model.getShowOfferListExpanded() != null) {
                if (widthProperty.intValue() == COLLAPSED_LIST_WIDTH && model.getShowOfferListExpanded().get()) {
                    controller.toggleOfferList();
                }
            }
        });

        model.getAvailableMarketPayments().addListener(availablePaymentsChangeListener);
        updateMarketPaymentFilters();
        model.getSelectedMarketPayments().addListener(selectedPaymentsChangeListener);
        updatePaymentsSelection();

        headline.setOnMouseEntered(e -> headline.setGraphic(offerListExpandedWhiteIcon));
        headline.setOnMouseExited(e -> headline.setGraphic(offerListGreenIcon));
        headline.setOnMouseClicked(e -> controller.toggleOfferList());
        collapseOfferListLabel.setOnMouseEntered(e -> collapseOfferListLabel.setGraphic(collapseOfferListWhiteIcon));
        collapseOfferListLabel.setOnMouseExited(e -> collapseOfferListLabel.setGraphic(collapseOfferListGreyIcon));
        collapseOfferListLabel.setOnMouseClicked(e -> controller.toggleOfferList());
        expandOfferListLabel.setOnMouseEntered(e -> expandOfferListLabel.setGraphic(expandOfferListWhiteIcon));
        expandOfferListLabel.setOnMouseExited(e -> expandOfferListLabel.setGraphic(expandOfferListGreyIcon));
        expandOfferListLabel.setOnMouseClicked(e -> controller.toggleOfferList());
        offerListCollapsedIconLabel.setOnMouseEntered(e -> offerListCollapsedIconLabel.setGraphic(offerListCollapsedWhiteIcon));
        offerListCollapsedIconLabel.setOnMouseExited(e -> offerListCollapsedIconLabel.setGraphic(offerListGreyIcon));
        offerListCollapsedIconLabel.setOnMouseClicked(e -> controller.toggleOfferList());

        buyFromOffers.setOnAction(e -> controller.onSelectBuyFromFilter());
        sellToOffers.setOnAction(e -> controller.onSelectSellToFilter());
    }

    @Override
    protected void onViewDetached() {
        paymentsFilterLabel.textProperty().unbind();
        showOnlyMyMessages.selectedProperty().unbindBidirectional(model.getShowMyOffersOnly());

        showOfferListExpandedPin.unsubscribe();
        offerListTableViewSelectionPin.unsubscribe();
        showBuyFromOffersPin.unsubscribe();
        activeMarketPaymentsCountPin.unsubscribe();
        isCustomPaymentsSelectedPin.unsubscribe();
        showMyOffersOnlyPin.unsubscribe();
        widthPropertyPin.unsubscribe();

        model.getAvailableMarketPayments().removeListener(availablePaymentsChangeListener);
        model.getSelectedMarketPayments().removeListener(selectedPaymentsChangeListener);

        headline.setOnMouseEntered(null);
        headline.setOnMouseExited(null);
        headline.setOnMouseClicked(null);
        collapseOfferListLabel.setOnMouseEntered(null);
        collapseOfferListLabel.setOnMouseExited(null);
        collapseOfferListLabel.setOnMouseClicked(null);
        expandOfferListLabel.setOnMouseEntered(null);
        expandOfferListLabel.setOnMouseExited(null);
        expandOfferListLabel.setOnMouseClicked(null);
        offerListCollapsedIconLabel.setOnMouseEntered(null);
        offerListCollapsedIconLabel.setOnMouseExited(null);
        offerListCollapsedIconLabel.setOnMouseClicked(null);

        buyFromOffers.setOnAction(null);
        sellToOffers.setOnAction(null);

        headline.setTooltip(null);
        collapseOfferListLabel.setTooltip(null);
        expandOfferListLabel.setTooltip(null);
        offerListCollapsedIconLabel.setTooltip(null);

        cleanUpPaymentsFilterMenu();
    }

    private void collapseListView() {
        if (model.isUseAnimations()) {
            UIScheduler.run(this::applyCollapsedViewChanges).after(SPLITPANE_ANIMATION_DURATION);
        } else {
            applyCollapsedViewChanges();
        }
    }

    private void applyCollapsedViewChanges() {
        root.setMaxWidth(COLLAPSED_LIST_WIDTH);
        root.setPrefWidth(COLLAPSED_LIST_WIDTH);
        root.setMinWidth(COLLAPSED_LIST_WIDTH);
        VBox.setMargin(content, new Insets(0, 0, 0, 2));
        content.getStyleClass().remove("chat-container");
        content.getStyleClass().add("collapsed-offer-list-container");
    }

    private void expandListView() {
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMinWidth(COLLAPSED_LIST_WIDTH);
        content.getStyleClass().remove("collapsed-offer-list-container");
        content.getStyleClass().add("chat-container");
        VBox.setMargin(content, new Insets(0, 0, 0, 4.5));
    }

    private DropdownMenu createAndGetOffersDirectionFilterMenu() {
        DropdownMenu menu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        menu.getStyleClass().add("dropdown-offer-list-direction-filter-menu");
        menu.setOpenToTheRight(true);
        offerDirectionFilterLabel = new Label();
        menu.setContent(offerDirectionFilterLabel);
        buyFromOffers = new DropdownBisqMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.buyFrom"));
        sellToOffers = new DropdownBisqMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.sellTo"));
        menu.addMenuItems(buyFromOffers, sellToOffers);
        return menu;
    }

    private DropdownMenu createAndGetPaymentsFilterDropdownMenu() {
        DropdownMenu menu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        menu.getStyleClass().add("dropdown-offer-list-payment-filter-menu");
        menu.setOpenToTheRight(true);
        paymentsFilterLabel = new Label();
        menu.setContent(paymentsFilterLabel);
        return menu;
    }

    private void updateMarketPaymentFilters() {
        cleanUpPaymentsFilterMenu();

        model.getAvailableMarketPayments().forEach(payment -> {
            ImageView paymentIcon = ImageUtil.getImageViewById(payment.getName());
            Label paymentLabel = new Label(payment.getDisplayString(), paymentIcon);
            paymentLabel.setGraphicTextGap(10);
            PaymentMenuItem paymentItem = new PaymentMenuItem(payment, paymentLabel);
            paymentItem.setHideOnClick(false);
            paymentItem.setOnAction(e -> controller.togglePaymentFilter(payment, paymentItem.isSelected()));
            paymentsFilterMenu.addMenuItems(paymentItem);
        });

        StackPane customPaymentIcon = BisqEasyViewUtils.getCustomPaymentMethodIcon("C");
        Label customPaymentLabel = new Label(
                Res.get("bisqEasy.offerbook.offerList.table.filters.paymentMethods.customPayments"), customPaymentIcon);
        customPaymentLabel.setGraphicTextGap(10);
        PaymentMenuItem customItem = new PaymentMenuItem(null, customPaymentLabel);
        customItem.setHideOnClick(false);
        customItem.setOnAction(e -> controller.toggleCustomPaymentFilter(customItem.isSelected()));
        paymentsFilterMenu.addMenuItems(customItem);

        SeparatorMenuItem separator = new SeparatorMenuItem();
        DropdownBisqMenuItem clearFilters = new DropdownBisqMenuItem("delete-t-grey", "delete-t-white",
                Res.get("bisqEasy.offerbook.offerList.table.filters.paymentMethods.clearFilters"));
        clearFilters.setHideOnClick(false);
        clearFilters.setOnAction(e -> controller.clearPaymentFilters());
        paymentsFilterMenu.addMenuItems(separator, clearFilters);
    }

    private void cleanUpPaymentsFilterMenu() {
        paymentsFilterMenu.getMenuItems().stream()
                .filter(item -> item instanceof PaymentMenuItem)
                .map(item -> (PaymentMenuItem) item)
                .forEach(PaymentMenuItem::dispose);
        paymentsFilterMenu.clearMenuItems();
    }

    private void updatePaymentsSelection() {
        paymentsFilterMenu.getMenuItems().stream()
                .filter(item -> item instanceof PaymentMenuItem)
                .map(item -> (PaymentMenuItem) item)
                .forEach(paymentMenuItem ->
                        paymentMenuItem.getPaymentMethod()
                                .ifPresentOrElse(
                                        payment -> paymentMenuItem.updateSelection(model.getSelectedMarketPayments().contains(payment)),
                                        () -> paymentMenuItem.updateSelection(model.getIsCustomPaymentsSelected().get()))
                );
    }

    private void configOffersTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        BisqTableColumn<OfferbookListItem> userProfileColumn = new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.peerProfile"))
                .left()
                .minWidth(150)
                .setCellFactory(getUserProfileCellFactory())
                .comparator(Comparator.comparingLong(OfferbookListItem::getTotalScore).reversed())
                .build();
        tableView.getColumns().add(userProfileColumn);
        tableView.getSortOrder().add(userProfileColumn);

        BisqTableColumn<OfferbookListItem> priceColumn = new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.price"))
                .right()
                .minWidth(75)
                .setCellFactory(getPriceCellFactory())
                .comparator((o1, o2) -> {
                    if (o1.getBisqEasyOffer().getDirection().isSell()) {
                        return Double.compare(o1.getPriceSpecAsPercent(), o2.getPriceSpecAsPercent());
                    } else {
                        return Double.compare(o2.getPriceSpecAsPercent(), o1.getPriceSpecAsPercent());
                    }
                })
                .build();
        tableView.getColumns().add(priceColumn);
        tableView.getSortOrder().add(priceColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .titleProperty(model.getFiatAmountTitle())
                .right()
                .minWidth(120)
                .setCellFactory(getFiatAmountCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getQuoteSideMinAmount))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.paymentMethod"))
                .right()
                .minWidth(105)
                .setCellFactory(getPaymentCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getFiatPaymentMethodsAsString))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.settlementMethod"))
                .left()
                .minWidth(95)
                .setCellFactory(getSettlementCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getBitcoinPaymentMethodsAsString))
                .build());
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userNameLabel = new Label();
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
            private final VBox nameAndReputationBox = new VBox(userNameLabel, reputationScoreDisplay);
            private final UserProfileIcon userProfileIcon = new UserProfileIcon();
            private final HBox userProfileBox = new HBox(10, userProfileIcon, nameAndReputationBox);

            {
                userNameLabel.setId("chat-user-name");
                HBox.setMargin(userProfileIcon, new Insets(0, 0, 0, -1));
                nameAndReputationBox.setAlignment(Pos.CENTER_LEFT);
                userProfileBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userNameLabel.setText(item.getUserNickname());
                    reputationScoreDisplay.setReputationScore(item.getReputationScore());
                    userProfileIcon.setUserProfile(item.getSenderUserProfile(), false);
                    setGraphic(userProfileBox);
                } else {
                    userNameLabel.setText("");
                    reputationScoreDisplay.setReputationScore(null);
                    userProfileIcon.dispose();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final Label percentagePriceLabel = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    percentagePriceLabel.setText(item.getFormattedPercentagePrice());
                    percentagePriceLabel.setStyle(item.isFixPrice() ? "-fx-text-fill: -bisq2-green-lit-20" : "");
                    tooltip.setText(item.getPriceTooltipText());
                    percentagePriceLabel.setTooltip(tooltip);
                    setGraphic(percentagePriceLabel);
                } else {
                    percentagePriceLabel.setText("");
                    percentagePriceLabel.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getFiatAmountCellFactory() {
        return column -> new TableCell<>() {
            private final Label fiatAmountLabel = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    fiatAmountLabel.setText(item.getFormattedRangeQuoteAmount());
                    tooltip.setText(item.getFormattedRangeQuoteAmount());
                    fiatAmountLabel.setTooltip(tooltip);
                    setGraphic(fiatAmountLabel);
                } else {
                    fiatAmountLabel.setText("");
                    fiatAmountLabel.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getPaymentCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(5);
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                hbox.setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    for (FiatPaymentMethod fiatPaymentMethod : item.getFiatPaymentMethods()) {
                        Node icon = !fiatPaymentMethod.isCustomPaymentMethod()
                                ? ImageUtil.getImageViewById(fiatPaymentMethod.getName())
                                : BisqEasyViewUtils.getCustomPaymentMethodIcon(fiatPaymentMethod.getDisplayString());
                        hbox.getChildren().add(icon);
                    }
                    tooltip.setText(Joiner.on("\n").join(item.getFiatPaymentMethods().stream()
                            .map(PaymentMethod::getDisplayString)
                            .toList()));
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getSettlementCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(5);
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    for (BitcoinPaymentMethod bitcoinPaymentMethod : item.getBitcoinPaymentMethods()) {
                        ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                        ColorAdjust colorAdjust = new ColorAdjust();
                        colorAdjust.setBrightness(-0.2);
                        icon.setEffect(colorAdjust);
                        hbox.getChildren().add(icon);
                    }
                    tooltip.setText(Joiner.on("\n").join(item.getBitcoinPaymentMethods().stream()
                            .map(PaymentMethod::getDisplayString)
                            .toList()));
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }

    @Getter
    private static final class PaymentMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final Optional<FiatPaymentMethod> paymentMethod;

        private PaymentMenuItem(FiatPaymentMethod paymentMethod, Label displayLabel) {
            super("check-white", "check-white", displayLabel);

            this.paymentMethod = Optional.ofNullable(paymentMethod);
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
            initialize();
        }

        public void initialize() {
        }

        public void dispose() {
            setOnAction(null);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }
    }
}
