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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.common.util.StringUtils;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.OrderedList;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class FormView<M extends FormModel, C extends FormController<?, ?, ?>> extends View<StackPane, M, C> {
    private static final double TOP_PANE_HEIGHT = 55;
    private final static int FEEDBACK_WIDTH = 900;

    protected final Map<String, Label> errorLabels = new HashMap<>();
    protected final Map<String, ChangeListener<?>> listeners = new HashMap<>();
    protected final VBox overlay;
    protected final VBox content;
    private final Button overlayCloseButton;
    protected final Label titleLabel;
    protected Subscription showOverlayPin;
    @Nullable
    protected VBox overlayContentBox;

    protected FormView(M model, C controller) {
        super(new StackPane(), model, controller);

        titleLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.title"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");

        VBox.setMargin(titleLabel, new Insets(50, 0, 10, 0));
        content = new VBox(10, titleLabel);
        content.setAlignment(Pos.TOP_CENTER);

        overlay = new VBox(20);
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setAlignment(Pos.TOP_CENTER);

        overlayCloseButton = new Button(Res.get("action.iUnderstand"));
        overlayCloseButton.setDefaultButton(true);

        StackPane.setMargin(overlay, new Insets(-TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, overlay);
    }

    @Override
    protected void onViewAttached() {
        showOverlayPin = EasyBind.subscribe(model.getShowOverlay(),
                show -> {
                    overlay.setManaged(show);
                    overlay.setVisible(show);
                    if (show) {
                        root.setOnKeyPressed(controller::onKeyPressedWhileShowingOverlay);
                        // We need a bit of delay as we set managed to true and animation would not work otherwise
                        // as layout properties are not set yet.
                        // Set opacity to 0 to avoid flick. Using only visible would cause the validation on the
                        // fields due requestFocus
                        overlay.setOpacity(0);
                        UIThread.runOnNextRenderFrame(() -> {
                            overlay.setOpacity(1);
                            Transitions.blurStrong(content, 0);
                            Transitions.slideInTop(overlay, 450);
                        });
                        overlay.requestFocus();
                    } else {
                        root.setOnKeyPressed(null);
                        overlay.setVisible(false);
                        Transitions.removeEffect(content);
                    }
                });
        overlayCloseButton.setOnAction(e -> controller.onCloseOverlay());
    }

    @Override
    protected void onViewDetached() {
        showOverlayPin.unsubscribe();
        overlayCloseButton.setOnAction(null);
        root.setOnKeyPressed(null);
    }

    protected void configOverlay(String info) {
        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.headline"), info);
    }

    protected void configOverlay(String headline, String info) {
        VBox contentBox = getOverlayContentBox();
        overlayContentBox = contentBox;

        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setTextAlignment(TextAlignment.CENTER);

        ArrayList<String> hyperlinks = new ArrayList<>();
        info = StringUtils.extractHyperlinks(info, hyperlinks);
        //Label subtitleLabel = new Label(info);

        OrderedList subtitleLabel= new OrderedList(info,"bisq-text-21");
        subtitleLabel.setMinWidth(getFeedbackTextWidth());
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        VBox hyperlinksBox = getHyperlinksBox(hyperlinks);

        VBox.setMargin(overlayCloseButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headlineLabel, subtitleLabel, hyperlinksBox, overlayCloseButton);

        overlay.getChildren().setAll(contentBox);
    }

    protected VBox getOverlayContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(getFeedbackWidth());
        return contentBox;
    }

    protected VBox getHyperlinksBox(ArrayList<String> hyperlinks) {
        VBox footerBox = new VBox();
        for (int i = 0; i < hyperlinks.size(); i++) {
            Label enumeration = new Label(String.format("[%d]", i + 1));
            enumeration.getStyleClass().add("overlay-message");
            Hyperlink link = new Hyperlink(hyperlinks.get(i));
            link.getStyleClass().add("overlay-message");
            link.setOnAction(event -> Browser.open(link.getText()));
            String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                    ? Res.get("popup.hyperlink.copy.tooltip", link.getText())
                    : Res.get("popup.hyperlink.openInBrowser.tooltip", link.getText());
            link.setTooltip(new BisqTooltip(tooltipText));
            HBox hBox = new HBox(5, enumeration, link);
            hBox.setAlignment(Pos.CENTER_LEFT);
            footerBox.getChildren().addAll(hBox);
        }
        footerBox.setMinWidth(getFeedbackTextWidth());
        footerBox.setMaxWidth(getFeedbackTextWidth());
        return footerBox;
    }

    protected int getFeedbackWidth() {
        return FEEDBACK_WIDTH;
    }

    protected int getFeedbackTextWidth() {
        return getFeedbackWidth() - 200;
    }
}