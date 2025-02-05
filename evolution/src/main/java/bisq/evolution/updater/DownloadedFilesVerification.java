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
    public static void verify(String directory, String fileName, List<String> keyIds, boolean ignoreSigningKeyInResourcesCheck) throws IOException {
        String signingKeyId = getSigningKeyId(directory);
        checkArgument(keyIds.contains(signingKeyId), "signingKeyId not matching any of the provided keys");
        String signingKey = getSigningKey(directory, signingKeyId);

        // We require that the signing key is provided on the Bisq webpage
        DownloadedFilesVerification.checkIfSigningKeyMatchesKeyFromWebpage(directory, signingKeyId, signingKey);

        if (!ignoreSigningKeyInResourcesCheck) {
            DownloadedFilesVerification.checkIfSigningKeyMatchesKeyInResources(signingKeyId, signingKey);
        }

        File signingKeyFile = Path.of(directory, signingKeyId + ASC_EXTENSION).toFile();
        File sigFile = Path.of(directory, fileName + ASC_EXTENSION).toFile();
        File dataFile = Path.of(directory, fileName).toFile();
        checkArgument(PgPUtils.isSignatureValid(signingKeyFile, sigFile, dataFile), "Signature verification failed");
        log.info("signature verification succeeded");
    }
    private static void checkIfSigningKeyMatchesKeyFromWebpage(String directory, String keyId, String signingKey) throws IOException {
        String keyFileName = FROM_BISQ_WEBPAGE_PREFIX + keyId + ASC_EXTENSION;
        String keyFromWebpage = FileUtils.readStringFromFile(Path.of(directory, keyFileName).toFile());
        checkArgument(keyFromWebpage.equals(signingKey),
                "Key from webpage not matching signing key. keyFromWebpage=" + keyFromWebpage + "; signingKey=" + signingKey);
    }


    private static void checkIfSigningKeyMatchesKeyInResources(String keyId, String signingKey) throws IOException {
        String keyFromResources = FileUtils.readStringFromResource("keys/" + keyId + ASC_EXTENSION);
        checkArgument(keyFromResources.equals(signingKey),
                "Key from resources not matching signing key. keyFromResources=" + keyFromResources + "; signingKey=" + signingKey);
    }
}