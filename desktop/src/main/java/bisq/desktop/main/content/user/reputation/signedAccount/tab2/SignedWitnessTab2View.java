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

package bisq.desktop.main.content.user.reputation.signedAccount.tab2;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.user.reputation.components.AgeSlider;
import bisq.i18n.Res;
import bisq.user.reputation.SignedWitnessService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessTab2View extends View<VBox, SignedWitnessTab2Model, SignedWitnessTab2Controller> {
    private final Button backButton, nextButton;
    private final Hyperlink learnMore;
    private final AgeSlider ageSlider;
    private final MaterialTextField score;
    private final MaterialTextField age;


    public SignedWitnessTab2View(SignedWitnessTab2Model model, SignedWitnessTab2Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        GridPane gridPane = GridPaneUtil.getTwoColumnsGridPane(20, 10,
                new Insets(0, 0, 0, 0));

        Label headline = new Label(Res.get("user.reputation.signedWitness.score.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Text infoLabelText = new Text(Res.get("user.reputation.signedWitness.score.info"));
        infoLabelText.getStyleClass().addAll("bisq-text-13");
        TextFlow info = new TextFlow(infoLabelText);

        Text formulaLabelText = new Text(Res.get("user.reputation.score.formulaHeadline"));
        formulaLabelText.getStyleClass().add("bisq-text-1");
        TextFlow formulaHeadline = new TextFlow(formulaLabelText);
        gridPane.add(formulaHeadline,
                0, 0, 1, 1);
        gridPane.add(getField("weight", String.valueOf(SignedWitnessService.WEIGHT)),
                0, 1, 1, 1);
        gridPane.add(getFieldKey("totalScore"),
                0, 2, 1, 1);

        Text simHeadlineText = new Text(Res.get("user.reputation.sim.headline"));
        simHeadlineText.getStyleClass().add("bisq-text-1");
        TextFlow simHeadline = new TextFlow(simHeadlineText);
        GridPane.setValignment(simHeadline, VPos.TOP);
        gridPane.add(simHeadline, 1, 0, 1, 1);
        age = getInputField("user.reputation.sim.age");
        gridPane.add(age, 1, 1, 1, 1);
        ageSlider = new AgeSlider(0, 400, 0);
        gridPane.add(ageSlider.getView().getRoot(), 1, 2, 1, 1);
        score = getField(Res.get("user.reputation.sim.score"));
        gridPane.add(score, 1, 3, 1, 1);
        GridPane.setMargin(ageSlider.getView().getRoot(), new Insets(15, 0, 0, 0));



        backButton = new Button(Res.get("action.back"));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, backButton, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, info, gridPane, buttons);
    }

    @Override
    protected void onViewAttached() {
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());
        UIThread.runOnNextRenderFrame(root::requestFocus);
        ageSlider.valueProperty().bindBidirectional(model.getAge());
        age.textProperty().bindBidirectional(model.getAgeAsString());
        score.textProperty().bind(model.getScore());
    }

    @Override
    protected void onViewDetached() {
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
        ageSlider.valueProperty().unbindBidirectional(model.getAge());
        age.textProperty().unbindBidirectional(model.getAgeAsString());
        score.textProperty().unbind();
    }

    private MaterialTextField getFieldKey(String key) {
        return getField(key, Res.get("user.reputation.signedWitness." + key));
    }

    private MaterialTextField getField(String key, String value) {
        MaterialTextField field = new MaterialTextField(Res.get("user.reputation." + key));
        field.setEditable(false);
        field.setText(value);
        return field;
    }

    private MaterialTextField getField(String description) {
        MaterialTextField field = new MaterialTextField(description);
        field.setEditable(false);
        return field;
    }

    private MaterialTextField getInputField(String key) {
        return new MaterialTextField(Res.get(key), Res.get(key + ".prompt"));
    }

}
