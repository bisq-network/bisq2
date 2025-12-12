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

import bisq.common.file.FileMutatorUtils;
import bisq.common.observable.Pin;
import bisq.common.platform.PlatformUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.persistence.PersistenceService;
import bisq.persistence.backup.BackupFileInfo;
import bisq.presentation.formatters.TimeFormatter;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static bisq.common.util.StringUtils.isEmpty;
import static bisq.desktop.main.content.support.resources.ResourcesView.BackupSnapshotStoreItem;

@Slf4j
public class ResourcesController implements Controller {
    @Getter
    private final ResourcesView view;
    private final ResourcesModel model;
    private final Path appDataDirPath;
    private final String appName;
    private final PersistenceService persistenceService;
    private final SettingsService settingsService;
    private Pin backupLocationPin;

    public ResourcesController(ServiceProvider serviceProvider) {
        appDataDirPath = serviceProvider.getConfig().getAppDataDirPath();
        appName = serviceProvider.getConfig().getAppName();
        settingsService = serviceProvider.getSettingsService();
        persistenceService = serviceProvider.getPersistenceService();

        model = new ResourcesModel();
        view = new ResourcesView(model, this);
    }

    @Override
    public void onActivate() {
        model.getBackupButtonDefault().bind(model.getBackupLocation().isEmpty().not());
        model.getBackupButtonDisabled().bind(model.getBackupLocation().isEmpty());
        backupLocationPin = FxBindings.bindBiDir(model.getBackupLocation())
                .to(settingsService.getBackupLocation(), settingsService::setBackupLocation);

        fillBackupSnapshots();
    }

    @Override
    public void onDeactivate() {
        model.getBackupButtonDefault().unbind();
        model.getBackupButtonDisabled().unbind();
        backupLocationPin.unbind();

        model.getBackupSnapshotStoreItems().clear();
    }

    void onOpenLogFile() {
        PlatformUtils.open(appDataDirPath.resolve("bisq.log"));
    }

    void onOpenTorLogFile() {
        PlatformUtils.open(appDataDirPath.resolve("tor").resolve("debug.log"));
    }

    void onOpenDataDir() {
        PlatformUtils.open(appDataDirPath);
    }

    void onSetBackupLocation() {
        String backupLocation = model.getBackupLocation().get();
        Path path = isEmpty(backupLocation)
                ? PlatformUtils.getHomeDirectoryPath()
                : Path.of(backupLocation);
        String title = Res.get("support.resources.backup.selectLocation");
        FileChooserUtil.chooseDirectory(getView().getRoot().getScene(), path, title)
                .ifPresent(directory -> model.getBackupLocation().set(directory.toAbsolutePath().toString()));
    }

    void onBackup() {
        if (isEmpty(model.getBackupLocation().get())) {
            model.getBackupLocation().set(null);
            return;
        }
        persistenceService.persistAllClients()
                .whenComplete((result, throwable) -> UIThread.run(() -> {
                    if (throwable == null) {
                        String dateString = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
                        String destinationDirName = appName + "_backup_" + dateString;
                        Path destinationPath = Path.of(model.getBackupLocation().get(), destinationDirName);
                        if (!Files.exists(Path.of(model.getBackupLocation().get()))) {
                            new Popup().warning(Res.get("support.resources.backup.destinationNotExist", model.getBackupLocation().get())).show();
                            return;
                        }
                        try {
                            // Files with .log extension are not necessary to include in the backup, so we exclude them from the copy
                            FileMutatorUtils.copyDirectory(appDataDirPath, destinationPath, Set.of("log"));
                            new Popup().feedback(Res.get("support.resources.backup.success", destinationPath)).show();
                        } catch (IOException e) {
                            new Popup().error(e).show();
                        }
                    } else {
                        new Popup().error(throwable).show();
                    }
                }));
    }

    private void fillBackupSnapshots() {
        List<BackupFileInfo> backupSnapshots = persistenceService.getAllBackups();

        var backupSnapshotsByStore = backupSnapshots
                .stream()
                .collect(Collectors.groupingBy(
                        bfi -> bfi.getPath().getParent().getFileName().toString(),
                        TreeMap::new,
                        Collectors.toList()));

        var result = backupSnapshotsByStore.entrySet().stream()
                .map(entry -> new BackupSnapshotStoreItem(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(bfi -> {
                                    long timestamp = bfi.getLocalDateTime()
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli();
                                    return new BackupSnapshotStoreItem.File(
                                            bfi.getPath().toString(),
                                            TimeFormatter.formatAge(Math.max(0, System.currentTimeMillis() - timestamp))
                                    );
                                })
                                .toList()
                ))
                .toList();

        model.getBackupSnapshotStoreItems().clear();
        model.getBackupSnapshotStoreItems().addAll(result);
    }

    void onOpenChatRules() {
        Navigation.navigateTo(NavigationTarget.CHAT_RULES);
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }

    void onOpenWalletGuide() {
        Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
    }

    void onTac() {
        Navigation.navigateTo(NavigationTarget.TAC);
    }

    void onOpenLicense() {
        Browser.open("https://github.com/bisq-network/bisq2/blob/main/LICENSE");
    }

    void onOpenLearnMoreAutomaticBackup() {
        Browser.open("https://bisq.wiki/Automatic_backup");
    }

    void onOpenWebpage() {
        Browser.open("https://bisq.network/");
    }

    void onOpenDao() {
        Browser.open("https://bisq.network/dao");
    }

    void onOpenSourceCode() {
        Browser.open("https://github.com/bisq-network/bisq2");
    }

    void onOpenContribute() {
        Browser.open("https://bisq.wiki/Contributor_checklist");
    }

    void onOpenCommunity() {
        Browser.open("https://matrix.to/#/%23bisq:bitcoin.kyoto");
    }
}
