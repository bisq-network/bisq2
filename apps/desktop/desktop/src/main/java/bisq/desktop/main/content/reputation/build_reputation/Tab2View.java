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

package bisq.desktop.main.content.reputation.build_reputation;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqHyperlink;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class Tab2View<M extends Tab2Model, C extends Tab2Controller<?, ?, ?>> extends View<VBox, Tab2Model, Tab2Controller<?, ?, ?>> {
    private final Button backButton, nextButton;
    private final Hyperlink learnMore;
    protected final MaterialTextField formulaTradeLimit;
    private final HBox buttons;
    private Subscription reducePaddingPin;

    public Tab2View(Tab2Model model,
                    Tab2Controller<?, ?, ?> controller,
                    VBox simulation) {
        super(new VBox(), model, controller);

        GridPane contentBox = new GridPane(20, 10);
        GridPaneUtil.setGridPaneTwoColumnsConstraints(contentBox);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");

        int rowIndex = 0;

        // Headline and text
        Label headline = new Label(Res.get("reputation.score.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");
        GridPane.setMargin(headline, new Insets(10, 0, 0, 0));
        contentBox.add(headline, 0, rowIndex, 2, 1);

        Label info = new Label(model.getInfo());
        info.setWrapText(true);
        info.getStyleClass().addAll("bisq-text-13");
        GridPane.setMargin(info, new Insets(0, 0, 10, 0));
        contentBox.add(info, 0, ++rowIndex, 2, 1);

        ++rowIndex;
        // Left side
        Label formulaHeadline = new Label(Res.get("reputation.score.formulaHeadline"));
        formulaHeadline.getStyleClass().add("bisq-sub-title-label");

        MaterialTextField weight = getField(Res.get("reputation.weight"), model.getWeight());
        MaterialTextField formulaScore = getField(Res.get("reputation.formulaScore.description"), model.getFormula());
        formulaTradeLimit = getField(Res.get("reputation.formulaTradeLimit.description"), Res.get("reputation.formulaTradeLimit"));
        VBox formulaBox = new VBox(10, formulaHeadline, weight, formulaScore, formulaTradeLimit);
        contentBox.add(formulaBox, 0, rowIndex);

        // Right side
        contentBox.add(simulation, 1, rowIndex);

        // Buttons
        backButton = new Button(Res.get("action.back"));
        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);
        learnMore = new BisqHyperlink(Res.get("action.learnMore"), "https://bisq.wiki/Reputation");
        buttons = new HBox(20, backButton, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);
        contentBox.add(buttons, 0, ++rowIndex, 2, 1);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 0, 0));
    }

    private MaterialTextField getField(String description, String value) {
        MaterialTextField field = new MaterialTextField(description);
        field.setEditable(false);
        field.setText(value);
        return field;
    }

    @Override
    protected void onViewAttached() {
        reducePaddingPin = EasyBind.subscribe(model.getReducePadding(), reducePadding -> {
            if (reducePadding != null && reducePadding) {
                GridPane.setMargin(buttons, new Insets(-10.5, 0, 0, 0));
            } else {
                GridPane.setMargin(buttons, new Insets(10, 0, 0, 0));
            }
        });
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());

        UIThread.runOnNextRenderFrame(root::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        reducePaddingPin.unsubscribe();
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
