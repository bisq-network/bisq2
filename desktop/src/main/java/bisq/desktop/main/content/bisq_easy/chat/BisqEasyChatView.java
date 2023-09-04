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

import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatModel bisqEasyChatModel;
    private Switch offersOnlySwitch;
    private final Region bisqEasyPrivateTradeChatChannelSelection;
    private final VBox tradeStateViewRoot;
    private final BisqEasyChatController bisqEasyChatController;
    private Subscription isBisqEasyPrivateTradeChatChannelPin;
    private Button createOfferButton;

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
        titleHBox.getStyleClass().add("bisq-easy-chat-header-bg");
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-chat-title-bg");

        channelTitle.setId("chat-messages-headline");

        int iconSize = 20;
        double opacity = 0.3;
        helpButton = BisqIconButton.createIconButton(AwesomeIcon.QUESTION_SIGN, model.getHelpTitle(), iconSize);
        helpButton.setOpacity(opacity);
        infoButton = BisqIconButton.createIconButton(AwesomeIcon.INFO_SIGN, Res.get("chat.topMenu.channelInfoIcon.tooltip"), iconSize);
        infoButton.setOpacity(opacity);

        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 4));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(
                channelTitle,
                Spacer.fillHBox(),
                helpButton,
                infoButton
        );
    }

    protected void configCenterVBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        searchBox.setPrefWidth(200);
        searchBox.setMinHeight(32);
        searchBox.setMaxHeight(32);
        searchBox.getStyleClass().add("small-search-box-light");

        offersOnlySwitch = new Switch();
        offersOnlySwitch.setText(Res.get("bisqEasy.topPane.filter.offersOnly"));

        createOfferButton = new Button();
        createOfferButton.setText(Res.get("offer.createOffer"));
        createOfferButton.setMaxWidth(Double.MAX_VALUE);
        createOfferButton.getStyleClass().add("outlined-button");

        HBox.setMargin(searchBox, new Insets(0.5, 0, 0, 0));
        HBox toolsHBox = new HBox(15, searchBox, offersOnlySwitch, Spacer.fillHBox(), createOfferButton);
        toolsHBox.setAlignment(Pos.CENTER);
        toolsHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        toolsHBox.getStyleClass().add("bisq-easy-chat-tools-bg");

        VBox topPanelVBox = new VBox(10, titleHBox, toolsHBox);

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
    }
}
