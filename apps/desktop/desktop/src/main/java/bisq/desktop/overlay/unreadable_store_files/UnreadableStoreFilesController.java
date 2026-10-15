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

import bisq.common.platform.PlatformUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.persistence.UnreadableStoreArchive;
import bisq.persistence.UnreadableStoreArchive.UnreadableStoreFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tells the user which stores this version could not read completely, and where their copies are (see
 * docs/specifications/persistence/unreadable-store-files.md).
 */
@Slf4j
public class UnreadableStoreFilesController implements InitWithDataController<UnreadableStoreFilesController.InitData> {
    // The desktop controller shows it before onboarding, the main controller when the main screen opens. The list does
    // not change after the start, so it is shown at most once.
    private static final AtomicBoolean DISPLAYED = new AtomicBoolean();

    /**
     * Whether the overlay has to be shown. Returns true at most once, so that the caller which gets true shows it.
     */
    public static boolean isToBeShown(UnreadableStoreArchive archive) {
        return !archive.getUnreadableStoreFiles().isEmpty() && DISPLAYED.compareAndSet(false, true);
    }

    @Getter
    public static final class InitData {
        private final Runnable closeHandler;

        public InitData(Runnable closeHandler) {
            this.closeHandler = closeHandler;
        }
    }

    private final UnreadableStoreFilesModel model;
    @Getter
    private final UnreadableStoreFilesView view;
    private final OverlayController overlayController;
    private final Path directory;
    @Nullable
    private Runnable closeHandler;
    private boolean useEscapeKeyHandler;

    public UnreadableStoreFilesController(ServiceProvider serviceProvider) {
        overlayController = OverlayController.getInstance();
        UnreadableStoreArchive archive = serviceProvider.getPersistenceService().getUnreadableStoreArchive();
        directory = archive.getDirectory();
        model = new UnreadableStoreFilesModel();
        view = new UnreadableStoreFilesView(model, this);
        List<UnreadableStoreFile> unreadableStoreFiles = archive.getUnreadableStoreFiles();
        model.getListItems().setAll(unreadableStoreFiles.stream()
                .map(file -> new UnreadableStoreFilesView.ListItem(file.storeName(),
                        toReasonText(file),
                        toCopyText(file)))
                .toList());
        model.getDirectory().set(directory.toString());
    }

    @Override
    public void initWithData(InitData data) {
        closeHandler = data.getCloseHandler();
    }

    @Override
    public void onActivate() {
        model.getDirectoryExists().set(Files.isDirectory(directory));
        // The escape key hides the overlay without the close handler, which starts onboarding or the updater
        useEscapeKeyHandler = overlayController.isUseEscapeKeyHandler();
        overlayController.setUseEscapeKeyHandler(false);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(useEscapeKeyHandler);
    }

    void onOpenFolder() {
        PlatformUtils.open(directory);
    }

    void onClose() {
        OverlayController.hide(closeHandler);
    }

    private static String toReasonText(UnreadableStoreFile file) {
        return switch (file.reason()) {
            case SKIPPED_ENTRIES -> Res.get("unreadableStoreFiles.reason.skippedEntries");
            case NOT_READABLE -> Res.get("unreadableStoreFiles.reason.notReadable");
        };
    }

    private static String toCopyText(UnreadableStoreFile file) {
        return switch (file.outcome()) {
            case ARCHIVED -> file.archivedFile()
                    .map(path -> path.getFileName().toString())
                    .orElse("");
            case SIZE_LIMIT_REACHED -> Res.get("unreadableStoreFiles.copy.sizeLimitReached");
            case FAILED -> Res.get("unreadableStoreFiles.copy.failed");
        };
    }
}
