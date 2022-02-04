package bisq.desktop.robohash;

import bisq.common.data.ByteArray;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.robohash.buckets.VariableSizeHashing;
import bisq.desktop.robohash.handle.Handle;
import bisq.desktop.robohash.handle.HandleFactory;
import bisq.desktop.robohash.paths.Configuration;
import bisq.desktop.robohash.paths.Set1Configuration;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO Maybe add backgrounds?
public class RoboHash {
    private static final Configuration configuration = new Set1Configuration();
//    private static final Configuration configuration = new Set2Configuration();
//    private static final Configuration configuration = new Set3Configuration();
    private static final VariableSizeHashing hashing = new VariableSizeHashing(configuration.getBucketSizes());
    private static final HandleFactory handleFactory = new HandleFactory();
    private static final Map<ByteArray, Node> smallCache = new ConcurrentHashMap<>();
    private static final Map<ByteArray, Node> largeCache = new ConcurrentHashMap<>();

    public static Node getLarge(ByteArray pubKeyHash) {
        if (largeCache.containsKey(pubKeyHash)) {
            return largeCache.get(pubKeyHash);
        }
        byte[] data = hashing.createBuckets(new BigInteger(pubKeyHash.getBytes()));
        Handle handle = handleFactory.calculateHandle(data);
        Node node = imageForHandle(handle, configuration.height());
        largeCache.put(pubKeyHash, node);
        return node;
    }

    public static Node getSmall(ByteArray pubKeyHash) {
        if (smallCache.containsKey(pubKeyHash)) {
            return smallCache.get(pubKeyHash);
        }
        byte[] data = hashing.createBuckets(new BigInteger(pubKeyHash.getBytes()));
        Handle handle = handleFactory.calculateHandle(data);
        Node node = imageForHandle(handle, (int) (configuration.height() / 4d));
        smallCache.put(pubKeyHash, node);
        return node;
    }

    private static Node imageForHandle(Handle handle, int size) {
        byte[] bucketValues = handle.bucketValues();
        String[] paths = configuration.convertToFacetParts(bucketValues);
        
        Image image = ImageUtil.composeImage(paths, size, size);
        return new ImageView(image);
    }
}
