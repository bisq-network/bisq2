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

package bisq.evolution.updater;

import bisq.common.file.FileMutatorUtils;
import bisq.common.threading.ExecutorFactory;
import bisq.security.PgPUtils;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the verification of the in-app updater of v2.1.13 against the key files and the release assets of a new release.
 * Users of v2.1.13 update with this code, so a release which it does not accept cannot be installed with the updater.
 * tools/compat/old-updater-replay.sh runs the tests. Each test runs only if its system properties are set.
 * <p>
 * The updater of v2.1.13 downloads signingkey.asc, both key files from the GitHub release and from
 * https://bisq.network/pubkey/, the installer and its signature (DownloadItem.createDescriptorList). Then it reads the
 * key id from signingkey.asc without trimming and verifies the signature with the key from the web page, with the key
 * which it has in its resources, and with the key from the GitHub release (DownloadedFilesVerification.verify).
 */
class OldUpdaterReplayTest {
    // apps/desktop/desktop-app/src/main/resources/desktop.conf of v2.1.13
    private static final List<String> KEY_IDS = List.of("E222AA02", "387C8307");
    private static final List<String> INSTALLER_EXTENSIONS = List.of(".dmg", ".exe", ".deb", ".rpm");

    /**
     * Checks that the code of v2.1.13 reads each key file of a directory, for example
     * evolution/src/main/resources/keys of a new release, and that the key ring contains the key of its file name.
     */
    @Test
    @EnabledIfSystemProperty(named = "bisq.compat.keyDir", matches = ".+")
    void oldVersionReadsTheKeyFiles() throws Exception {
        Path keyDir = Path.of(System.getProperty("bisq.compat.keyDir"));
        for (String keyId : KEY_IDS) {
            Path keyFile = keyDir.resolve(keyId + ".asc");
            PGPPublicKeyRing keyRing = PgPUtils.readPgpPublicKeyRing(keyFile);
            List<String> keyIds = new ArrayList<>();
            Iterator<PGPPublicKey> keys = keyRing.getPublicKeys();
            while (keys.hasNext()) {
                PGPPublicKey key = keys.next();
                keyIds.add(shortKeyId(key.getKeyID()) + (key.isMasterKey() ? " (primary)" : " (subkey)"));
            }
            System.out.println(keyFile + ": " + keyIds);
            assertEquals(keyId, shortKeyId(keyRing.getPublicKey().getKeyID()), keyFile + " has another primary key");
        }
    }

    /**
     * Checks a detached signature of any file with the code of v2.1.13, for example a test file which the release
     * manager signs with the renewed key: {@code gpg --local-user <key id> --detach-sign --armor <file>}. The signature
     * must verify with the key file of each given directory, for example the keys of the new release and the keys which
     * v2.1.13 has in its resources.
     */
    @Test
    @EnabledIfSystemProperty(named = "bisq.compat.signedFile", matches = ".+")
    void oldVersionVerifiesASignatureWithEachKeyFile() {
        Path signedFile = Path.of(System.getProperty("bisq.compat.signedFile"));
        Path signatureFile = Path.of(signedFile + ".asc");
        String keyId = System.getProperty("bisq.compat.signingKeyId", "387C8307");
        String keyDirs = System.getProperty("bisq.compat.keyDirs", "src/main/resources/keys");
        for (String keyDir : keyDirs.split(",")) {
            Path keyFile = Path.of(keyDir).resolve(keyId + ".asc");
            assertTrue(PgPUtils.isSignatureValid(keyFile, signatureFile, signedFile),
                    "v2.1.13 cannot verify " + signatureFile + " with " + keyFile);
        }
    }

