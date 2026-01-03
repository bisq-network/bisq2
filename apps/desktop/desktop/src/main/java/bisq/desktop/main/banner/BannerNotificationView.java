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

import bisq.common.data.Pair;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class BannerNotificationView extends View<StackPane, BannerNotificationModel, BannerNotificationController> {
    private static final double BANNER_HEIGHT = 30;

    private final Label requestingInventoryLabel;
    private final BisqTooltip inventoryRequestsTooltip;
    private final HBox inventoryRequestsCompletedHBox;
    private UIScheduler scheduler;
    private Subscription initialInventoryRequestsCompletedPin, inventoryDataChangeFlagPin;

    public BannerNotificationView(BannerNotificationModel model,
                                  BannerNotificationController controller) {
        super(new StackPane(), model, controller);

        Pair<HBox, Label> requestingInventory = getInventoryRequestBox();
        HBox requestingInventoryHBox = requestingInventory.getFirst();
        requestingInventoryHBox.getStyleClass().add("requesting-inventory");
        requestingInventoryLabel = requestingInventory.getSecond();

        Pair<HBox, Label> inventoryRequestsCompleted = getInventoryRequestBox();
        inventoryRequestsCompletedHBox = inventoryRequestsCompleted.getFirst();
        inventoryRequestsCompletedHBox.getStyleClass().add("inventory-requests-completed");
        Label inventoryCompletedLabel = inventoryRequestsCompleted.getSecond();
        inventoryCompletedLabel.setText(Res.get("navigation.network.info.inventoryRequest.completed"));
        inventoryCompletedLabel.setGraphic(ImageUtil.getImageViewById("check-white"));

        inventoryRequestsTooltip = new BisqTooltip();

        root.setMinHeight(BANNER_HEIGHT);
        root.setMaxHeight(BANNER_HEIGHT);
        root.getChildren().addAll(requestingInventoryHBox, inventoryRequestsCompletedHBox);
        root.getStyleClass().add("banner-notification");
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    private Pair<HBox, Label> getInventoryRequestBox() {
        Label infoLabel = new Label();
        infoLabel.getStyleClass().add("inventory-requests-label");

        HBox infoBox = new HBox(infoLabel);
        infoBox.setMinWidth(200);
        infoBox.setMaxWidth(200);
        infoBox.setMinHeight(BANNER_HEIGHT);
        infoBox.setMaxHeight(BANNER_HEIGHT);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox hBox = new HBox(5, Spacer.fillHBox(), infoBox, Spacer.fillHBox());
        hBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(hBox, Priority.ALWAYS);
        return new Pair<>(hBox, infoLabel);
    }

    @Override
    protected void onViewAttached() {
        requestingInventoryLabel.setText(model.getMaxInventoryRequests());
        Tooltip.install(root, inventoryRequestsTooltip);
        inventoryRequestsCompletedHBox.setTranslateY(-BANNER_HEIGHT);

        initialInventoryRequestsCompletedPin = EasyBind.subscribe(model.getInitialInventoryRequestsCompleted(), allInventoryDataReceived -> {
            if (allInventoryDataReceived) {
                Transitions.slideInTop(inventoryRequestsCompletedHBox, 450);
                scheduler = UIScheduler.run(() ->
                        Transitions.animateHeight(root, BANNER_HEIGHT, 0, 450, this::hideBanner)).
                        after(2000);
            }
        });
        inventoryDataChangeFlagPin = EasyBind.subscribe(model.getInventoryDataChangeFlag(), inventoryDataChangeFlag -> {
            requestingInventoryLabel.setText(model.getInventoryRequestsInfo());
            boolean initialInventoryRequestsCompleted = model.getInitialInventoryRequestsCompleted().get();
            String allReceived = initialInventoryRequestsCompleted ? Res.get("confirmation.yes") : Res.get("confirmation.no");
            inventoryRequestsTooltip.setText(
                Res.get("navigation.network.info.inventoryRequests.tooltip",
                        model.getPendingInventoryRequests(),
                        model.getMaxInventoryRequests(),
                        allReceived));
        });
    }

    @Override
    protected void onViewDetached() {
        Tooltip.uninstall(root, inventoryRequestsTooltip);
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }

        inventoryDataChangeFlagPin.unsubscribe();
        initialInventoryRequestsCompletedPin.unsubscribe();
    }

    private void hideBanner() {
        root.setManaged(false);
        root.setVisible(false);
    }
}
