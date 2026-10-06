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

package bisq.e2e;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The settings of a run, as system properties which tools/compat/run-mixed-network.sh sets:
 * <ul>
 *     <li>bisq.e2e.newTree: the source tree of this checkout, with the installed distributions of api-app and
 *     seed-node-app</li>
 *     <li>bisq.e2e.oldTree: the source tree of the released version, built by tools/compat/baseline.sh</li>
 *     <li>bisq.e2e.oldVersion: the tag of the released version, for example v2.1.13</li>
 *     <li>bisq.e2e.workDir: the directory for the data directories and logs of the nodes</li>
 * </ul>
 */
final class E2eSettings {
    private E2eSettings() {
    }

    static boolean isConfigured() {
        return !System.getProperty("bisq.e2e.newTree", "").isEmpty() &&
                !System.getProperty("bisq.e2e.oldTree", "").isEmpty();
    }

    static Path newTree() {
        return existingDir("bisq.e2e.newTree");
    }

    static Path oldTree() {
        return existingDir("bisq.e2e.oldTree");
    }

    static String oldVersion() {
        return System.getProperty("bisq.e2e.oldVersion", "old");
    }

    static Path workDir() {
        String value = System.getProperty("bisq.e2e.workDir", "");
        return value.isEmpty() ? Path.of("build", "e2e") : Path.of(value);
    }

    private static Path existingDir(String property) {
        String value = System.getProperty(property, "");
        if (value.isEmpty()) {
            throw new IllegalStateException("System property " + property + " is not set");
        }
        Path path = Path.of(value);
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(property + " is not a directory: " + path);
        }
        return path;
    }
}
