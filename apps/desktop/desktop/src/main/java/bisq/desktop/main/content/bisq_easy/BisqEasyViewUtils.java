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

package bisq.desktop.main.content.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.data.Quadruple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableView;
import bisq.security.DigestUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.math.BigInteger;
import java.util.List;

public class BisqEasyViewUtils {
    public static final String POSITIVE_NUMERIC_WITH_DECIMAL_REGEX = "\\d*([.,]\\d*)?";
    public static final String NUMERIC_WITH_DECIMAL_REGEX = "-?\\d*([.,]\\d*)?";
    private static final String[] customPaymentIconIds = {
            "CUSTOM_PAYMENT_1", "CUSTOM_PAYMENT_2", "CUSTOM_PAYMENT_3",
            "CUSTOM_PAYMENT_4", "CUSTOM_PAYMENT_5", "CUSTOM_PAYMENT_6"};

    public static Quadruple<Label, HBox, AnchorPane, VBox> getTableViewContainer(String headline,
                                                                                 BisqTableView<?> tableView) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");
        HBox header = new HBox(10, headlineLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.getStyleClass().add("chat-container-header");

        AnchorPane tableViewAnchorPane = new AnchorPane(tableView);
        Layout.pinToAnchorPane(tableView, 0, 0, 0, 0);

        VBox.setMargin(tableViewAnchorPane, new Insets(0, 30, 15, 30));
        VBox.setVgrow(tableViewAnchorPane, Priority.ALWAYS);
        VBox vBox = new VBox(header, Layout.hLine(), tableViewAnchorPane);
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("bisq-easy-container");

        return new Quadruple<>(headlineLabel, header, tableViewAnchorPane, vBox);
    }

    public static StackPane getCustomPaymentMethodIcon(String customPaymentMethod) {
        char initial = customPaymentMethod.charAt(0);

        Label initialLabel = new Label(String.valueOf(initial).toUpperCase());
        initialLabel.getStyleClass().add("bisq-easy-custom-payment-icon");

        int deterministicInt = Math.abs(new BigInteger(DigestUtil.sha256(customPaymentMethod.getBytes())).intValue());
        int iconIndex = deterministicInt % customPaymentIconIds.length;
        ImageView customPaymentIcon = ImageUtil.getImageViewById(customPaymentIconIds[iconIndex]);

        StackPane stackPane = new StackPane(customPaymentIcon, initialLabel);
        stackPane.setAlignment(Pos.CENTER);
        return stackPane;
    }

    public static HBox getPaymentAndSettlementMethodsBox(List<FiatPaymentMethod> paymentMethods,
                                                         List<BitcoinPaymentMethod> settlementMethods) {
        HBox hBox = new HBox(8);
        for (FiatPaymentMethod paymentMethod : paymentMethods) {
            hBox.getChildren().add(createMethodLabel(paymentMethod));
        }

        ImageView icon = ImageUtil.getImageViewById("interchangeable-grey");
        Label interchangeableIcon = new Label();
        interchangeableIcon.setGraphic(icon);
        interchangeableIcon.setPadding(new Insets(0, 0, 1, 0));
        hBox.getChildren().add(interchangeableIcon);

        for (BitcoinPaymentMethod settlementMethod : settlementMethods) {
            Label label = createMethodLabel(settlementMethod);
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.2);
            label.setEffect(colorAdjust);
            hBox.getChildren().add(label);
        }
        return hBox;
    }

    private static Label createMethodLabel(PaymentMethod<?> paymentMethod) {
        Node icon = !paymentMethod.isCustomPaymentMethod()
                ? ImageUtil.getImageViewById(paymentMethod.getName())
                : BisqEasyViewUtils.getCustomPaymentMethodIcon(paymentMethod.getDisplayString());
        Label label = new Label();
        label.setGraphic(icon);
        label.setTooltip(new BisqTooltip(paymentMethod.getDisplayString()));
        return label;
    }
}
