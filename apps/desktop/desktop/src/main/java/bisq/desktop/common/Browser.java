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

import bisq.common.platform.PlatformUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.HostServices;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import static bisq.settings.DontShowAgainKey.HYPERLINKS_OPEN_IN_BROWSER;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class Browser {
    @Nullable
    private static HostServices hostServices;
    private static SettingsService settingsService;
    private static DontShowAgainService dontShowAgainService;

    public static void initialize(HostServices hostServices,
                                  SettingsService settingsService,
                                  DontShowAgainService dontShowAgainService) {
        Browser.hostServices = hostServices;
        Browser.settingsService = settingsService;
        Browser.dontShowAgainService = dontShowAgainService;
    }

    public static void open(String url) {
        if (!isWebUrl(url)) {
            // The value can come from text of another user, so we do not log it.
            log.warn("We do not open the link because it is not a web URL with the scheme http or https and a host");
            return;
        }

        if (dontShowAgainService.showAgain(HYPERLINKS_OPEN_IN_BROWSER)) {
            // A link in a chat message comes from another user. java.net.URI accepts "[" and "]" in the query and
            // the fragment, so the URL must not create a link in the popup.
            String displayedUrl = StringUtils.neutralizeHyperlinkMarkers(url);
            new Popup().headline(Res.get("hyperlinks.openInBrowser.attention.headline"))
                    .feedback(Res.get("hyperlinks.openInBrowser.attention", displayedUrl))
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
                    .dontShowAgainId(HYPERLINKS_OPEN_IN_BROWSER)
                    .show();
        } else if (settingsService.getCookie().asBoolean(CookieKey.PERMIT_OPENING_BROWSER).orElse(false)) {
            doOpen(url);
        } else {
            ClipboardUtil.copyToClipboard(url);

            // TODO create custom popup style and animation similar like Bisq1 notifications
            //   See https://github.com/bisq-network/bisq2/issues/1883
            Popup popup = new Popup().notify(Res.get("hyperlinks.copiedToClipboard"));
            popup.show();
            UIScheduler.run(popup::hide).after(3000);
        }
    }

    /**
     * The application opens only a web URL: {@link URI} parses the value, the scheme is http or https in any case,
     * and the URI has a host. The operating system opens other values with other programs, for example a file path
     * or a UNC path ({@code \\host\share\file.exe}) on Windows, or the handler of another scheme.
     * See docs/specifications/desktop/links.md.
     */
    @VisibleForTesting
    static boolean isWebUrl(@Nullable String url) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            return (normalizedScheme.equals("http") || normalizedScheme.equals("https")) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean hyperLinksGetCopiedWithoutPopup() {
        return !dontShowAgainService.showAgain(HYPERLINKS_OPEN_IN_BROWSER) &&
                !settingsService.getCookie().asBoolean(CookieKey.PERMIT_OPENING_BROWSER).orElse(false);
    }

    private static void doOpen(String url) {
        checkNotNull(hostServices, "hostServices must be set before doOpen is called");
        try {
            hostServices.showDocument(url);
        } catch (Exception e) {
            log.info("Error at opening {} with hostServices.showDocument. We try to open it via OsUtils.browse.", url, e);

            try {
                PlatformUtils.browse(url);
            } catch (Exception e2) {
                log.error("Error at opening {} with OsUtils.browse.", url, e);
            }
        }
    }
}
