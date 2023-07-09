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

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.presentation.notifications.NotificationsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

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
    private Pin bondedRoleSetPin, selectedUserIdentityPin;
    private Subscription tradeAppsSubMenuExpandedPin;

    public LeftNavController(ServiceProvider serviceProvider) {
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        notificationsService = serviceProvider.getNotificationsService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        model = new LeftNavModel(serviceProvider);
        view = new LeftNavView(model, this);
    }

    @Override
    public void onActivate() {
        notificationsService.addListener(this::updateNumNotifications);
        tradeAppsSubMenuExpandedPin = EasyBind.subscribe(model.getTradeAppsSubMenuExpanded(),
                tradeAppsSubMenuExpanded ->
                        notificationsService.getNotConsumedNotificationIds().forEach(this::updateNumNotifications));

        bondedRoleSetPin = authorizedBondedRolesService.getAuthorizedBondedRoleSet().addListener(this::updateAuthorizedRoleVisible);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> updateAuthorizedRoleVisible());
    }

    @Override
    public void onDeactivate() {
        bondedRoleSetPin.unbind();
        selectedUserIdentityPin.unbind();
        tradeAppsSubMenuExpandedPin.unsubscribe();
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

        switch (supportedNavigationTarget) {
            case BISQ_ACADEMY:
            case BITCOIN_ACADEMY:
            case SECURITY_ACADEMY:
            case PRIVACY_ACADEMY:
            case WALLETS_ACADEMY:
            case FOSS_ACADEMY:
                onLearSubMenuExpanded(true);
                break;
            case BISQ_EASY:
            case MULTISIG:
            case SUBMARINE:
            case LIQUID_MULTISIG:
            case LIGHTNING_FIAT:
            case LIQUID_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_ESCROW:
            case MONERO_SWAP:
                onTradeAppsSubMenuExpanded(true);
                break;
        }
    }

    void onNavigationTargetSelected(NavigationTarget navigationTarget) {
        findNavButton(navigationTarget).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    void onToggleHorizontalExpandState() {
        model.getMenuHorizontalExpanded().set(!model.getMenuHorizontalExpanded().get());
    }

    void onNavigationButtonCreated(LeftNavButton leftNavButton) {
        model.getLeftNavButtons().add(leftNavButton);
        model.getNavigationTargets().add(leftNavButton.getNavigationTarget());
    }

    void onTradeAppsSubMenuExpanded(boolean value) {
        model.getTradeAppsSubMenuExpanded().set(value);
    }

    void onLearSubMenuExpanded(boolean value) {
        model.getLearnsSubMenuExpanded().set(value);
    }

    Optional<LeftNavButton> findNavButton(NavigationTarget navigationTarget) {
        return model.getLeftNavButtons().stream()
                .filter(leftNavButton -> navigationTarget == leftNavButton.getNavigationTarget())
                .findAny();
    }

    private void updateAuthorizedRoleVisible() {
        UIThread.run(() -> {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            log.error("selectedUserIdentity " + selectedUserIdentity);
            boolean authorizedRoleVisible = selectedUserIdentity != null &&
                    authorizedBondedRolesService.getAuthorizedBondedRoleSet().stream()
                            .anyMatch(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()));
            model.getAuthorizedRoleVisible().set(authorizedRoleVisible);
        });
    }

    private void updateNumNotifications(String notificationId) {
        UIThread.run(() -> {
            ChatChannelDomain chatChannelDomain = ChatNotificationService.getChatChannelDomain(notificationId);
            findLeftNavButton(chatChannelDomain).ifPresent(leftNavButton ->
                    leftNavButton.setNumNotifications(chatNotificationService.getNumNotificationsByDomain(chatChannelDomain)));
        });
    }

    private Optional<LeftNavButton> findLeftNavButton(ChatChannelDomain chatChannelDomain) {
        return findNavigationTarget(chatChannelDomain)
                .flatMap(this::findNavButton);
    }

    private Optional<NavigationTarget> findNavigationTarget(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY:
                if (model.getTradeAppsSubMenuExpanded().get()) {
                    return Optional.of(NavigationTarget.BISQ_EASY);
                } else {
                    return Optional.of(NavigationTarget.TRADE_OVERVIEW);
                }
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
