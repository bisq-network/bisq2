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
        logo.setId("logo_white");
        logo.setOpacity(0.25);
        VBox.setMargin(logo, new Insets(-65, 0, 0, 0));

        Label logoSub = new Label("Exchange, Decentralized ");
        logoSub.setStyle("-fx-font-size: 3em; -fx-font-family: \"IBM Plex Sans Light\"; -fx-text-fill: #4F4F4F;");
        logoSub.setTextAlignment(TextAlignment.CENTER);
        VBox.setMargin(logoSub, new Insets(15, 0, 133, 0));

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 1.3em;  -fx-text-fill: -fx-light-text-color;");
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        progressBar = new BisqProgressBar(-1);
        progressBar.setMinHeight(5);
        progressBar.setMaxHeight(5);
        progressBar.setMinWidth(535);
        VBox.setMargin(progressBar, new Insets(28, 0, 28, 0));

        Label connectingTitle = new BisqLabel(Res.get("satoshisquareapp.splash.connecting").toUpperCase());
        connectingTitle.setStyle("-fx-font-size: 1.3em; -fx-text-fill: #6A6A6A;");

        root.getChildren().addAll(logo, logoSub, statusLabel, progressBar, connectingTitle);
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

    
   /* public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        // root.getStyleClass().add("content-pane");
        root.setAlignment(Pos.CENTER);

        ImageView logo = new ImageView();
        logo.setId("image-splash-logo");
        // logo.setId("satoshisquare-splash");

     
       

        root.getChildren().addAll(
                logo,
                Spacer.height(50),
                statusLabel
        );
    }*/
}
