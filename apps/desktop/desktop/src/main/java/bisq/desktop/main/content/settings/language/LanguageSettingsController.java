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
import bisq.common.util.StringUtils;
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

    private Pin supportedLanguageTagsPin;

    public LanguageSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new LanguageSettingsModel();
        view = new LanguageSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getLanguageTags().setAll(LanguageRepository.LANGUAGE_TAGS);
        model.setSelectedLanguageTag(settingsService.getLanguageTag().get());
        model.getSupportedLanguageTags().setAll(LanguageRepository.LANGUAGE_TAGS);

        supportedLanguageTagsPin = FxBindings.<String, String>bind(model.getSelectedSupportedLanguageTags())
                .to(settingsService.getSupportedLanguageTags());

        model.getSupportedLanguageTagsFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageTags().contains(e));
    }

    @Override
    public void onDeactivate() {
        supportedLanguageTagsPin.unbind();
    }

    void onSelectLanguageTag(String languageTag) {
        model.setSelectedLanguageTag(languageTag);
        settingsService.setLanguageTag(languageTag);
        new Popup().feedback(Res.get("settings.language.restart")).useShutDownButton().show();
    }

    String getDisplayLanguage(String languageTag) {
        return LanguageRepository.getDisplayString(languageTag);
    }

    void onSelectSupportedLanguage(String languageTag) {
        if (languageTag != null) {
            model.getSelectedLSupportedLanguageTag().set(languageTag);
        }
    }

    void onAddSupportedLanguage() {
        String selected = model.getSelectedLSupportedLanguageTag().get();
        if (StringUtils.isNotEmpty(selected)) {
            settingsService.getSupportedLanguageTags().add(selected);
            model.getSelectedLSupportedLanguageTag().set(null);
            model.getSupportedLanguageTagsFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageTags().contains(e));
        }
    }

    void onRemoveSupportedLanguage(String languageTag) {
        if (StringUtils.isNotEmpty(languageTag)) {
            settingsService.getSupportedLanguageTags().remove(languageTag);
            model.getSupportedLanguageTagsFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageTags().contains(e));
        }
    }
}
