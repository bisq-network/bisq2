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

package bisq.satoshisquareapp.primary.splash;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqProgressBar;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.concurrent.TimeUnit;

public class SplashView extends View<VBox, SplashModel, SplashController> {
    private final BisqProgressBar progressBar;
    private final Label subTitle1, subTitle2, subTitle3;
    private final UIScheduler scheduler;
    private int sloganCounter = 1;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.getStyleClass().add("content-pane");
        root.setAlignment(Pos.CENTER);

        ImageView logo = new ImageView();
        logo.setId("satoshisquare-splash");
        VBox.setMargin(logo, new Insets(-60, 0, 60, 0));

        subTitle1 = getSubTitle(Res.get("satoshisquareapp.splash.subTitle1"));
        subTitle2 = getSubTitle(Res.get("satoshisquareapp.splash.subTitle2"));
        subTitle3 = getSubTitle(Res.get("satoshisquareapp.splash.subTitle3"));
        Transitions.fadeIn(subTitle1);
        scheduler = UIScheduler.run(() -> {
                    if (sloganCounter == 1) {
                        Transitions.crossFade(subTitle1, subTitle2);
                    } else if (sloganCounter == 2) {
                        Transitions.crossFade(subTitle2, subTitle3);
                    } else {
                        Transitions.crossFade(subTitle3, subTitle1);
                    }
                    sloganCounter++;
                    if (sloganCounter > 3) {
                        sloganCounter = 1;
                    }
                })
                .periodically(5, TimeUnit.SECONDS);

        progressBar = new BisqProgressBar(-1);
        progressBar.setMaxHeight(2);
        progressBar.setMinWidth(400);
        VBox.setMargin(progressBar, new Insets(60, 0, 0, 0));

        Label connectingTitle = new BisqLabel(Res.get("satoshisquareapp.splash.connecting"));
        VBox.setMargin(connectingTitle, new Insets(20, 0, 0, 0));
        Pane slogansPane = new StackPane();
        slogansPane.getChildren().addAll(subTitle1, subTitle2, subTitle3);
        root.getChildren().addAll(logo, slogansPane, progressBar, connectingTitle);
    }

    private Label getSubTitle(String text) {
        Label label = new BisqLabel(text);
        label.setStyle("-fx-font-size: 1.6em; -fx-text-fill: -bs-color-gray-4;");
        label.setTextAlignment(TextAlignment.CENTER);
        label.setOpacity(0);
        return label;
    }

    protected void onViewDetached() {
        progressBar.setProgress(0);
        scheduler.stop();
    }
}
