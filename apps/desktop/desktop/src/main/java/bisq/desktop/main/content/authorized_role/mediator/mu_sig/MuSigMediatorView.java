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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig;

import bisq.desktop.CssConfig;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MuSigMediatorView extends View<ScrollPane, MuSigMediatorModel, MuSigMediatorController> {
    private final VBox centerVBox, chatVBox, chatMessagesComponent, chatUnavailablePlaceholder;
    private final Button toggleChatWindowButton;
    private final MuSigMediationTableView muSigMediationTableView;
    private final Label chatUnavailableTitle, chatUnavailableDescription;
    private Subscription noOpenCasesPin, chatWindowPin, chatAvailablePin, chatUnavailableTitlePin, chatUnavailableDescriptionPin;
    private Stage detachedChatWindow;

    public MuSigMediatorView(MuSigMediatorModel model,
                             MuSigMediatorController controller,
                             HBox mediationCaseHeader,
                             VBox chatMessagesComponent) {

        super(new ScrollPane(), model, controller);

        muSigMediationTableView = new MuSigMediationTableView(model, controller);

        toggleChatWindowButton = new Button();
        toggleChatWindowButton.setGraphicTextGap(10);
        toggleChatWindowButton.getStyleClass().add("outlined-button");
        toggleChatWindowButton.setMinWidth(120);
        toggleChatWindowButton.setStyle("-fx-padding: 5 16 5 16");
        mediationCaseHeader.getChildren().add(toggleChatWindowButton);

        this.chatMessagesComponent = chatMessagesComponent;
        this.chatMessagesComponent.setMinHeight(200);
        this.chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));
        chatUnavailableTitle = new Label();
        chatUnavailableDescription = new Label();
        chatUnavailableTitle.getStyleClass().add("large-text");
        chatUnavailableTitle.setTextAlignment(TextAlignment.CENTER);
        chatUnavailableDescription.getStyleClass().add("normal-text");
        chatUnavailableDescription.setTextAlignment(TextAlignment.CENTER);
        chatUnavailablePlaceholder = new VBox(10, chatUnavailableTitle, chatUnavailableDescription);
        chatUnavailablePlaceholder.setAlignment(Pos.CENTER);
        chatUnavailablePlaceholder.getStyleClass().add("chat-container-placeholder-text");
        chatUnavailablePlaceholder.setStyle("-fx-padding: 0;");

        StackPane chatContentSlot = new StackPane();
        chatContentSlot.setAlignment(Pos.CENTER);
        chatContentSlot.setMinHeight(200);
        chatContentSlot.setMaxWidth(Double.MAX_VALUE);
        chatContentSlot.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(chatContentSlot, Priority.ALWAYS);
        VBox.setMargin(chatContentSlot, new Insets(0, 30, 15, 30));
        chatContentSlot.getChildren().addAll(chatMessagesComponent, chatUnavailablePlaceholder);
        chatUnavailablePlaceholder.setVisible(false);
        chatUnavailablePlaceholder.setManaged(false);

        chatVBox = new VBox(mediationCaseHeader, Layout.hLine(), chatContentSlot);
        chatVBox.setAlignment(Pos.CENTER);
        chatVBox.getStyleClass().add("bisq-easy-container");

        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        VBox.setMargin(muSigMediationTableView, new Insets(0, 0, 10, 0));
        centerVBox = new VBox(muSigMediationTableView, chatVBox);
        centerVBox.setAlignment(Pos.TOP_CENTER);
        centerVBox.setFillWidth(true);
        centerVBox.setPadding(new Insets(25, 0, 0, 0));

        VBox.setVgrow(centerVBox, Priority.ALWAYS);
        root.setContent(centerVBox);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
    }

    @Override
    protected void onViewAttached() {
        muSigMediationTableView.initialize();
        noOpenCasesPin = EasyBind.subscribe(model.getNoOpenCases(), noOpenCases -> {
            chatVBox.setVisible(!noOpenCases);
            chatVBox.setManaged(!noOpenCases);
        });
        chatAvailablePin = EasyBind.subscribe(model.getChatAvailable(), chatAvailable -> {
            chatMessagesComponent.setVisible(chatAvailable);
            chatMessagesComponent.setManaged(chatAvailable);
            chatUnavailablePlaceholder.setVisible(!chatAvailable);
            chatUnavailablePlaceholder.setManaged(!chatAvailable);
        });
        chatUnavailableTitlePin = EasyBind.subscribe(model.getChatUnavailableTitle(), chatUnavailableTitle::setText);
        chatUnavailableDescriptionPin = EasyBind.subscribe(model.getChatUnavailableDescription(), chatUnavailableDescription::setText);
        chatWindowPin = EasyBind.subscribe(model.getChatWindow(), this::chatWindowChanged);
        toggleChatWindowButton.setOnAction(e -> controller.onToggleChatWindow());
    }

    @Override
    protected void onViewDetached() {
        muSigMediationTableView.dispose();
        noOpenCasesPin.unsubscribe();
        chatAvailablePin.unsubscribe();
        chatUnavailableTitlePin.unsubscribe();
        chatUnavailableDescriptionPin.unsubscribe();
        chatWindowPin.unsubscribe();
        toggleChatWindowButton.setOnAction(null);
    }

    private void chatWindowChanged(Stage chatWindow) {
        if (detachedChatWindow != null) {
            detachedChatWindow.titleProperty().unbind();
            detachedChatWindow.setOnCloseRequest(null);
            detachedChatWindow.setScene(null);
            detachedChatWindow = null;
        }

        if (chatWindow == null) {
            ImageView icon = ImageUtil.getImageViewById("detach");
            toggleChatWindowButton.setText(Res.get("bisqEasy.openTrades.chat.detach"));
            toggleChatWindowButton.setTooltip(new BisqTooltip(Res.get("bisqEasy.openTrades.chat.detach.tooltip")));
            toggleChatWindowButton.setGraphic(icon);
            if (!centerVBox.getChildren().contains(chatVBox)) {
                centerVBox.getChildren().add(chatVBox);
            }
        } else {
            detachedChatWindow = chatWindow;
            ImageView icon = ImageUtil.getImageViewById("attach");
            toggleChatWindowButton.setText(Res.get("bisqEasy.openTrades.chat.attach"));
            toggleChatWindowButton.setTooltip(new BisqTooltip(Res.get("bisqEasy.openTrades.chat.attach.tooltip")));
            toggleChatWindowButton.setGraphic(icon);

            chatWindow.titleProperty().bind(model.getChatWindowTitle());
            ImageUtil.addAppIcons(chatWindow);
            chatWindow.initModality(Modality.NONE);

            // We open the window at the button position (need to be done before we remove the chatVBox
            // TODO we could persist the position and size of the window and use it for next time opening...
            Point2D windowPoint = new Point2D(root.getScene().getWindow().getX(), root.getScene().getWindow().getY());
            Point2D scenePoint = new Point2D(root.getScene().getX(), root.getScene().getY());
            Point2D buttonPoint = toggleChatWindowButton.localToScene(0.0, 0.0);
            double x = Math.round(windowPoint.getX() + scenePoint.getX() + buttonPoint.getX());
            double y = Math.round(windowPoint.getY() + scenePoint.getY() + buttonPoint.getY());
            chatWindow.setX(x);
            chatWindow.setY(y);
            chatWindow.setMinWidth(600);
            chatWindow.setMinHeight(400);
            chatWindow.setWidth(1100);
            chatWindow.setHeight(700);

            chatWindow.setOnCloseRequest(event -> {
                event.consume();
                chatWindow.titleProperty().unbind();
                controller.onCloseChatWindow();
                chatWindow.hide();
            });

            chatWindow.show();

            centerVBox.getChildren().remove(chatVBox);
            UIThread.runOnNextRenderFrame(() -> {
                if (detachedChatWindow != chatWindow || model.getChatWindow().get() != chatWindow) {
                    return;
                }

                ScrollPane scrollPane = new ScrollPane(chatVBox);
                scrollPane.setFitToHeight(true);
                scrollPane.setFitToWidth(true);
                Layout.pinToAnchorPane(scrollPane, 0, 0, 0, 0);
                AnchorPane windowRoot = new AnchorPane(scrollPane);
                windowRoot.getStyleClass().add("bisq-popup");
                Scene scene = new Scene(windowRoot);
                CssConfig.addAllCss(scene);
                chatWindow.setScene(scene);

                // Avoid flicker
                chatWindow.setOpacity(0);
                UIThread.runOnNextRenderFrame(() -> chatWindow.setOpacity(1));
            });
        }
    }
}
