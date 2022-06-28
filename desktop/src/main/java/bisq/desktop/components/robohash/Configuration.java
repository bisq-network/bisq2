package bisq.desktop.components.robohash;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Configuration {
    private final static String ROOT = "";

    private final static int BUCKET_COLOR = 0;

    private static final int COLOR_COUNT = 3;
    private static final int BG_COUNT = 15;
    private static final int BODY_COUNT = 15;
    private static final int FACE_COUNT = 15;
    private static final int MOUTH_COUNT = 15;
    private static final int EYES_COUNT = 15;
    private static final int ACCESSORY_COUNT = 15;

    private final static int BUCKET_COUNT = 7;
    private final static int FACET_COUNT = 6;

    private final static byte[] BUCKET_SIZES = new byte[]{COLOR_COUNT, BG_COUNT, BODY_COUNT, FACE_COUNT, MOUTH_COUNT, EYES_COUNT, ACCESSORY_COUNT};

    private final static String[] INT_TO_COLOR = new String[]{
            "dark",
            "green",
            "light",
    };

    private final static String[] FACET_PATH_TEMPLATES;

    static {
        String postFix = ".png";
        FACET_PATH_TEMPLATES = new String[]{
                ROOT + "bg/#BG_ITEM#" + postFix,
                ROOT + "#COLOR#/0_body/#COLOR#_body-#ITEM#" + postFix,
                ROOT + "#COLOR#/1_face/#COLOR#_face-#ITEM#" + postFix,
                ROOT + "#COLOR#/2_mouth/#COLOR#_mouth-#ITEM#" + postFix,
                ROOT + "#COLOR#/3_eyes/#COLOR#_eyes-#ITEM#" + postFix,
                ROOT + "#COLOR#/4_accessory/#COLOR#_accessory-#ITEM#" + postFix,
        };
    }

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

    public byte[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    public int width() {
        return 300;
    }

    public int height() {
        return 300;
    }
}
