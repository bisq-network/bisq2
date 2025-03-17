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

package bisq.desktop.main.content.authorized_role.release_manager;

import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.observable.Pin;
import bisq.common.platform.Version;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.i18n.Res;
import bisq.support.release_manager.ReleaseManagerService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public class ReleaseManagerController implements Controller {
    @Getter
    private final ReleaseManagerView view;
    private final ReleaseManagerModel model;
    private final UserIdentityService userIdentityService;
    private final ReleaseNotificationsService releaseNotificationsService;
    private final ReleaseManagerService releaseManagerService;
    private Pin getReleaseNotificationsPin;

    public ReleaseManagerController(ServiceProvider serviceProvider) {
        releaseManagerService = serviceProvider.getSupportService().getReleaseManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        UserProfileService userProfileService = serviceProvider.getUserService().getUserProfileService();
        releaseNotificationsService = serviceProvider.getBondedRolesService().getReleaseNotificationsService();
        RoleInfo roleInfo = new RoleInfo(serviceProvider);
        model = new ReleaseManagerModel();
        view = new ReleaseManagerView(model, this, roleInfo.getRoot());
    }

    @Override
    public void onActivate() {
        model.getIsLauncherUpdate().set(true);
        getReleaseNotificationsPin = FxBindings.<ReleaseNotification, ReleaseManagerView.ListItem>bind(model.getListItems())
                .map(ReleaseManagerView.ListItem::new)
                .to(releaseNotificationsService.getReleaseNotifications());

        model.getActionButtonDisabled().bind(model.getReleaseNotes().isEmpty().or(model.getVersion().isEmpty()));


        KeyPair keyPair = userIdentityService.getSelectedUserIdentity().getIdentity().getKeyBundle().getKeyPair();
        releaseNotificationsService.getReleaseNotifications().forEach(releaseNotification ->
                releaseManagerService.republishReleaseNotification(releaseNotification, keyPair));
    }

    @Override
    public void onDeactivate() {
        getReleaseNotificationsPin.unbind();
        model.getActionButtonDisabled().unbind();
    }

    void onSendReleaseNotification() {
        String releaseNotes = model.getReleaseNotes().get();
        if (releaseNotes != null && releaseNotes.length() > ReleaseNotification.MAX_MESSAGE_LENGTH) {
            new Popup().warning(Res.get("validation.tooLong", ReleaseNotification.MAX_MESSAGE_LENGTH)).show();
            return;
        }
        Version.validate(model.getVersion().get());
        releaseManagerService.publishReleaseNotification(model.getIsPreRelease().get(),
                        model.getIsLauncherUpdate().get(),
                        releaseNotes,
                        model.getVersion().get())
                .whenComplete((result, throwable) -> UIThread.run(() -> {
                    if (throwable != null) {
                        new Popup().error(throwable).show();
                    } else {
                        model.getIsPreRelease().set(false);
                        model.getIsLauncherUpdate().set(true);
                        model.getReleaseNotes().set(null);
                        model.getVersion().set(null);
                    }
                }));
    }

    void onRemoveReleaseNotification(ReleaseNotification releaseNotification) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        releaseManagerService.removeReleaseNotification(releaseNotification, userIdentity.getNetworkIdWithKeyPair().getKeyPair());
    }

    boolean isRemoveButtonVisible(ReleaseNotification releaseNotification) {
        return userIdentityService.getSelectedUserIdentity().getId().equals(releaseNotification.getReleaseManagerProfileId());
    }
}
