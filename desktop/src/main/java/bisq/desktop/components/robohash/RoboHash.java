package bisq.desktop.components.robohash;

import bisq.common.data.ByteArray;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.robohash.buckets.VariableSizeHashing;
import bisq.desktop.components.robohash.handle.Handle;
import bisq.desktop.components.robohash.handle.HandleFactory;
import bisq.desktop.components.robohash.paths.Configuration;
import bisq.desktop.components.robohash.paths.Set1Configuration;
import bisq.desktop.components.robohash.paths.Set2Configuration;
import bisq.desktop.components.robohash.paths.Set3Configuration;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class RoboHash {
    private static final int MAX_CACHE_SIZE = 10000;
    private static final List<Configuration> CONFIGURATION_LIST = List.of(new Set1Configuration(),
            new Set2Configuration(),
            new Set3Configuration());
    private static final HandleFactory HANDLE_FACTORY = new HandleFactory();
    private static final ConcurrentHashMap<ByteArray, Image> CACHE = new ConcurrentHashMap<>();

    public static Image getImage(ByteArray pubKeyHash) {
        return getImage(pubKeyHash, true);
    }

    public static Image getImage(ByteArray pubKeyHash, boolean useCache) {
        if (useCache && CACHE.containsKey(pubKeyHash)) {
            return CACHE.get(pubKeyHash);
        }
        BigInteger bigInteger = new BigInteger(pubKeyHash.getBytes());
        // Set 2 and set 3 are not that high quality as set 1, so lets stick with set 1 for now
        // int selector = bigInteger.mod(BigInteger.valueOf(3)).intValue();
        int selector = 0;
        Configuration configuration = CONFIGURATION_LIST.get(selector);
        VariableSizeHashing hashing = new VariableSizeHashing(configuration.getBucketSizes());
        byte[] data = hashing.createBuckets(bigInteger);
        Handle handle = HANDLE_FACTORY.calculateHandle(data);
        Image image = imageForHandle(handle, configuration);
        if (useCache && CACHE.size() < MAX_CACHE_SIZE) {
            CACHE.put(pubKeyHash, image);
        }
        return image;
    }

    private static Image imageForHandle(Handle handle, Configuration configuration) {
        long t0 = System.currentTimeMillis();
        byte[] bucketValues = handle.bucketValues();
        String[] paths = configuration.convertToFacetParts(bucketValues);
        log.debug("Generated paths for RoboHash image in {} ms", System.currentTimeMillis() - t0); // typically <1ms
        return ImageUtil.composeImage(paths, configuration.width(), configuration.height());
    }
}
