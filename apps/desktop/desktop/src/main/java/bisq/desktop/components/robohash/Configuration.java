package bisq.desktop.components.robohash;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Configuration {
    private final static String ROOT = "";

//    private final static int BUCKET_COLOR = 0;
//
//    private static final int COLOR_COUNT = 3;
//    private static final int BG_COUNT = 15;
//    private static final int BODY_COUNT = 15;
//    private static final int FACE_COUNT = 15;
//    private static final int MOUTH_COUNT = 15;
//    private static final int EYES_COUNT = 15;
//    private static final int ACCESSORY_COUNT = 15;
//
//    private final static int BUCKET_COUNT = 7;
//    private final static int FACET_COUNT = 6;
//
//    private final static byte[] BUCKET_SIZES = new byte[]{COLOR_COUNT, BG_COUNT, BODY_COUNT, FACE_COUNT, MOUTH_COUNT, EYES_COUNT, ACCESSORY_COUNT};
//
//    private final static String[] INT_TO_COLOR = new String[]{
//            "dark",
//            "green",
//            "light",
//    };

    private static final int BG0_COUNT = 15;
    private static final int BG1_COUNT = 31;
    private static final int EARS0_COUNT = 15;
    private static final int EARS1_COUNT = 2;
    private static final int FACE0_COUNT = 15;
    private static final int FACE1_COUNT = 15;
    private static final int EYES0_COUNT = 15;
    private static final int NOSE0_COUNT = 5;
    private static final int WHISKERS0_COUNT = 7;

    private final static int BUCKET_COUNT = 9;
    private final static int FACET_COUNT = 9;

    private final static byte[] BUCKET_SIZES = new byte[]{BG0_COUNT, BG1_COUNT, EARS0_COUNT, EARS1_COUNT, FACE0_COUNT,
            FACE1_COUNT, EYES0_COUNT, NOSE0_COUNT, WHISKERS0_COUNT};

//    private final static String[] INT_TO_COLOR = new String[]{
//            "dark",
//            "green",
//            "light",
//    };

    private final static String[] FACET_PATH_TEMPLATES;

//    static {
//        String postFix = ".png";
//        FACET_PATH_TEMPLATES = new String[]{
//                ROOT + "bg/#BG_ITEM#" + postFix,
//                ROOT + "#COLOR#/0_body/#COLOR#_body-#ITEM#" + postFix,
//                ROOT + "#COLOR#/1_face/#COLOR#_face-#ITEM#" + postFix,
//                ROOT + "#COLOR#/2_mouth/#COLOR#_mouth-#ITEM#" + postFix,
//                ROOT + "#COLOR#/3_eyes/#COLOR#_eyes-#ITEM#" + postFix,
//                ROOT + "#COLOR#/4_accessory/#COLOR#_accessory-#ITEM#" + postFix,
//        };
//    }

    static {
        String postFix = ".png";
        FACET_PATH_TEMPLATES = new String[]{
                ROOT + "bg0/bg0_#ITEM#" + postFix,
                ROOT + "bg1/bg1_#ITEM#" + postFix,
                ROOT + "ears0/ears0_#ITEM#" + postFix,
                ROOT + "ears1/ears1_#ITEM#" + postFix,
                ROOT + "face0/face0_#ITEM#" + postFix,
                ROOT + "face1/face1_#ITEM#" + postFix,
                ROOT + "eyes0/eyes0_#ITEM#" + postFix,
                ROOT + "nose0/nose0_#ITEM#" + postFix,
                ROOT + "whiskers0/whiskers0_#ITEM#" + postFix,
        };
    }

    public String[] convertToFacetParts(byte[] bucketValues) {
        if (bucketValues.length != BUCKET_COUNT) throw new IllegalArgumentException();

        //String color = INT_TO_COLOR[bucketValues[BUCKET_COLOR]];
        String[] paths = new String[FACET_COUNT];

        //int firstFacetBucket = BUCKET_COLOR + 1;

//        for (int facet = 0; facet < FACET_COUNT; facet++) {
//            int bucketValue = bucketValues[firstFacetBucket + facet];
//            paths[facet] = generatePath(FACET_PATH_TEMPLATES[facet], color, bucketValue);
//        }

        for (int facet = 0; facet < FACET_COUNT; facet++) {
            int bucketValue = bucketValues[facet];
            paths[facet] = generatePath(FACET_PATH_TEMPLATES[facet], bucketValue);
        }
        return paths;
    }

    private String generatePath(String facetPathTemplate, int bucketValue) {
        return facetPathTemplate.replaceAll("#ITEM#", String.format("%03d", bucketValue));
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
