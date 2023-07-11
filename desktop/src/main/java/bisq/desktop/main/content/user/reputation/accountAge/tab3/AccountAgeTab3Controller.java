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

package bisq.desktop.main.content.user.reputation.accountAge.tab3;

import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.main.content.user.reputation.accountAge.AccountAgeView;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.AccountAgeService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountAgeTab3Controller implements Controller {
    private final static String PREFIX = "BISQ2_ACCOUNT_AGE:";
    private final AccountAgeTab3Model model;
    @Getter
    private final AccountAgeTab3View view;
    private final UserIdentityService userIdentityService;
    private final AccountAgeView parentView;
    private final AccountAgeService accountAgeService;
    private Pin selectedUserProfilePin;

    public AccountAgeTab3Controller(ServiceProvider serviceProvider, AccountAgeView parentView) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        this.parentView = parentView;
        UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);
        accountAgeService = serviceProvider.getUserService().getReputationService().getAccountAgeService();

        model = new AccountAgeTab3Model();
        this.view = new AccountAgeTab3View(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                chatUserIdentity -> {
                    UIThread.run(() -> {
                        model.getSelectedChatUserIdentity().set(chatUserIdentity);
                        if (chatUserIdentity != null) {
                            model.getPubKeyHash().set(chatUserIdentity.getId());
                        }
                    });
                }
        );
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE_TAB_2);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/accountAge");
    }

    void onCopyToClipboard(String pubKeyHash) {
        ClipboardUtil.copyToClipboard(PREFIX + pubKeyHash);
    }

    void onClose() {
        OverlayController.hide();
    }

    public void onRequestAuthorization() {
        ClipboardUtil.getClipboardString().ifPresent(clipboard -> {
            if (clipboard.startsWith(PREFIX)) {
                String json = clipboard.replace(PREFIX, "");
                boolean success = accountAgeService.requestAuthorization(json);
                if (success) {
                    new Popup().information(Res.get("user.reputation.request.success"))
                            .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                            .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                            .owner(parentView.getRoot())
                            .onClose(this::onClose)
                            .show();
                    return;
                }
            }

            new Popup().warning(Res.get("user.reputation.request.error", StringUtils.truncate(clipboard)))
                    .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                    .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                    .owner(parentView.getRoot())
                    .onClose(this::onClose)
                    .show();
        });
    }
}
