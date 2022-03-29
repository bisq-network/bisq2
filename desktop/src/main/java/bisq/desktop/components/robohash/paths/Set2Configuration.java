package bisq.desktop.components.robohash.paths;


public class Set2Configuration implements Configuration {
    private final static String ROOT = "sets/set2";

    private static final int FACE_COLORS_COUNT = 10;
    private static final int BODY_COLORS_COUNT = 10;
    private static final int FACES_COUNT = 16;
    private static final int BODIES_COUNT = 10;
    private static final int EYES_COUNT = 10;
    private static final int MOUTHS_COUNT = 10;
    private static final int NOSES_COUNT = 10;

    private final static byte[] BUCKET_SIZES = new byte[]{
            FACE_COLORS_COUNT, 
            BODY_COLORS_COUNT, 
            FACES_COUNT, 
            BODIES_COUNT, 
            EYES_COUNT, 
            MOUTHS_COUNT, 
            NOSES_COUNT
    };

    private final static String[] FACET_PATH_TEMPLATES = new String[]{
            ROOT + "/01FaceColors/final#ITEM#.png",
            ROOT + "/02BodyColors/final#ITEM#.png",
            ROOT + "/03Faces/final#ITEM#.png",
            ROOT + "/04Body/final#ITEM#.png",
            ROOT + "/Eyes/final#ITEM#.png",
            ROOT + "/Mouth/final#ITEM#.png",
            ROOT + "/Nose/final#ITEM#.png",
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
        return 350;
    }

    @Override
    public int height() {
        return 350;
    }
}
