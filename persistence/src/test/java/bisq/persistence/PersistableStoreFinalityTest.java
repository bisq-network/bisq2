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

package bisq.persistence;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PersistableStoreResolver derives the store's proto type name from the runtime class of the instance handed to
 * getOrCreatePersistence, while getResolver() unpacks the proto of the class which declares it. A subclass would make
 * those two differ, and since some stores are registered under an explicit file name, the subclass would read the
 * parent's file with a key that does not match it, so the file and its backups get moved aside as corrupted.
 * Requiring every store to be final keeps the two classes the same by construction.
 * <p>
 * This scans sources rather than the classpath because the stores are spread over 16 modules and no module depends on
 * all of them.
 */
class PersistableStoreFinalityTest {
    private static final Pattern STORE_DECLARATION = Pattern.compile(
            "^\\s*(?:public\\s+)?(final\\s+)?(?:abstract\\s+)?class\\s+(\\w+)[^{]*\\bimplements\\b[^{]*\\bPersistableStore<");
    private static final String BUILD_DIR = File.separator + "build" + File.separator;

    @Test
    void everyPersistableStoreIsFinal() throws IOException {
        Path repoRoot = findRepoRoot();
        List<String> stores = new ArrayList<>();
        List<String> notFinal = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains(BUILD_DIR))
                    .forEach(path -> collectStores(path, stores, notFinal));
        }

        // Guards against a scan which found nothing, which would let the assertion below pass for the wrong reason.
        assertTrue(stores.contains("SettingsStore") && stores.contains("BisqEasyTradeStore"),
                "Source scan did not reach the known stores. Root was " + repoRoot + ", found " + stores);
        assertEquals(List.of(), notFinal, "PersistableStore implementations must be final");
    }

    private static void collectStores(Path path, List<String> stores, List<String> notFinal) {
        try {
            for (String line : Files.readAllLines(path)) {
                Matcher matcher = STORE_DECLARATION.matcher(line);
                if (matcher.find()) {
                    stores.add(matcher.group(2));
                    if (matcher.group(1) == null) {
                        notFinal.add(matcher.group(2));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + path, e);
        }
    }

    private static Path findRepoRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.exists(path.resolve("settings.gradle.kts"))) {
            path = path.getParent();
        }
        if (path == null) {
            throw new IllegalStateException("No settings.gradle.kts found above " + Path.of("").toAbsolutePath());
        }
        return path;
    }
}
