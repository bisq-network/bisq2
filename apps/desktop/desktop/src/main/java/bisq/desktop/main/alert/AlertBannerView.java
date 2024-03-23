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

package bisq.desktop.main.alert;

import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class AlertBannerView extends View<BorderPane, AlertBannerModel, AlertBannerController> {
    public static final int DURATION = Transitions.DEFAULT_DURATION / 2;
    private final VBox contentVBox;

    Label headline = new Label();
    Label message = new Label();
    private final Button closeButton;
    private final HBox banner;
    private final ChangeListener<Number> heightListener;
    private Timeline slideInRightTimeline, slideOutTopTimeline;
    private Subscription isVisiblePin, alertTypePin;

    public AlertBannerView(AlertBannerModel model, AlertBannerController controller) {
        super(new BorderPane(), model, controller);

        root.setManaged(false);
        root.setVisible(false);

        message.setWrapText(true);

        closeButton = BisqIconButton.createIconButton("close-white");

        ImageView broadcastIcon = ImageUtil.getImageViewById("bisq-broadcast-white");
        headline.setGraphic(broadcastIcon);
        headline.setGraphicTextGap(20);

        message.setPadding(new Insets(0, 0, 0, 5));

        contentVBox = new VBox(10, headline, message);
        banner = new HBox(contentVBox, Spacer.fillHBox(), closeButton);
        banner.setAlignment(Pos.TOP_CENTER);
        banner.setPadding(new Insets(10, 10, 10, 15));

        heightListener = ((observable, oldValue, newValue) -> banner.setMinHeight(contentVBox.getHeight() + 25)); // padding = 25

        root.setCenter(banner);
        root.setPadding(new Insets(20, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        message.textProperty().bind(model.getMessage());

        isVisiblePin = EasyBind.subscribe(model.getIsAlertVisible(), isVisible -> {
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

        alertTypePin = EasyBind.subscribe(model.getAlertType(), alertType -> {
            if (alertType != null) {
                addAlertTypeStyleClass(alertType);
            }
        });

        message.heightProperty().addListener(heightListener);

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        message.textProperty().unbind();

        isVisiblePin.unsubscribe();
        alertTypePin.unsubscribe();

        message.heightProperty().removeListener(heightListener);

        closeButton.setOnAction(null);
    }

    private void addAlertTypeStyleClass(AlertType alertType) {
        headline.getStyleClass().clear();
        headline.getStyleClass().add("alert-banner-headline");

        message.getStyleClass().clear();
        message.getStyleClass().add("alert-banner-message");

        String alertTypeClass;
        if (alertType == AlertType.INFO) {
            alertTypeClass = "info-banner";
        } else if (alertType == AlertType.WARN) {
            alertTypeClass = "warn-banner";
        } else {
            alertTypeClass = "emergency-banner";
        }

        banner.getStyleClass().clear();
        banner.getStyleClass().add(alertTypeClass);
    }
}
