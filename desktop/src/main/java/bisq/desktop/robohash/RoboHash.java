package bisq.desktop.robohash;

import bisq.common.data.ByteArray;
import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Derived from https://github.com/neuhalje/android-robohash

// TODO Use low level image composition not StackPane and ImageView
// TODO add set2 and set3
// TODO Maybe add backgrounds?
public class RoboHash {
    private static final int MAX_CACHE_SIZE = 10000;
    private static final Configuration CONFIGURATION = new Set1Configuration();
    private static final Buckets BUCKETS = new Buckets(CONFIGURATION.getBucketSizes());
    private static final Map<ByteArray, Node> SMALL_CACHE = new ConcurrentHashMap<>();
    private static final Map<ByteArray, Node> LARGE_CACHE = new ConcurrentHashMap<>();

    public static Node getLarge(ByteArray pubKeyHash) {
        if (LARGE_CACHE.containsKey(pubKeyHash)) {
            return LARGE_CACHE.get(pubKeyHash);
        }
        byte[] buckets = BUCKETS.createBuckets(pubKeyHash.getBytes());
        long handle = HandleFactory.calculateHandleValue(buckets);
        Node node = imageForHandle(handle, CONFIGURATION.height());

        if (LARGE_CACHE.size() < MAX_CACHE_SIZE) {
            LARGE_CACHE.put(pubKeyHash, node);
        }
        return node;
    }

    public static Node getSmall(ByteArray pubKeyHash) {
        if (SMALL_CACHE.containsKey(pubKeyHash)) {
            return SMALL_CACHE.get(pubKeyHash);
        }
        byte[] buckets = BUCKETS.createBuckets(pubKeyHash.getBytes());
        long handle = HandleFactory.calculateHandleValue(buckets);
        Node node = imageForHandle(handle, CONFIGURATION.height() / 4d);
        if (SMALL_CACHE.size() < MAX_CACHE_SIZE) {
            SMALL_CACHE.put(pubKeyHash, node);
        }
        return node;
    }

    private static Node imageForHandle(long handle, double size) {
        byte[] bucketValues = HandleFactory.bucketValues(handle);
        String[] paths = CONFIGURATION.convertToFacetParts(bucketValues);

        List<ImageView> imageViews = new ArrayList<>();
        for (String path : paths) {
            Image image = getImage(path, size);
            imageViews.add(new ImageView(image));
        }
        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(imageViews);
        return stackPane;
    }

    private static Image getImage(String path, double size) {
        return ImageUtil.getImageByPath("/images/robohash/" + path, size);
    }
}
