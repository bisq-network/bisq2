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

package bisq.tor.process;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class LdPreload {
    public static String computeLdPreloadVariable(Path dirPath) {
        File[] sharedLibraries = dirPath.toFile()
                .listFiles((file, fileName) -> fileName.contains(".so."));
        Objects.requireNonNull(sharedLibraries);

        return Arrays.stream(sharedLibraries)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }
}
