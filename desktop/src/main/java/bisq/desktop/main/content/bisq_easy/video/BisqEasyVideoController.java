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

package bisq.desktop.main.content.bisq_easy.video;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyVideoController implements Controller {
    private final BisqEasyVideoModel model;
    @Getter
    private final BisqEasyVideoView view;
    private final SettingsService settingsService;
    private Runnable completeHandler;

    public BisqEasyVideoController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new BisqEasyVideoModel();
        view = new BisqEasyVideoView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onClose() {
        OverlayController.hide();
    }

    void onCompleted() {
        settingsService.setCookie(CookieKey.BISQ_EASY_VIDEO_OPENED, true);
    }
}
