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

package bisq.desktop.overlay.restore;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestoreFromBackupView extends View<VBox, RestoreFromBackupModel, RestoreFromBackupController> {
    private final BisqTableView<ListItem> tableView;
    private final Button closeIconButton;
    private final Button closeButton;

    public RestoreFromBackupView(RestoreFromBackupModel model, RestoreFromBackupController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(30));

        Label headline = new Label(Res.get("restoreFromBackup.headline"));
        headline.getStyleClass().add("restore-from-backup-headline");
        headline.setWrapText(true);

        Label content = new Label(Res.get("restoreFromBackup.info"));
        content.setWrapText(true);
        content.getStyleClass().add("restore-from-backup-content");

        closeIconButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeIconButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(headline, Spacer.fillHBox(), closeIconButton);

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setPrefHeight(200);
        tableView.getStyleClass().add("restore-from-backup-table-view");
        configTableView();

        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        VBox.setMargin(closeButton, new Insets(15, 0, 0, 0));
        root.getChildren().addAll(hBox, content, tableView, closeButton);
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
        closeIconButton.setOnAction(e -> controller.onClose());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
        closeIconButton.setOnAction(null);
        closeButton.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("restoreFromBackup.table.store"))
                .isSortable(false)
                .left()
                .minWidth(200)
                .valueSupplier(ListItem::getStore)
                .tooltipSupplier(ListItem::getStore)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("restoreFromBackup.table.age"))
                .isSortable(false)
                .left()
                .minWidth(250)
                .valueSupplier(ListItem::getAge)
                .tooltipSupplier(ListItem::getAge)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("restoreFromBackup.table.path"))
                .isSortable(false)
                .left()
                .valueSupplier(ListItem::getPath)
                .tooltipSupplier(ListItem::getPath)
                .build());
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem {
        private final String store;
        private final String timestamp;
        private final String age;
        private final String path;

        public ListItem(String store, String timestamp, String age, String path) {
            this.store = store;
            this.timestamp = timestamp;
            this.age = age;
            this.path = path;
        }
    }
}