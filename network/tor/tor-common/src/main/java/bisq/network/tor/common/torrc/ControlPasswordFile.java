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

package bisq.network.tor.common.torrc;

import bisq.common.encoding.Hex;
import bisq.common.file.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

public class ControlPasswordFile {
    private final Path path;

    public ControlPasswordFile(Path torrcPath) {
        this.path = torrcPath;
    }

    public byte[] readPassword() {
        try {
            String content = FileUtils.readAsString(path.toFile());
            return Hex.decode(content);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read password file: " + path.toAbsolutePath());
        }
    }

    public void savePassword(byte[] content) {
        try {
            FileUtils.writeToFile(Hex.encode(content), path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't write password file: " + path.toAbsolutePath());
        }
    }

}
