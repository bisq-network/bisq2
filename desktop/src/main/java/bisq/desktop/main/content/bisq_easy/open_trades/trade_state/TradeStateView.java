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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final static double HEADER_HEIGHT = 61;

    private final HBox phaseAndInfoHBox;
    private final Button closeButton;
    private final Triple<Text, Text, VBox> leftAmount, rightAmount, tradeId;
    private final UserProfileDisplay peersUserProfileDisplay;
    private Subscription stateInfoVBoxPin;
    private Subscription userProfilePin;

    public TradeStateView(bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateModel model,
                          bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController controller,
                          VBox tradePhaseBox) {
        super(new VBox(0), model, controller);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        Label peerDescription = new Label(Res.get("bisqEasy.tradeState.header.peer").toUpperCase());
        peerDescription.getStyleClass().add("bisq-easy-open-trades-header-description");
        peersUserProfileDisplay = new UserProfileDisplay(25);
        peersUserProfileDisplay.setMinWidth(120);
        VBox.setMargin(peerDescription, new Insets(2, 0, 3, 0));
        VBox peerVBox = new VBox(0, peerDescription, peersUserProfileDisplay);
        peerVBox.setAlignment(Pos.CENTER_LEFT);
        leftAmount = getValueBox(null);
        rightAmount = getValueBox(null);
        tradeId = getValueBox(Res.get("bisqEasy.tradeState.header.tradeId"));

        closeButton = new Button();
        closeButton.setMinWidth(150);
        closeButton.getStyleClass().add("outlined-button");

        HBox header = new HBox(40,
                peerVBox,
                leftAmount.getThird(),
                rightAmount.getThird(),
                tradeId.getThird(),
                Spacer.fillHBox(),
                closeButton);

        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 30, 0, 30));

        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox vBox = new VBox(header, Layout.hLine(), phaseAndInfoHBox);
        vBox.getStyleClass().add("bisq-easy-container");

        root.getChildren().add(vBox);
    }


    @Override
    protected void onViewAttached() {
        leftAmount.getFirst().textProperty().bind(model.getLeftAmountDescription());
        leftAmount.getSecond().textProperty().bind(model.getLeftAmount());
        rightAmount.getFirst().textProperty().bind(model.getRightAmountDescription());
        rightAmount.getSecond().textProperty().bind(model.getRightAmount());
        tradeId.getSecond().textProperty().bind(model.getTradeId());

        closeButton.textProperty().bind(model.getCloseButtonText());

        userProfilePin = EasyBind.subscribe(model.getPeersUserProfile(), userProfile -> {
            if (userProfile != null) {
                peersUserProfileDisplay.setUserProfile(userProfile);
                peersUserProfileDisplay.applyReputationScore(model.getPeersReputationScore());
            }
        });

        stateInfoVBoxPin = EasyBind.subscribe(model.getStateInfoVBox(),
                stateInfoVBox -> {
                    if (phaseAndInfoHBox.getChildren().size() == 2) {
                        phaseAndInfoHBox.getChildren().remove(1);
                    }
                    if (stateInfoVBox != null) {
                        HBox.setHgrow(stateInfoVBox, Priority.ALWAYS);
                        HBox.setMargin(stateInfoVBox, new Insets(20, 0, 0, 0));
                        phaseAndInfoHBox.getChildren().add(stateInfoVBox);
                    }
                });

        closeButton.setOnAction(e -> controller.onCloseTrade());
    }

    @Override
    protected void onViewDetached() {
        leftAmount.getFirst().textProperty().unbind();
        leftAmount.getSecond().textProperty().unbind();
        rightAmount.getFirst().textProperty().unbind();
        rightAmount.getSecond().textProperty().unbind();
        tradeId.getSecond().textProperty().unbind();

        closeButton.textProperty().unbind();

        userProfilePin.unsubscribe();
        stateInfoVBoxPin.unsubscribe();

        closeButton.setOnAction(null);
        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }

    private Triple<Text, Text, VBox> getValueBox(@Nullable String description) {
        Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
        descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
        Text valueLabel = new Text();
        valueLabel.getStyleClass().add("bisq-easy-open-trades-header-value");
        VBox.setMargin(descriptionLabel, new Insets(2, 0, 0, 0));
        VBox vBox = new VBox(2, descriptionLabel, valueLabel);
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setMinHeight(HEADER_HEIGHT);
        vBox.setMaxHeight(HEADER_HEIGHT);
        return new Triple<>(descriptionLabel, valueLabel, vBox);
    }

}
