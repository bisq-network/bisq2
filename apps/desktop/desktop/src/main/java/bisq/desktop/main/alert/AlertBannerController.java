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

package bisq.desktop.main.alert;

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.mu_sig.MusigNotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AlertBannerController implements Controller {
    private final AlertBannerModel model;
    @Getter
    private final AlertBannerView view;
    private final AlertNotificationsService alertNotificationsService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final MusigNotificationService musigNotificationService;
    private Pin unconsumedAlertsPin, isBisqEasyNotificationVisiblePin, isMusigNotificationVisiblePin;

    public AlertBannerController(ServiceProvider serviceProvider) {
        alertNotificationsService = serviceProvider.getAlertNotificationsService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();
        musigNotificationService = serviceProvider.getMuSigService().getMusigNotificationService();
        model = new AlertBannerModel();
        view = new AlertBannerView(model, this);
    }

    @Override
    public void onActivate() {
        unconsumedAlertsPin = alertNotificationsService.getUnconsumedAlerts().addObserver(this::showAlertBanner);
        isBisqEasyNotificationVisiblePin = bisqEasyNotificationsService.getIsNotificationPanelVisible().addObserver(isVisible -> {
            if (isVisible != null) {
                updateIsNotificationPanelVisible();
            }
        });
        isMusigNotificationVisiblePin = musigNotificationService.getIsNotificationPanelVisible().addObserver(isVisible -> {
            if (isVisible != null) {
                updateIsNotificationPanelVisible();
            }
        });
    }

    @Override
    public void onDeactivate() {
        unconsumedAlertsPin.unbind();
        isBisqEasyNotificationVisiblePin.unbind();
        isMusigNotificationVisiblePin.unbind();
    }

    void onClose() {
        UIThread.run(() -> {
            alertNotificationsService.dismissAlert(model.getDisplayedAuthorizedAlertData());
            model.reset();
            showAlertBanner();
        });
    }

    private void showAlertBanner() {
        UIThread.run(() -> {
            Optional<AuthorizedAlertData> mostRelevantAlert = alertNotificationsService.getUnconsumedAlerts().stream()
                    .max(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR);
            if (mostRelevantAlert.isPresent() && !mostRelevantAlert.get().equals(model.getDisplayedAuthorizedAlertData())) {
                model.reset();
                add(mostRelevantAlert.get());
            }
        });
    }

    private void add(AuthorizedAlertData authorizedAlertData) {
        model.setDisplayedAuthorizedAlertData(authorizedAlertData);
        model.getHeadline().set(authorizedAlertData.getHeadline().orElseThrow());
        authorizedAlertData.getMessage().ifPresent(message -> model.getMessage().set(message));
        model.getAlertType().set(authorizedAlertData.getAlertType());
        model.getIsAlertVisible().set(true);
    }

    private void updateIsNotificationPanelVisible() {
        model.getIsNotificationPanelVisible().set(
                bisqEasyNotificationsService.getIsNotificationPanelVisible().get() ||
                        musigNotificationService.getIsNotificationPanelVisible().get());
    }
}
