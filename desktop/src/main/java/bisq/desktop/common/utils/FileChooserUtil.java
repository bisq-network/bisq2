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

import bisq.common.util.OsUtils;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Paths;

@Slf4j
public class FileChooserUtil {
    @Nullable
    public static File openFile(Scene scene) {
        FileChooser fileChooser = new FileChooser();
        String initialDirectory = SettingsService.getInstance().getCookie().asString(CookieKey.FILE_CHOOSER_DIR)
                .orElse(OsUtils.getDownloadOfHomeDir());
        File initDir = new File(initialDirectory);
        if (initDir.isDirectory()) {
            fileChooser.setInitialDirectory(initDir);
        }
        File file = fileChooser.showOpenDialog(scene.getWindow());
        if (file != null) {
            String path = file.getAbsolutePath();
            String parentDir = Paths.get(path).getParent().toString();
            SettingsService.getInstance().setCookie(CookieKey.FILE_CHOOSER_DIR, parentDir);
        }
        return file;
    }
}