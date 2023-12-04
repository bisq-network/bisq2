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

package bisq.desktop.main.content.settings.utils;

import bisq.bisq_easy.NavigationTarget;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class UtilsController implements Controller {
    @Getter
    private final UtilsView view;
    private final UtilsModel model;
    private final String baseDir;
    private final String appName;
    private final PersistenceService persistenceService;

    public UtilsController(ServiceProvider serviceProvider) {
        baseDir = serviceProvider.getConfig().getBaseDir().toAbsolutePath().toString();
        appName = serviceProvider.getConfig().getAppName();
        persistenceService = serviceProvider.getPersistenceService();

        model = new UtilsModel();
        view = new UtilsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getBackupButtonDefault().bind(model.getBackupLocation().isEmpty().not());
    }

    @Override
    public void onDeactivate() {
    }

    void onOpenLogFile() {
        OsUtils.open(Path.of(baseDir, "bisq.log").toFile());
    }

    void onOpenDataDir() {
        OsUtils.open(baseDir);
    }

    void onSetBackupLocation() {
        String path = model.getBackupLocation().get();
        if (StringUtils.isEmpty(path)) {
            path = OsUtils.getHomeDirectory();
        }
        File directory = FileChooserUtil.chooseDirectory(getView().getRoot().getScene(),
                path,
                Res.get("settings.utils.backup.selectLocation"));
        if (directory != null) {
            model.getBackupLocation().set(directory.getAbsolutePath());
        }
    }

    void onBackup() {
        persistenceService.persistAllClients()
                .whenComplete((result, throwable) -> {
                    UIThread.run(() -> {
                        if (throwable == null) {
                            String dateString = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
                            String destinationDirName = appName + "_backup_" + dateString;
                            String destination = Paths.get(model.getBackupLocation().get(), destinationDirName).toString();
                            if (!new File(model.getBackupLocation().get()).exists()) {
                                new Popup().warning(Res.get("settings.utils.backup.destinationNotExist", model.getBackupLocation().get())).show();
                                return;
                            }
                            try {
                                FileUtils.copyDirectory(baseDir, destination);
                                new Popup().feedback(Res.get("settings.utils.backup.success", destination)).show();
                            } catch (IOException e) {
                                new Popup().error(e).show();
                            }
                        } else {
                            new Popup().error(throwable).show();
                        }
                    });
                });
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
        OsUtils.browse("https://github.com/bisq-network/bisq2/blob/main/LICENSE");
    }

    void onOpenWebpage() {
        OsUtils.browse("https://bisq.network/");
    }

    void onOpenDao() {
        OsUtils.browse("https://bisq.network/dao");
    }

    void onOpenSourceCode() {
        OsUtils.browse("https://github.com/bisq-network/bisq2");
    }

    void onOpenContribute() {
        OsUtils.browse("https://bisq.wiki/Contributor_checklist");
    }

    void onOpenCommunity() {
        OsUtils.browse("https://matrix.to/#/%23bisq:bitcoin.kyoto");
    }
}
