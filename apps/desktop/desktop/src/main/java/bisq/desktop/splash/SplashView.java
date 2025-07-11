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

package bisq.desktop.splash;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    public static final int WIDTH = 535;

    private final ProgressBar progressBar;
    private final Label applicationServiceState, duration;
    private Button deleteTorButton;
    private Subscription isSlowStartupPin;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);

        ImageView logo = new ImageView();
        logo.setId("logo-splash");

        Label version = new Label(model.getVersion());
        version.setOpacity(0.5);
        version.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");

        StackPane logoAndVersion = new StackPane(logo, version);
        logoAndVersion.setAlignment(Pos.CENTER);
        StackPane.setMargin(version, new Insets(-25, 0, 0, 200));

        applicationServiceState = new Label();
        applicationServiceState.getStyleClass().add("splash-application-state");
        applicationServiceState.setTextAlignment(TextAlignment.CENTER);

        Label separator = new Label("|");
        separator.setOpacity(0.75);
        separator.getStyleClass().add("splash-application-state");

        duration = new Label();
        duration.getStyleClass().add("splash-application-duration");
        duration.setMouseTransparent(true);
        HBox hBox = new HBox(12.5, applicationServiceState, separator, duration);
        hBox.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar();
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMinWidth(WIDTH);

        VBox.setMargin(logoAndVersion, new Insets(-52, 0, 83, 0));
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));
        root.getChildren().addAll(logoAndVersion, hBox, progressBar);
    }

    @Override
    protected void onViewAttached() {
        model.getBootstrapElementsPerTransports().forEach(bootstrapStateDisplay -> {
            VBox bootstrapStateDisplayRoot = bootstrapStateDisplay.getView().getRoot();
            HBox hBox = new HBox(Spacer.fillHBox(), bootstrapStateDisplayRoot, Spacer.fillHBox());
            root.getChildren().add(hBox);
        });

        applicationServiceState.textProperty().bind(model.getApplicationServiceState());
        duration.textProperty().bind(model.getDuration());
        progressBar.progressProperty().bind(model.getProgress());

        isSlowStartupPin = EasyBind.subscribe(model.getIsSlowStartup(), isSlowStartup -> {
            if (isSlowStartup) {
                Label warning = new Label(Res.get("splash.applicationServiceState.slowStartup.warning", model.getMaxExpectedStartupTime()));
                warning.setWrapText(true);
                warning.getStyleClass().addAll("bisq-text-yellow-dim", "normal-text");
                warning.setAlignment(Pos.CENTER);
                warning.setTextAlignment(TextAlignment.CENTER);

                deleteTorButton = new Button(Res.get("splash.applicationServiceState.slowStartup.deleteTor"));
                deleteTorButton.getStyleClass().add("yellow-outlined-button");
                deleteTorButton.setOnAction(e -> controller.onDeleteTor());

                VBox vBox = new VBox(20, warning, deleteTorButton);
                vBox.setAlignment(Pos.CENTER);
                VBox.setMargin(vBox, new Insets(20));
                root.getChildren().add(2, vBox);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        applicationServiceState.textProperty().unbind();
        duration.textProperty().unbind();
        progressBar.progressProperty().unbind();

        isSlowStartupPin.unsubscribe();

        if (deleteTorButton != null) {
            deleteTorButton.setOnAction(null);
        }
    }
}
