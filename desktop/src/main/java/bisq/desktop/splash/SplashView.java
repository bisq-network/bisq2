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

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    private final ProgressBar progressBar;
    private final Label appStatusLabel;
    private final HBox clearStatusHBox;
    private final HBox torStatusHBox;
    private final HBox i2pStatusHBox;
    private final Label clearStatusLabel;
    private final Label torStatusLabel;
    private final Label i2pStatusLabel;
    private final StackPane clearIcon;
    private final StackPane torIcon;
    private final StackPane i2pIcon;
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

        clearStatusLabel = new Label();
        torStatusLabel = new Label();
        i2pStatusLabel = new Label();

        clearStatusHBox = new HBox(new Label(Res.get("loading.network.clearnet")), clearStatusLabel);
        torStatusHBox = new HBox(new Label(Res.get("loading.network.tor")), torStatusLabel);
        i2pStatusHBox = new HBox(new Label(Res.get("loading.network.i2p")), i2pStatusLabel);

        clearStatusHBox.setSpacing(7);
        torStatusHBox.setSpacing(7);
        i2pStatusHBox.setSpacing(7);

        clearIcon = new StackPane(ImageUtil.getImageViewById("clearnet"));
        torIcon = new StackPane(ImageUtil.getImageViewById("tor"));
        i2pIcon = new StackPane(ImageUtil.getImageViewById("i2p"));

        HBox networkStatusBox = this.createNetworkStatusBox();
        VBox.setMargin(logo, new Insets(-52, 0, 83, 0));
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));
        root.getChildren().addAll(logo, appStatusLabel, progressBar, networkStatusBox);
    }

    @Override
    protected void onViewAttached() {
        this.bindNetworkState(clearStatusHBox, clearStatusLabel, clearIcon, model.getClearState());
        this.bindNetworkState(torStatusHBox, torStatusLabel, torIcon, model.getTorState());
        this.bindNetworkState(i2pStatusHBox, i2pStatusLabel, i2pIcon, model.getI2pState());
        progressBar.progressProperty().bind(model.getProgress());
        appStateSubscription = EasyBind.subscribe(model.getApplicationState(),
                state -> {
                    if (state != null) {
                        appStatusLabel.setText(Res.get("loading.state." + state.name()).toUpperCase());
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        this.unbindNetworkState(clearStatusHBox, clearStatusLabel, clearIcon);
        this.unbindNetworkState(torStatusHBox, torStatusLabel, torIcon);
        this.unbindNetworkState(i2pStatusHBox, i2pStatusLabel, i2pIcon);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        appStateSubscription.unsubscribe();
    }

    private HBox createNetworkStatusBox() {
        double height = 24;
        this.setNetworkProperties(clearIcon, height, Pos.CENTER_RIGHT);
        this.setNetworkProperties(torIcon, height, Pos.CENTER_RIGHT);
        this.setNetworkProperties(i2pIcon, height, Pos.CENTER_RIGHT);
        this.setNetworkProperties(clearStatusHBox, height, Pos.CENTER_LEFT);
        this.setNetworkProperties(torStatusHBox, height, Pos.CENTER_LEFT);
        this.setNetworkProperties(i2pStatusHBox, height, Pos.CENTER_LEFT);

        clearStatusLabel.getStyleClass().add("text-color-green");
        torStatusLabel.getStyleClass().add("text-color-green");
        i2pStatusLabel.getStyleClass().add("text-color-green");

        VBox transportIcons = new VBox(clearIcon, torIcon, i2pIcon);
        transportIcons.setAlignment(Pos.TOP_RIGHT);

        VBox transportStatus = new VBox(clearStatusHBox, torStatusHBox, i2pStatusHBox);
        transportStatus.setAlignment(Pos.TOP_LEFT);

        HBox networkStatusBox = new HBox(transportIcons, transportStatus);
        networkStatusBox.setSpacing(5);
        networkStatusBox.setAlignment(Pos.TOP_CENTER);
        networkStatusBox.getStyleClass().add("bisq-small-light-label-dimmed");
        return networkStatusBox;
    }

    private void setNetworkProperties(HBox hbox, double height, Pos position) {
        hbox.setPrefHeight(height);
        hbox.setMinHeight(height);
        hbox.setMaxHeight(height);
        hbox.setAlignment(position);
    }

    private void setNetworkProperties(StackPane stackPane, double height, Pos position) {
        stackPane.setPrefHeight(height);
        stackPane.setMinHeight(height);
        stackPane.setMaxHeight(height);
        stackPane.setAlignment(position);
    }

    private void bindNetworkState(HBox hbox, Label label, StackPane stackPane, StringProperty state) {
        label.textProperty().bind(state);
        BooleanBinding hasNetwork = state.isNotEmpty();
        hbox.visibleProperty().bind(hasNetwork);
        hbox.managedProperty().bind(hasNetwork);
        stackPane.visibleProperty().bind(hasNetwork);
        stackPane.managedProperty().bind(hasNetwork);
    }

    private void unbindNetworkState(HBox hbox, Label label, StackPane stackPane) {
        label.textProperty().unbind();
        hbox.visibleProperty().unbind();
        hbox.managedProperty().unbind();
        stackPane.visibleProperty().unbind();
        stackPane.managedProperty().unbind();
    }
}
