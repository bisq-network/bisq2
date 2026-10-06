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

package bisq.user.cathash;

import bisq.common.encoding.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatHashServiceTest {
    private static final byte[] PUB_KEY_HASH = filled(20, 7);
    private static final byte[] POW_SOLUTION = filled(72, 3);
    private static final List<Integer> UNSUPPORTED_VERSIONS = List.of(1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);

    @TempDir
    Path tempDir;

    @Test
    void unsupportedAvatarVersionIsRenderedLikeVersion0() throws IOException {
        String imageOfVersion0 = newService("v0").getImage(PUB_KEY_HASH, POW_SOLUTION, 0, 60);
        for (int avatarVersion : UNSUPPORTED_VERSIONS) {
            // A fresh service for each version, so the image does not come from the cache of an earlier call
            String image = newService("v" + avatarVersion).getImage(PUB_KEY_HASH, POW_SOLUTION, avatarVersion, 60);
            assertEquals(imageOfVersion0, image, "avatarVersion " + avatarVersion);
        }
    }

    @Test
    void unsupportedAvatarVersionIsRenderedLikeVersion0AlsoWithoutCache() throws IOException {
        String imageOfVersion0 = newService("large-v0").getImage(PUB_KEY_HASH, POW_SOLUTION, 0, 150);
        String image = newService("large-v1").getImage(PUB_KEY_HASH, POW_SOLUTION, 1, 150);
        assertEquals(imageOfVersion0, image);
    }

    @Test
    void iconOfAnUnsupportedAvatarVersionIsCachedInTheDirectoryOfVersion0() throws IOException {
        Path appDataDir = Files.createDirectories(tempDir.resolve("cache-dir"));
        new TestCatHashService(appDataDir).getImage(PUB_KEY_HASH, POW_SOLUTION, 7, 60);

        Path iconsDir = appDataDir.resolve("db").resolve("cache").resolve("cat_hash_icons");
        assertTrue(Files.exists(iconsDir.resolve("v0").resolve(Hex.encode(PUB_KEY_HASH) + ".raw")));
        assertFalse(Files.exists(iconsDir.resolve("v7")));
    }

    @Test
    void renderedAvatarVersionIsTheVersionIfSupportedOtherwise0() {
        assertEquals(0, CatHashService.toRenderedAvatarVersion(0));
        for (int avatarVersion : UNSUPPORTED_VERSIONS) {
            assertEquals(0, CatHashService.toRenderedAvatarVersion(avatarVersion), "avatarVersion " + avatarVersion);
        }
    }

    private TestCatHashService newService(String name) throws IOException {
        return new TestCatHashService(Files.createDirectories(tempDir.resolve(name)));
    }

    private static byte[] filled(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    // Composes an "image" from the paths of its parts, so that two images are equal if they use the same parts
    private static class TestCatHashService extends CatHashService<String> {
        TestCatHashService(Path appDataDirPath) {
            super(appDataDirPath);
        }

        @Override
        protected String composeImage(String[] paths, double size) {
            return String.join("|", paths);
        }

        @Override
        protected void writeRawImage(String image, Path iconPath) throws IOException {
            Files.writeString(iconPath, image, StandardCharsets.UTF_8);
        }

        @Override
        protected String readRawImage(Path iconPath) throws IOException {
            return Files.readString(iconPath, StandardCharsets.UTF_8);
        }
    }
}
