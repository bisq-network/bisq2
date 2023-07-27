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

package bisq.desktop.main.content.settings.preferences;

import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PreferencesController implements Controller {
    @Getter
    private final PreferencesView view;
    private final SettingsService settingsService;
    private final PreferencesModel model;
    private Pin chatNotificationTypePin, useAnimationsPin, getPreventStandbyModePin, closeMyOfferWhenTakenPin;
    private Subscription notifyForPreReleasePin;

    public PreferencesController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new PreferencesModel();
        view = new PreferencesView(model, this);
    }

    @Override
    public void onActivate() {
        model.getLanguageCodes().setAll(LanguageRepository.I18N_CODES);
        model.setSelectedLanguageCode(settingsService.getLanguageCode());

        chatNotificationTypePin = FxBindings.bindBiDir(model.getChatNotificationType()).to(settingsService.getChatNotificationType());
        useAnimationsPin = FxBindings.bindBiDir(model.getUseAnimations()).to(settingsService.getUseAnimations());
        getPreventStandbyModePin = FxBindings.bindBiDir(model.getPreventStandbyMode()).to(settingsService.getPreventStandbyMode());
        closeMyOfferWhenTakenPin = FxBindings.bindBiDir(model.getCloseMyOfferWhenTaken()).to(settingsService.getCloseMyOfferWhenTaken());

        model.getNotifyForPreRelease().set(settingsService.getCookie().asBoolean(CookieKey.NOTIFY_FOR_PRE_RELEASE).orElse(false));
        notifyForPreReleasePin = EasyBind.subscribe(model.getNotifyForPreRelease(),
                notifyForPreRelease -> settingsService.setCookie(CookieKey.NOTIFY_FOR_PRE_RELEASE, notifyForPreRelease));
    }

    @Override
    public void onDeactivate() {
        chatNotificationTypePin.unbind();
        useAnimationsPin.unbind();
        closeMyOfferWhenTakenPin.unbind();
        getPreventStandbyModePin.unbind();
        notifyForPreReleasePin.unsubscribe();
    }

    void onSelectLanguage(String selectedLanguageCode) {
        model.setSelectedLanguageCode(selectedLanguageCode);
        settingsService.setLanguageCode(selectedLanguageCode);
        new Popup().feedback(Res.get("settings.preferences.language.restart")).useShutDownButton().show();
    }

    void onResetDontShowAgain() {
        DontShowAgainService.resetDontShowAgain();
    }

    void onSetChatNotificationType(ChatNotificationType type) {
        model.getChatNotificationType().set(type);
    }

    String getDisplayLanguage(String languageCode) {
        return LanguageRepository.getDisplayString(languageCode);
    }
}
