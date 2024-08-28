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

package bisq.desktop.main.content.reputation.build_reputation.signedAccount.tab3;

import bisq.bisq_easy.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.SignedWitnessService;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessTab3Controller implements Controller {
    private final static String PREFIX = "BISQ2_SIGNED_WITNESS:";
    private final SignedWitnessTab3Model model;
    @Getter
    private final SignedWitnessTab3View view;
    private final UserIdentityService userIdentityService;
    private final VBox popupOwner;
    private final SignedWitnessService signedWitnessService;
    private Pin selectedUserProfilePin;

    public SignedWitnessTab3Controller(ServiceProvider serviceProvider, VBox popupOwner) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        this.popupOwner = popupOwner;
        UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider);
        signedWitnessService = serviceProvider.getUserService().getReputationService().getSignedWitnessService();

        model = new SignedWitnessTab3Model();
        this.view = new SignedWitnessTab3View(model, this, userProfileSelection.getRoot());
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

        model.getRequestCertificateButtonDisabled().bind(model.getJsonData().isEmpty());
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
        model.getRequestCertificateButtonDisabled().unbind();
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS_TAB_2);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }

    void onCopyToClipboard(String pubKeyHash) {
        ClipboardUtil.copyToClipboard(pubKeyHash);
    }

    void onClose() {
        OverlayController.hide();
    }

    public void onRequestAuthorization() {
        String jsonData = model.getJsonData().get();
        if (jsonData.startsWith(PREFIX)) {
            jsonData = jsonData.replace(PREFIX, "");
        }

        boolean success = signedWitnessService.requestAuthorization(jsonData);
        if (success) {
            new Popup().information(Res.get("reputation.request.success"))
                    .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                    .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                    .owner(popupOwner)
                    .onClose(this::onClose)
                    .show();
        } else {
            new Popup().warning(Res.get("reputation.request.error", jsonData))
                    .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                    .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                    .owner(popupOwner)
                    .onClose(this::onClose)
                    .show();
        }
    }
}
