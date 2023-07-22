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
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.overlay.update.service.DownloadInfo;
import bisq.i18n.Res;
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

import java.util.Comparator;

@Slf4j
public class UpdaterView extends View<VBox, UpdaterModel, UpdaterController> {
    private static final double PADDING = 30;
    private final Label version, releaseNodes;
    private final Hyperlink downloadUrl;
    private final Button downloadButton, downloadLaterButton, ignoreButton;
    private final BisqTableView<ListItem> tableView;

    public UpdaterView(UpdaterModel model, UpdaterController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(PADDING));
        //updater.headline=A new Bisq 2 update is available
        //updater.version=Version {0} is available for download
        //updater.gitHub=Please see the release notes for more details at:\n{0}
        //updater.download=Download and verify
        //updater.downloadLater=Download later
        //updater.ignore=Ignore this version
        //updater.table.file=File
        //updater.table.downloadProgress=Download progress
        //updater.table.verified=Verified

        Label headline = new Label(Res.get("updater.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        version = new Label();

        releaseNodes = new Label();
        releaseNodes.setWrapText(true);
        Label gitHub = new Label(Res.get("updater.gitHub"));
        downloadUrl = new Hyperlink();


        downloadButton = new Button(Res.get("updater.download"));
        downloadButton.setDefaultButton(true);
        downloadLaterButton = new Button(Res.get("updater.downloadLater"));
        downloadLaterButton.getStyleClass().add("outlined-button");
        ignoreButton = new Button(Res.get("updater.ignore"));

        HBox buttons = new HBox(20, downloadButton, downloadLaterButton, ignoreButton);

        tableView = new BisqTableView<>(model.getListItems());
        tableView.setMinHeight(200);
        //tableView.getStyleClass().add("user-bonded-roles-table-view");
        configTableView();
        root.getChildren().addAll(headline, version,
                releaseNodes, gitHub, downloadUrl,
                buttons,
                tableView);
    }

    @Override
    protected void onViewAttached() {
        version.setText(Res.get("updater.version", model.getVersion()));
        releaseNodes.setText(model.getReleaseNodes());
        downloadUrl.setText(model.getDownloadUrl());

        downloadButton.setOnAction(e -> controller.onDownload());
        downloadLaterButton.setOnAction(e -> controller.onDownloadLater());
        ignoreButton.setOnAction(e -> controller.onIgnore());
    }

    @Override
    protected void onViewDetached() {
        downloadButton.setOnAction(null);
        downloadLaterButton.setOnAction(null);
        ignoreButton.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("updater.table.file"))
                .minWidth(150)
                .left()
                .comparator(Comparator.comparing(ListItem::getFileName))
                .valueSupplier(ListItem::getFileName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .fixWidth(200)
                .setCellFactory(getProgressCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .fixWidth(250)
                .right()
                .setCellFactory(getIsVerifiedellFactory())
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

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getIsVerifiedellFactory() {
        return column -> new TableCell<>() {
            private final Switch isVerifiedIndicator = new Switch();

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