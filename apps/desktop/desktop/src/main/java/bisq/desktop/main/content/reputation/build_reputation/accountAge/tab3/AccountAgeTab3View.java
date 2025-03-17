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

package bisq.desktop.main.content.reputation.build_reputation.accountAge.tab3;

import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountAgeTab3View extends View<VBox, AccountAgeTab3Model, AccountAgeTab3Controller> {
    private final Button closeButton, backButton, requestCertificateButton;
    private final Hyperlink learnMore;
    private MaterialTextField pubKeyHash;
    private MaterialTextArea jsonData;

    public AccountAgeTab3View(AccountAgeTab3Model model,
                              AccountAgeTab3Controller controller,
                              Pane userProfileSelection) {
        super(new VBox(), model, controller);

        VBox stepOne = createAndGetStepOne(userProfileSelection);
        VBox stepTwo = createAndGetStepTwo();
        VBox stepThree = createAndGetStepThree();
        VBox stepFour = createAndGetStepFour();

        requestCertificateButton = new Button(Res.get("reputation.request"));
        requestCertificateButton.getStyleClass().add("outlined-button");
        VBox.setMargin(requestCertificateButton, new Insets(-5, 0, 15, 0));

        backButton = new Button(Res.get("action.back"));

        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, backButton, closeButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox contentBox = new VBox(30);
        contentBox.getChildren().addAll(stepOne, stepTwo, stepThree, stepFour, requestCertificateButton, buttons);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(20, 0, 0, 0));
        root.getStyleClass().add("account-age");
    }

    @Override
    protected void onViewAttached() {
        pubKeyHash.textProperty().bind(model.getPubKeyHash());
        pubKeyHash.getIconButton().setOnAction(e -> controller.onCopyToClipboard(pubKeyHash.getText()));

        jsonData.textProperty().bindBidirectional(model.getJsonData());

        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());

        requestCertificateButton.disableProperty().bind(model.getRequestCertificateButtonDisabled());
        requestCertificateButton.setOnAction(e -> controller.onRequestAuthorization());
    }

    @Override
    protected void onViewDetached() {
        pubKeyHash.textProperty().unbind();
        pubKeyHash.getIconButton().setOnAction(null);

        jsonData.textProperty().unbindBidirectional(model.getJsonData());

        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);

        requestCertificateButton.disableProperty().unbind();
        requestCertificateButton.setOnAction(null);
    }

    private VBox createAndGetStepOne(Pane userProfileSelection) {
        Label title = createAndGetStepLabel(Res.get("reputation.accountAge.import.step1.title"));
        Label instruction = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step1.instruction"));
        VBox vBox = new VBox(title, instruction, userProfileSelection, Layout.hLine());
        vBox.getStyleClass().add("import-step");
        VBox.setMargin(vBox, new Insets(20, 0, 0, 0));
        return vBox;
    }

    private VBox createAndGetStepTwo() {
        Label title = createAndGetStepLabel(Res.get("reputation.accountAge.import.step2.title"));
        Label instruction = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step2.instruction"));
        pubKeyHash = new MaterialTextField(Res.get("reputation.accountAge.import.step2.profileId"), "");
        pubKeyHash.setEditable(false);
        pubKeyHash.showCopyIcon();
        pubKeyHash.getStyleClass().add("material-field");
        VBox.setMargin(pubKeyHash, new Insets(10, 0, 15, 2));
        VBox vBox = new VBox(title, instruction, pubKeyHash, Layout.hLine());
        vBox.getStyleClass().add("import-step");
        return vBox;
    }

    private VBox createAndGetStepThree() {
        Label title = createAndGetStepLabel(Res.get("reputation.accountAge.import.step3.title"));
        Label instructionOne = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step3.instruction1"));
        Label instructionTwo = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step3.instruction2"));
        Label instructionThree = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step3.instruction3"));
        VBox.setMargin(instructionThree, new Insets(0, 0, 15, 0));
        VBox vBox = new VBox(title, instructionOne, instructionTwo, instructionThree, Layout.hLine());
        vBox.getStyleClass().add("import-step");
        return vBox;
    }

    private VBox createAndGetStepFour() {
        Label title = createAndGetStepLabel(Res.get("reputation.accountAge.import.step4.title"));
        Label instruction = createAndGetStepInstructionLabel(Res.get("reputation.accountAge.import.step4.instruction"));
        jsonData = new MaterialTextArea(Res.get("reputation.accountAge.import.step4.signedMessage"));
        jsonData.setEditable(true);
        jsonData.getStyleClass().add("material-field");
        VBox.setMargin(jsonData, new Insets(10, 0, 15, 2));
        UIThread.runOnNextRenderFrame(jsonData::requestFocus);
        VBox vBox = new VBox(title, instruction, jsonData);
        vBox.getStyleClass().add("import-step");
        return vBox;
    }

    private Label createAndGetStepLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.getStyleClass().add("step-title");
        return label;
    }

    private Label createAndGetStepInstructionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("step-instruction");
        return label;
    }
}
