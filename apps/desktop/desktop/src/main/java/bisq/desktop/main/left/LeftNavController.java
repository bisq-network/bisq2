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

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.bisq_easy.ChatChannelDomainNavigationTargetMapper;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.ApplicationVersion;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.evolution.updater.UpdaterService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LeftNavController implements Controller {
    private final LeftNavModel model;
    @Getter
    private final LeftNavView view;
    private final ChatNotificationService chatNotificationService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private final UpdaterService updaterService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final SettingsService settingsService;
    private Pin bondedRolesPin, selectedUserIdentityPin, releaseNotificationPin;
    private Pin changedChatNotificationPin;

    public LeftNavController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        updaterService = serviceProvider.getUpdaterService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();

        NetworkInfo networkInfo = new NetworkInfo(serviceProvider, this::onNavigationTargetSelected);
        model = new LeftNavModel(serviceProvider.getWalletService().isPresent());
        model.setVersion("v" + ApplicationVersion.getVersion().toString());
        view = new LeftNavView(model, this, networkInfo.getRoot());
    }

    @Override
    public void onActivate() {
        bondedRolesPin = authorizedBondedRolesService.getBondedRoles().addObserver(this::onBondedRolesChanged);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> onBondedRolesChanged());

        releaseNotificationPin = updaterService.getReleaseNotification().addObserver(releaseNotification ->
                UIThread.run(() -> model.getNewVersionAvailable().set(releaseNotification != null)));

        model.getMenuHorizontalExpanded().set(settingsService.getCookie().asBoolean(CookieKey.MENU_HORIZONTAL_EXPANDED).orElse(true));
    }

    @Override
    public void onDeactivate() {
        bondedRolesPin.unbind();
        selectedUserIdentityPin.unbind();
        releaseNotificationPin.unbind();
        changedChatNotificationPin.unbind();
    }

    public void setNavigationTarget(NavigationTarget navigationTarget) {
        if (changedChatNotificationPin == null) {
            chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
            changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
        }

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

    private void handleNotification(ChatNotification notification) {
        if (notification == null) {
            return;
        }

        UIThread.run(() -> {
            // todo (deferred) add moderators notification support
            AtomicLong numMediatorsNotConsumedNotifications = new AtomicLong();
            findNavButton(NavigationTarget.AUTHORIZED_ROLE).ifPresent(authorizedRoleButton -> {
                numMediatorsNotConsumedNotifications.set(bisqEasyNotificationsService.getMediatorsNotConsumedNotifications().count());
                authorizedRoleButton.setNumNotifications(numMediatorsNotConsumedNotifications.get());
                if (!authorizedRoleButton.getNumMessagesBadge().getStyleClass().contains("open-trades-badge")) {
                    authorizedRoleButton.getNumMessagesBadge().getStyleClass().add("open-trades-badge");
                }
            });

            findLeftNavButton(notification.getChatChannelDomain()).ifPresent(leftNavButton -> {
                NavigationTarget navigationTarget = leftNavButton.getNavigationTarget();
                long numNotifications = bisqEasyNotificationsService.getNumNotifications(navigationTarget);
                if (notification.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK || notification.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OPEN_TRADES || notification.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT) {
                    // In case we are a mediator we ignore the notifications in the BISQ_EASY_OPEN_TRADES as those
                    // we handle in the mediation view.
                    numNotifications = Math.max(0, numNotifications - numMediatorsNotConsumedNotifications.get());
                }

                leftNavButton.setNumNotifications(numNotifications);
                leftNavButton.getNumMessagesBadge().getStyleClass().remove("open-trades-badge");
                switch (notification.getChatChannelDomain()) {
                    case BISQ_EASY_OFFERBOOK:
                    case BISQ_EASY_OPEN_TRADES:
                    case BISQ_EASY_PRIVATE_CHAT:
                        if (bisqEasyNotificationsService.hasTradeIdsOfNotConsumedNotifications()) {
                            leftNavButton.getNumMessagesBadge().getStyleClass().add("open-trades-badge");
                        }
                        break;
                    case DISCUSSION:
                    case EVENTS:
                    case SUPPORT:
                        break;
                }
            });
        });
    }

    void onNavigationTargetSelected(NavigationTarget navigationTarget) {
        findNavButton(navigationTarget).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    void onToggleHorizontalExpandState() {
        boolean newState = !model.getMenuHorizontalExpanded().get();
        settingsService.setCookie(CookieKey.MENU_HORIZONTAL_EXPANDED, newState);
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
            // If we got banned we still want to show the admin UI
            boolean authorizedRoleVisible = authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                    .anyMatch(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()));
            if (model.getAuthorizedRoleVisible().get() && !authorizedRoleVisible &&
                    model.getSelectedNavigationTarget().get() == NavigationTarget.AUTHORIZED_ROLE) {
                UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.DASHBOARD));
            }
            model.getAuthorizedRoleVisible().set(authorizedRoleVisible);
        });
    }


    private Optional<LeftNavButton> findLeftNavButton(ChatChannelDomain chatChannelDomain) {
        return ChatChannelDomainNavigationTargetMapper.fromChatChannelDomain(chatChannelDomain)
                .flatMap(this::findNavButton);
    }
}
