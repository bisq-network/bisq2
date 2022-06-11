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
import bisq.network.p2p.ServiceNode;
import com.google.common.base.Joiner;
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

import java.util.ArrayList;

@Slf4j
public class SplashView extends View<VBox, SplashModel, SplashController> {
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label perNetworkStatusLabel;
    private Subscription appStateSubscription;
    private Subscription clearnetStateSubscription;
    private Subscription torStateSubscription;
    private Subscription i2pStateSubscription;

    public SplashView(SplashModel model, SplashController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-content-bg");

        ImageView logo = new ImageView();
        logo.setId("logo-splash");
        VBox.setMargin(logo, new Insets(-52, 0, 83, 0));

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("bisq-small-light-label");
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        progressBar = new ProgressBar(-1);
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMinWidth(535);
        VBox.setMargin(progressBar, new Insets(16, 0, 16, 0));

        Label connectingTitle = new Label(Res.get("satoshisquareapp.splash.connecting").toUpperCase());
        connectingTitle.getStyleClass().add("bisq-small-light-label-dimmed");

        perNetworkStatusLabel = new Label("");
        perNetworkStatusLabel.getStyleClass().add("bisq-small-light-label-dimmed");

        root.getChildren().addAll(logo, statusLabel, progressBar, connectingTitle, perNetworkStatusLabel);
    }

    @Override
    protected void onViewAttached() {
        appStateSubscription = EasyBind.subscribe(model.getApplicationState(),
                state -> {
                    if (state != null) {
                        statusLabel.setText(Res.get("defaultApplicationService.state." + state.name()).toUpperCase());
                    }
                });

        clearnetStateSubscription = EasyBind.subscribe(model.getClearServiceNodeState(),
                clearnetState -> {
                    if (clearnetState != null) {
                        String composite = getCompositeNetworkStatus(clearnetState, model.getTorServiceNodeState().get(), model.getI2pServiceNodeState().get());
                        perNetworkStatusLabel.setText(composite);
                    }
                });

        torStateSubscription = EasyBind.subscribe(model.getTorServiceNodeState(),
                torState -> {
                    if (torState != null) {
                        String composite = getCompositeNetworkStatus(model.getClearServiceNodeState().get(), torState, model.getI2pServiceNodeState().get());
                        perNetworkStatusLabel.setText(composite);
                    }
                });

        i2pStateSubscription = EasyBind.subscribe(model.getI2pServiceNodeState(),
                i2pState -> {
                    if (i2pState != null) {
                        String composite = getCompositeNetworkStatus(model.getClearServiceNodeState().get(), model.getTorServiceNodeState().get(), i2pState);
                        perNetworkStatusLabel.setText(composite);
                    }
                });

        progressBar.progressProperty().bind(model.getProgress());
    }

    private String getCompositeNetworkStatus(ServiceNode.State clearnetState, ServiceNode.State torState, ServiceNode.State i2pState) {
        ArrayList<String> networkStatuses = new ArrayList<>();
        if (clearnetState != null) {
            networkStatuses.add( String.format("Clear %s%%", getServiceNodeStateProgress(clearnetState)) );
        }
        if (torState != null) {
            networkStatuses.add( String.format("Tor %s%%", getServiceNodeStateProgress(torState)) );
        }
        if (i2pState != null) {
            networkStatuses.add(  String.format("I2P %s%%", getServiceNodeStateProgress(i2pState)) );
        }
        return Joiner.on(" / ").join(networkStatuses).toUpperCase();
    }

    private String getServiceNodeStateProgress(ServiceNode.State state) {
        return state == null ? "" :
                "" + 100 * state.ordinal() / ServiceNode.State.PEER_GROUP_INITIALIZED.ordinal();
    }

    @Override
    protected void onViewDetached() {
        if (appStateSubscription != null) {
            appStateSubscription.unsubscribe();
        }
        if (clearnetStateSubscription != null) {
            clearnetStateSubscription.unsubscribe();
        }
        if (torStateSubscription != null) {
            torStateSubscription.unsubscribe();
        }
        if (i2pStateSubscription != null) {
            i2pStateSubscription.unsubscribe();
        }
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
    }
}
