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

package bisq.desktop.overlay.unreadable_store_files;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import bisq.persistence.UnreadableStoreArchive;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// Same layout and style as the restore from backup overlay, which a store whose file cannot be read also triggers
@Slf4j
public class UnreadableStoreFilesView extends View<VBox, UnreadableStoreFilesModel, UnreadableStoreFilesController> {
    private final BisqTableView<ListItem> tableView;
    private final Label directory;
    private final Button closeIconButton;
    private final Button openFolderButton;
    private final Button closeButton;

    public UnreadableStoreFilesView(UnreadableStoreFilesModel model, UnreadableStoreFilesController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(30));

        Label headline = new Label(Res.get("unreadableStoreFiles.headline"));
        headline.getStyleClass().add("restore-from-backup-headline");
        headline.setWrapText(true);

        Label content = new Label(Res.get("unreadableStoreFiles.info", UnreadableStoreArchive.RETENTION.toDays()));
        content.setWrapText(true);
        content.getStyleClass().add("restore-from-backup-content");

        closeIconButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeIconButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(headline, Spacer.fillHBox(), closeIconButton);

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setPrefHeight(200);
        tableView.getStyleClass().add("restore-from-backup-table-view");
        configTableView();

        directory = new Label();
        directory.setWrapText(true);
        directory.getStyleClass().add("restore-from-backup-content");

        openFolderButton = new Button(Res.get("unreadableStoreFiles.openFolder"));
        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);
        HBox buttons = new HBox(10, closeButton, openFolderButton);

        VBox.setMargin(buttons, new Insets(15, 0, 0, 0));
        root.getChildren().addAll(hBox, content, tableView, directory, buttons);
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
        directory.textProperty().bind(model.getDirectory());
        openFolderButton.visibleProperty().bind(model.getDirectoryExists());
        openFolderButton.managedProperty().bind(model.getDirectoryExists());
        closeIconButton.setOnAction(e -> controller.onClose());
        openFolderButton.setOnAction(e -> controller.onOpenFolder());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
        directory.textProperty().unbind();
        openFolderButton.visibleProperty().unbind();
        openFolderButton.managedProperty().unbind();
        closeIconButton.setOnAction(null);
        openFolderButton.setOnAction(null);
        closeButton.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("unreadableStoreFiles.table.store"))
                .isSortable(false)
                .left()
                .minWidth(200)
                .valueSupplier(ListItem::getStore)
                .tooltipSupplier(ListItem::getStore)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("unreadableStoreFiles.table.reason"))
                .isSortable(false)
                .left()
                .minWidth(250)
                .valueSupplier(ListItem::getReason)
                .tooltipSupplier(ListItem::getReason)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("unreadableStoreFiles.table.copy"))
                .isSortable(false)
                .left()
                .valueSupplier(ListItem::getCopy)
                .tooltipSupplier(ListItem::getCopy)
                .build());
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem {
        private final String store;
        private final String reason;
        private final String copy;

        ListItem(String store, String reason, String copy) {
            this.store = store;
            this.reason = reason;
            this.copy = copy;
        }
    }
}
