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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final BisqEasyOfferbookController bisqEasyOfferbookController;
    private SearchBox marketSelectorSearchBox;
    private Label chatDomainTitle;
    private BisqTableView<MarketChannelItem> tableView;
    private BisqTableColumn<MarketChannelItem> marketLabelTableColumn;
    private VBox marketSelectionList;
    private Subscription tableViewSelectionPin, selectedModelItemPin, marketSelectorHeaderIconPin;
    private Button createOfferButton;
    private DropdownMenu dropdownMenu;
    private MenuItem offers, nameAZ, nameZA;
    private CheckBox hideUserMessagesCheckbox;

    public BisqEasyOfferbookView(BisqEasyOfferbookModel model,
                                 BisqEasyOfferbookController controller,
                                 VBox chatMessagesComponent,
                                 Pane channelSidebar) {
        super(model, controller, chatMessagesComponent, channelSidebar);

        bisqEasyOfferbookController = controller;
        bisqEasyOfferbookModel = model;
    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        chatDomainTitle = new Label(Res.get("bisqEasy.offerbook"));
        chatDomainTitle.getStyleClass().add("chat-header-title");

        HBox headerTitle = new HBox(10, chatDomainTitle, channelDescription);
        headerTitle.setAlignment(Pos.BASELINE_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        titleHBox.getChildren().setAll(headerTitle, searchBox, headerDropdownMenu);
    }

    @Override
    protected void configCenterVBox() {
        addMarketSelectionList();
        addChatBox();
    }

    @Override
    protected void configContainerHBox() {
        super.configContainerHBox();

        containerHBox.getChildren().setAll(marketSelectionList, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        hideUserMessagesCheckbox.selectedProperty().bindBidirectional(bisqEasyOfferbookModel.getOfferOnly());
        marketSelectorSearchBox.textProperty().bindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin = EasyBind.subscribe(getModel().getSelectedMarketChannelItem(), selected -> {
            tableView.getSelectionModel().select(selected);
        });
        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });
        marketSelectorHeaderIconPin = EasyBind.subscribe(model.getChannelIconNode(), this::updateMarketSelectorHeaderIcon);

        offers.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketActivity(), offers));
        nameAZ.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketNameAsc(), nameAZ));
        nameZA.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketNameDesc(), nameZA));
        createOfferButton.setOnAction(e -> getController().onCreateOffer());

        sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketActivity(), offers);
    }

    private void sortTableViewColumn(Comparator<MarketChannelItem> comparator, MenuItem menuItem) {
        dropdownMenu.setLabel(menuItem.getText());
        tableView.getSortOrder().clear();
        marketLabelTableColumn.setComparator(comparator);
        tableView.getSortOrder().add(marketLabelTableColumn);
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        hideUserMessagesCheckbox.selectedProperty().unbindBidirectional(bisqEasyOfferbookModel.getOfferOnly());
        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        marketSelectorHeaderIconPin.unsubscribe();

        offers.setOnAction(null);
        nameAZ.setOnAction(null);
        nameZA.setOnAction(null);
        createOfferButton.setOnAction(null);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private BisqEasyOfferbookController getController() {
        return (BisqEasyOfferbookController) controller;
    }

    private void addMarketSelectionList() {
        HBox header = new HBox(channelTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 0, 15));
        header.getStyleClass().add("chat-header-title");

        marketSelectorSearchBox = new SearchBox();
        marketSelectorSearchBox.getStyleClass().add("market-selection-search-box");

        dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.getStyleClass().add("market-selection-dropdown-menu");
        offers = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.offers"));
        nameAZ = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.nameAZ"));
        nameZA = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.nameZA"));
        dropdownMenu.addMenuItems(offers, nameAZ, nameZA);

        HBox subheader = new HBox(marketSelectorSearchBox, Spacer.fillHBox(), dropdownMenu);
        dropdownMenu.setMaxWidth(Double.MAX_VALUE);
        subheader.getStyleClass().add("market-selection-subheader");

        tableView = new BisqTableView<>(getModel().getSortedMarketChannelItems());
        tableView.getStyleClass().add("market-selection-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        tableView.setFixedCellSize(53);
        configTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        createOfferButton = createAndGetCreateOfferButton();
        HBox offerButtonContainer = new HBox(createOfferButton);
        offerButtonContainer.setAlignment(Pos.CENTER);
        offerButtonContainer.setPadding(new Insets(14, 20, 14, 20));

        marketSelectionList = new VBox(header, Layout.hLine(), subheader, tableView, offerButtonContainer);
        marketSelectionList.setPrefWidth(210);
        marketSelectionList.setMinWidth(210);
        marketSelectionList.setFillWidth(true);
        marketSelectionList.getStyleClass().add("chat-container");
    }

    private Button createAndGetCreateOfferButton() {
        Button createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        createOfferButton.setMinWidth(170);

        double height = 42;
        createOfferButton.setMinHeight(height);
        createOfferButton.setMaxHeight(height);
        createOfferButton.setPrefHeight(height);
        return createOfferButton;
    }

    private void configTableView() {
        BisqTableColumn<MarketChannelItem> marketLogoTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .fixWidth(55)
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLogoCellFactory())
                .isSortable(false)
                .build();

        marketLabelTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .minWidth(100)
                .left()
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLabelCellFactory())
                .build();

        tableView.getColumns().add(tableView.getSelectionMarkerColumn());
        tableView.getColumns().add(marketLogoTableColumn);
        tableView.getColumns().add(marketLabelTableColumn);
    }

    private void addChatBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        Label label = new Label(Res.get("bisqEasy.topPane.filter.hideUserMessages"));
        hideUserMessagesCheckbox = new CheckBox();
        HBox checkbox = new HBox(5, label, hideUserMessagesCheckbox);
        checkbox.getStyleClass().add("market-selection-subheader-checkbox");
        checkbox.setAlignment(Pos.CENTER);

        HBox subheaderContent = new HBox(5, checkbox);
        subheaderContent.getStyleClass().add("market-selection-subheader-content");
        HBox.setHgrow(subheaderContent, Priority.ALWAYS);

        HBox subheader = new HBox(subheaderContent);
        subheader.setAlignment(Pos.CENTER);
        subheader.getStyleClass().add("market-selection-subheader");

        chatMessagesComponent.setMinWidth(700);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), subheader, chatMessagesComponent);
        centerVBox.getStyleClass().add("bisq-easy-container");
        centerVBox.setAlignment(Pos.CENTER);
    }

    private void updateMarketSelectorHeaderIcon(Node node) {
        channelTitle.setGraphic(node);
    }
}
