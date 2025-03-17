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

package bisq.desktop.main.content.settings.language;

import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LanguageSettingsController implements Controller {
    @Getter
    private final LanguageSettingsView view;
    private final LanguageSettingsModel model;
    private final SettingsService settingsService;

    private Pin supportedLanguageCodesPin;

    public LanguageSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new LanguageSettingsModel();
        view = new LanguageSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getLanguageCodes().setAll(LanguageRepository.I18N_CODES);
        model.setSelectedLanguageCode(settingsService.getLanguageCode().get());
        model.getSupportedLanguageCodes().setAll(LanguageRepository.CODES);

        supportedLanguageCodesPin = FxBindings.<String, String>bind(model.getSelectedSupportedLanguageCodes())
                .to(settingsService.getSupportedLanguageCodes());

        model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
    }

    @Override
    public void onDeactivate() {
        supportedLanguageCodesPin.unbind();
    }

    void onSelectLanguage(String languageCode) {
        model.setSelectedLanguageCode(languageCode);
        settingsService.setLanguageCode(languageCode);
        new Popup().feedback(Res.get("settings.language.restart")).useShutDownButton().show();
    }

    String getDisplayLanguage(String languageCode) {
        return LanguageRepository.getDisplayString(languageCode);
    }

    void onSelectSupportedLanguage(String languageCode) {
        if (languageCode != null) {
            model.getSelectedLSupportedLanguageCode().set(languageCode);
        }
    }

    void onAddSupportedLanguage() {
        if (model.getSelectedLSupportedLanguageCode() != null) {
            settingsService.getSupportedLanguageCodes().add(model.getSelectedLSupportedLanguageCode().get());
            model.getSelectedLSupportedLanguageCode().set(null);
            model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
        }
    }

    void onRemoveSupportedLanguage(String languageCode) {
        if (languageCode != null) {
            settingsService.getSupportedLanguageCodes().remove(languageCode);
            model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
        }
    }
}
