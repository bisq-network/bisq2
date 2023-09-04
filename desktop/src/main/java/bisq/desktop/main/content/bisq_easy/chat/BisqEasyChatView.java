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

package bisq.desktop.main.content.bisq_easy.chat;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.overlay.ComboBoxOverlay;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private static double filterPaneHeight;

    private final BisqEasyChatModel bisqEasyChatModel;
    private Switch offersOnlySwitch;
    private final Region bisqEasyPrivateTradeChatChannelSelection;
    private final VBox tradeStateViewRoot;
    private final BisqEasyChatController bisqEasyChatController;
    private Subscription isBisqEasyPrivateTradeChatChannelPin;
    private Button createOfferButton;
    protected Label switchChannelIcon;
    private Button filterButton;
    private Pane filterPane;
    private Subscription showFilterOverlayPin;
    private Subscription filterPaneHeightPin;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Region bisqEasyPublicChatChannelSelection,
                            Region bisqEasyPrivateTradeChatChannelSelection,
                            Region twoPartyPrivateChatChannelSelection,
                            VBox chatMessagesComponent,
                            Pane channelSidebar,
                            VBox tradeStateViewRoot) {
        super(model,
                controller,
                bisqEasyPublicChatChannelSelection,
                twoPartyPrivateChatChannelSelection,
                chatMessagesComponent,
                channelSidebar);

        this.bisqEasyPrivateTradeChatChannelSelection = bisqEasyPrivateTradeChatChannelSelection;
        this.tradeStateViewRoot = tradeStateViewRoot;
        bisqEasyChatController = controller;
        bisqEasyChatModel = model;

        root.setPadding(new Insets(0, 0, -67, 0));
    }

    protected void configLeftVBox(Region publicChannelSelection,
                                  Region twoPartyPrivateChatChannelSelection) {
    }

    protected void configTitleHBox() {
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-chat-title-bg");
        titleHBox.setCursor(Cursor.HAND);

        switchChannelIcon = Icons.getIcon(AwesomeIcon.CHEVRON_DOWN, "12");
        switchChannelIcon.setCursor(Cursor.HAND);
        switchChannelIcon.setPadding(new Insets(0, 0, 0, -2));
        Tooltip tooltip = new BisqTooltip(Res.get("bisqEasy.channelSelection.public.switchMarketChannel"));
        tooltip.getStyleClass().add("dark-tooltip");
        switchChannelIcon.setTooltip(tooltip);

        channelTitle.setId("chat-messages-headline");

        double scale = 1.15;
        helpButton = BisqIconButton.createIconButton("icon-help");
        helpButton.setScaleX(scale);
        helpButton.setScaleY(scale);
        infoButton = BisqIconButton.createIconButton("icon-info");
        infoButton.setScaleX(scale);
        infoButton.setScaleY(scale);

        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 4));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(
                channelTitle, switchChannelIcon,
                Spacer.fillHBox(),
                helpButton, infoButton
        );
    }

    protected void configCenterVBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        searchBox.setMaxWidth(200);
        searchBox.setMinHeight(32);
        searchBox.setMaxHeight(32);
        searchBox.getStyleClass().add("small-search-box-light");

        filterButton = new Button(Res.get("bisqEasy.topPane.filter"));
        ImageView filterIcon = ImageUtil.getImageViewById("filter");
        filterIcon.setOpacity(0.3);
        filterButton.setAlignment(Pos.CENTER_LEFT);
        filterButton.setTextAlignment(TextAlignment.LEFT);
        filterButton.setPadding(new Insets(0, -110, 0, 0));
        filterButton.setGraphic(filterIcon);
        filterButton.setGraphicTextGap(10);
        filterButton.getStyleClass().add("grey-transparent-outlined-button");
        filterButton.setStyle("-fx-padding: 5 12 5 12;");

        offersOnlySwitch = new Switch();
        offersOnlySwitch.setText(Res.get("bisqEasy.topPane.filter.offersOnly"));

        createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().add("outlined-button");

        Label filterLabel = new Label(Res.get("bisqEasy.topPane.filter"));
        filterLabel.getStyleClass().add("bisq-easy-chat-filter-headline");
        closeFilterButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeFilterButton, new Insets(0, 0, 0, 0));
        HBox headlineAndCloseButton = new HBox(filterLabel, Spacer.fillHBox(), closeFilterButton);
        headlineAndCloseButton.setAlignment(Pos.CENTER);

        filterPane = new VBox(20, headlineAndCloseButton, searchBox, offersOnlySwitch);
        filterPane.getStyleClass().add("bisq-easy-chat-filter-panel-bg");
        filterPane.setPadding(new Insets(10));

        HBox.setMargin(searchBox, new Insets(0.5, 0, 0, 0));
        HBox toolsHBox = new HBox(15, filterButton, Spacer.fillHBox(), createOfferButton);
        toolsHBox.setAlignment(Pos.CENTER);
        toolsHBox.setPadding(new Insets(12.5, 25, 12.5, 25));

        VBox topPanelVBox = new VBox(titleHBox, toolsHBox, filterPane);
        topPanelVBox.getStyleClass().add("bisq-easy-chat-tools-bg");

        chatMessagesComponent.setMinWidth(700);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(topPanelVBox, Layout.hLine(), chatMessagesComponent);
    }

    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        root.getChildren().add(containerHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        topSeparator.visibleProperty().bind(bisqEasyChatModel.getTopSeparatorVisible());
        topSeparator.managedProperty().bind(bisqEasyChatModel.getTopSeparatorVisible());
        createOfferButton.visibleProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        createOfferButton.managedProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        offersOnlySwitch.visibleProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        offersOnlySwitch.managedProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        offersOnlySwitch.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());

        isBisqEasyPrivateTradeChatChannelPin = EasyBind.subscribe(bisqEasyChatModel.getIsBisqEasyPrivateTradeChatChannel(),
                isBisqEasyPrivateTradeChatChannel -> {
                    if (isBisqEasyPrivateTradeChatChannel) {
                        if (!chatMessagesComponent.getChildren().contains(tradeStateViewRoot)) {
                            chatMessagesComponent.getChildren().add(0, tradeStateViewRoot);
                            VBox.setMargin(tradeStateViewRoot, new Insets(2, 25, 25, 25));
                        }
                    } else {
                        chatMessagesComponent.getChildren().remove(tradeStateViewRoot);
                    }
                });


        createOfferButton.setOnAction(e -> bisqEasyChatController.onCreateOffer());
        filterButton.setOnAction(e -> bisqEasyChatController.onToggleFilter());
        closeFilterButton.setOnAction(e -> bisqEasyChatController.onCloseFilter());
        if (filterPaneHeight == 0) {
            filterPaneHeightPin = EasyBind.subscribe(filterPane.heightProperty(), h -> {
                if (h.doubleValue() > 0) {
                    filterPaneHeight = h.doubleValue();
                    double target = bisqEasyChatModel.getShowFilterOverlay().get() ? filterPaneHeight : 0;
                    filterPane.setMinHeight(target);
                    filterPane.setMaxHeight(target);
                    filterPaneHeightPin.unsubscribe();
                }
            });
        } else {
            double target = bisqEasyChatModel.getShowFilterOverlay().get() ? filterPaneHeight : 0;
            filterPane.setMinHeight(target);
            filterPane.setMaxHeight(target);
        }
        showFilterOverlayPin = EasyBind.subscribe(bisqEasyChatModel.getShowFilterOverlay(),
                showFilterOverlay -> {
                    if (filterPaneHeight > 0) {
                        if (showFilterOverlay) {
                            filterButton.setText(Res.get("bisqEasy.topPane.closeFilter"));
                            root.getScene().setOnKeyReleased(keyEvent -> KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, bisqEasyChatController::onCloseFilter));
                        } else {
                            filterButton.setText(Res.get("bisqEasy.topPane.filter"));
                            root.getScene().setOnKeyReleased(null);
                        }
                        double target = showFilterOverlay ? filterPaneHeight : 0;
                        if (filterPane.getMaxHeight() != target) {
                            double start = showFilterOverlay ? 0 : filterPaneHeight;
                            Transitions.animateMaxHeight(filterPane, start, target, Transitions.DEFAULT_DURATION / 4d, () -> {
                            });
                        }
                    }
                });

        switchChannelIcon.setOnMouseClicked(e -> onSwitchChannel());
        titleHBox.setOnMouseClicked(e -> {
            // On click of icon we show channel info
            if (e.getTarget() instanceof Text) {
                onSwitchChannel();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        topSeparator.visibleProperty().unbind();
        topSeparator.managedProperty().unbind();

        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();
        offersOnlySwitch.visibleProperty().unbind();
        offersOnlySwitch.managedProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().unbind();
        offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
        isBisqEasyPrivateTradeChatChannelPin.unsubscribe();

        createOfferButton.setOnAction(null);
        switchChannelIcon.setOnMouseClicked(null);
        titleHBox.setOnMouseClicked(null);
        filterButton.setOnAction(null);
        closeFilterButton.setOnAction(null);
    }

    private void onSwitchChannel() {
        new ComboBoxOverlay<>(switchChannelIcon,
                bisqEasyChatModel.getSortedMarketChannelItems(),
                c -> getMarketListCell(),
                bisqEasyChatController::onSwitchMarketChannel,
                Res.get("bisqEasy.channelSelection.public.switchMarketChannel").toUpperCase(),
                Res.get("action.search"),
                350, 5, 23, 31.5)
                .show();
    }

    protected ListCell<MarketChannelItem> getMarketListCell() {
        return new ListCell<>() {
            final Label label = new Label();
            final HBox hBox = new HBox();
            final Badge badge = new Badge(hBox);

            {
                setCursor(Cursor.HAND);
                setPrefHeight(40);
                setPadding(new Insets(0, 0, -20, 0));

                badge.setTooltip(Res.get("bisqEasy.channelSelection.public.numMessages"));
                badge.setPosition(Pos.CENTER_RIGHT);

                hBox.setSpacing(10);
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getChildren().addAll(label, Spacer.fillHBox());
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    label.setText(item.toString());
                    int numMessages = bisqEasyChatController.getNumMessages(item.getMarket());
                    badge.setText(numMessages > 0 ? String.valueOf(numMessages) : "");
                    setGraphic(badge);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

}
