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

package bisq.desktop.main.content.mu_sig.trade.trade_limits.simulation;

import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.MathUtils;
import bisq.desktop.common.converters.LongStringConverter;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqHyperlink;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.mu_sig.trade.trade_limits.TradeLimitsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigTradeLimitsSimulationView extends View<VBox, MuSigTradeLimitsSimulationModel, MuSigTradeLimitsSimulationController> {
    private final Button backButton, closeButton;
    private final Hyperlink learnMore;
    private final MaterialTextField tradeLimit, rateLimit;
    private final SliderWithValue accountAge;
    private final AutoCompleteComboBox<FiatPaymentRail> paymentRailSelection;
    private final Label paymentRailMaxLimit;
    private final Switch hasBisq1AccountAgeWitness;

    public MuSigTradeLimitsSimulationView(MuSigTradeLimitsSimulationModel model,
                                          MuSigTradeLimitsSimulationController controller) {
        super(new VBox(), model, controller);

        root.setPadding(new Insets(20, 0, 0, 0));

        Label headline = TradeLimitsViewUtils.getHeadline(Res.get("muSig.trade.limits.simulation.headline"));
        Label info = TradeLimitsViewUtils.getInfo(Res.get("muSig.trade.limits.simulation.info"));

        GridPane gridPane = new GridPane(15, 15);
        GridPaneUtil.setGridPaneTwoColumnsConstraints(gridPane);

        int rowIndex = 0;

        paymentRailSelection = new AutoCompleteComboBox<>(model.getFiatPaymentRails(), Res.get("muSig.trade.limits.simulation.fiatRail"));
        paymentRailSelection.setMaxWidth(Double.MAX_VALUE);
        paymentRailSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(FiatPaymentRail fiatPaymentRail) {
                return fiatPaymentRail != null ? fiatPaymentRail.getShortDisplayString() : "";
            }

            @Override
            public FiatPaymentRail fromString(String string) {
                return null;
            }
        });

        paymentRailMaxLimit = new Label();

        VBox paymentRailSelectionBox = new VBox(10, paymentRailSelection, paymentRailMaxLimit);

        GridPane.setHgrow(paymentRailSelectionBox, Priority.ALWAYS);
        gridPane.add(paymentRailSelectionBox, 0, rowIndex);

        // Row 2
        rowIndex++;
        accountAge = new SliderWithValue(0, model.getMinAccountAge(), model.getMaxAccountAge(),
                "muSig.trade.limits.simulation.accountAge",
                value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                new LongStringConverter(0),
                1);
        GridPane.setHgrow(accountAge.getViewRoot(), Priority.ALWAYS);
        gridPane.add(accountAge.getViewRoot(), 0, rowIndex);

        hasBisq1AccountAgeWitness = new Switch(Res.get("muSig.trade.limits.simulation.hasBisq1AccountAgeWitness"));

        GridPane.setHgrow(hasBisq1AccountAgeWitness, Priority.ALWAYS);
        GridPane.setMargin(hasBisq1AccountAgeWitness, new Insets(0, 0, 37.5, 0));
        gridPane.add(hasBisq1AccountAgeWitness, 1, rowIndex);

        tradeLimit = new MaterialTextField(Res.get("muSig.trade.limits.simulation.tradeLimit"));
        tradeLimit.setEditable(false);
        rateLimit = new MaterialTextField(Res.get("muSig.trade.limits.simulation.rateLimit"));
        rateLimit.setEditable(false);

        // Row 3
        rowIndex++;
        gridPane.add(tradeLimit, 0, rowIndex, 1, 1);
        gridPane.add(rateLimit, 1, rowIndex, 1, 1);


        backButton = new Button(Res.get("action.back"));

        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        learnMore = new BisqHyperlink(Res.get("action.learnMore"), "https://bisq.wiki/MuSig-tradelimits");

        HBox buttons = new HBox(20, backButton, closeButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 10, 0));
        VBox.setMargin(info, new Insets(0, 0, 10, 0));
        VBox.setMargin(buttons, new Insets(25, 0, 0, 0));
        VBox contentBox = new VBox(5,headline,
                info,
                gridPane,
                buttons);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");

        root.getChildren().addAll(contentBox);
    }

    @Override
    protected void onViewAttached() {
        model.getAccountAge().bind(accountAge.valueProperty());

        paymentRailMaxLimit.textProperty().bind(model.getFiatPaymentRailMaxLimit());
        tradeLimit.textProperty().bind(model.getTradeLimit());
        rateLimit.textProperty().bind(model.getRateLimit());

        paymentRailSelection.getSelectionModel().select(model.getSelectedFiatPaymentRail().get());
        paymentRailSelection.setOnChangeConfirmed(e -> {
            if (paymentRailSelection.getSelectionModel().getSelectedItem() == null) {
                paymentRailSelection.getSelectionModel().select(model.getSelectedFiatPaymentRail().get());
                return;
            }
            controller.onSelectFiatPaymentRail(paymentRailSelection.getSelectionModel().getSelectedItem());
        });

        hasBisq1AccountAgeWitness.setOnAction(e -> controller.onHasBisq1AccountAgeWitnessChanged(hasBisq1AccountAgeWitness.isSelected()));

        backButton.setOnAction(e -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());
        learnMore.setOnAction(e -> controller.onLearnMore());

        UIThread.runOnNextRenderFrame(root::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        model.getAccountAge().unbind();
        paymentRailMaxLimit.textProperty().unbind();
        tradeLimit.textProperty().unbind();
        rateLimit.textProperty().unbind();

        paymentRailSelection.setOnChangeConfirmed(null);
        hasBisq1AccountAgeWitness.setOnAction(null);

        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
