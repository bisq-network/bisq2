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

package bisq.desktop.primary.main.content.wallet.dashboard;

import bisq.common.data.Triple;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletDashboardView extends View<VBox, WalletDashboardModel, WalletDashboardController> {
    private final Button send, receive;
    private final Label balanceLabel;

    public WalletDashboardView(WalletDashboardModel model, WalletDashboardController controller) {
        super(new VBox(20), model, controller);

        root.setSpacing(20);

        Triple<VBox, Label, Label> balanceTriple = createBalanceBox();
        VBox balanceBox = balanceTriple.getFirst();
        balanceLabel = balanceTriple.getSecond();
        VBox.setMargin(balanceBox, new Insets(40, 0, 0, 0));

        send = getCardButton(Res.get("wallet.sendBtc"), "green-card-button");
        receive = getCardButton(Res.get("wallet.receiveBtc"), "grey-card-button");

        HBox boxes = new HBox(25, send, receive);
        boxes.setAlignment(Pos.CENTER);
        HBox.setHgrow(send, Priority.ALWAYS);
        HBox.setHgrow(receive, Priority.ALWAYS);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(balanceBox, boxes);
    }

    @Override
    protected void onViewAttached() {
        balanceLabel.textProperty().bind(model.getFormattedBalanceProperty());
        send.setOnAction(e -> controller.onSend());
        receive.setOnAction(e -> controller.onReceive());
    }

    @Override
    protected void onViewDetached() {
        balanceLabel.textProperty().unbind();
        send.setOnAction(null);
        receive.setOnAction(null);
    }

    private Triple<VBox, Label, Label> createBalanceBox() {
        Label titleLabel = new Label(Res.get("wallet.yourBalance"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(9, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);

        VBox vBox = new VBox(0, titleLabel, hBox);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(30, 0, 30, 0));
        return new Triple<>(vBox, valueLabel, codeLabel);
    }

    private Button getCardButton(String title, String styleClass) {
        Button button = new Button(title);
        button.getStyleClass().add("bisq-text-headline-2");
        button.getStyleClass().setAll(styleClass);
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setPrefWidth(width);
        button.setMinHeight(112);
        return button;
    }
}
