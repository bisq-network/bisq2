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

package bisq.bonded_roles.security_manager.alert;

import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.platform.Version;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class AlertNotificationsService implements Service {
    private final SettingsService settingsService;
    private final AlertService alertService;
    @Getter
    private final ObservableSet<AuthorizedAlertData> unconsumedAlerts = new ObservableSet<>();
    @Getter
    private final Observable<Boolean> isAlertBannerVisible = new Observable<>(false);
    private Pin authorizedAlertDataSetPin, unconsumedAlertsPin;

    public AlertNotificationsService(SettingsService settingsService, AlertService alertService) {
        this.settingsService = settingsService;
        this.alertService = alertService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData == null) {
                    return;
                }

                if (shouldProcessAlert(authorizedAlertData)) {
                    unconsumedAlerts.add(authorizedAlertData);
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedAlertData) {
                    unconsumedAlerts.remove((AuthorizedAlertData) element);
                }
            }

            @Override
            public void clear() {
                unconsumedAlerts.clear();
            }
        });

        unconsumedAlertsPin = unconsumedAlerts.addObserver(this::updateIsNotificationBannerVisible);

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (authorizedAlertDataSetPin != null) {
            authorizedAlertDataSetPin.unbind();
            unconsumedAlertsPin.unbind();
        }

        return CompletableFuture.completedFuture(true);
    }

    public void dismissAlert(AuthorizedAlertData authorizedAlertData) {
        checkNotNull(authorizedAlertData, "Cannot dismiss alert because it's null.");
        settingsService.getConsumedAlertIds().add(authorizedAlertData.getId());
        unconsumedAlerts.remove(authorizedAlertData);
    }

    private boolean shouldProcessAlert(AuthorizedAlertData authorizedAlertData) {
        boolean minVersionNotRequiredOrAboveApplicationVersion = true;
        if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.getMinVersion().isPresent()) {
            Version minRequiredVersion = new Version(authorizedAlertData.getMinVersion().get());
            minVersionNotRequiredOrAboveApplicationVersion = ApplicationVersion.getVersion().below(minRequiredVersion);
        }
        return AlertType.isMessageAlert(authorizedAlertData.getAlertType())
                && !settingsService.getConsumedAlertIds().contains(authorizedAlertData.getId())
                && authorizedAlertData.getMessage().isPresent()
                && minVersionNotRequiredOrAboveApplicationVersion;
    }

    private void updateIsNotificationBannerVisible() {
        isAlertBannerVisible.set(!unconsumedAlerts.isEmpty());
    }
}
