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

package bisq.desktop.overlay.update;

import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.update.DownloadInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UpdaterView extends View<VBox, UpdaterModel, UpdaterController> {
    private static final double PADDING = 30;
    private final Label headline, version, releaseNotes, releaseNotesInfo, tableViewHeadline;
    private final Hyperlink downloadUrl;
    private final Button downloadButton, downloadLaterButton, ignoreButton, cancelButton, restartButton;
    private final BisqTableView<ListItem> tableView;
    private Subscription isTableVisiblePin;

    public UpdaterView(UpdaterModel model, UpdaterController controller) {
        super(new VBox(20), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPadding(new Insets(30));
        root.setFillWidth(true);

        headline = new Label(Res.get("updater.headline"));
        headline.getStyleClass().add("updater-headline");

        version = new Label();
        version.getStyleClass().add("updater-sub-headline");

        releaseNotes = new Label();
        releaseNotes.setWrapText(true);
        releaseNotes.getStyleClass().add("updater-text");

        releaseNotesInfo = new Label(Res.get("updater.gitHub"));
        releaseNotesInfo.getStyleClass().add("updater-text");

        downloadUrl = new Hyperlink();
        downloadUrl.getStyleClass().add("updater-text");

        downloadButton = new Button(Res.get("updater.download"));
        downloadButton.setDefaultButton(true);
        downloadLaterButton = new Button(Res.get("updater.downloadLater"));
        downloadLaterButton.getStyleClass().add("outlined-button");
        ignoreButton = new Button(Res.get("updater.ignore"));
        cancelButton = new Button(Res.get("updater.cancel"));
        restartButton = new Button(Res.get("updater.restart"));
        restartButton.setDefaultButton(true);
        HBox buttons = new HBox(20, downloadButton, restartButton, Spacer.fillHBox(), downloadLaterButton, ignoreButton, cancelButton);

        tableViewHeadline = new Label(Res.get("updater.table.headline"));
        tableViewHeadline.getStyleClass().add("updater-sub-headline");

        tableView = new BisqTableView<>(model.getListItems());
        tableView.setMinHeight(200);
        //tableView.getStyleClass().add("updater-table-view");
        configTableView();

        VBox.setMargin(releaseNotes, new Insets(0, 0, 10, 0));
        VBox.setMargin(downloadUrl, new Insets(-20, 0, 30, -5));
        root.getChildren().addAll(headline, version,
                releaseNotes, releaseNotesInfo, downloadUrl,
                tableView,
                buttons);
    }

    @Override
    protected void onViewAttached() {
        version.setText(Res.get("updater.version", model.getVersion().get()));
        releaseNotes.setText(model.getReleaseNotes().get());
        downloadUrl.setText(model.getDownloadUrl().get());

        isTableVisiblePin = EasyBind.subscribe(model.getTableVisible(), isTableVisible -> {
            tableViewHeadline.setVisible(isTableVisible);
            tableViewHeadline.setManaged(isTableVisible);
            tableView.setVisible(isTableVisible);
            tableView.setManaged(isTableVisible);

            version.setVisible(!isTableVisible);
            version.setManaged(!isTableVisible);
            releaseNotes.setVisible(!isTableVisible);
            releaseNotes.setManaged(!isTableVisible);
            releaseNotesInfo.setVisible(!isTableVisible);
            releaseNotesInfo.setManaged(!isTableVisible);
            downloadUrl.setVisible(!isTableVisible);
            downloadUrl.setManaged(!isTableVisible);

            downloadButton.setVisible(!isTableVisible);
            downloadButton.setManaged(!isTableVisible);
            downloadLaterButton.setVisible(!isTableVisible);
            downloadLaterButton.setManaged(!isTableVisible);
            ignoreButton.setVisible(!isTableVisible);
            ignoreButton.setManaged(!isTableVisible);

            cancelButton.setVisible(isTableVisible);
            cancelButton.setManaged(isTableVisible);

            if (isTableVisible) {
                headline.setText(Res.get("updater.table.headline"));
            }
        });

        restartButton.visibleProperty().bind(model.getRestartButtonVisible());
        restartButton.managedProperty().bind(model.getRestartButtonVisible());

        downloadUrl.setOnAction(e -> controller.onOpenUrl());
        downloadButton.setOnAction(e -> controller.onDownload());
        downloadLaterButton.setOnAction(e -> controller.onDownloadLater());
        ignoreButton.setOnAction(e -> controller.onIgnore());
        restartButton.setOnAction(e -> controller.onRestart());
        cancelButton.setOnAction(e -> controller.onCancel());
    }

    @Override
    protected void onViewDetached() {
        restartButton.visibleProperty().unbind();
        restartButton.managedProperty().unbind();

        downloadButton.setOnAction(null);
        downloadLaterButton.setOnAction(null);
        ignoreButton.setOnAction(null);
        restartButton.setOnAction(null);
        cancelButton.setOnAction(null);

        isTableVisiblePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("updater.table.file"))
                .isSortable(false)
                .left()
                .valueSupplier(ListItem::getFileName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("updater.table.progress"))
                .isSortable(false)
                .setCellFactory(getProgressCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("updater.table.verified"))
                .isSortable(false)
                .right()
                .setCellFactory(getIsVerifiedCellFactory())
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getProgressCellFactory() {
        return column -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    progressBar.progressProperty().bind(item.getProgress());
                    setGraphic(progressBar);
                } else {
                    progressBar.progressProperty().unbind();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getIsVerifiedCellFactory() {
        return column -> new TableCell<>() {
            private final CheckBox isVerifiedIndicator = new CheckBox();

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    isVerifiedIndicator.selectedProperty().bind(item.getIsVerified());
                    isVerifiedIndicator.setMouseTransparent(true);
                    setGraphic(isVerifiedIndicator);
                } else {
                    isVerifiedIndicator.selectedProperty().unbind();
                    setGraphic(null);
                }
            }
        };
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem implements TableItem {
        private final String fileName;
        private final DoubleProperty progress = new SimpleDoubleProperty();
        private final BooleanProperty isVerified = new SimpleBooleanProperty();

        ListItem(DownloadInfo downloadInfo) {
            fileName = downloadInfo.getFileName();
            FxBindings.bind(progress).to(downloadInfo.getProgress());
            FxBindings.bind(isVerified).to(downloadInfo.getIsVerified());
        }
    }
}