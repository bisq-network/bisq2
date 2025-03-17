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

package bisq.desktop.main.content.settings.display;

import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisplaySettingsController implements Controller {
    @Getter
    private final DisplaySettingsView view;
    private final DisplaySettingsModel model;
    private final SettingsService settingsService;
    private final DontShowAgainService dontShowAgainService;
    private Pin useAnimationsPin, preventStandbyModePin;

    public DisplaySettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();
        model = new DisplaySettingsModel();
        view = new DisplaySettingsView(model, this);
    }

    @Override
    public void onActivate() {
        useAnimationsPin = FxBindings.bindBiDir(model.getUseAnimations())
                .to(settingsService.getUseAnimations(), settingsService::setUseAnimations);
        preventStandbyModePin = FxBindings.bindBiDir(model.getPreventStandbyMode())
                .to(settingsService.getPreventStandbyMode(), settingsService::setPreventStandbyMode);
    }

    @Override
    public void onDeactivate() {
        useAnimationsPin.unbind();
        preventStandbyModePin.unbind();
    }

    void onResetDontShowAgain() {
        dontShowAgainService.resetDontShowAgain();
    }

    String getDisplayLanguage(String languageCode) {
        return LanguageRepository.getDisplayString(languageCode);
    }
}
