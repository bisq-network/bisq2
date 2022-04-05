package bisq.desktop.components.robohash.paths;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Set1Configuration implements Configuration {
    private final static String ROOT = "sets/set1";

    private final static int BUCKET_COLOR = 0;

    private static final int BG_COUNT = 14;
    private static final int COLOR_COUNT = 10;
    private static final int BODY_COUNT = 10;
    private static final int FACE_COUNT = 10;
    private static final int MOUTH_COUNT = 10;
    private static final int EYES_COUNT = 10;
    private static final int ACCESSORY_COUNT = 10;

    private final static int BUCKET_COUNT = 7;
    private final static int FACET_COUNT = 6;

    private final static byte[] BUCKET_SIZES = new byte[]{COLOR_COUNT, BG_COUNT, BODY_COUNT, FACE_COUNT, MOUTH_COUNT, EYES_COUNT, ACCESSORY_COUNT};

    private final static String[] INT_TO_COLOR = new String[]{
            "blue",
            "brown",
            "green",
            "grey",
            "orange",
            "pink",
            "purple",
            "red",
            "white",
            "yellow",
    };

    private final static String[] FACET_PATH_TEMPLATES = new String[]{
            ROOT + "/bg/#BG_ITEM#.png",
            ROOT + "/#COLOR#/01Body/#COLOR#_body-#ITEM#.png",
            ROOT + "/#COLOR#/02Face/#COLOR#_face-#ITEM#.png",
            ROOT + "/#COLOR#/Mouth/#COLOR#_mouth-#ITEM#.png",
            ROOT + "/#COLOR#/Eyes/#COLOR#_eyes-#ITEM#.png",
            ROOT + "/#COLOR#/Accessory/#COLOR#_accessory-#ITEM#.png",
    };

    @Override
    public String[] convertToFacetParts(byte[] bucketValues) {
        if (bucketValues.length != BUCKET_COUNT) throw new IllegalArgumentException();

        String color = INT_TO_COLOR[bucketValues[BUCKET_COLOR]];
        String[] paths = new String[FACET_COUNT];

        int firstFacetBucket = BUCKET_COLOR + 1;

        for (int facet = 0; facet < FACET_COUNT; facet++) {
            int bucketValue = bucketValues[firstFacetBucket + facet];
            paths[facet] = generatePath(FACET_PATH_TEMPLATES[facet], color, bucketValue);
        }
        return paths;
    }

    private String generatePath(String facetPathTemplate, String color, int bucketValue) {
        return facetPathTemplate
                .replaceAll("#COLOR#", color)
                .replaceAll("#ITEM#", String.format("%02d", bucketValue + 1))
                .replaceAll("#BG_ITEM#", String.format("%03d", bucketValue));
    }

    @Override
    public byte[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    @Override
    public int width() {
        return 300;
    }

    @Override
    public int height() {
        return 300;
    }
}
