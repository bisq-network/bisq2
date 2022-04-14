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
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqProgressBar;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    private final BisqProgressBar progressBar;
    private final Label statusLabel;
    private Subscription stateSubscription;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("content-pane");
        
        ImageView logo = new ImageView();
        logo.setId("logo-splash");
        VBox.setMargin(logo, new Insets(-52, 0, 83, 0)); 

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("bisq-small-light-label");
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        progressBar = new BisqProgressBar(-1);
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMinWidth(535);
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));

        Label connectingTitle = new BisqLabel(Res.get("satoshisquareapp.splash.connecting").toUpperCase());
        connectingTitle.getStyleClass().add("bisq-small-light-label-dimmed");

        root.getChildren().addAll(logo,  statusLabel, progressBar, connectingTitle);
    }

    @Override
    protected void onViewAttached() {
        stateSubscription = EasyBind.subscribe(model.getState(),
                state -> {
                    if (state != null) {
                        statusLabel.setText(Res.get("defaultApplicationService.state." + state.name()).toUpperCase());
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
        }
        progressBar.setProgress(0);
    }
}
