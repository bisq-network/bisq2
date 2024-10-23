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

package bisq.desktop.components.cathash;

import bisq.desktop.common.utils.ImageUtil;
import bisq.user.cathash.CatHashService;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class JavaFxCatHashService extends CatHashService<Image> {
    public JavaFxCatHashService(Path baseDir) {
        super(baseDir);
    }

    @Override
    protected Image composeImage(String[] paths, double size) {
        return ImageUtil.composeImage(paths, size);
    }

    @Override
    protected void writeRawImage(Image image, File iconFile) throws IOException {
        ImageUtil.writeRawImage(image, iconFile);
    }

    @Override
    protected Image readRawImage(File iconFile) throws IOException {
        return ImageUtil.readRawImage(iconFile);
    }
}
