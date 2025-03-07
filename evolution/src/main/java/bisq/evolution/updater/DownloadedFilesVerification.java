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

import bisq.common.file.FileUtils;
import bisq.security.PgPUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static bisq.evolution.updater.UpdaterUtils.*;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class DownloadedFilesVerification {
    public static void verify(String directory,
                              String dataFileName,
                              List<String> keyIds,
                              boolean ignoreSigningKeyInResourcesCheck) throws IOException {
        String signingKeyId = getSigningKeyId(directory);
        checkArgument(keyIds.contains(signingKeyId), "signingKeyId not matching any of the provided keys");
        String signingKey = getSigningKey(directory, signingKeyId);
        File sigFile = Path.of(directory, dataFileName + ASC_EXTENSION).toFile(); // E.g. Bisq-2.1.3.dmg.asc
        File dataFile = Path.of(directory, dataFileName).toFile(); // E.g. Bisq2.dmg

        // We require that the signing key is provided on the Bisq webpage
        checkSignatureWithKeyFromWebpage(directory, signingKeyId, signingKey, sigFile, dataFile);

        if (!ignoreSigningKeyInResourcesCheck) {
            checkSignatureWithKeyInResources(directory, signingKeyId, signingKey, sigFile, dataFile);
        }

        String signingKeyFileName = signingKeyId + ASC_EXTENSION;
        File signingKeyFile = Path.of(directory, signingKeyId + ASC_EXTENSION).toFile(); // E.g. E222AA02.asc
        checkArgument(PgPUtils.isSignatureValid(signingKeyFile, sigFile, dataFile), "Signature verification failed: signingKeyFileName=" + signingKeyFileName);
        log.info("signature verification succeeded");
    }

    private static void checkSignatureWithKeyFromWebpage(String directory,
                                                         String signingKeyId,
                                                         String signingKey,
                                                         File sigFile,
                                                         File dataFile) {

        String signingKeyFileName = FROM_BISQ_WEBPAGE_PREFIX + signingKeyId + ASC_EXTENSION;
        File signingKeyFile = Path.of(directory, signingKeyFileName).toFile(); // E.g. from_bisq_webpage_E222AA02.asc
        checkArgument(PgPUtils.isSignatureValid(signingKeyFile, sigFile, dataFile), "Signature verification failed: signingKeyFileName=" + signingKeyFileName);
    }

    private static void checkSignatureWithKeyInResources(String directory,
                                                         String signingKeyId,
                                                         String signingKey,
                                                         File sigFile,
                                                         File dataFile) throws IOException {
        String signingKeyFileName = FROM_RESOURCES_PREFIX + signingKeyId + ASC_EXTENSION;
        File signingKeyFile = Path.of(directory, signingKeyFileName).toFile(); // E.g. from_resources_E222AA02.asc
        FileUtils.resourceToFile("keys/" + signingKeyId + ASC_EXTENSION, signingKeyFile); // We copy key from resources to download directory
        checkArgument(PgPUtils.isSignatureValid(signingKeyFile, sigFile, dataFile), "Signature verification failed: signingKeyFileName=" + signingKeyFileName);
    }
}