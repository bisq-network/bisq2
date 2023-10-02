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

package bisq.desktop.overlay.onboarding.password;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class OnboardingPasswordController implements Controller {
    @Getter
    private final OnboardingPasswordView view;
    private final OnboardingPasswordModel model;
    private final UserIdentityService userIdentityService;

    public OnboardingPasswordController(ServiceProvider serviceProvider) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        model = new OnboardingPasswordModel();
        view = new OnboardingPasswordView(model, this);
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        model.getPassword().set("");
        model.getConfirmedPassword().set("");
    }

    void onSetPassword() {
        CharSequence password = model.getPassword().get();
        if (userIdentityService.getAESSecretKey().isPresent()) {
            log.warn("Password is already set. This should not happen in the normal flow of the screens.");
            return;
        }
        if (model.getPasswordIsValid().get() && model.getConfirmedPasswordIsValid().get()) {
            userIdentityService.deriveKeyFromPassword(password)
                    .whenComplete((key, throwable) -> maybeHandleError(throwable))
                    .thenCompose(key -> userIdentityService.encryptDataStore())
                    .whenComplete((encryptedData, throwable) -> {
                        maybeHandleError(throwable);
                        if (throwable == null) {
                            UIThread.run(() -> {
                                OverlayController.hide();
                                UIThread.runOnNextRenderFrame(() -> new Popup().feedback(Res.get("onboarding.password.savePassword.success"))
                                        .onClose(this::close)
                                        .show());
                            });
                        }
                    });
        }
    }

    void onSkip() {
        close();
    }

    private void close() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        UIThread.runOnNextRenderFrame(() -> {
            OverlayController.hide();
            Navigation.navigateTo(NavigationTarget.DASHBOARD);
        });
    }

    private void maybeHandleError(@Nullable Throwable throwable) {
        if (throwable != null) {
            UIThread.run(() -> {
                new Popup().error(throwable).show();
                reset();
            });
        }
    }

    private void reset() {
        model.getPasswordIsMasked().set(true);
        model.getConfirmedPasswordIsMasked().set(true);
        model.getPassword().set("");
        model.getConfirmedPassword().set("");
    }
}
