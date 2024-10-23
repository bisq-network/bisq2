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
import bisq.common.file.FileUtils;
import bisq.common.util.ByteArrayUtils;
import bisq.user.profile.UserProfile;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class CatHashService<T> {
    // Largest size in offerbook is 60px, in reputationListView it is 40px and in chats 30px.
    // Larger images are used only rarely and are not cached.
    public static final double SIZE_OF_CACHED_ICONS = 60;

    // This is a 120*120 image meaning 14400 pixels. At 4 bytes each, that takes 57.6 KB in memory (and on disk as we use raw format).
    // With 5000 images we would get about 288 MB.
    private static final int MAX_CACHE_SIZE = 5000;

    private final ConcurrentHashMap<BigInteger, T> cache = new ConcurrentHashMap<>();
    @Setter
    private Path baseDir;

    public CatHashService(Path baseDir) {
        this.baseDir = baseDir;
    }

    protected abstract T composeImage(String[] paths, double size);

    protected abstract void writeRawImage(T image, File iconFile) throws IOException;

    protected abstract T readRawImage(File iconFile) throws IOException;

    public T getImage(UserProfile userProfile, double size) {
        return getImage(userProfile.getPubKeyHash(),
                userProfile.getProofOfWork().getSolution(),
                userProfile.getAvatarVersion(),
                size);
    }

    public T getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion, double size) {
        byte[] combined = ByteArrayUtils.concat(powSolution, pubKeyHash);
        BigInteger catHashInput = new BigInteger(combined);
        String userProfileId = Hex.encode(pubKeyHash);
        File iconsDir = Path.of(getCatHashIconsDirectory().toString(), "v" + avatarVersion).toFile();
        File iconFile = Path.of(iconsDir.getAbsolutePath(), userProfileId + ".raw").toFile();

        boolean useCache = size <= SIZE_OF_CACHED_ICONS;
        if (useCache) {
            // First approach is to look up the cache
            if (cache.containsKey(catHashInput)) {
                return cache.get(catHashInput);
            }


            if (!iconsDir.exists()) {
                try {
                    FileUtils.makeDirs(iconsDir);
                } catch (IOException e) {
                    log.error(e.toString());
                }
            }

            // Next approach is to read the image from file
            if (iconFile.exists()) {
                try {
                    T image = readRawImage(iconFile);
                    if (cache.size() < MAX_CACHE_SIZE) {
                        cache.put(catHashInput, image);
                    }
                    return image;
                } catch (Exception e) {
                    log.error("Read image failed", e);
                }
            }
        }

        // Image size might be larger as our cached images, or we did not find it in the
        // cache and also not from persisted files. We create the image. This is an expensive operation taking
        // about 12 ms on a high-end laptop, and it needs to be done on the UI thread.
        long ts = System.currentTimeMillis();
        BucketConfig bucketConfig = getBucketConfig(avatarVersion);
        int[] buckets = BucketEncoder.encode(catHashInput, bucketConfig.getBucketSizes());
        String[] paths = BucketEncoder.toPaths(buckets, bucketConfig.getPathTemplates());
        // For retina support we scale by 2
        T image = composeImage(paths, 2 * SIZE_OF_CACHED_ICONS);
        log.info("Creating user profile icon for {} took {} ms.", userProfileId, System.currentTimeMillis() - ts);
        if (useCache && cache.size() < MAX_CACHE_SIZE) {
            cache.put(catHashInput, image);

            // We use the MAX_CACHE_SIZE also as limit for files on disk
            try {
                writeRawImage(image, iconFile);
            } catch (IOException e) {
                log.error("Write image failed", e);
            }
        }
        return image;
    }

    // Remove the user profile icons which are not contained anymore in the current user profile list
    public void pruneOutdatedProfileIcons(Collection<UserProfile> userProfiles) {
        if (userProfiles.isEmpty()) {
            return;
        }
        File iconsDirectory = getCatHashIconsDirectory().toFile();
        File[] versionDirs = iconsDirectory.listFiles();
        if (versionDirs == null) {
            return;
        }
        Map<String, List<File>> iconFilesByVersion = Stream.of(versionDirs)
                .filter(File::isDirectory)
                .filter(dir -> dir.listFiles() != null)
                .collect(Collectors.toMap(File::getName,
                        dir -> Arrays.asList(Objects.requireNonNull(dir.listFiles()))));

        Map<Integer, List<UserProfile>> userProfilesByVersion = userProfiles.stream()
                .collect(Collectors.groupingBy(UserProfile::getAvatarVersion));

        iconFilesByVersion.forEach((versionDir, iconFiles) -> {
            try {
                int version = Integer.parseInt(versionDir
                        .replace("v", ""));
                Set<String> fromDisk = iconFiles.stream()
                        .map(File::getName)
                        .collect(Collectors.toSet());
                Set<String> fromData = Optional.of(userProfilesByVersion.get(version).stream()
                                .map(userProfile -> userProfile.getId() + ".raw")
                                .collect(Collectors.toSet()))
                        .orElse(new HashSet<>());
                Set<String> toRemove = new HashSet<>(fromDisk);
                toRemove.removeAll(fromData);
                log.info("We remove following user profile icons which are not found in the current user profile list:{}", toRemove);
                toRemove.forEach(fileName -> {
                    File file = Path.of(iconsDirectory.getAbsolutePath(), versionDir, fileName).toFile();
                    try {
                        log.error("Remove {}", file);
                        FileUtils.deleteFile(file);
                    } catch (IOException e) {
                        log.error("Failed to remove file {}", file, e);
                    }
                });
            } catch (Exception e) {
                log.error("Unexpected versionDir {}", versionDir, e);
            }
        });
    }

    public int currentAvatarsVersion() {
        return BucketConfig.CURRENT_VERSION;
    }

    private Path getCatHashIconsDirectory() {
        return Path.of(baseDir.toString(), "db", "cache", "cat_hash_icons");
    }

    private BucketConfig getBucketConfig(int avatarVersion) {
        if (avatarVersion == 0) {
            return new BucketConfigV0();
        } else {
            throw new IllegalArgumentException("Provided avatarVersion not supported. avatarVersion=" + avatarVersion);
        }
    }
}
