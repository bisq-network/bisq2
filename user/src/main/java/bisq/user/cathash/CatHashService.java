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
import bisq.common.file.FileMutatorUtils;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.ByteArrayUtils;
import bisq.user.profile.UserProfile;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class CatHashService<T> {
    // Largest size in offerbook is 60px, in reputationListView it is 40px and in chats 30px.
    // Larger images are used only rarely and are not cached. We use 2x60 for retina resolution
    public static final double SIZE_OF_CACHED_ICONS = 120;

    // We limit size to max. 300 px as the png files for the image composition are of that size.
    public static final double MAX_ICON_SIZE = 300;

    // This is a 120*120 image meaning 14400 pixels. At 4 bytes each, that takes 57.6 KB in memory (and on disk as we use raw format).
    // With 5000 images we would get about 288 MB.
    private static final int MAX_CACHE_SIZE = 5000;

    private final ConcurrentHashMap<BigInteger, T> cache = new ConcurrentHashMap<>();
    @Setter
    private Path appDataDirPath;

    public CatHashService(Path appDataDirPath) {
        this.appDataDirPath = appDataDirPath;
    }

    protected abstract T composeImage(String[] paths, double size);

    protected abstract void writeRawImage(T image, Path iconPath) throws IOException;

    protected abstract T readRawImage(Path iconPath) throws IOException;

    public T getImage(UserProfile userProfile, double size) {
        checkArgument(size > 0, "Size must be > 0 at getImage");
        return getImage(userProfile.getPubKeyHash(),
                userProfile.getProofOfWork().getSolution(),
                userProfile.getAvatarVersion(),
                size);
    }

    public T getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion, double size) {
        byte[] combined = ByteArrayUtils.concat(powSolution, pubKeyHash);
        BigInteger catHashInput = new BigInteger(combined);
        String userProfileId = Hex.encode(pubKeyHash);
        Path iconsDirPath = getCatHashIconsDirPath().resolve("v" + avatarVersion);
        Path iconFilePath = iconsDirPath.resolve(userProfileId + ".raw");

        // We create the images internally with 2x size for retina resolution
        double scaledSize = 2 * size;
        if (scaledSize > MAX_ICON_SIZE) {
            log.warn("Scaled size for cat hash image is {} px. We limit size to max. {} px as the png files for the image composition " +
                    "are of that size.", scaledSize, MAX_ICON_SIZE);
            scaledSize = MAX_ICON_SIZE;
        }
        boolean useCache = scaledSize <= getSizeOfCachedIcons();
        if (useCache) {
            // First approach is to look up the cache
            if (cache.containsKey(catHashInput)) {
                return cache.get(catHashInput);
            }

            if (!Files.exists(iconsDirPath)) {
                try {
                    FileMutatorUtils.createDirectories(iconsDirPath);
                } catch (IOException e) {
                    log.error(e.toString());
                }
            }

            // Next approach is to read the image from file
            if (Files.exists(iconFilePath)) {
                try {
                    T image = readRawImage(iconFilePath);
                    if (cache.size() < getMaxCacheSize()) {
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
        T image = composeImage(paths, scaledSize);
        //log.info("Creating user profile icon for {} took {} ms.", userProfileId, System.currentTimeMillis() - ts);
        // We use the MAX_CACHE_SIZE as limit for files on disk
        if (useCache && cache.size() < getMaxCacheSize()) {
            cache.put(catHashInput, image);
            try {
                writeRawImage(image, iconFilePath);
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
        Path iconsDirPath = getCatHashIconsDirPath();

        try (Stream<Path> versionDirStream = Files.list(iconsDirPath)) {
            Map<String, List<Path>> iconFilesPathByVersion = versionDirStream
                    .filter(Files::isDirectory)
                    .filter(dir -> {
                        String name = dir.getFileName().toString();
                        if (!name.startsWith("v")) {
                            log.warn("Version directory in the cat_hash_icons directory not prefixed with 'v' {}", name);
                            return false;
                        }
                        try {
                            int version = Integer.parseInt(name.replace("v", ""));
                            boolean contains = BucketConfig.ALL_VERSIONS.contains(version);
                            if (!contains) {
                                log.warn("Version string '{}' of the version directory found in the existing versions: {}",
                                        version, BucketConfig.ALL_VERSIONS);
                            }
                            return contains;
                        } catch (NumberFormatException e) {
                            log.warn("Version postfix is not an integer in the directory: {}", name, e);
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString(),
                            dirPath -> {
                                try (Stream<Path> files = Files.list(dirPath)) {
                                    return files.collect(Collectors.toList());
                                } catch (IOException e) {
                                    log.error("Failed to list files in directory {}", dirPath, e);
                                    return Collections.emptyList();
                                }
                            }
                    ));

            Map<Integer, List<UserProfile>> userProfilesByVersion = userProfiles.stream()
                    .collect(Collectors.groupingBy(UserProfile::getAvatarVersion));
            CompletableFuture.runAsync(() -> {
                iconFilesPathByVersion.forEach((versionDir, iconFiles) -> {
                    try {
                        int version = Integer.parseInt(versionDir.replace("v", ""));
                        Set<String> fromDisk = iconFiles.stream()
                                .map(path -> path.getFileName().toString())
                                .collect(Collectors.toSet());
                        Set<String> fromData = Optional.ofNullable(userProfilesByVersion.get(version))
                                .map(profiles -> profiles.stream()
                                        .map(userProfile -> userProfile.getId() + ".raw")
                                        .collect(Collectors.toSet()))
                                .orElseGet(Collections::emptySet);
                        Set<String> toRemove = new HashSet<>(fromDisk);
                        toRemove.removeAll(fromData);

                        log.info("We remove {} outdated user profile icons (not found in the current user profile list)", toRemove.size());
                        if (toRemove.size() < 10) {
                            log.info("Removed user profile icons: {}", toRemove);
                        }
                        toRemove.forEach(fileName -> {
                            Path filePath = iconsDirPath.resolve(versionDir).resolve(fileName);
                            try {
                                log.debug("Remove {}", filePath);
                                Files.deleteIfExists(filePath);
                            } catch (IOException e) {
                                log.error("Failed to remove file {}", filePath, e);
                            }
                        });

                    } catch (Exception e) {
                        log.error("Unexpected versionDir {}", versionDir, e);
                    }
                });
            }, ExecutorFactory.newSingleThreadExecutor("pruneOutdatedProfileIcons"));
        } catch (IOException e) {
            log.error("Failed to list version directories in the cat_hash_icons directory", e);
        }
    }

    public int currentAvatarsVersion() {
        return BucketConfig.CURRENT_VERSION;
    }

    protected double getSizeOfCachedIcons() {
        return SIZE_OF_CACHED_ICONS;
    }

    protected int getMaxCacheSize() {
        return MAX_CACHE_SIZE;
    }

    private Path getCatHashIconsDirPath() {
        return appDataDirPath
                .resolve("db")
                .resolve("cache")
                .resolve("cat_hash_icons");
    }

    private BucketConfig getBucketConfig(int avatarVersion) {
        if (avatarVersion == 0) {
            return new BucketConfigV0();
        } else {
            throw new IllegalArgumentException("Provided avatarVersion not supported. avatarVersion=" + avatarVersion);
        }
    }
}
