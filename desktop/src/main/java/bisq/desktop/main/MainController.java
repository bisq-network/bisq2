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

package bisq.desktop.main;

import bisq.bonded_roles.alert.AlertService;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.left.LeftNavController;
import bisq.desktop.main.top.TopPanelController;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MainController extends NavigationController {
    @Getter
    private final MainModel model = new MainModel();
    @Getter
    private final MainView view;
    private final ServiceProvider serviceProvider;
    private final LeftNavController leftNavController;
    private final AlertService alertService;
    private final SettingsService settingsService;
    private Pin alertsPin;

    public MainController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MAIN);

        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();

        alertService = serviceProvider.getBondedRolesService().getAlertService();

        leftNavController = new LeftNavController(serviceProvider);
        TopPanelController topPanelController = new TopPanelController(serviceProvider);

        view = new MainView(model,
                this,
                leftNavController.getView().getRoot(),
                topPanelController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        alertsPin = alertService.getAuthorizedDataSet().addListener(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedData authorizedData) {
                if (authorizedData == null) {
                    return;
                }
                UIThread.run(() -> {
                    if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedAlertData) {
                        AuthorizedAlertData authorizedAlertData = (AuthorizedAlertData) authorizedData.getAuthorizedDistributedData();
                        if (settingsService.getConsumedAlertIds().contains(authorizedAlertData.getId())) {
                            return;
                        }
                        settingsService.addConsumedAlertId(authorizedAlertData.getId());
                        switch (authorizedAlertData.getAlertType()) {
                            case INFO:
                                new Popup().attention(authorizedAlertData.getMessage().orElseThrow()).show();
                                break;
                            case WARN:
                                new Popup().warning(authorizedAlertData.getMessage().orElseThrow()).show();
                                break;
                            case EMERGENCY:
                                new Popup().warning(authorizedAlertData.getMessage().orElseThrow()).show();
                                break;
                            case BAN:
                                break;
                        }
                    }
                });
            }

            @Override
            public void remove(Object element) {
            }

            @Override
            public void clear() {
            }
        });
    }

    @Override
    public void onDeactivate() {
        alertsPin.unbind();
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CONTENT: {
                return Optional.of(new ContentController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        leftNavController.setNavigationTarget(navigationTarget);
    }
}
