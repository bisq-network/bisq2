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

package bisq.desktop.primary.splash;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplashController implements Controller {
    private final SplashModel model;
    @Getter
    private final SplashView view;

    public SplashController(DefaultApplicationService applicationService) {
        model = new SplashModel();
        view = new SplashView(model, this);

        applicationService.addListener(new DefaultApplicationService.Listener() {
            @Override
            public void onStateChanged(DefaultApplicationService.State state) {
                UIThread.run(() ->  model.getStatus().setValue(
                        Res.get("defaultApplicationService.state." + state.name())));
            }
        });
    }

    @Override
    public void onActivate() { }

    @Override
    public void onDeactivate() { }
}
