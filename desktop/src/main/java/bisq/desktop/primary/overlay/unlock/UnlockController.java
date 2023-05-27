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

package bisq.desktop.primary.overlay.unlock;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UnlockController implements InitWithDataController<UnlockController.InitData> {
    @Getter
    public static final class InitData {
        private final Runnable completeHandler;

        public InitData(Runnable completeHandler) {
            this.completeHandler = completeHandler;
        }
    }

    private final UnlockModel model;
    @Getter
    private final UnlockView view;
    private final DefaultApplicationService applicationService;
    private final UserIdentityService userIdentityService;
    private Subscription pin;
    private Runnable completeHandler;

    public UnlockController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        userIdentityService = applicationService.getUserService().getUserIdentityService();

        model = new UnlockModel();
        view = new UnlockView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        completeHandler = data.getCompleteHandler();
    }

    @Override
    public void onActivate() {
        model.getPasswordIsMasked().set(true);
        model.getPassword().set("");

        pin = EasyBind.subscribe(model.getPassword(),
                password -> model.getUnlockButtonDisabled().set(isPasswordInvalid(password)));
    }

    @Override
    public void onDeactivate() {
        pin.unsubscribe();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    void onUnlock() {
        userIdentityService.decryptDataStore(model.getPassword().get())
                .whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        new Popup().error(throwable.toString()).show();
                        model.getPassword().set("");
                        return;
                    }
                    if (!success) {
                        new Popup().warning(Res.get("unlock.failed")).show();
                        model.getPassword().set("");
                        return;
                    }

                    OverlayController.hide(() -> {
                        if (completeHandler != null) {
                            completeHandler.run();
                        }
                    });
                });
    }

    void onCancel() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    private boolean isPasswordInvalid(String password) {
        return password == null || password.length() < 8;
    }
}
