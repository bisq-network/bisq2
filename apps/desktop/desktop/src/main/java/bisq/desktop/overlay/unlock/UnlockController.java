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

package bisq.desktop.overlay.unlock;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnlockController implements InitWithDataController<UnlockController.InitData> {
    private final OverlayController overlayController;

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
    private final ServiceProvider serviceProvider;
    private final UserIdentityService userIdentityService;
    private Runnable completeHandler;

    public UnlockController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        overlayController = OverlayController.getInstance();
        model = new UnlockModel();
        view = new UnlockView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        completeHandler = data.getCompleteHandler();
    }

    @Override
    public void onActivate() {
        overlayController.setEnterKeyHandler(null);
        overlayController.setUseEscapeKeyHandler(false);
        model.getPasswordIsMasked().set(true);
        model.getPassword().set("");
    }

    @Override
    public void onDeactivate() {
    }

    void onUnlock() {
        if (view.validatePassword()) {
            userIdentityService.deriveKeyFromPassword(model.getPassword().get())
                    .whenComplete((aesSecretKey, throwable) -> {
                        if (throwable == null) {
                            userIdentityService.decryptDataStore(aesSecretKey)
                                    .whenComplete((nil, throwable2) -> {
                                        if (throwable2 == null) {
                                            OverlayController.hide(() -> {
                                                if (completeHandler != null) {
                                                    completeHandler.run();
                                                }
                                            });
                                        } else {
                                            handleError();
                                        }
                                    });
                        } else {
                            handleError();
                        }
                    });
        }
    }

    void onCancel() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    private void handleError() {
        OverlayController.hide(() -> {
            new Popup().warning(Res.get("unlock.failed"))
                    .onClose(() -> Navigation.navigateTo(NavigationTarget.UNLOCK))
                    .show();
            model.getPassword().set("");
            view.resetValidation();
        });
    }
}
