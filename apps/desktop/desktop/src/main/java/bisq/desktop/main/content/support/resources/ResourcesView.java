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

package bisq.desktop.main.content.support.resources;

import bisq.common.application.ApplicationVersion;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqHyperlink;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ResourcesView extends View<VBox, ResourcesModel, ResourcesController> {
    private final Button setBackupLocationButton, backupButton;
    private final MaterialTextField backupLocation;
    private final Hyperlink learnMoreAutomaticBackup, webpage, dao, sourceCode, community, contribute,
            openLogFileButton, openTorLogFileButton, openDataDirButton, chatRules, tradeGuide, walletGuide, license, tac;

    private final TreeView<String> backupSnapshotTreeView;

    public ResourcesView(ResourcesModel model, ResourcesController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label guidesHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.guides.headline"));
        tradeGuide = new Hyperlink(Res.get("support.resources.guides.tradeGuide"));
        walletGuide = new Hyperlink(Res.get("support.resources.guides.walletGuide"));
        chatRules = new Hyperlink(Res.get("support.resources.guides.chatRules"));
        VBox guidesBox = new VBox(5, tradeGuide, walletGuide, chatRules);

        Label backupHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.backup.headline"));
        backupLocation = new MaterialTextField(Res.get("support.resources.backup.location"),
                Res.get("support.resources.backup.location.prompt"),
                Res.get("support.resources.backup.location.help"));
        backupLocation.setValidators(model.getDirectoryPathValidator());
        setBackupLocationButton = new Button(Res.get("support.resources.backup.setLocationButton"));
        backupButton = new Button(Res.get("support.resources.backup.backupButton"));
        HBox backupButtons = new HBox(10, setBackupLocationButton, backupButton);
        VBox backupBox = new VBox(20, backupLocation, backupButtons);

        Label backupSnapshotHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.backupSnapshot.headline"));
        Label backupSnapshotDescription = new Label(Res.get("support.resources.backupSnapshot.description"));
        backupSnapshotDescription.setWrapText(true);
        backupSnapshotDescription.getStyleClass().add("support-normal-text");
        learnMoreAutomaticBackup = new BisqHyperlink(Res.get("support.resources.backupSnapshot.wikiLink"), "https://bisq.wiki/Automatic_backup");
        Label backupSnapshotTreeViewTitle = new Label(Res.get("support.resources.backupSnapshot.treeView.title"));
        backupSnapshotTreeViewTitle.getStyleClass().add("support-sub-headline");

        TreeItem<String> backupSnapshotTreeViewRootItem = new TreeItem<>("root");
        backupSnapshotTreeViewRootItem.setExpanded(true);
        backupSnapshotTreeView = new TreeView<>(backupSnapshotTreeViewRootItem);
        backupSnapshotTreeView.setMaxHeight(186);
        configureBackupSnapshotTreeView();

        VBox backupSnapshotBox = new VBox(20, backupSnapshotDescription, learnMoreAutomaticBackup, backupSnapshotTreeViewTitle, backupSnapshotTreeView);

        Label localDataHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.localData.headline"));
        openDataDirButton = new Hyperlink(Res.get("support.resources.localData.openDataDir"));
        openLogFileButton = new Hyperlink(Res.get("support.resources.localData.openLogFile"));
        openTorLogFileButton = new Hyperlink(Res.get("support.resources.localData.openTorLogFile"));

        VBox localDataBox = new VBox(5, openDataDirButton, openLogFileButton, openTorLogFileButton);

        Label localVersionHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.localVersion.headline"));

        String versionString = ApplicationVersion.getVersion() != null ?
                ApplicationVersion.getVersion().getVersionAsString() : Res.get("na");
        String commitHash = ApplicationVersion.getBuildCommitShortHash() != null ?
                ApplicationVersion.getBuildCommitShortHash() : Res.get("na");
        String torVersion = ApplicationVersion.getTorVersionString() != null ?
                ApplicationVersion.getTorVersionString() : Res.get("na");

        Label details = new Label(Res.get("support.resources.localVersion.details",
                versionString, commitHash, torVersion));
        details.getStyleClass().add("user-content-note");
        VBox localVersionBox = new VBox(5, details);

        Label resourcesHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.resources.headline"));
        webpage = new BisqHyperlink(Res.get("support.resources.resources.webpage"), "https://bisq.network/");
        dao = new BisqHyperlink(Res.get("support.resources.resources.dao"), "https://bisq.network/dao");
        sourceCode = new BisqHyperlink(Res.get("support.resources.resources.sourceCode"), "https://github.com/bisq-network/bisq2");
        community = new BisqHyperlink(Res.get("support.resources.resources.community"), "https://matrix.to/#/%23bisq:bitcoin.kyoto");
        contribute = new BisqHyperlink(Res.get("support.resources.resources.contribute"), "https://bisq.wiki/Contributor_checklist");

        Label legalHeadline = SettingsViewUtils.getHeadline(Res.get("support.resources.legal.headline"));
        tac = new Hyperlink(Res.get("support.resources.legal.tac"));
        license = new BisqHyperlink(Res.get("support.resources.legal.license"), "https://github.com/bisq-network/bisq2/blob/main/LICENSE");
        VBox legalBox = new VBox(5, tac, license);

        VBox resourcesBox = new VBox(5, webpage, dao, sourceCode, community, contribute);

        Insets value = new Insets(0, 5, 0, 5);
        VBox.setMargin(backupBox, value);
        VBox.setMargin(backupSnapshotBox, value);
        VBox.setMargin(localDataBox, value);
        VBox.setMargin(guidesBox, value);
        VBox.setMargin(legalBox, value);
        VBox.setMargin(resourcesBox, value);
        VBox.setMargin(localVersionBox, value);
        VBox contentBox = new VBox(50);
        contentBox.getChildren().addAll(
                guidesHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), guidesBox,
                localDataHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), localDataBox,
                backupHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), backupBox,
                backupSnapshotHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), backupSnapshotBox,
                localVersionHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), localVersionBox,
                resourcesHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), resourcesBox,
                legalHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), legalBox
        );
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        backupLocation.resetValidation();
        backupLocation.textProperty().bindBidirectional(model.getBackupLocation());
        backupLocation.validate();
        setBackupLocationButton.defaultButtonProperty().bind(model.getBackupButtonDefault().not());
        backupButton.defaultButtonProperty().bind(model.getBackupButtonDefault());
        backupButton.disableProperty().bind(model.getBackupButtonDisabled());

        updateBackupSnapshotTreeView();

        openLogFileButton.setOnAction(e -> controller.onOpenLogFile());
        openTorLogFileButton.setOnAction(e -> controller.onOpenTorLogFile());
        openDataDirButton.setOnAction(e -> controller.onOpenDataDir());
        setBackupLocationButton.setOnAction(e -> controller.onSetBackupLocation());
        backupButton.setOnAction(e -> onBackupButtonPressed());
        chatRules.setOnAction(e -> controller.onOpenChatRules());
        tradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        walletGuide.setOnAction(e -> controller.onOpenWalletGuide());
        tac.setOnAction(e -> controller.onTac());
        license.setOnAction(e -> controller.onOpenLicense());
        learnMoreAutomaticBackup.setOnAction(e -> controller.onOpenLearnMoreAutomaticBackup());
        webpage.setOnAction(e -> controller.onOpenWebpage());
        dao.setOnAction(e -> controller.onOpenDao());
        sourceCode.setOnAction(e -> controller.onOpenSourceCode());
        community.setOnAction(e -> controller.onOpenCommunity());
        contribute.setOnAction(e -> controller.onOpenContribute());
    }

    private void onBackupButtonPressed() {
        if (backupLocation.validate()) {
            controller.onBackup();
        }
    }

    @Override
    protected void onViewDetached() {
        backupLocation.setText("");
        backupLocation.resetValidation();
        backupLocation.textProperty().unbindBidirectional(model.getBackupLocation());
        setBackupLocationButton.defaultButtonProperty().unbind();
        backupButton.defaultButtonProperty().unbind();
        backupButton.disableProperty().unbind();

        backupSnapshotTreeView.getRoot().getChildren().clear();

        openLogFileButton.setOnAction(null);
        openTorLogFileButton.setOnAction(null);
        openDataDirButton.setOnAction(null);
        setBackupLocationButton.setOnAction(null);
        backupButton.setOnAction(null);
        chatRules.setOnAction(null);
        tradeGuide.setOnAction(null);
        walletGuide.setOnAction(null);
        tac.setOnAction(null);
        license.setOnAction(null);
        learnMoreAutomaticBackup.setOnAction(null);
        webpage.setOnAction(null);
        dao.setOnAction(null);
        sourceCode.setOnAction(null);
        community.setOnAction(null);
        contribute.setOnAction(null);
    }

    private void configureBackupSnapshotTreeView() {
        backupSnapshotTreeView.setShowRoot(false);

        backupSnapshotTreeView.setCellFactory(tv -> new TreeCell<>() {

            private final Tooltip tooltip = new BisqTooltip();

            {
                setPrefWidth(0);
                setMinWidth(0);
                setMaxWidth(Double.MAX_VALUE);
                setTextOverrun(OverrunStyle.ELLIPSIS);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    setOnMouseClicked(null);
                    return;
                }

                setText(item);

                tooltip.setText(item);
                setTooltip(tooltip);

                setOnMouseClicked(event -> {
                    TreeItem<String> treeItem = getTreeItem();
                    if (treeItem == null) return;

                    // Allow normal behavior if click was on the arrow
                    if (!clickedOnArrow(event)) {
                        // Toggle expansion only for items with children
                        if (!treeItem.isLeaf()) {
                            treeItem.setExpanded(!treeItem.isExpanded());
                        }
                        // Always clear selection to prevent highlighting
                        getTreeView().getSelectionModel().clearSelection();
                    }
                });
            }

            private boolean clickedOnArrow(MouseEvent event) {
                Node node = event.getPickResult().getIntersectedNode();
                while (node != null) {
                    if (node.getStyleClass().contains("tree-disclosure-node")) return true;
                    node = node.getParent();
                }
                return false;
            }
        });
    }

    private void updateBackupSnapshotTreeView() {
        backupSnapshotTreeView.getRoot().getChildren().clear();

        for (BackupSnapshotStoreItem item : model.getSortedBackupSnapshotStoreItems()) {
            TreeItem<String> storeItem = new TreeItem<>(
                    item.getStore() + " " + getFormattedIncrementalBackupNumber(item.getNumBackups()));
            storeItem.setExpanded(false);
            for (BackupSnapshotStoreItem.File file : item.getFiles()) {
                TreeItem<String> fileItem = new TreeItem<>(
                        Res.get("support.resources.backupSnapshot.treeView.file",
                                file.getAge(), file.getPath()));
                fileItem.setExpanded(false);
                storeItem.getChildren().add(fileItem);
            }
            backupSnapshotTreeView.getRoot().getChildren().add(storeItem);
        }
    }

    private static String getFormattedIncrementalBackupNumber(long numBackups) {
        if (numBackups == 0) {
            return "";
        }
        return String.format("(%s)",
                numBackups > 1
                        ? Res.get("support.resources.backupSnapshot.storeTable.numBackups.many", numBackups)
                        : Res.get("support.resources.backupSnapshot.storeTable.numBackups.one", numBackups)
        );
    }

    @Getter
    @EqualsAndHashCode
    static class BackupSnapshotStoreItem {
        private final String store;
        private final List<File> files;

        public BackupSnapshotStoreItem(String store, List<File> files) {
            this.store = store;
            this.files = files;
        }

        public int getNumBackups() {
            return files.size();
        }

        @Getter
        @EqualsAndHashCode
        static class File {
            private final String path;
            private final String age;

            public File(String path, String age) {
                this.path = path;
                this.age = age;
            }
        }
    }
}
