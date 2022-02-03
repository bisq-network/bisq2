package bisq.desktop.robohash;


import java.math.BigInteger;

/**
 * "Hash" a big integer (expected: hash or uuid) into buckets. The goal is to deterministically
 * "repack" the randomness of the hash into the bucket.
 * <p>
 * Each bucket is defined by a maximum value. The implementation guarantees that the values in bucket n is in the
 * range 0..(bucketSize[n]-1).
 */
public class Buckets {
    private final byte[] bucketSizes;

    public Buckets(byte[] bucketSizes) {
        this.bucketSizes = bucketSizes;
    }

    /**
     * Takes the hash value and distributes it over the buckets.
     * <p>
     * Assumption: the value of hash is (much) larger than 16^bucketSizes.length and uniformly distributed (random)
     *
     * @param hash Any BigInteger that is to be split up in buckets according to the bucket configuration #bucketSizes.
     * @return buckets The distributed hash
     */
    public byte[] createBuckets(byte[] hash) {
        BigInteger bigInteger = new BigInteger(hash);
        int currentBucket = 0;
        byte[] ret = new byte[bucketSizes.length];

        while (currentBucket < bucketSizes.length) {
            BigInteger[] divisorReminder = bigInteger.divideAndRemainder(BigInteger.valueOf(bucketSizes[currentBucket]));

            bigInteger = divisorReminder[0];
            long reminder = divisorReminder[1].longValue();

            ret[currentBucket] = (byte) Math.abs(reminder % bucketSizes[currentBucket]);

            currentBucket += 1;
        }
        return ret;
    }
}
