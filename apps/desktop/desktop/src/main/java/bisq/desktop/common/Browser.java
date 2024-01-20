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

package bisq.desktop.common;

import bisq.common.util.OsUtils;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import javafx.application.HostServices;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class Browser {
    @Nullable
    private static HostServices hostServices;
    private static SettingsService settingsService;

    public static void initialize(HostServices hostServices, SettingsService settingsService) {
        Browser.hostServices = hostServices;
        Browser.settingsService = settingsService;
    }

    public static void open(String url) {
        String id = "hyperlinks.openInBrowser";
        if (DontShowAgainService.showAgain(id)) {
            new Popup().attention(Res.get("hyperlinks.openInBrowser.attention", url))
                    .closeButtonText(Res.get("hyperlinks.openInBrowser.no"))
                    .onClose(() -> {
                        settingsService.setCookie(CookieKey.PERMIT_OPENING_BROWSER, false);
                        ClipboardUtil.copyToClipboard(url);
                    })
                    .actionButtonText(Res.get("confirmation.yes"))
                    .onAction(() -> {
                        settingsService.setCookie(CookieKey.PERMIT_OPENING_BROWSER, true);
                        doOpen(url);
                    })
                    .dontShowAgainId(id)
                    .show();
        } else if (settingsService.getCookie().asBoolean(CookieKey.PERMIT_OPENING_BROWSER).orElse(false)) {
            doOpen(url);
        } else {
            ClipboardUtil.copyToClipboard(url);
        }
    }

    private static void doOpen(String url) {
        checkNotNull(hostServices, "hostServices must be set before doOpen is called");
        try {
            hostServices.showDocument(url);
        } catch (Exception e) {
            log.info("Error at opening {} with hostServices.showDocument. We try to open it via OsUtils.browse.", url, e);

            try {
                OsUtils.browse(url);
            } catch (Exception e2) {
                log.error("Error at opening {} with OsUtils.browse.", url, e);
            }
        }
    }
}