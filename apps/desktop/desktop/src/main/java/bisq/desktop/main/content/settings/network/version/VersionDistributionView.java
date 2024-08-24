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

package bisq.desktop.main.content.settings.network.version;

import bisq.common.application.ApplicationVersion;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class VersionDistributionView extends View<VBox, VersionDistributionModel, VersionDistributionController> {
    private final HBox versionDistributionHBox;
    private final BisqTooltip tooltip;

    public VersionDistributionView(VersionDistributionModel model, VersionDistributionController controller) {
        super(new VBox(20), model, controller);

        Label headline = SettingsViewUtils.getHeadline(Res.get("settings.network.version.headline"));

        versionDistributionHBox = new HBox(20);


        Label localVersionHeadline = new Label(Res.get("settings.network.version.localVersion.headline"));
        localVersionHeadline.getStyleClass().add("standard-table-headline");

        Label details = new Label(Res.get("settings.network.version.localVersion.details",
                ApplicationVersion.getVersion().getVersionAsString(),
                ApplicationVersion.getBuildCommitShortHash(),
                ApplicationVersion.getTorVersionString()));

        tooltip = new BisqTooltip();
        Tooltip.install(versionDistributionHBox, tooltip);

        VBox.setMargin(localVersionHeadline, new Insets(10, 0, -10, 0));
        root.getChildren().addAll(headline,
                SettingsViewUtils.getLineAfterHeadline(root.getSpacing()),
                versionDistributionHBox,
                localVersionHeadline,
                details);
    }


    @Override
    protected void onViewAttached() {
        versionDistributionHBox.getChildren().clear();
        model.getVersionDistribution().forEach(pair -> versionDistributionHBox.getChildren().add(addVersion(pair.getFirst(), pair.getSecond())));
        tooltip.setText(model.getVersionDistributionTooltip());
    }

    @Override
    protected void onViewDetached() {
    }

    private static VBox addVersion(String version, double percentage) {
        if (percentage == 1) {
            // We want to avoid the 'check' icon is shown when 100% is reached
            percentage = 0.999999999999;
        }
        ProgressIndicator progressIndicator = new ProgressIndicator(percentage);
        progressIndicator.setMinSize(100, 100);
        Label label = new Label(Res.get("settings.network.version.versionDistribution.version", version, "67"));
        VBox vBox = new VBox(10, label, progressIndicator);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(10));
        return vBox;
    }
}
