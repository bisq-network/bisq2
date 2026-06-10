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

package bisq.desktop_ui_harness_app;

import bisq.desktop.automation.DesktopAutomationMetadata;
import bisq.desktop.common.view.View;
import bisq.desktop.common.view.ViewLifecycleObserver;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerView;
import bisq.desktop.main.left.LeftNavView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.onboarding.create_profile.CreateProfileView;
import bisq.desktop.overlay.onboarding.welcome.WelcomeView;
import bisq.desktop.overlay.tac.TacView;
import bisq.desktop.splash.SplashView;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
final class DesktopAutomationViewObserver implements ViewLifecycleObserver {
    private static final Map<NavigationTarget, String> LEFT_NAV_IDS = Map.ofEntries(
            Map.entry(NavigationTarget.DASHBOARD, "dashboard"),
            Map.entry(NavigationTarget.BISQ_EASY, "bisq-easy"),
            Map.entry(NavigationTarget.MU_SIG, "mu-sig"),
            Map.entry(NavigationTarget.REPUTATION, "reputation"),
            Map.entry(NavigationTarget.CONTACTS_LIST, "contacts"),
            Map.entry(NavigationTarget.TRADE_PROTOCOLS, "trade-protocols"),
            Map.entry(NavigationTarget.WALLET, "wallet"),
            Map.entry(NavigationTarget.ACADEMY, "academy"),
            Map.entry(NavigationTarget.CHAT, "chat"),
            Map.entry(NavigationTarget.SUPPORT, "support"),
            Map.entry(NavigationTarget.USER, "user"),
            Map.entry(NavigationTarget.NETWORK, "network"),
            Map.entry(NavigationTarget.SETTINGS, "settings"),
            Map.entry(NavigationTarget.AUTHORIZED_ROLE, "authorized-role")
    );

    @Override
    public void onViewAttached(View<?, ?, ?> view) {
        bind(view);
    }

    private void bind(View<?, ?, ?> view) {
        if (view instanceof ChatMessageContainerView chatView) {
            bindChatMessageContainer(chatView);
        } else if (view instanceof LeftNavView leftNavView) {
            bindLeftNav(leftNavView);
        } else if (view instanceof SplashView splashView) {
            bindSplash(splashView);
        } else if (view instanceof TacView tacView) {
            bindTac(tacView);
        } else if (view instanceof WelcomeView welcomeView) {
            bindWelcome(welcomeView);
        } else if (view instanceof CreateProfileView createProfileView) {
            bindCreateProfile(createProfileView);
        }
    }

    private void bindChatMessageContainer(ChatMessageContainerView view) {
        scope(view.getRoot(), "chat-message-container");
        id(view.getInputField(), "input");
        id(view.getSendButton(), "send");
    }

    private void bindLeftNav(LeftNavView view) {
        scope(view.getMainMenuItems(), "left-nav");
        LEFT_NAV_IDS.forEach((target, automationId) ->
                view.findNavigationButtonNode(target).ifPresent(node -> id(node, automationId)));
    }

    private void bindSplash(SplashView view) {
        scope(view.getRoot(), "splash");
        id(view.getLogo(), "logo");
    }

    private void bindTac(TacView view) {
        scope(view.getRoot(), "tac");
        id(view.getConfirmCheckBox(), "confirm");
        id(view.getAcceptButton(), "accept");
        id(view.getRejectButton(), "reject");
    }

    private void bindWelcome(WelcomeView view) {
        scope(view.getRoot(), "onboarding-welcome");
        id(view.getNextButton(), "next");
    }

    private void bindCreateProfile(CreateProfileView view) {
        scope(view.getRoot(), "create-profile");
        id(view.getNickname().getTextInputControl(), "nickname");
        id(view.getCreateProfileButton(), "create");
    }

    private void scope(Node node, String scope) {
        DesktopAutomationMetadata.setScope(node, scope);
    }

    private void id(Node node, String automationId) {
        DesktopAutomationMetadata.setId(node, automationId);
    }
}
