package bisq.desktop.components.robohash.paths;


public class Set3Configuration implements Configuration {
    private final static String ROOT = "sets/set3";

    private static final int BASE_FACES_COUNT = 15; // Max nibble value
    private static final int WAVES_COUNT = 1;
    private static final int ANTENNAS_COUNT = 10;
    private static final int EYES_COUNT = 12;
    private static final int EYEBROWS_COUNT = 12;
    private static final int NOSES_COUNT = 11;
    private static final int MOUTHS_COUNT = 9;

    private final static byte[] BUCKET_SIZES = new byte[]{
            BASE_FACES_COUNT,
            WAVES_COUNT,
            ANTENNAS_COUNT,
            EYES_COUNT,
            EYEBROWS_COUNT,
            NOSES_COUNT,
            MOUTHS_COUNT
    };

    private final static String[] FACET_PATH_TEMPLATES = new String[]{
            ROOT + "/01BaseFace/Robot-Design#ITEM#.png",
            ROOT + "/02Wave/wave#ITEM#.png",
            ROOT + "/03Antenna/Robot-Design#ITEM#.png",
            ROOT + "/04Eyes/Robot-Design#ITEM#.png",
            ROOT + "/05Eyebrows/Robot-Design#ITEM#.png",
            ROOT + "/06Nose/Robot-Design#ITEM#.png",
            ROOT + "/07Mouth/Robot-Design#ITEM#.png",
    };

    @Override
    public String[] convertToFacetParts(byte[] bucketValues) {
        if (bucketValues.length != BUCKET_SIZES.length) throw new IllegalArgumentException();

        String[] paths = new String[FACET_PATH_TEMPLATES.length];

        for (int i = 0; i < FACET_PATH_TEMPLATES.length; i++) {
            int bucketValue = bucketValues[i] + 1;
            String facetPathTemplate = FACET_PATH_TEMPLATES[i];
            paths[i] = facetPathTemplate
                    .replaceAll("#ITEM#", String.valueOf(bucketValue));
        }
        return paths;
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
