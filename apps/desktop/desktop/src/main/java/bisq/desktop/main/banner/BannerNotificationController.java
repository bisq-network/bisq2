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

package bisq.desktop.main.banner;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BannerNotificationController implements Controller {
    @Getter
    private final BannerNotificationView view;
    private final BannerNotificationModel model;
    private final NetworkService networkService;
    private Pin numPendingInventoryRequestsPin, initialInventoryRequestsCompletedPin;
    private UIScheduler inventoryRequestAnimation;

    public BannerNotificationController(ServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();

        model = new BannerNotificationModel();
        view = new BannerNotificationView(model, this);
    }

    @Override
    public void onActivate() {
        networkService.getSupportedTransportTypes().forEach(type ->
                networkService.getServiceNodesByTransport().findServiceNode(type)
                        .flatMap(serviceNode -> serviceNode.getPeerGroupManager()
                        .flatMap(peerGroupManager -> serviceNode.getInventoryService()))
                        .ifPresent(this::applyInventoryInfo));
    }

    @Override
    public void onDeactivate() {
        if (numPendingInventoryRequestsPin != null) {
            numPendingInventoryRequestsPin.unbind();
        }
        if (initialInventoryRequestsCompletedPin != null) {
            initialInventoryRequestsCompletedPin.unbind();
        }
        if (inventoryRequestAnimation != null) {
            inventoryRequestAnimation.stop();
            inventoryRequestAnimation = null;
        }
    }

    private void applyInventoryInfo(InventoryService inventoryService) {
        numPendingInventoryRequestsPin = inventoryService.getNumPendingInventoryRequests().addObserver(numPendingInventoryRequests -> {
            if (numPendingInventoryRequests != null) {
                UIThread.run(() -> {
                    model.setPendingInventoryRequests(String.valueOf(numPendingInventoryRequests));
                    updateInventoryDataChangeFlag();
                });
            }
        });
        initialInventoryRequestsCompletedPin = inventoryService.getInitialInventoryRequestsCompleted().addObserver(initialInventoryRequestsCompleted -> {
            if (initialInventoryRequestsCompleted != null) {
                UIThread.run(() -> {
                    model.getInitialInventoryRequestsCompleted().set(initialInventoryRequestsCompleted);

                    if (initialInventoryRequestsCompleted) {
                        if (inventoryRequestAnimation != null) {
                            inventoryRequestAnimation.stop();
                        }
                    }
                    updateInventoryDataChangeFlag();
                });
            }
        });
        inventoryRequestAnimation = UIScheduler.run(() -> {
            StringBuilder dots = new StringBuilder();
            long numDots = inventoryRequestAnimation.getCounter() % 6;
            for (long l = 0; l < numDots; l++) {
                dots.append(".");
            }
            if (!inventoryService.getInitialInventoryRequestsCompleted().get()) {
                model.setInventoryRequestsInfo(Res.get("navigation.network.info.inventoryRequest.requesting") + dots);
                updateInventoryDataChangeFlag();
            }
        }).periodically(250);


        model.setMaxInventoryRequests(String.valueOf(inventoryService.getConfig().getMaxPendingRequests()));
        updateInventoryDataChangeFlag();
    }

    private void updateInventoryDataChangeFlag() {
        model.getInventoryDataChangeFlag().set(!model.getInventoryDataChangeFlag().get());
    }
}
