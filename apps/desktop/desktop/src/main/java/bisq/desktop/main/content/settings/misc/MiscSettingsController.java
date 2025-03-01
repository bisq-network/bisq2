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

import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MiscSettingsController implements Controller {
    @Getter
    private final MiscSettingsView view;
    private final MiscSettingsModel model;
    private final SettingsService settingsService;
    private final DifficultyAdjustmentService difficultyAdjustmentService;

    private Pin ignoreDiffAdjustmentFromSecManagerPin,
            mostRecentDifficultyAdjustmentFactorOrDefaultPin, difficultyAdjustmentFactorPin, totalMaxBackupSizeInMBPin;
    private Subscription difficultyAdjustmentFactorDescriptionTextPin;

    public MiscSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        difficultyAdjustmentService = serviceProvider.getBondedRolesService().getDifficultyAdjustmentService();
        model = new MiscSettingsModel();
        view = new MiscSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        ignoreDiffAdjustmentFromSecManagerPin = FxBindings.bindBiDir(model.getIgnoreDiffAdjustmentFromSecManager())
                .to(settingsService.getIgnoreDiffAdjustmentFromSecManager(), settingsService::setIgnoreDiffAdjustmentFromSecManager);
        model.getDifficultyAdjustmentFactorEditable().bind(model.getIgnoreDiffAdjustmentFromSecManager());
        difficultyAdjustmentFactorDescriptionTextPin = EasyBind.subscribe(model.getIgnoreDiffAdjustmentFromSecManager(),
                value -> {
                    if (value) {
                        model.getDifficultyAdjustmentFactorDescriptionText().set(Res.get("settings.network.difficultyAdjustmentFactor.description.self"));
                        if (mostRecentDifficultyAdjustmentFactorOrDefaultPin != null) {
                            mostRecentDifficultyAdjustmentFactorOrDefaultPin.unbind();
                        }
                        difficultyAdjustmentFactorPin = FxBindings.bindBiDir(model.getDifficultyAdjustmentFactor())
                                .to(settingsService.getDifficultyAdjustmentFactor(), settingsService::setDifficultyAdjustmentFactor);
                    } else {
                        model.getDifficultyAdjustmentFactorDescriptionText().set(Res.get("settings.network.difficultyAdjustmentFactor.description.fromSecManager"));
                        if (difficultyAdjustmentFactorPin != null) {
                            difficultyAdjustmentFactorPin.unbind();
                        }
                        mostRecentDifficultyAdjustmentFactorOrDefaultPin = difficultyAdjustmentService.getMostRecentValueOrDefault()
                                .addObserver(mostRecentValueOrDefault ->
                                        UIThread.run(() -> model.getDifficultyAdjustmentFactor().set(mostRecentValueOrDefault)));
                    }
                });

        totalMaxBackupSizeInMBPin = FxBindings.bindBiDir(model.getTotalMaxBackupSizeInMB())
                .to(settingsService.getTotalMaxBackupSizeInMB(), settingsService::setTotalMaxBackupSizeInMB);
    }

    @Override
    public void onDeactivate() {
        ignoreDiffAdjustmentFromSecManagerPin.unbind();
        model.getDifficultyAdjustmentFactorEditable().unbind();
        difficultyAdjustmentFactorDescriptionTextPin.unsubscribe();
        totalMaxBackupSizeInMBPin.unbind();
        if (difficultyAdjustmentFactorPin != null) {
            difficultyAdjustmentFactorPin.unbind();
        }
        if (mostRecentDifficultyAdjustmentFactorOrDefaultPin != null) {
            mostRecentDifficultyAdjustmentFactorOrDefaultPin.unbind();
        }
    }
}
