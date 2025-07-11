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
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.overlay.OverlayModel;
import bisq.evolution.updater.DownloadItem;
import bisq.evolution.updater.UpdaterUtils;
import bisq.i18n.Res;
import bisq.presentation.formatters.PercentageFormatter;
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
    private final Label headline, releaseNotesHeadline, furtherInfo, verificationInfo;
    private final Hyperlink downloadUrl;
    private final Button downloadButton, downloadLaterButton, closeButton, shutDownButton;
    private final BisqTableView<ListItem> tableView;
    private final TextArea releaseNotes;
    private final Switch ignoreVersionSwitch;
    private Subscription downloadStartedPin;

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
        releaseNotes.setContextMenu(new ContextMenu());
        releaseNotes.setWrapText(true);
        releaseNotes.getStyleClass().add("updater-release-notes");
        releaseNotes.setEditable(false);
        releaseNotes.setMinHeight(230);

        furtherInfo = new Label();
        furtherInfo.getStyleClass().add("updater-text");

        downloadUrl = new Hyperlink();
        downloadUrl.getStyleClass().add("updater-text");

        downloadButton = new Button(Res.get("updater.download"));
        downloadButton.setDefaultButton(true);

        downloadLaterButton = new Button(Res.get("updater.downloadLater"));

        ignoreVersionSwitch = new Switch(Res.get("updater.ignore"));

        shutDownButton = new Button();
        shutDownButton.setDefaultButton(true);

        closeButton = new Button(Res.get("action.close"));

        HBox buttons = new HBox(20, ignoreVersionSwitch, Spacer.fillHBox(), downloadLaterButton, downloadButton, closeButton, shutDownButton);
        buttons.setAlignment(Pos.CENTER);

        verificationInfo = new Label();
        verificationInfo.setWrapText(true);
        verificationInfo.getStyleClass().add("updater-text");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setPrefHeight(307);
        tableView.getStyleClass().add("updater-table-view");
        configTableView();

        VBox.setMargin(releaseNotes, new Insets(-10, 0, 10, 0));
        VBox.setMargin(downloadUrl, new Insets(-20, 0, 20, 0));
        VBox.setMargin(verificationInfo, new Insets(-10, 0, 0, 0));
        root.getChildren().addAll(headline, releaseNotesHeadline,
                releaseNotes, furtherInfo, downloadUrl,
                verificationInfo, tableView,
                buttons);
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
        releaseNotesHeadline.setText(Res.get("updater.releaseNotesHeadline", model.getVersion().get()));
        releaseNotes.setText(model.getReleaseNotes().get());
        downloadUrl.setText(model.getDownloadUrl().get());

        headline.textProperty().bind(model.getHeadline());
        furtherInfo.textProperty().bind(model.getFurtherInfo());
        verificationInfo.textProperty().bind(model.getVerificationInfo());
        shutDownButton.textProperty().bind(model.getShutDownButtonText());
        shutDownButton.disableProperty().bind(model.getDownloadAndVerifyCompleted().not());
        ignoreVersionSwitch.visibleProperty().bind(model.getIgnoreVersionSwitchVisible());
        ignoreVersionSwitch.managedProperty().bind(model.getIgnoreVersionSwitchVisible());

        downloadStartedPin = EasyBind.subscribe(model.getDownloadStarted(), downloadStarted -> {
            verificationInfo.setVisible(downloadStarted);
            verificationInfo.setManaged(downloadStarted);
            tableView.setVisible(downloadStarted);
            tableView.setManaged(downloadStarted);
            verificationInfo.setVisible(downloadStarted);
            verificationInfo.setManaged(downloadStarted);

            shutDownButton.setVisible(downloadStarted);
            shutDownButton.setManaged(downloadStarted);
            closeButton.setVisible(downloadStarted);
            closeButton.setManaged(downloadStarted);

            releaseNotesHeadline.setVisible(!downloadStarted);
            releaseNotesHeadline.setManaged(!downloadStarted);
            releaseNotes.setVisible(!downloadStarted);
            releaseNotes.setManaged(!downloadStarted);
            furtherInfo.setVisible(!downloadStarted);
            furtherInfo.setManaged(!downloadStarted);
            downloadUrl.setVisible(!downloadStarted);
            downloadUrl.setManaged(!downloadStarted);

            downloadButton.setVisible(!downloadStarted);
            downloadButton.setManaged(!downloadStarted);
            downloadLaterButton.setVisible(!downloadStarted);
            downloadLaterButton.setManaged(!downloadStarted);
        });

        ignoreVersionSwitch.setSelected(model.getIgnoreVersion().getValue());

        downloadUrl.setOnAction(e -> controller.onOpenUrl());
        downloadButton.setOnAction(e -> controller.onDownload());
        downloadLaterButton.setOnAction(e -> controller.onDownloadLater());
        ignoreVersionSwitch.setOnAction(e -> controller.onIgnoreVersionSelected(ignoreVersionSwitch.isSelected()));
        shutDownButton.setOnAction(e -> controller.onShutdown());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
        headline.textProperty().unbind();
        furtherInfo.textProperty().unbind();
        verificationInfo.textProperty().unbind();
        shutDownButton.textProperty().unbind();
        shutDownButton.disableProperty().unbind();
        ignoreVersionSwitch.visibleProperty().unbind();
        ignoreVersionSwitch.managedProperty().unbind();

        downloadUrl.setOnAction(null);
        downloadButton.setOnAction(null);
        downloadLaterButton.setOnAction(null);
        ignoreVersionSwitch.setOnAction(null);
        shutDownButton.setOnAction(null);
        closeButton.setOnAction(null);

        downloadStartedPin.unsubscribe();
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
            protected void updateItem(ListItem item, boolean empty) {
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
                    progressBar.setProgress(0);
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
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    isVerifiedIndicator.selectedProperty().bind(model.getDownloadAndVerifyCompleted());
                    isVerifiedIndicator.visibleProperty().bind(item.getShowVerified());
                    isVerifiedIndicator.managedProperty().bind(item.getShowVerified());
                    isVerifiedIndicator.setMouseTransparent(true);
                    setGraphic(isVerifiedIndicator);
                } else {
                    isVerifiedIndicator.selectedProperty().unbind();
                    isVerifiedIndicator.visibleProperty().unbind();
                    isVerifiedIndicator.managedProperty().unbind();
                    setGraphic(null);
                }
            }
        };
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    static class ListItem {
        @EqualsAndHashCode.Include
        private final DownloadItem downloadItem;

        private final String fileName;
        private final DoubleProperty progress = new SimpleDoubleProperty();
        private final BooleanProperty showVerified = new SimpleBooleanProperty(true);

        ListItem(DownloadItem downloadItem) {
            this.downloadItem = downloadItem;

            fileName = downloadItem.getDestinationFile().getName();
            FxBindings.bind(progress).to(downloadItem.getProgress());
            showVerified.set(UpdaterUtils.isDownloadedFile(downloadItem.getSourceFileName()));
        }
    }
}