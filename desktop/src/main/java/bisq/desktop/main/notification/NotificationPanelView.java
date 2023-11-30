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
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NotificationPanelView extends View<BorderPane, NotificationPanelModel, NotificationPanelController> {
    public static final int DURATION = Transitions.DEFAULT_DURATION / 2;
    private final Label notificationHeadline;
    private final Button closeButton;
    private final Hyperlink hyperlink;
    private Timeline slideInRightTimeline, slideOutTopTimeline;
    private Subscription isVisiblePin;

    public NotificationPanelView(NotificationPanelModel model,
                                 NotificationPanelController controller) {
        super(new BorderPane(), model, controller);

        root.setManaged(false);
        root.setVisible(false);

        notificationHeadline = new Label();
        notificationHeadline.getStyleClass().add("notification-headline");

        closeButton = BisqIconButton.createIconButton("close-black");

        hyperlink = new Hyperlink();
        hyperlink.getStyleClass().add("notification-hyperlink");

        HBox.setMargin(hyperlink, new Insets(5, 0, 5, 0));

        Label separator = new Label("|");
        separator.getStyleClass().add("notification-headline");
        HBox notificationContent = new HBox(10, notificationHeadline, separator, hyperlink);
        notificationContent.setAlignment(Pos.CENTER);

        HBox notificationHBox = new HBox(notificationContent, Spacer.fillHBox(), closeButton);
        notificationHBox.setPadding(new Insets(0, 10, 0, 15));
        notificationHBox.getStyleClass().add("notification-box");
        notificationHBox.setAlignment(Pos.CENTER);

        root.setCenter(notificationHBox);
        root.setPadding(new Insets(20, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        notificationHeadline.textProperty().bind(model.getHeadline());
        hyperlink.textProperty().bind(model.getButtonText());

        isVisiblePin = EasyBind.subscribe(model.getIsNotificationVisible(), isVisible -> {
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
                slideInRightTimeline = Transitions.slideInRight(root, DURATION, () -> {
                });
            } else {
                root.setTranslateX(0);
                root.setTranslateY(0);
                slideOutTopTimeline = Transitions.slideAndFadeOutTop(root, DURATION, () -> {
                    root.setManaged(false);
                    root.setVisible(false);
                });
            }
        });

        closeButton.setOnAction(e -> controller.onClose());
        hyperlink.setOnAction(e -> controller.onNavigateToTarget());
    }

    @Override
    protected void onViewDetached() {
        notificationHeadline.textProperty().unbind();
        hyperlink.textProperty().unbind();

        isVisiblePin.unsubscribe();

        closeButton.setOnAction(null);
        hyperlink.setOnAction(null);
    }
}
