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

import bisq.common.util.MathUtils;
import bisq.desktop.common.converters.Converters;
import bisq.desktop.common.converters.DoubleStringConverter;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.backup.BackupService;
import bisq.settings.SettingsService;
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
    private static final ValidatorBase DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR =
            new NumberValidator(Res.get("settings.network.difficultyAdjustmentFactor.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    NetworkLoad.MIN_DIFFICULTY_ADJUSTMENT, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT);
    private static final ValidatorBase TOTAL_MAX_BACKUP_SIZE_VALIDATOR =
            new NumberValidator(Res.get("settings.backup.totalMaxBackupSizeInMB.invalid", SettingsService.MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB, SettingsService.MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB),
                    SettingsService.MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB, SettingsService.MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB);

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
        difficultyAdjustmentFactor.setValidators(DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR);
        difficultyAdjustmentFactor.setStringConverter(Converters.DOUBLE_STRING_CONVERTER);
        ignoreDiffAdjustFromSecManagerSwitch = new Switch(Res.get("settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager"));

        VBox networkVBox = new VBox(10, difficultyAdjustmentFactor, ignoreDiffAdjustFromSecManagerSwitch);

        Label backupHeadline = new Label(Res.get("settings.backup.headline"));
        backupHeadline.getStyleClass().add("large-thin-headline");

        totalMaxBackupSizeInMB = new MaterialTextField(Res.get("settings.backup.totalMaxBackupSizeInMB.description"));
        totalMaxBackupSizeInMB.setMaxWidth(TEXT_FIELD_WIDTH);
        totalMaxBackupSizeInMB.setValidators(TOTAL_MAX_BACKUP_SIZE_VALIDATOR);
        totalMaxBackupSizeInMB.setStringConverter(Converters.DOUBLE_STRING_CONVERTER);
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
                Converters.DOUBLE_STRING_CONVERTER);
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().bind(model.getDifficultyAdjustmentFactorEditable());
        difficultyAdjustmentFactor.descriptionProperty().bind(model.getDifficultyAdjustmentFactorDescriptionText());

        Bindings.bindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB(),
                new DoubleStringConverter() {
                    @Override
                    public Number fromString(String value) {
                        double result = MathUtils.parseToDouble(value);
                        if(TOTAL_MAX_BACKUP_SIZE_VALIDATOR.validateAndGet()){
                            return result;
                        }else{
                            return BackupService.TOTAL_MAX_BACKUP_SIZE_IN_MB;
                        }
                    }
                });

        ignoreDiffAdjustFromSecManagerSwitchPin = EasyBind.subscribe(
                ignoreDiffAdjustFromSecManagerSwitch.selectedProperty(), s -> difficultyAdjustmentFactor.validate());
    }

    @Override
    protected void onViewDetached() {
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().unbindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());
        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().unbind();
        difficultyAdjustmentFactor.descriptionProperty().unbind();

        Bindings.unbindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB());

        ignoreDiffAdjustFromSecManagerSwitchPin.unsubscribe();
        difficultyAdjustmentFactor.resetValidation();
        totalMaxBackupSizeInMB.resetValidation();
    }
}
