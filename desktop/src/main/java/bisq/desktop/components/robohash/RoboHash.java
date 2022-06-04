package bisq.desktop.components.robohash;

import bisq.common.data.ByteArray;
import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

//todo use equihash
// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class RoboHash {
    private static final int MAX_CACHE_SIZE = 10000;
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
        Configuration configuration = new Configuration();
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
        long ts = System.currentTimeMillis();
        byte[] bucketValues = handle.bucketValues();
        String[] paths = configuration.convertToFacetParts(bucketValues);
        log.debug("Generated paths for RoboHash image in {} ms", System.currentTimeMillis() - ts); // typically <1ms
        return ImageUtil.composeImage(paths, configuration.width(), configuration.height());
    }
}
