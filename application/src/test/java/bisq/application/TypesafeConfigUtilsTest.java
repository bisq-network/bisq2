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

package bisq.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.application.TypesafeConfigUtils.coerce;
import static bisq.application.TypesafeConfigUtils.mapCustomArgsToTypesafeEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypesafeConfigUtilsTest {
    private static final String SANDBOX_ENABLED = "application.network.configByTransportType.tor.sandbox.enabled";

    @Test
    void anAppendedEntryOverridesTheFileAndKeepsItsComments(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        String content = """
                # bridges of the user
                application.network.configByTransportType.tor {
                    sandbox.enabled = true
                    torrcOverrides.ORPort = 443
                }""";
        Files.writeString(configFilePath, content, StandardCharsets.UTF_8);

        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false));

        String newContent = Files.readString(configFilePath, StandardCharsets.UTF_8);
        assertEquals(content + "\n" + SANDBOX_ENABLED + "=false\n", newContent);
        assertOnlyTheConfigFileIsLeft(appDataDirPath);
        Config config = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElseThrow();
        assertFalse(config.getBoolean(SANDBOX_ENABLED));
        assertEquals(443, config.getInt("application.network.configByTransportType.tor.torrcOverrides.ORPort"));
    }

    // The start fails for a bisq.conf with a substitution, so an appended entry would have no effect
    @Test
    void aFileWhichTheStartRefusesIsLeftAsItIs(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        String content = """
                bridgePort = 443
                application.network.configByTransportType.tor.torrcOverrides.ORPort = ${bridgePort}
                """;
        Files.writeString(configFilePath, content, StandardCharsets.UTF_8);
        assertThrows(CustomConfigException.class, () -> TypesafeConfigUtils.resolveCustomConfig(appDataDirPath));

        IOException exception = assertThrows(IOException.class,
                () -> TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false)));

        assertTrue(exception.getMessage().contains("is not valid"));
        assertEquals(content, Files.readString(configFilePath, StandardCharsets.UTF_8));
        assertOnlyTheConfigFileIsLeft(appDataDirPath);
    }

    @Test
    void anEntryAppendedWithoutAFileCreatesIt(@TempDir Path appDataDirPath) throws IOException {
        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false));

        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        assertEquals(SANDBOX_ENABLED + "=false\n", Files.readString(configFilePath, StandardCharsets.UTF_8));
    }

    // For example the choice to use no bridges, which sets a string and a list
    @Test
    void entriesOfOtherTypesAreAppendedSortedAndParseToTheirValues(@TempDir Path appDataDirPath) throws IOException {
        String torPath = "application.network.configByTransportType.tor";
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(configFilePath, torPath + " { builtInBridges = \"obfs4\" }\n", StandardCharsets.UTF_8);

        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath,
                Map.of(torPath + ".builtInBridges", "", torPath + ".bridges", List.of()));

        String content = Files.readString(configFilePath, StandardCharsets.UTF_8);
        assertTrue(content.endsWith(torPath + ".bridges=[]\n" + torPath + ".builtInBridges=\"\"\n"), content);
        Config config = ConfigFactory.parseFile(configFilePath.toFile());
        assertEquals("", config.getString(torPath + ".builtInBridges"));
        assertTrue(config.getStringList(torPath + ".bridges").isEmpty());
    }

    // The start fails for a bisq.conf which it cannot parse, so an appended entry would have no effect
    @Test
    void aFileWhichCannotBeParsedIsLeftAsItIs(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        String content = "application.network {\n";
        Files.writeString(configFilePath, content, StandardCharsets.UTF_8);

        IOException exception = assertThrows(IOException.class,
                () -> TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false)));

        assertTrue(exception.getMessage().contains("is not valid"));
        assertEquals(content, Files.readString(configFilePath, StandardCharsets.UTF_8));
        assertOnlyTheConfigFileIsLeft(appDataDirPath);
    }

    // The start parses bisq.conf as a file, so an include relative to it is valid
    @Test
    void anIncludeRelativeToTheFileIsAccepted(@TempDir Path appDataDirPath) throws IOException {
        Files.writeString(appDataDirPath.resolve("bridges.conf"), "bridgePort = 443\n", StandardCharsets.UTF_8);
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(configFilePath, "include required(\"bridges.conf\")\n", StandardCharsets.UTF_8);

        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false));

        Config config = ConfigFactory.parseFile(configFilePath.toFile()).resolve();
        assertFalse(config.getBoolean(SANDBOX_ENABLED));
        assertEquals(443, config.getInt("bridgePort"));
    }

    @Test
    void aLinkedFileGetsTheLinesAndTheLinkStays(@TempDir Path appDataDirPath, @TempDir Path otherDirPath)
            throws IOException {
        Path linkTargetPath = otherDirPath.resolve("my-bisq.conf");
        Files.writeString(linkTargetPath, "# mine\n", StandardCharsets.UTF_8);
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        try {
            Files.createSymbolicLink(configFilePath, linkTargetPath);
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.abort("The file system does not support symbolic links: " + e.getMessage());
        }

        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false));

        assertTrue(Files.isSymbolicLink(configFilePath));
        assertEquals("# mine\n" + SANDBOX_ENABLED + "=false\n",
                Files.readString(linkTargetPath, StandardCharsets.UTF_8));
    }

    // The start parses bisq.conf by its own path, so a relative include resolves from the directory of the link
    @Test
    void anIncludeRelativeToALinkedFileResolvesFromTheDirectoryOfTheLink(@TempDir Path appDataDirPath,
                                                                         @TempDir Path otherDirPath)
            throws IOException {
        Files.writeString(appDataDirPath.resolve("bridges.conf"), "bridgePort = 443\n", StandardCharsets.UTF_8);
        Path linkTargetPath = otherDirPath.resolve("my-bisq.conf");
        String content = "include required(\"bridges.conf\")\n";
        Files.writeString(linkTargetPath, content, StandardCharsets.UTF_8);
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        try {
            Files.createSymbolicLink(configFilePath, linkTargetPath);
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.abort("The file system does not support symbolic links: " + e.getMessage());
        }

        TypesafeConfigUtils.appendToCustomConfig(appDataDirPath, Map.of(SANDBOX_ENABLED, false));

        assertEquals(content + SANDBOX_ENABLED + "=false\n", Files.readString(linkTargetPath, StandardCharsets.UTF_8));
        Config config = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElseThrow();
        assertFalse(config.getBoolean(SANDBOX_ENABLED));
        assertEquals(443, config.getInt("bridgePort"));
        try (Stream<Path> files = Files.list(appDataDirPath)) {
            assertEquals(Set.of(configFilePath, appDataDirPath.resolve("bridges.conf")), files.collect(Collectors.toSet()));
        }
        try (Stream<Path> files = Files.list(otherDirPath)) {
            assertEquals(List.of(linkTargetPath), files.toList());
        }
    }

    @Test
    void withoutAFileThereIsNoCustomConfig(@TempDir Path appDataDirPath) {
        assertTrue(TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).isEmpty());
    }

    // Earlier versions ignored such a file and started without its settings, for example without the bridges or the
    // torrc override ExcludeNodes
    @Test
    void aFileWhichCannotBeParsedStopsTheStart(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        // Without quotes, {ch} is an object without a value
        Files.writeString(configFilePath, "application.network.configByTransportType.tor.torrcOverrides.ExcludeNodes = {ch}\n",
                StandardCharsets.UTF_8);

        CustomConfigException exception = assertThrows(CustomConfigException.class,
                () -> TypesafeConfigUtils.resolveCustomConfig(appDataDirPath));

        assertEquals(configFilePath.toAbsolutePath(), exception.getCustomConfigFilePath());
        assertTrue(exception.getMessage().contains(configFilePath.toAbsolutePath().toString()));
        assertFalse(exception.getReason().isEmpty());
    }

    // Typesafe config would read a file which it cannot open as an empty file
    @Test
    void aFileWhichCannotBeReadStopsTheStart(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(configFilePath, "application.network.configByTransportType.tor.sandbox.enabled = true\n",
                StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(configFilePath, Set.of());
        } catch (UnsupportedOperationException e) {
            Assumptions.abort("The file system does not support POSIX permissions");
        }
        Assumptions.assumeFalse(Files.isReadable(configFilePath), "The user can read files without permission");

        assertThrows(CustomConfigException.class, () -> TypesafeConfigUtils.resolveCustomConfig(appDataDirPath));
    }

    @Test
    void aLinkToAMissingFileStopsTheStart(@TempDir Path appDataDirPath, @TempDir Path otherDirPath) {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        try {
            Files.createSymbolicLink(configFilePath, otherDirPath.resolve("missing.conf"));
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.abort("The file system does not support symbolic links: " + e.getMessage());
        }

        assertThrows(CustomConfigException.class, () -> TypesafeConfigUtils.resolveCustomConfig(appDataDirPath));
    }

    // In HOCON, an include without required() may name a missing file. Only bisq.conf itself must exist.
    @Test
    void anOptionalIncludeOfAMissingFileIsAccepted(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(configFilePath, "include \"missing.conf\"\n" +
                "application.network.configByTransportType.tor.sandbox.enabled = false\n", StandardCharsets.UTF_8);

        Config config = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElseThrow();

        assertFalse(config.getBoolean(SANDBOX_ENABLED));
    }

    private static void assertOnlyTheConfigFileIsLeft(Path appDataDirPath) throws IOException {
        try (Stream<Path> files = Files.list(appDataDirPath)) {
            assertEquals(List.of(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME)), files.toList());
        }
    }

    @Test
    void testMapCustomArgsAppNameWithEqualsSyntax() {
        String[] args = {"--app-name=bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
    }

    @Test
    void testMapCustomArgsAppNameWithSeparateArgSyntax() {
        String[] args = {"--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
    }

    @Test
    void testMapCustomArgsDataDirWithEqualsSyntax() {
        String[] args = {"--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsDataDirWithSeparateArgSyntax() {
        String[] args = {"--data-dir", "/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsMultipleArgsTogether() {
        String[] args = {"--app-name=bisq", "--data-dir", "/opt/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/opt/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsMultipleArgsTogether2() {
        String[] args = {"--app-name=bisq", "--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsIgnoreUnknownArgs() {
        String[] args = {"--foo=bar", "--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
        assertEquals(1, result.size()); // Only appName should be present
    }


    @Test
    void testCoerceBooleanTrue() {
        assertEquals(Boolean.TRUE, coerce("true"));
        assertEquals(Boolean.TRUE, coerce("TrUe"));
        assertEquals(Boolean.TRUE, coerce("TRUE"));
    }

    @Test
    void testCoerceBooleanFalse() {
        assertEquals(Boolean.FALSE, coerce("false"));
        assertEquals(Boolean.FALSE, coerce("FaLsE"));
    }

    @Test
    void testCoerceInteger() {
        Object coerce = coerce("123");
        int expected = 123;
        assertEquals(expected, coerce);
        assertEquals(-456, coerce("-456"));
    }

    @Test
    void testCoerceLong() {
        long longValue1 = (long) Integer.MAX_VALUE + 1;
        assertEquals(longValue1, coerce(String.valueOf(longValue1)));
        long longValue2 = (long) Integer.MIN_VALUE - 1; // overflow
        assertEquals(longValue2, coerce(String.valueOf(longValue2)));
        int intValue = Integer.MIN_VALUE + 1;
        assertEquals(intValue, coerce(String.valueOf(intValue)));
    }

    @Test
    void testCoerceDouble() {
        assertEquals(3.14, coerce("3.14"));
        assertEquals(-0.001, coerce("-0.001"));
        assertEquals(1.23e4, coerce("1.23e4"));
        assertEquals(-5.67E-8, coerce("-5.67E-8"));
    }

    @Test
    void testCoerceFallbackString() {
        assertEquals("hello", coerce("hello"));
        assertEquals("123abc", coerce("123abc"));
        assertEquals("1.2.3", coerce("1.2.3"));
    }

    @Test
    void testCoerceTrimmedString() {
        assertEquals(42, coerce(" 42 "));
        assertEquals(Boolean.TRUE, coerce(" true "));
        assertEquals("  some text  ", coerce("  some text  ")); // spaces preserved if not numeric/boolean
    }
}
