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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationVersionTest {

    // The resource is written by generateBuildCommitResource in common/build.gradle.kts. This test fails if that task
    // and ApplicationVersion disagree on its path or key.
    @Test
    void testBuildCommitShortHashComesFromBuildResource() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = ApplicationVersion.class.getResourceAsStream(ApplicationVersion.BUILD_COMMIT_RESOURCE)) {
            assertNotNull(inputStream, ApplicationVersion.BUILD_COMMIT_RESOURCE + " is missing");
            properties.load(inputStream);
        }
        String commitShortHash = properties.getProperty(ApplicationVersion.COMMIT_SHORT_HASH_KEY);

        assertNotNull(commitShortHash, ApplicationVersion.BUILD_COMMIT_RESOURCE + " has no " + ApplicationVersion.COMMIT_SHORT_HASH_KEY);
        assertTrue(commitShortHash.matches("[0-9a-f]{10}|unknown"), commitShortHash);
        assertEquals(commitShortHash, ApplicationVersion.getBuildCommitShortHash());
    }
}
