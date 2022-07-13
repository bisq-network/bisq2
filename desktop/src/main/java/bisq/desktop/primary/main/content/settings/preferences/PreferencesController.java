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

package bisq.desktop.primary.main.content.settings.preferences;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PreferencesController implements Controller {
    @Getter
    private final PreferencesView view;
    private final SettingsService settingsService;
    private final PreferencesModel model;
    private Pin useAnimationsPin;

    public PreferencesController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        model = new PreferencesModel();
        view = new PreferencesView(model, this);
    }

    @Override
    public void onActivate() {
        useAnimationsPin = FxBindings.bindBiDir(model.getUseAnimations()).to(settingsService.getUseAnimations());
    }

    @Override
    public void onDeactivate() {
        useAnimationsPin.unbind();
    }

    public void onResetDontShowAgain(boolean isSelected) {
        if (isSelected) {
            DontShowAgainService.resetDontShowAgain();
        }
    }
}
