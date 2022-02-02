package bisq.desktop.robohash;

/*import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.LruCache;*/

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
import javafx.scene.layout.StackPane;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO Use low level image manipulation not StackPane and ImageView
// TODO add set2 and set3
// TODO Maybe add backgrounds?
public class RoboHash {
    private static final Configuration configuration = new Set1Configuration();
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
        Node node = imageForHandle(handle, configuration.height() / 4d);
        smallCache.put(pubKeyHash, node);
        return node;
    }

    private static Node imageForHandle(Handle handle, double size) {
        byte[] bucketValues = handle.bucketValues();
        String[] paths = configuration.convertToFacetParts(bucketValues);

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
