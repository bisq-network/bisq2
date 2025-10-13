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

package bisq.desktop.common.utils;

import bisq.common.platform.PlatformUtils;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class FileChooserUtil {

    private static final boolean IS_DIRECTORY_CHOOSER = true;
    private static final boolean IS_FILE_CHOOSER = false;

    public static Optional<Path> openFile(Scene scene) {
        return openFile(scene, Optional.empty());
    }

    public static Optional<Path> openFile(Scene scene, String initialFileName) {
        return openFile(scene, Optional.of(initialFileName));
    }

    private static Optional<Path> openFile(Scene scene, Optional<String> initialFileName) {
        FileChooser fileChooser = getFileChooser(initialFileName);
        Optional<Path> result = Optional.ofNullable(fileChooser.showOpenDialog(scene.getWindow())).map(File::toPath);
        result.ifPresent(file -> persistFileChooserDirectory(file, IS_FILE_CHOOSER));
        return result;
    }

    public static Optional<Path> saveFile(Scene scene) {
        return saveFile(scene, Optional.empty());
    }

    public static Optional<Path> saveFile(Scene scene, String initialFileName) {
        return saveFile(scene, Optional.of(initialFileName));
    }

    private static Optional<Path> saveFile(Scene scene, Optional<String> initialFileName) {
        FileChooser fileChooser = getFileChooser(initialFileName);
        Optional<Path> result = Optional.ofNullable(fileChooser.showSaveDialog(scene.getWindow())).map(File::toPath);
        result.ifPresent(file -> persistFileChooserDirectory(file, IS_FILE_CHOOSER));
        return result;
    }

    public static Optional<Path> chooseDirectory(Scene scene, String title) {
        return chooseDirectory(scene, Optional.empty(), title);
    }

    public static Optional<Path> chooseDirectory(Scene scene, Path initialDirectory, String title) {
        return chooseDirectory(scene, Optional.of(initialDirectory), title);
    }

    private static Optional<Path> chooseDirectory(Scene scene, Optional<Path> initialDirectory, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        Path initDir = initialDirectory
                .orElseGet(() -> SettingsService.getInstance().getCookie().asString(CookieKey.FILE_CHOOSER_DIR).map(Path::of)
                        .orElseGet(PlatformUtils::getDownloadOfHomeDir));
        if (Files.isDirectory(initDir)) {
            directoryChooser.setInitialDirectory(initDir.toFile());
        }
        directoryChooser.setTitle(title);
        Optional<Path> result = Optional.ofNullable(directoryChooser.showDialog(scene.getWindow())).map(File::toPath);
        result.ifPresent(file -> persistFileChooserDirectory(file, IS_DIRECTORY_CHOOSER));
        return result;
    }

    private static FileChooser getFileChooser(Optional<String> initialFileName) {
        FileChooser fileChooser = new FileChooser();
        initialFileName.ifPresent(fileChooser::setInitialFileName);
        Path initDir = SettingsService.getInstance().getCookie().asString(CookieKey.FILE_CHOOSER_DIR).map(Path::of)
                .orElseGet(PlatformUtils::getDownloadOfHomeDir);
        if (Files.isDirectory(initDir)) {
            fileChooser.setInitialDirectory(initDir.toFile());
        }
        return fileChooser;
    }

    private static void persistFileChooserDirectory(Path file, boolean isDirectoryChooser) {
        Path directoryToPersist = isDirectoryChooser ? file : file.getParent();
        if (directoryToPersist == null) {
            log.warn("Skipping persistence of file chooser directory for path without parent: {}", file);
            return;
        }
        SettingsService.getInstance().setCookie(CookieKey.FILE_CHOOSER_DIR, directoryToPersist.toAbsolutePath().toString());
    }
}