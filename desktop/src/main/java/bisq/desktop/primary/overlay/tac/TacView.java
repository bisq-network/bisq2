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

package bisq.desktop.primary.overlay.tac;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.primary.PrimaryStageModel;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TacView extends View<AnchorPane, TacModel, TacController> {
    private static final double SPACING = 20;
    private static final double PADDING = 30;
    private static final double HEADLINE_HEIGHT = 30;
    private static final double BUTTONS_HEIGHT = 31;
    private final Label headline;
    private final HBox buttons;
    private Scene rootScene;
    private final TextArea tac;
    private final Button acceptButton, rejectButton;
    private Subscription widthPin, heightPin;

    public TacView(TacModel model, TacController controller) {
        super(new AnchorPane(), model, controller);

        root.setPrefWidth(PrimaryStageModel.MIN_WIDTH - 4 * PADDING);
        root.setPrefHeight(PrimaryStageModel.MIN_HEIGHT - 4 * PADDING);

        root.setPadding(new Insets(PADDING));

        headline = new Label(Res.get("tac.headline"));
        headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");
        Layout.pinToAnchorPane(headline, PADDING, PADDING, null, PADDING);

        String text = "1. In no event, unless for damages caused by acts of intent and gross negligence, damages resulting from personal injury, " +
                "or damages ensuing from other instances where liability is required by applicable law or agreed to in writing, will any " +
                "developer, copyright holder and/or any other party who modifies and/or conveys the software as permitted above or " +
                "facilitates its operation, be liable for damages, including any general, special, incidental or consequential damages " +
                "arising out of the use or inability to use the software (including but not limited to loss of data or data being " +
                "rendered inaccurate or losses sustained by you or third parties or a failure of the software to operate with any " +
                "other software), even if such developer, copyright holder and/or other party has been advised of the possibility of such damages.\n\n" +

                "2. The user is responsible for using the software in compliance with local laws. Don't use the software if using it is not legal in your jurisdiction.\n\n" +

                "3. Any market prices, network fee estimates, or other data obtained from servers operated by the Bisq DAO is provided on an 'as is, as available' basis without representation or warranty of any kind. It is your responsibility to verify any data provided in regards to inaccuracies or omissions.\n\n" +

                "4. Any Fiat payment method carries a potential risk for bank chargeback. By accepting the \"User Agreement\" the user confirms " +
                "to be aware of those risks and in no case will claim legal responsibility to the authors or copyright holders of the software.\n\n" +

                "5. Any dispute, controversy or claim arising out of or relating to the use of the software shall be settled by arbitration in " +
                "accordance with the Bisq arbitration rules as at present in force. The arbitration is conducted online. " +
                "The language to be used in the arbitration proceedings shall be English if not otherwise stated.\n\n" +

                "6. The user confirms that they have read and agreed to the rules regarding the dispute process:\n" +
                "    - Leave the \"reason for payment\" field empty. NEVER put the trade ID or any other text like 'bitcoin', 'BTC', or 'Bisq'.\n" +
                "    - If the bank of the fiat sender charges fees, the sender (BTC buyer) has to cover the fees.\n" +
                "    - In case of mediation, you must cooperate with the mediator and respond to each message within 48 hours.\n" +
                "    - The mediator has no enforcement power over the trade. They can only help the traders to come to a cooperative resolution.\n" +
                "    - In case of clear evidence for a scam or severe violation of the trade rules the mediator can ban the misbehaving trader and in\n" +
                "      case that the trader was the Bitcoin seller and used 'account age' or 'signed account age witness' as reputation source, they will also\n" +
                "      get banned on Bisq 1. If the seller has used 'bonded BSQ' as reputation source the mediator will report the incident to the DAO and\n" +
                "      make a proposal for confiscating their bonded BSQ.\n";

        tac = new TextArea(text);
        Layout.pinToAnchorPane(tac, PADDING + SPACING + HEADLINE_HEIGHT, PADDING,
                PADDING + 2 * SPACING + BUTTONS_HEIGHT, PADDING);
        tac.setWrapText(true);
        tac.setEditable(false);

        acceptButton = new Button(Res.get("tac.accept"));
        acceptButton.setDefaultButton(true);
        rejectButton = new Button(Res.get("tac.reject"));
        buttons = new HBox(20, acceptButton, rejectButton);
        Layout.pinToAnchorPane(buttons, null, PADDING, PADDING, PADDING);
        root.getChildren().setAll(headline, tac, buttons);
    }

    @Override
    protected void onViewAttached() {
        rootScene = root.getScene();

        Region applicationRoot = OverlayController.getInstance().getApplicationRoot();
        widthPin = EasyBind.subscribe(applicationRoot.widthProperty(), w -> updateWidth());
        heightPin = EasyBind.subscribe(applicationRoot.heightProperty(), h -> updateHeight());

        UIThread.runOnNextRenderFrame(() -> {
            updateWidth();
            updateHeight();
        });

        acceptButton.setOnAction(e -> controller.onAccept());
        rejectButton.setOnAction(e -> controller.onReject());

        // Replace the key handler of OverlayView as we do not support escape/enter at this popup
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });
    }

    private void updateHeight() {
        double height = OverlayController.getInstance().getApplicationRoot().getHeight();
        if (height > 0) {
            double paddedHeight = height - 4 * PADDING;
            rootScene.getWindow().setHeight(paddedHeight);
            root.setPrefHeight(paddedHeight);
        }
    }

    private void updateWidth() {
        double width = OverlayController.getInstance().getApplicationRoot().getWidth();
        if (width > 0) {
            double scale = (width - PrimaryStageModel.MIN_WIDTH) / (PrimaryStageModel.PREF_WIDTH - PrimaryStageModel.MIN_WIDTH);
            double boundedScale = Math.max(0.25, Math.min(1, scale));
            double padding = 4 * PADDING * boundedScale;
            double paddedWidth = width - padding;
            rootScene.getWindow().setWidth(paddedWidth);
            root.setPrefWidth(paddedWidth);
        }
    }

    @Override
    protected void onViewDetached() {
        widthPin.unsubscribe();
        heightPin.unsubscribe();
        acceptButton.setOnAction(null);
        rejectButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
    }
}