    /**
     * Runs the download and the verification of the updater of v2.1.13 for each installer of a release. By default it
     * downloads from GitHub and https://bisq.network/pubkey/. With bisq.compat.stagingDir it serves the files of a
     * directory with a local HTTP server instead, so a release can be checked before its upload. The staging directory
     * contains the release assets, and the web page keys in the subdirectory pubkey.
     */
    @Test
    @EnabledIfSystemProperty(named = "bisq.compat.releaseVersion", matches = ".+")
    void oldUpdaterDownloadsAndVerifiesEachInstaller() throws Exception {
        String version = System.getProperty("bisq.compat.releaseVersion");
        Path downloadDir = Path.of(System.getProperty("bisq.compat.downloadDir"));
        String extensions = System.getProperty("bisq.compat.installerExtensions", String.join(",", INSTALLER_EXTENSIONS));
        String stagingDir = System.getProperty("bisq.compat.stagingDir", "");

        HttpServer server = stagingDir.isEmpty() ? null : startStagingServer(Path.of(stagingDir), version);
        ExecutorService executorService = ExecutorFactory.newSingleThreadExecutor("OldUpdaterReplay");
        try {
            for (String extension : extensions.split(",")) {
                String installerFileName = "Bisq-" + version + extension;
                Path destinationDir = downloadDir.resolve(installerFileName + "-download");
                if (Files.exists(destinationDir)) {
                    FileMutatorUtils.deleteFileOrDirectory(destinationDir);
                }
                FileMutatorUtils.createDirectories(destinationDir);

                List<DownloadItem> items = DownloadItem.createDescriptorList(version, destinationDir, installerFileName, KEY_IDS);
                if (server != null) {
                    items = redirectToStagingServer(items, version, server.getAddress().getPort());
                }
                UpdaterService.download(items, executorService).join();

                byte[] signingKeyMarker = Files.readAllBytes(destinationDir.resolve(UpdaterUtils.SIGNING_KEY_FILE));
                assertTrue(KEY_IDS.contains(UpdaterUtils.getSigningKeyId(destinationDir)),
                        "signingkey.asc must contain exactly one of " + KEY_IDS + ", it has " + signingKeyMarker.length + " byte(s)");
                DownloadedFilesVerification.verify(destinationDir, installerFileName, KEY_IDS, false);
                System.out.println(installerFileName + ": the updater of v2.1.13 accepts the download");
            }
        } finally {
            executorService.shutdownNow();
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private static HttpServer startStagingServer(Path stagingDir, String version) throws IOException {
        assertTrue(Files.isDirectory(stagingDir), stagingDir + " is not a directory");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            Path file = path.startsWith("/pubkey/")
                    ? stagingDir.resolve("pubkey").resolve(path.substring("/pubkey/".length()))
                    : stagingDir.resolve(path.substring("/release/".length()));
            if (!file.normalize().startsWith(stagingDir.normalize()) || !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(200, Files.size(file));
            try (OutputStream outputStream = exchange.getResponseBody()) {
                Files.copy(file, outputStream);
            }
        });
        server.start();
        return server;
    }

    // Keeps the list and the file names of v2.1.13 and changes only the host of each URL
    private static List<DownloadItem> redirectToStagingServer(List<DownloadItem> items, String version, int port) throws Exception {
        String githubBaseUrl = UpdaterUtils.GITHUB_DOWNLOAD_URL + version + "/";
        String stagingRelease = "http://127.0.0.1:" + port + "/release/";
        String stagingPubKeys = "http://127.0.0.1:" + port + "/pubkey/";
        Method create = DownloadItem.class.getDeclaredMethod("create", String.class, String.class, String.class, Path.class);
        create.setAccessible(true);
        List<DownloadItem> redirected = new ArrayList<>();
        for (DownloadItem item : items) {
            String url = item.getUrlPath();
            String baseUrl;
            if (url.startsWith(githubBaseUrl)) {
                baseUrl = stagingRelease;
            } else if (url.startsWith(UpdaterUtils.PUB_KEYS_URL)) {
                baseUrl = stagingPubKeys;
            } else {
                throw new IllegalStateException("Unexpected URL of v2.1.13: " + url);
            }
            Path destination = item.getDestinationFilePath();
            redirected.add((DownloadItem) create.invoke(null, item.getSourceFileName(),
                    destination.getFileName().toString(), baseUrl, destination.getParent()));
        }
        assertEquals(items.size(), redirected.size());
        assertFalse(redirected.isEmpty());
        return redirected;
    }

    private static String shortKeyId(long keyId) {
        String hex = Long.toHexString(keyId).toUpperCase(Locale.ROOT);
        assertNotNull(hex);
        return hex.length() > 8 ? hex.substring(hex.length() - 8) : hex;
    }
}
