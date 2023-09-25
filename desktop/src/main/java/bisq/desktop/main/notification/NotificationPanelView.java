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

package bisq.desktop.main.notification;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NotificationPanelView extends View<VBox, NotificationPanelModel, NotificationPanelController> {
    private final Label notificationHeadline;
    private final Button closeButton, goToOpenTradesButton;
    private final Text notificationContent;
    private Timeline slideInRightTimeline, slideOutTopTimeline;
    private Subscription useExtraPaddingPin, isVisiblePin;

    public NotificationPanelView(NotificationPanelModel model,
                                 NotificationPanelController controller) {
        super(new VBox(), model, controller);

        root.setManaged(false);
        root.setVisible(false);

        notificationHeadline = new Label();
        notificationHeadline.getStyleClass().add("notification-headline");
        notificationContent = new Text();
        notificationContent.getStyleClass().add("notification-content");
        TextFlow notificationContentTextFlow = new TextFlow(notificationContent);

        closeButton = BisqIconButton.createIconButton("close-black");

        goToOpenTradesButton = new Button(Res.get("notificationPanel.button"));
        goToOpenTradesButton.setDefaultButton(true);

        VBox.setMargin(goToOpenTradesButton, new Insets(10, 0, 0, 0));
        VBox notificationVBox = new VBox(10, notificationHeadline, notificationContentTextFlow, goToOpenTradesButton);

        HBox.setMargin(notificationVBox, new Insets(30, 48, 44, 48));
        HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
        HBox notificationHBox = new HBox(notificationVBox, Spacer.fillHBox(), closeButton);
        notificationHBox.getStyleClass().add("notification-box");

        root.getChildren().add(notificationHBox);
    }

    @Override
    protected void onViewAttached() {
        notificationHeadline.textProperty().bind(model.getHeadline());
        notificationContent.textProperty().bind(model.getContent());

        useExtraPaddingPin = EasyBind.subscribe(model.getUseExtraPadding(), useExtraPadding -> {
            double bottom = useExtraPadding ? 33 : 0;
            root.setPadding(new Insets(33, 67, bottom, 67));

            if (useExtraPadding) {
                root.getStyleClass().remove("notification-pane");
                root.getStyleClass().add("notification-pane-dark");
            } else {
                root.getStyleClass().add("notification-pane");
                root.getStyleClass().remove("notification-pane-dark");
            }
        });

        isVisiblePin = EasyBind.subscribe(model.getIsVisible(), isVisible -> {
            if (slideInRightTimeline != null) {
                slideInRightTimeline.stop();
                slideInRightTimeline = null;
                root.setTranslateX(0);
                root.setOpacity(1);
            }
            if (slideOutTopTimeline != null) {
                slideOutTopTimeline.stop();
                slideOutTopTimeline = null;
                root.setTranslateY(0);
                root.setOpacity(0);
                root.setManaged(false);
                root.setVisible(false);
            }
            if (isVisible) {
                root.setManaged(true);
                root.setVisible(true);
                root.setTranslateY(0);
                slideInRightTimeline = Transitions.slideInRight(root, Transitions.DEFAULT_DURATION / 2, () -> {
                });
            } else {
                root.setTranslateX(0);
                slideOutTopTimeline = Transitions.slideAndFadeOutTop(root, Transitions.DEFAULT_DURATION / 2, () -> {
                    root.setManaged(false);
                    root.setVisible(false);
                });
            }
        });

        closeButton.setOnAction(e -> controller.onClose());
        goToOpenTradesButton.setOnAction(e -> controller.onGoToOpenTrades());
    }

    @Override
    protected void onViewDetached() {
        notificationHeadline.textProperty().unbind();
        notificationContent.textProperty().unbind();

        useExtraPaddingPin.unsubscribe();
        isVisiblePin.unsubscribe();

        closeButton.setOnAction(null);
        goToOpenTradesButton.setOnAction(null);
    }
}
