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
import bisq.presentation.formatters.PercentageFormatter;
import bisq.update.DownloadDescriptor;
import bisq.update.UpdateService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final Label headline, releaseNotesHeadline, releaseNotesInfo, tableViewHeadline;
    private final Hyperlink downloadUrl;
    private final Button downloadButton, downloadLaterButton, ignoreButton, shutDownButton;
    private final BisqTableView<ListItem> tableView;
    private final TextArea releaseNotes;
    private Subscription isTableVisiblePin;

    public UpdaterView(UpdaterModel model, UpdaterController controller) {
        super(new VBox(20), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.setPadding(new Insets(30));
        root.setFillWidth(true);

        headline = new Label();
        headline.getStyleClass().add("updater-headline");

        releaseNotesHeadline = new Label();
        releaseNotesHeadline.getStyleClass().add("updater-sub-headline");

        releaseNotes = new TextArea();
        releaseNotes.setWrapText(true);
        releaseNotes.getStyleClass().add("updater-release-notes");
        releaseNotes.setEditable(false);
        releaseNotes.setMinHeight(210);

        releaseNotesInfo = new Label(Res.get("updater.gitHub"));
        releaseNotesInfo.getStyleClass().add("updater-text");

        downloadUrl = new Hyperlink();
        downloadUrl.getStyleClass().add("updater-text");

        downloadButton = new Button(Res.get("updater.download"));
        downloadButton.setDefaultButton(true);
        downloadLaterButton = new Button(Res.get("updater.downloadLater"));
        downloadLaterButton.getStyleClass().add("outlined-button");
        ignoreButton = new Button(Res.get("updater.ignore"));
        shutDownButton = new Button(Res.get("updater.shutDown"));
        shutDownButton.setDefaultButton(true);
        HBox buttons = new HBox(20, downloadButton, Spacer.fillHBox(), downloadLaterButton, ignoreButton, shutDownButton);

        tableViewHeadline = new Label(Res.get("updater.table.headline"));
        tableViewHeadline.getStyleClass().add("updater-sub-headline");

        tableView = new BisqTableView<>(model.getListItems());
        tableView.setPrefHeight(307);
        tableView.getStyleClass().add("updater-table-view");
        configTableView();

        VBox.setMargin(releaseNotesHeadline, new Insets(0, 0, -10, 0));
        VBox.setMargin(downloadUrl, new Insets(-20, 0, 30, -5));
        root.getChildren().addAll(headline, releaseNotesHeadline,
                releaseNotes, releaseNotesInfo, downloadUrl,
                tableView,
                buttons);
    }

    @Override
    protected void onViewAttached() {
        headline.setText(Res.get("updater.headline", model.getVersion().get()));
        releaseNotesHeadline.setText(Res.get("updater.releaseNotesHeadline"));
        releaseNotes.setText(model.getReleaseNotes().get());
        downloadUrl.setText(model.getDownloadUrl().get());

        isTableVisiblePin = EasyBind.subscribe(model.getTableVisible(), isTableVisible -> {
            tableViewHeadline.setVisible(isTableVisible);
            tableViewHeadline.setManaged(isTableVisible);
            tableView.setVisible(isTableVisible);
            tableView.setManaged(isTableVisible);
            shutDownButton.setVisible(isTableVisible);
            shutDownButton.setManaged(isTableVisible);

            releaseNotesHeadline.setVisible(!isTableVisible);
            releaseNotesHeadline.setManaged(!isTableVisible);
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

            if (isTableVisible) {
                headline.setText(Res.get("updater.table.headline"));
            }
        });

        shutDownButton.disableProperty().bind(model.getDownloadAndVerifyCompleted().not());

        downloadUrl.setOnAction(e -> controller.onOpenUrl());
        downloadButton.setOnAction(e -> controller.onDownload());
        downloadLaterButton.setOnAction(e -> controller.onDownloadLater());
        ignoreButton.setOnAction(e -> controller.onIgnore());
        shutDownButton.setOnAction(e -> controller.onRestart());
    }

    @Override
    protected void onViewDetached() {
        shutDownButton.disableProperty().unbind();

        downloadButton.setOnAction(null);
        downloadLaterButton.setOnAction(null);
        ignoreButton.setOnAction(null);
        shutDownButton.setOnAction(null);

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
                .left()
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
            private Subscription progressPin;
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final HBox hBox = new HBox(20, progressBar, label);

            {
                progressBar.setMinHeight(5);
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    progressPin = EasyBind.subscribe(item.getProgress(), progress -> {
                        if (progress != null) {
                            double value = progress.doubleValue();
                            progressBar.setProgress(value);
                            if (value == 1) {
                                label.setText(Res.get("updater.table.progress.completed"));
                            } else {
                                label.setText(PercentageFormatter.formatToPercentWithSymbol(Math.max(0, value)));
                            }
                        }
                    });
                    setGraphic(hBox);
                } else {
                    if (progressPin != null) {
                        progressPin.unsubscribe();
                    }
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
                    isVerifiedIndicator.selectedProperty().bind(model.getDownloadAndVerifyCompleted());
                    isVerifiedIndicator.visibleProperty().bind(item.getShowVerified());
                    isVerifiedIndicator.managedProperty().bind(item.getShowVerified());
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
        private final BooleanProperty showVerified = new SimpleBooleanProperty(true);

        ListItem(DownloadDescriptor downloadDescriptor) {
            fileName = downloadDescriptor.getSourceFileName();
            FxBindings.bind(progress).to(downloadDescriptor.getProgress());
            showVerified.set((downloadDescriptor.getDestination().getName().equals(UpdateService.DESTINATION_FILE_NAME)));
        }
    }
}