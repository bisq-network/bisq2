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

package bisq.desktop.main.left;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.presentation.notifications.NotificationsService;
import bisq.settings.CookieKey;
import bisq.updater.UpdaterService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class LeftNavController implements Controller {
    private final LeftNavModel model;
    @Getter
    private final LeftNavView view;
    private final ChatNotificationService chatNotificationService;
    private final NotificationsService notificationsService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private final UpdaterService updaterService;
    private Pin bondedRolesPin, selectedUserIdentityPin, releaseNotificationPin;

    public LeftNavController(ServiceProvider serviceProvider) {
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        notificationsService = serviceProvider.getNotificationsService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        updaterService = serviceProvider.getUpdaterService();
        model = new LeftNavModel(serviceProvider);
        model.setVersion("v" + serviceProvider.getConfig().getVersion().toString());
        view = new LeftNavView(model, this);
    }

    @Override
    public void onActivate() {
        notificationsService.addListener(this::updateNumNotifications);

        bondedRolesPin = authorizedBondedRolesService.getBondedRoles().addListener(this::onBondedRolesChanged);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> onBondedRolesChanged());

        releaseNotificationPin = updaterService.getReleaseNotification().addObserver(releaseNotification ->
                UIThread.run(() -> model.getNewVersionAvailable().set(releaseNotification != null)));

        model.getMenuHorizontalExpanded().set(model.getSettingsService().getCookie().asBoolean(CookieKey.MENU_HORIZONTAL_EXPANDED).orElse(true));
    }

    @Override
    public void onDeactivate() {
        bondedRolesPin.unbind();
        selectedUserIdentityPin.unbind();
        releaseNotificationPin.unbind();
        notificationsService.removeListener(this::updateNumNotifications);
    }

    public void setNavigationTarget(NavigationTarget navigationTarget) {
        NavigationTarget supportedNavigationTarget;
        Set<NavigationTarget> navigationTargets = model.getNavigationTargets();
        if (navigationTargets.contains(navigationTarget)) {
            supportedNavigationTarget = navigationTarget;
        } else {
            // We get NavigationTarget.CONTENT sometimes due to some timing issues at startup. 
            // If that happens we use the persisted target if present or the default NavigationTarget 
            // otherwise.
            supportedNavigationTarget = navigationTarget.getPath().stream()
                    .filter(navigationTargets::contains)
                    .findAny()
                    .orElse(Navigation.getPersistedNavigationTarget()
                            .orElse(NavigationTarget.DASHBOARD));
        }

        findNavButton(supportedNavigationTarget)
                .ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(supportedNavigationTarget);
    }

    void onNavigationTargetSelected(NavigationTarget navigationTarget) {
        findNavButton(navigationTarget).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    void onToggleHorizontalExpandState() {
        boolean newState = ! model.getMenuHorizontalExpanded().get();
        model.getSettingsService().setCookie(CookieKey.MENU_HORIZONTAL_EXPANDED, newState);
        model.getMenuHorizontalExpanded().set(newState);
    }

    void onNavigationButtonCreated(LeftNavButton leftNavButton) {
        model.getLeftNavButtons().add(leftNavButton);
        model.getNavigationTargets().add(leftNavButton.getNavigationTarget());
    }

    void onOpenUpdateWindow() {
        Navigation.navigateTo(NavigationTarget.UPDATER);
    }

    Optional<LeftNavButton> findNavButton(NavigationTarget navigationTarget) {
        return model.getLeftNavButtons().stream()
                .filter(leftNavButton -> navigationTarget == leftNavButton.getNavigationTarget())
                .findAny();
    }

    private void onBondedRolesChanged() {
        UIThread.run(() -> {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            boolean authorizedRoleVisible = selectedUserIdentity != null &&
                    authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                            .anyMatch(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()));
            if (model.getAuthorizedRoleVisible().get() && !authorizedRoleVisible &&
                    model.getSelectedNavigationTarget().get() == NavigationTarget.AUTHORIZED_ROLE) {
                UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.DASHBOARD));
            }
            model.getAuthorizedRoleVisible().set(authorizedRoleVisible);
        });
    }

    private void updateNumNotifications(String notificationId) {
        UIThread.run(() -> {
            ChatChannelDomain chatChannelDomain = ChatNotificationService.getChatChannelDomain(notificationId);
            findLeftNavButton(chatChannelDomain).ifPresent(leftNavButton ->
                    leftNavButton.setNumNotifications(chatNotificationService.getNumNotificationsMyDomainOrParentDomain(chatChannelDomain)));
        });
    }

    private Optional<LeftNavButton> findLeftNavButton(ChatChannelDomain chatChannelDomain) {
        return findNavigationTarget(chatChannelDomain)
                .flatMap(this::findNavButton);
    }

    private Optional<NavigationTarget> findNavigationTarget(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
            case BISQ_EASY_PRIVATE_CHAT:
                return Optional.of(NavigationTarget.BISQ_EASY);
            case DISCUSSION:
                return Optional.of(NavigationTarget.DISCUSSION);
            case EVENTS:
                return Optional.of(NavigationTarget.EVENTS);
            case SUPPORT:
                return Optional.of(NavigationTarget.SUPPORT);
            default:
                return Optional.empty();
        }
    }
}
