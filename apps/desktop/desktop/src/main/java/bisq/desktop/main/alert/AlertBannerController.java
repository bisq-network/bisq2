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

import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AlertBannerController implements Controller {
    private final AlertBannerModel model;
    @Getter
    private final AlertBannerView view;
    private final SettingsService settingsService;
    private final AlertService alertService;
    private Pin authorizedAlertDataSetPin;

    public AlertBannerController(SettingsService settingsService, AlertService alertService) {
        this.settingsService = settingsService;
        this.alertService = alertService;
        model = new AlertBannerModel();
        view = new AlertBannerView(model, this);
    }

    @Override
    public void onActivate() {
        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData == null) {
                    return;
                }
                UIThread.run(() -> addAlert(authorizedAlertData));
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedAlertData) {
                    UIThread.run(() -> removeAlert((AuthorizedAlertData) element));
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> removeAllAlerts());
            }
        });

        settingsService.getTradeRulesConfirmed().addObserver(e -> {

        });
    }


    @Override
    public void onDeactivate() {
        authorizedAlertDataSetPin.unbind();
    }


    private void addAlert(AuthorizedAlertData authorizedAlertData) {
        if (settingsService.getConsumedAlertIds().contains(authorizedAlertData.getId())) {
            model.getIsAlertVisible().set(false);
            return;
        }

        model.setDisplayedAuthorizedAlertData(authorizedAlertData);
        Optional<String> optionalMessage = authorizedAlertData.getMessage();

        if (optionalMessage.isPresent()) {
            log.info("Showing alert with message {}", optionalMessage.get());
            model.getMessage().set(authorizedAlertData.getMessage().orElseThrow());
            model.getAlertType().set(authorizedAlertData.getAlertType());
            model.getIsAlertVisible().set(true);
        } else {
            log.warn("optionalMessage not present");
            model.getIsAlertVisible().set(false);
        }
    }

    private void removeAlert(AuthorizedAlertData data) {
        model.reset();
    }

    private void removeAllAlerts() {
        model.reset();
    }

    void onClose() {
        settingsService.getConsumedAlertIds().add(model.getDisplayedAuthorizedAlertData().getId());
        model.reset();
    }
}
