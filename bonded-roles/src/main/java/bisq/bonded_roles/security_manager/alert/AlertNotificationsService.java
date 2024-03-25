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

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AlertNotificationsService implements Service {
    private final SettingsService settingsService;
    private final AlertService alertService;
    @Getter
    private final Observable<Boolean> isAlertBannerVisible = new Observable<>(false);
    private Pin authorizedAlertDataSetPin;
    @Getter
    private final ObservableSet<AuthorizedAlertData> unconsumedAlerts = new ObservableSet<>();

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

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        authorizedAlertDataSetPin.unbind();

        return CompletableFuture.completedFuture(true);
    }

    public void dismissAlert(AuthorizedAlertData authorizedAlertData) {
        settingsService.getConsumedAlertIds().add(authorizedAlertData.getId());
        unconsumedAlerts.remove(authorizedAlertData);
    }

    private boolean shouldProcessAlert(AuthorizedAlertData authorizedAlertData) {
        return authorizedAlertData.getAlertType() != AlertType.BAN
                && !settingsService.getConsumedAlertIds().contains(authorizedAlertData.getId())
                && authorizedAlertData.getMessage().isPresent();
    }
}
