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

package bisq.desktop.primary.splash;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    private final ProgressBar progressBar;
    private final Label appStatusLabel;
    private final Label transportStatusLabel;
    private Subscription appStateSubscription;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-content-bg");

        ImageView logo = new ImageView();
        logo.setId("logo-splash");
       
        appStatusLabel = new Label("");
        appStatusLabel.getStyleClass().add("bisq-small-light-label");
        appStatusLabel.setTextAlignment(TextAlignment.CENTER);

        progressBar = new ProgressBar(-1);
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMinWidth(535);

        transportStatusLabel = new Label("");
        transportStatusLabel.getStyleClass().add("bisq-small-light-label-dimmed");

        VBox.setMargin(logo, new Insets(-52, 0, 83, 0));
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));
        root.getChildren().addAll(logo, appStatusLabel, progressBar, transportStatusLabel);
    }

    @Override
    protected void onViewAttached() {
        transportStatusLabel.textProperty().bind(model.getTransportState());
        progressBar.progressProperty().bind(model.getProgress());
        appStateSubscription = EasyBind.subscribe(model.getApplicationState(),
                state -> {
                    if (state != null) {
                        appStatusLabel.setText(Res.get("applicationService.state." + state.name()).toUpperCase());
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        transportStatusLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        appStateSubscription.unsubscribe();
    }
}
