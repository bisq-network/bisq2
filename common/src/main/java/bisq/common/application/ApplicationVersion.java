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

package bisq.common.application;

import bisq.common.platform.Version;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class ApplicationVersion {
    // Written by the generateBuildCommitResource task of common
    private static final String BUILD_COMMIT_RESOURCE = "build-commit.properties";
    private static final String UNKNOWN_COMMIT = "unknown";

    private static Version version;
    private static String buildCommitShortHash;

    public static Version getVersion() {
        if (version == null) {
            try {
                version = new Version(BuildVersion.VERSION);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }
        return version;
    }

    public static String getTorVersionString() {
        return BuildVersion.TOR_VERSION;
    }

    public static String getBuildCommitShortHash() {
        if (buildCommitShortHash == null) {
            buildCommitShortHash = readBuildCommitShortHash();
        }
        return buildCommitShortHash;
    }

    private static String readBuildCommitShortHash() {
        try (InputStream inputStream = ApplicationVersion.class.getResourceAsStream(BUILD_COMMIT_RESOURCE)) {
            if (inputStream == null) {
                log.warn("Resource {} not found", BUILD_COMMIT_RESOURCE);
                return UNKNOWN_COMMIT;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("commitShortHash", UNKNOWN_COMMIT);
        } catch (IOException e) {
            log.warn("Could not read resource {}", BUILD_COMMIT_RESOURCE, e);
            return UNKNOWN_COMMIT;
        }
    }
}