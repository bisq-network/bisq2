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

package bisq.desktop.main.content.authorized_role;

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.desktop.main.content.authorized_role.mediator.MediatorController;
import bisq.desktop.main.content.authorized_role.moderator.ModeratorController;
import bisq.desktop.main.content.authorized_role.release_manager.ReleaseManagerController;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerController;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AuthorizedRoleController extends ContentTabController<AuthorizedRoleModel> {
    @Getter
    private final AuthorizedRoleView view;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final ChatNotificationService chatNotificationService;
    private Pin changedChatNotificationPin, bondedRolesPin, selectedUserIdentityPin;

    public AuthorizedRoleController(ServiceProvider serviceProvider) {
        super(new AuthorizedRoleModel(List.of(BondedRoleType.values())), NavigationTarget.AUTHORIZED_ROLE, serviceProvider);

        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();

        view = new AuthorizedRoleView(model, this);

        onBondedRolesChanged();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        bondedRolesPin = authorizedBondedRolesService.getBondedRoles().addObserver(this::onBondedRolesChanged);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> onBondedRolesChanged());
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        changedChatNotificationPin.unbind();
        bondedRolesPin.unbind();
        selectedUserIdentityPin.unbind();
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MEDIATOR -> Optional.of(new MediatorController(serviceProvider));
            case MODERATOR -> Optional.of(new ModeratorController(serviceProvider));
            case SECURITY_MANAGER -> Optional.of(new SecurityManagerController(serviceProvider));
            case RELEASE_MANAGER -> Optional.of(new ReleaseManagerController(serviceProvider));
            case SEED_NODE, ORACLE_NODE, EXPLORER_NODE, MARKET_PRICE_NODE ->
                    Optional.of(new RoleInfo(serviceProvider).getController());
            default -> Optional.empty();
        };
    }

    private void onBondedRolesChanged() {
        UIThread.run(() -> {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();

            model.getBannedAuthorizedBondedRoles().clear();
            model.getBannedAuthorizedBondedRoles().addAll(authorizedBondedRolesService.getBannedAuthorizedBondedRoleStream()
                    .filter(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()))
                    .map(AuthorizedBondedRole::getBondedRoleType)
                    .collect(Collectors.toSet()));
            // If we got banned we still want to show the admin UI
            model.getAuthorizedBondedRoles().setAll(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                    .filter(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()))
                    .map(AuthorizedBondedRole::getBondedRoleType)
                    .collect(Collectors.toSet()));
        });
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null) {
            return;
        }
        UIThread.run(() -> {
            long numNotifications = bisqEasyNotificationsService.isMediatorsNotification(notification) ?
                    bisqEasyNotificationsService.getMediatorsNotConsumedNotifications().count() :
                    0;

            findTabButton(NavigationTarget.MEDIATOR).ifPresent(tabButton ->
                    tabButton.setNumNotifications(numNotifications));
        });
    }

    private Optional<TabButton> findTabButton(NavigationTarget navigationTarget) {
        return model.getTabButtons().stream()
                .filter(tabButton -> navigationTarget == tabButton.getNavigationTarget())
                .findAny();
    }
}
