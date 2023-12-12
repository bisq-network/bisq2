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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    public static final int WIDTH = 535;
    private final ProgressBar progressBar;
    private final Label applicationServiceState;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-content-bg");

        ImageView logo = new ImageView();
        logo.setId("logo-splash");

        applicationServiceState = new Label("");
        applicationServiceState.getStyleClass().add("splash-application-state");
        applicationServiceState.setTextAlignment(TextAlignment.CENTER);

        progressBar = new ProgressBar();
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMinWidth(WIDTH);

        VBox.setMargin(logo, new Insets(-52, 0, 83, 0));
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));
        root.getChildren().addAll(logo, applicationServiceState, progressBar);
    }

    @Override
    protected void onViewAttached() {
        applicationServiceState.textProperty().bind(model.getApplicationServiceState());
        progressBar.progressProperty().bind(model.getProgress());
        model.getBootstrapStateDisplays().forEach(bootstrapStateDisplay -> {
            GridPane bootstrapStateDisplayRoot = bootstrapStateDisplay.getView().getRoot();
            bootstrapStateDisplayRoot.setMaxWidth(WIDTH);
            VBox.setMargin(bootstrapStateDisplayRoot, new Insets(0, 0, 7.5, 0));
            root.getChildren().add(bootstrapStateDisplayRoot);
        });
    }

    @Override
    protected void onViewDetached() {
        applicationServiceState.textProperty().unbind();
        progressBar.progressProperty().unbind();
    }
}
