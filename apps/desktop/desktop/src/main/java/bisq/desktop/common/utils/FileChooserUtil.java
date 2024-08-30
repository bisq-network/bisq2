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
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
public class FileChooserUtil {
    public static Optional<File> openFile(Scene scene) {
        return openFile(scene, Optional.empty());
    }

    public static Optional<File> openFile(Scene scene, String initialFileName) {
        return openFile(scene, Optional.of(initialFileName));
    }

    private static Optional<File> openFile(Scene scene, Optional<String> initialFileName) {
        FileChooser fileChooser = getFileChooser(initialFileName);
        Optional<File> result = Optional.ofNullable(fileChooser.showOpenDialog(scene.getWindow()));
        result.ifPresent(FileChooserUtil::persistFielChooserDirectory);
        return result;
    }

    public static Optional<File> saveFile(Scene scene) {
        return saveFile(scene, Optional.empty());
    }

    public static Optional<File> saveFile(Scene scene, String initialFileName) {
        return saveFile(scene, Optional.of(initialFileName));
    }

    private static Optional<File> saveFile(Scene scene, Optional<String> initialFileName) {
        FileChooser fileChooser = getFileChooser(initialFileName);
        Optional<File> result = Optional.ofNullable(fileChooser.showSaveDialog(scene.getWindow()));
        result.ifPresent(FileChooserUtil::persistFielChooserDirectory);
        return result;
    }

    public static Optional<File> chooseDirectory(Scene scene, String title) {
        return chooseDirectory(scene, Optional.empty(), title);
    }

    public static Optional<File> chooseDirectory(Scene scene, String initialDirectory, String title) {
        return chooseDirectory(scene, Optional.of(initialDirectory), title);
    }

    private static Optional<File> chooseDirectory(Scene scene, Optional<String> initialDirectory, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        String directory = initialDirectory
                .orElse(SettingsService.getInstance().getCookie().asString(CookieKey.FILE_CHOOSER_DIR)
                        .orElse(PlatformUtils.getDownloadOfHomeDir()));
        File initDir = new File(directory);
        if (initDir.isDirectory()) {
            directoryChooser.setInitialDirectory(initDir);
        }
        directoryChooser.setTitle(title);
        Optional<File> result = Optional.ofNullable(directoryChooser.showDialog(scene.getWindow()));
        result.ifPresent(FileChooserUtil::persistFielChooserDirectory);
        return result;
    }

    private static FileChooser getFileChooser(Optional<String> initialFileName) {
        FileChooser fileChooser = new FileChooser();
        initialFileName.ifPresent(fileChooser::setInitialFileName);
        String initialDirectory = SettingsService.getInstance().getCookie().asString(CookieKey.FILE_CHOOSER_DIR)
                .orElse(PlatformUtils.getDownloadOfHomeDir());
        File initDir = new File(initialDirectory);
        if (initDir.isDirectory()) {
            fileChooser.setInitialDirectory(initDir);
        }
        return fileChooser;
    }

    private static void persistFielChooserDirectory(File file) {
        SettingsService.getInstance().setCookie(CookieKey.FILE_CHOOSER_DIR, Paths.get(file.getAbsolutePath()).getParent().toString());
    }
}