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

package bisq.network.tor.installer;

import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.platform.OS;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class TorInstaller {
    private static final String CODESIGN_PATH = "/usr/bin/codesign";
    private static final int CODESIGN_TIMEOUT_SECONDS = 30;
    private static final String VERSION = "0.2.0";
    private final Path torDirPath;
    private final Path versionFilePath;

    public TorInstaller(Path torDirPath) {
        this.torDirPath = torDirPath;
        this.versionFilePath = this.torDirPath.resolve("version");
    }

    public void installIfNotUpToDate() {
        try {
            if (!isTorUpToDate()) {
                install();
            }
        } catch (IOException e) {
            throw new CannotInstallBundledTor(e);
        }
    }

    public void deleteVersionFile() {
        try {
            Files.deleteIfExists(versionFilePath);
            log.debug("Deleted {}", versionFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Couldn't delete version file {}", versionFilePath.toAbsolutePath(), e);
        }
    }

    private boolean isTorUpToDate() throws IOException {
        if (!Files.exists(versionFilePath)) {
            return false;
        }
        String torVersion = FileReaderUtils.readUTF8String(versionFilePath);
        return VERSION.equals(torVersion);
    }

    private void install() throws IOException {
        try {
            new TorBinaryZipExtractor(torDirPath).extractBinary();
            adHocSignTorBinariesOnMac();
            log.info("Tor files installed to {}", torDirPath.toAbsolutePath());
            // Only if we have successfully extracted all files we write our version file which is used to
            // check if we need to call installFiles.
            FileMutatorUtils.writeToPath(VERSION, versionFilePath);
        } catch (IOException e) {
            deleteVersionFile();
            throw e;
        }
    }

    private void adHocSignTorBinariesOnMac() throws IOException {
        if (!OS.isMacOs()) {
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(CODESIGN_PATH);
        command.add("-s");
        command.add("-");
        command.add("-f");
        command.add("--timestamp=none");
        int baseCommandSize = command.size();

        try (Stream<Path> stream = Files.walk(torDirPath, 2)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.equals("tor") || fileName.endsWith(".dylib");
                    })
                    .forEach(path -> command.add(path.toAbsolutePath().toString()));
        }

        if (command.size() == baseCommandSize) {
            throw new IOException("No Tor binaries found for ad-hoc signing in " + torDirPath.toAbsolutePath());
        }

        log.info("Applying macOS ad-hoc codesign to bundled Tor binaries in {}", torDirPath.toAbsolutePath());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        try {
            boolean completed = process.waitFor(CODESIGN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Ad-hoc signing Tor binaries timed out after " +
                        CODESIGN_TIMEOUT_SECONDS + " seconds.");
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Ad-hoc signing Tor binaries failed with exit code " + exitCode +
                        ". Output: " + output);
            }
            log.info("Successfully applied macOS ad-hoc codesign to bundled Tor binaries");
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while ad-hoc signing Tor binaries", e);
        }
    }
}
