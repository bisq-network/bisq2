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

import bisq.desktop.common.converters.DoubleStringConverter;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.settings.SettingsService;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MiscSettingsModel implements Model {
    private final DoubleProperty difficultyAdjustmentFactor = new SimpleDoubleProperty();
    private final BooleanProperty difficultyAdjustmentFactorEditable = new SimpleBooleanProperty();
    private final StringProperty difficultyAdjustmentFactorDescriptionText = new SimpleStringProperty();
    private final BooleanProperty ignoreDiffAdjustmentFromSecManager = new SimpleBooleanProperty();
    private final DoubleProperty totalMaxBackupSizeInMB = new SimpleDoubleProperty();

    private final DoubleStringConverter difficultyAdjustmentFactorConverter = new DoubleStringConverter(NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT);
    private final ValidatorBase difficultyAdjustmentFactorValidator =
            new NumberValidator(Res.get("settings.network.difficultyAdjustmentFactor.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    NetworkLoad.MIN_DIFFICULTY_ADJUSTMENT, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT);

    private final DoubleStringConverter totalMaxBackupSizeConverter = new DoubleStringConverter(SettingsService.DEFAULT_TOTAL_MAX_BACKUP_SIZE_IN_MB);
    private final ValidatorBase totalMaxBackupSizeValidator =
            new NumberValidator(Res.get("settings.backup.totalMaxBackupSizeInMB.invalid",
                    SettingsService.MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB, SettingsService.MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB),
                    SettingsService.MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB, SettingsService.MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB);

    public MiscSettingsModel() {
    }
}
