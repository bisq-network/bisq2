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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat;

import bisq.account.bisqeasy.BisqEasyPaymentAccount;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.primary.main.content.chat.ChatView;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatController bisqEasyChatController;
    private final BisqEasyChatModel bisqEasyChatModel;
    private final Switch offersOnlySwitch;
    private final Button createOfferButton, sendBtcAddressButton, sendPaymentAccountButton, openDisputeButton;
    private final Region bisqEasyPrivateTradeChatChannelSelection;
    private final AutoCompleteComboBox<BisqEasyPaymentAccount> paymentAccountsComboBox;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Region bisqEasyPublicChatChannelSelection,
                            Region bisqEasyPrivateTradeChatChannelSelection,
                            Region twoPartyPrivateChatChannelSelection,
                            Pane chatMessagesComponent,
                            HBox chatMessagesBottomHBox,
                            Pane channelSidebar) {
        super(model,
                controller,
                bisqEasyPublicChatChannelSelection,
                twoPartyPrivateChatChannelSelection,
                chatMessagesComponent,
                channelSidebar);

        this.bisqEasyPrivateTradeChatChannelSelection = bisqEasyPrivateTradeChatChannelSelection;

        left.getChildren().add(1, Layout.separator());
        left.getChildren().add(2, bisqEasyPrivateTradeChatChannelSelection);

        bisqEasyChatController = controller;
        bisqEasyChatModel = model;

        offersOnlySwitch = new Switch();
        offersOnlySwitch.setText(Res.get("bisqEasy.filter.offersOnly"));

        centerToolbar.getChildren().add(2, offersOnlySwitch);

        createOfferButton = new Button(Res.get("createOffer"));
        createOfferButton.setMaxWidth(Double.MAX_VALUE);
        createOfferButton.setMinHeight(32);
        createOfferButton.setDefaultButton(true);
        createOfferButton.setMinWidth(120);

        sendBtcAddressButton = new Button(Res.get("bisqEasy.sendBtcAddress"));
        sendBtcAddressButton.getStyleClass().add("default-button");
        sendBtcAddressButton.setStyle("-fx-label-padding: 0 -20 0 -20");
        sendBtcAddressButton.setMinHeight(32);
        sendBtcAddressButton.setMinWidth(160);
        sendBtcAddressButton.setTooltip(new Tooltip(Res.get("bisqEasy.sendBtcAddress.tooltip")));

        sendPaymentAccountButton = new Button(Res.get("bisqEasy.sendPaymentAccount"));
        sendPaymentAccountButton.getStyleClass().add("default-button");
        sendPaymentAccountButton.setStyle("-fx-label-padding: 0 -20 0 -20");
        sendPaymentAccountButton.setMinHeight(32);
        sendPaymentAccountButton.setMinWidth(160);
        sendPaymentAccountButton.setTooltip(new Tooltip(Res.get("bisqEasy.sendPaymentAccount.tooltip")));

        paymentAccountsComboBox = new AutoCompleteComboBox<>(model.getPaymentAccounts());
        paymentAccountsComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BisqEasyPaymentAccount object) {
                return object != null ? object.getName() : "";
            }

            @Override
            public BisqEasyPaymentAccount fromString(String string) {
                return null;
            }
        });

        openDisputeButton = new Button(Res.get("bisqEasy.openDispute"));
        openDisputeButton.getStyleClass().add("default-button");
        openDisputeButton.setStyle("-fx-label-padding: 0 -20 0 -20");
        openDisputeButton.setMinHeight(32);
        openDisputeButton.setMinWidth(110);

        chatMessagesBottomHBox.getChildren().addAll(createOfferButton, sendBtcAddressButton, sendPaymentAccountButton, openDisputeButton);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                // chatMessagesComponent is VBox
                VBox.setMargin(childRoot, new Insets(25, 25, 25, 25));
                chatMessagesComponent.getChildren().add(0, childRoot);
                UIThread.runOnNextRenderFrame(() -> Transitions.transitContentViews(oldValue, newValue));
            } else if (oldValue instanceof TradeGuideView) {
                chatMessagesComponent.getChildren().remove(0);
            }
        });
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        offersOnlySwitch.visibleProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        offersOnlySwitch.managedProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        createOfferButton.visibleProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        createOfferButton.managedProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        openDisputeButton.visibleProperty().bind(bisqEasyChatModel.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(bisqEasyChatModel.getOpenDisputeButtonVisible());
        sendBtcAddressButton.visibleProperty().bind(bisqEasyChatModel.getSendBtcAddressButtonVisible());
        sendBtcAddressButton.managedProperty().bind(bisqEasyChatModel.getSendBtcAddressButtonVisible());
        sendPaymentAccountButton.visibleProperty().bind(bisqEasyChatModel.getSendPaymentAccountButtonVisible());
        sendPaymentAccountButton.managedProperty().bind(bisqEasyChatModel.getSendPaymentAccountButtonVisible());
        openDisputeButton.disableProperty().bind(bisqEasyChatModel.getOpenDisputeDisabled());
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());

        createOfferButton.setOnAction(e -> bisqEasyChatController.onCreateOffer());
        sendBtcAddressButton.setOnAction(e -> bisqEasyChatController.onSendBtcAddress());
        sendPaymentAccountButton.setOnAction(e -> bisqEasyChatController.onSendPaymentAccount());
        openDisputeButton.setOnAction(e -> bisqEasyChatController.onRequestMediation());

        paymentAccountsComboBox.setOnChangeConfirmed(e -> {
            if (paymentAccountsComboBox.getSelectionModel().getSelectedItem() == null) {
                paymentAccountsComboBox.getSelectionModel().select(bisqEasyChatModel.getSelectedAccount());
                return;
            }
            bisqEasyChatController.onPaymentAccountSelected(paymentAccountsComboBox.getSelectionModel().getSelectedItem());
        });


        offersOnlySwitch.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        offersOnlySwitch.visibleProperty().unbind();
        offersOnlySwitch.managedProperty().unbind();
        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();
        openDisputeButton.visibleProperty().unbind();
        openDisputeButton.managedProperty().unbind();
        sendBtcAddressButton.visibleProperty().unbind();
        sendBtcAddressButton.managedProperty().unbind();
        sendPaymentAccountButton.visibleProperty().unbind();
        sendPaymentAccountButton.managedProperty().unbind();
        openDisputeButton.disableProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().unbind();

        createOfferButton.setOnAction(null);
        sendBtcAddressButton.setOnAction(null);
        sendPaymentAccountButton.setOnAction(null);
        openDisputeButton.setOnAction(null);

        paymentAccountsComboBox.setOnChangeConfirmed(null);

        offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
    }
}
