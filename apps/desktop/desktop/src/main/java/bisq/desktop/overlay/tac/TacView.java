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

package bisq.desktop.overlay.tac;

import bisq.desktop.DesktopModel;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.OrderedList;
import bisq.desktop.components.controls.UnorderedList;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TacView extends View<VBox, TacModel, TacController> {
    private static final double PADDING = 30;
    private Scene rootScene;
    private final Button acceptButton, rejectButton;
    private final CheckBox confirmCheckBox;
    private Subscription widthPin, heightPin;
    private Subscription tacConfirmedPin;

    public TacView(TacModel model, TacController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(PADDING));

        Label headline = new Label(Res.get("tac.headline"));
        headline.getStyleClass().add("tac-headline");
        headline.setWrapText(true);

        String text = "1. The user is responsible for using the software in compliance with local laws. Don't use the software if using it is not legal in your jurisdiction.\n\n" +

                "2. Any market prices, network fee estimates, or other data obtained from servers operated by the Bisq DAO is provided on an 'as is, as available' basis without representation or warranty of any kind. It is your responsibility to verify any data provided in regards to inaccuracies or omissions.\n\n" +

                "3. Any Fiat payment method carries a potential risk for bank chargeback. By accepting the \"User Agreement\" the user confirms " +
                "to be aware of those risks and in no case will claim legal responsibility to the authors or copyright holders of the software.\n\n" +

                "4. Any dispute, controversy or claim arising out of or relating to the use of the software shall be settled by arbitration in " +
                "accordance with the Bisq arbitration rules as at present in force. The arbitration is conducted online. " +
                "The language to be used in the arbitration proceedings shall be English if not otherwise stated.\n\n" +

                "5. In no event, unless for damages caused by acts of intent and gross negligence, damages resulting from personal injury, " +
                "or damages ensuing from other instances where liability is required by applicable law or agreed to in writing, will any " +
                "developer, copyright holder and/or any other party who modifies and/or conveys the software as permitted above or " +
                "facilitates its operation, be liable for damages, including any general, special, incidental or consequential damages " +
                "arising out of the use or inability to use the software (including but not limited to loss of data or data being " +
                "rendered inaccurate or losses sustained by you or third parties or a failure of the software to operate with any " +
                "other software), even if such developer, copyright holder and/or other party has been advised of the possibility of such damages.\n\n" +

                "6. The user confirms that they have read and agreed to the rules regarding the dispute process:";

        String rules = "- Use the trade ID for the \"reason for payment\" field. NEVER include terms like 'Bisq' or 'Bitcoin'" +
                "- If the bank of the fiat sender charges fees, the sender (BTC buyer) has to cover the fees." +
                "- In case of mediation, you must cooperate with the mediator and respond to each message within 48 hours." +
                "- The mediator has no enforcement power over the trade. They can only help the traders to come to a cooperative resolution. " +
                "- In case of clear evidence for a scam or severe violation of the trade rules the mediator can ban the misbehaving trader and in " +
                "case that the trader was the Bitcoin seller and used 'account age' or 'signed account age witness' as reputation source, they will also " +
                "get banned on Bisq 1. If the seller has used 'bonded BSQ' as reputation source the mediator will report the incident to the DAO and " +
                "make a proposal for confiscating their bonded BSQ.";

        OrderedList textList = new OrderedList(text, "tac-text", 5, 20);
        UnorderedList rulesList = new UnorderedList(rules, "tac-text");

        confirmCheckBox = new CheckBox(Res.get("tac.confirm"));
        confirmCheckBox.setFocusTraversable(false);

        acceptButton = new Button(Res.get("tac.accept"));
        acceptButton.setFocusTraversable(false);

        rejectButton = new Button(Res.get("tac.reject"));
        rejectButton.getStyleClass().add("outlined-button");
        rejectButton.setFocusTraversable(false);

        HBox buttons = new HBox(20, acceptButton, Spacer.fillHBox(), rejectButton);
        VBox.setMargin(rulesList, new Insets(-20, 0, 0, 20));
        VBox.setMargin(confirmCheckBox, new Insets(20, 0, 0, 0));
        root.getChildren().addAll(headline,
                textList,
                rulesList,
                confirmCheckBox,
                buttons);
    }

    @Override
    protected void onViewAttached() {
        root.requestFocus();
        rootScene = root.getScene();

        Region applicationRoot = OverlayController.getInstance().getApplicationRoot();
        widthPin = EasyBind.subscribe(applicationRoot.widthProperty(), w -> updateWidth());
        heightPin = EasyBind.subscribe(applicationRoot.heightProperty(), h -> updateHeight());

        UIThread.runOnNextRenderFrame(() -> {
            updateWidth();
            updateHeight();
        });

        confirmCheckBox.setSelected(model.getTacConfirmed().get());

        tacConfirmedPin = EasyBind.subscribe(model.getTacConfirmed(), confirmed -> {
            acceptButton.setDisable(!confirmed);
            acceptButton.setDefaultButton(confirmed);
        });

        confirmCheckBox.setOnAction(e -> controller.onConfirm(confirmCheckBox.isSelected()));

        acceptButton.setOnAction(e -> controller.onAccept());
        rejectButton.setOnAction(e -> controller.onReject());
    }

    @Override
    protected void onViewDetached() {
        widthPin.unsubscribe();
        heightPin.unsubscribe();
        tacConfirmedPin.unsubscribe();
        acceptButton.setOnAction(null);
        rejectButton.setOnAction(null);
    }

    private void updateHeight() {
        double height = OverlayController.getInstance().getApplicationRoot().getHeight();
        if (height > 0) {
            double scale = (height - DesktopModel.MIN_HEIGHT) / (DesktopModel.PREF_HEIGHT - DesktopModel.MIN_HEIGHT);
            double boundedScale = Math.max(0.25, Math.min(1, scale));
            double paddedHeight = height - 6 * PADDING * boundedScale;
            rootScene.getWindow().setHeight(paddedHeight);
            root.setPrefHeight(paddedHeight);
        }
    }

    private void updateWidth() {
        double width = OverlayController.getInstance().getApplicationRoot().getWidth();
        if (width > 0) {
            double scale = (width - DesktopModel.MIN_WIDTH) / (DesktopModel.PREF_WIDTH - DesktopModel.MIN_WIDTH);
            double boundedScale = Math.max(0.25, Math.min(1, scale));
            double padding = 6 * PADDING * boundedScale;
            double paddedWidth = width - padding;
            rootScene.getWindow().setWidth(paddedWidth);
            root.setPrefWidth(paddedWidth);
        }
    }
}