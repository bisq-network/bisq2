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

package bisq.desktop.main.content.settings.misc;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MiscSettingsView extends View<VBox, MiscSettingsModel, MiscSettingsController> {
    private static final double TEXT_FIELD_WIDTH = 500;

    private final Switch ignoreDiffAdjustFromSecManagerSwitch;
    private final MaterialTextField difficultyAdjustmentFactor, totalMaxBackupSizeInMB;
    private Subscription ignoreDiffAdjustFromSecManagerSwitchPin;

    public MiscSettingsView(MiscSettingsModel model, MiscSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label networkHeadline = new Label(Res.get("settings.network.headline"));
        networkHeadline.getStyleClass().add("large-thin-headline");

        difficultyAdjustmentFactor = new MaterialTextField();
        difficultyAdjustmentFactor.setMaxWidth(TEXT_FIELD_WIDTH);
        difficultyAdjustmentFactor.setValidators(model.getDifficultyAdjustmentFactorValidator());
        ignoreDiffAdjustFromSecManagerSwitch = new Switch(Res.get("settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager"));

        VBox networkVBox = new VBox(10, difficultyAdjustmentFactor, ignoreDiffAdjustFromSecManagerSwitch);

        Label backupHeadline = new Label(Res.get("settings.backup.headline"));
        backupHeadline.getStyleClass().add("large-thin-headline");

        totalMaxBackupSizeInMB = new MaterialTextField(Res.get("settings.backup.totalMaxBackupSizeInMB.description"));
        totalMaxBackupSizeInMB.setMaxWidth(TEXT_FIELD_WIDTH);
        totalMaxBackupSizeInMB.setValidators(model.getTotalMaxBackupSizeValidator());
        totalMaxBackupSizeInMB.setIcon(AwesomeIcon.INFO_SIGN);
        totalMaxBackupSizeInMB.setIconTooltip(Res.get("settings.backup.totalMaxBackupSizeInMB.info.tooltip"));

        VBox contentBox = new VBox(50);
        contentBox.getChildren().addAll(networkHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), networkVBox,
                backupHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), totalMaxBackupSizeInMB);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().bindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());

        Bindings.bindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor(),
                model.getDifficultyAdjustmentFactorConverter());
        difficultyAdjustmentFactor.validate();
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().bind(model.getDifficultyAdjustmentFactorEditable());
        difficultyAdjustmentFactor.descriptionProperty().bind(model.getDifficultyAdjustmentFactorDescriptionText());

        Bindings.bindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB(),
                model.getTotalMaxBackupSizeConverter());
        totalMaxBackupSizeInMB.validate();

        ignoreDiffAdjustFromSecManagerSwitchPin = EasyBind.subscribe(
                ignoreDiffAdjustFromSecManagerSwitch.selectedProperty(), s -> difficultyAdjustmentFactor.validate());
    }

    @Override
    protected void onViewDetached() {
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().unbindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());

        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());
        difficultyAdjustmentFactor.resetValidation();
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().unbind();
        difficultyAdjustmentFactor.descriptionProperty().unbind();

        Bindings.unbindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB());
        totalMaxBackupSizeInMB.resetValidation();

        ignoreDiffAdjustFromSecManagerSwitchPin.unsubscribe();
    }
}
