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

package bisq.desktop.main.content.reputation.build_reputation.signedAccount.tab2;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.user.reputation.SignedWitnessService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessTab2View extends View<VBox, SignedWitnessTab2Model, SignedWitnessTab2Controller> {
    private final Button backButton, nextButton;
    private final Hyperlink learnMore;

    public SignedWitnessTab2View(SignedWitnessTab2Model model, SignedWitnessTab2Controller controller, VBox simulation) {
        super(new VBox(), model, controller);

        Label headline = new Label(Res.get("reputation.signedWitness.score.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.signedWitness.score.info"));
        info.setWrapText(true);
        info.getStyleClass().addAll("bisq-text-13");

        Label formulaHeadline = new Label(Res.get("reputation.score.formulaHeadline"));
        formulaHeadline.getStyleClass().addAll("bisq-text-1");
        VBox formulaBox = new VBox(10, formulaHeadline, getField("weight", String.valueOf(SignedWitnessService.WEIGHT)), getField("totalScore"));

        HBox hBox = new HBox(20, formulaBox, simulation);

        backButton = new Button(Res.get("action.back"));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, backButton, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(headline, info, hBox, buttons);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(20, 0, 0, 0));
    }

    @Override
    protected void onViewAttached() {
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());

        UIThread.runOnNextRenderFrame(root::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
    }

    private MaterialTextField getField(String key) {
        return getField(key, Res.get("reputation.signedWitness." + key));
    }

    private MaterialTextField getField(String key, String value) {
        MaterialTextField field = new MaterialTextField(Res.get("reputation." + key));
        field.setEditable(false);
        field.setText(value);
        field.setMinWidth(380);
        field.setMaxWidth(380);
        return field;
    }
}
