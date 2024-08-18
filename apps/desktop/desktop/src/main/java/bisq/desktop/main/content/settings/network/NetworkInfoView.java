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

package bisq.desktop.main.content.settings.network;

import bisq.common.application.ApplicationVersion;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;


@Slf4j
public class NetworkInfoView extends View<VBox, NetworkInfoModel, NetworkInfoController> {
    private final Accordion accordion;
    private final HBox versionDistributionHBox;
    private final BisqTooltip tooltip;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller,
                           Optional<Node> clear,
                           Optional<Node> tor,
                           Optional<Node> i2p) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(-10, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        root.setFillWidth(true);

        versionDistributionHBox = new HBox(20);
        Label myVersionAndCommitHash = new Label(Res.get("settings.network.myVersionAndCommitHash",
                ApplicationVersion.getVersion().getVersionAsString(),
                ApplicationVersion.getBuildCommitShortHash()));


        VBox versionsVBox = new VBox(15, versionDistributionHBox, myVersionAndCommitHash);

        tooltip = new BisqTooltip();
        Tooltip.install(versionsVBox, tooltip);

        accordion = new Accordion();

        accordion.getPanes().add(new TitledPane(Res.get("settings.network.versions.headline"), versionsVBox));

        clear.ifPresent(childRoot -> accordion.getPanes().add(new TitledPane(Res.get("settings.network.clearNet"), childRoot)));
        tor.ifPresent(childRoot -> accordion.getPanes().add(new TitledPane(Res.get("settings.network.tor"), childRoot)));
        i2p.ifPresent(childRoot -> accordion.getPanes().add(new TitledPane(Res.get("settings.network.i2p"), childRoot)));

        root.getChildren().add(accordion);
    }


    @Override
    protected void onViewAttached() {
        versionDistributionHBox.getChildren().clear();
        model.getVersionDistribution().forEach(pair -> addVersion(pair.getFirst(), pair.getSecond()));
        tooltip.setText(model.getVersionDistributionTooltip());

        if (accordion.getPanes().size() > 1) {
            UIThread.runOnNextRenderFrame(() -> accordion.getPanes().get(1).setExpanded(true));
        } else if (!accordion.getPanes().isEmpty()) {
            UIThread.runOnNextRenderFrame(() -> accordion.getPanes().get(0).setExpanded(true));
        }
    }

    @Override
    protected void onViewDetached() {
    }

    private void addVersion(String version, double percentage) {
        if (percentage == 1) {
            // We want to avoid the check style shown when 100% is reached
            percentage = 0.999999999999;
        }
        ProgressIndicator progressIndicator = new ProgressIndicator(percentage);
        progressIndicator.setMinSize(100, 100);
        Label label = new Label(Res.get("settings.network.versionDistribution.version", version, "67"));
        VBox vBox = new VBox(10, label, progressIndicator);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(10));
        versionDistributionHBox.getChildren().add(vBox);
    }
}
