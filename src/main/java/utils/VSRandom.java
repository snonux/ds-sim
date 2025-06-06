package utils;

import java.util.Random;

/**
 * The class VSRandom. Some customization of the standard Random class of Java.
 *
 * @author Paul C. Buetow
 */
public final class VSRandom extends Random {
    /**
     * Instantiates a new VSrandom object.
     *
     * @param seedAdd the seed to add.
     */
    public VSRandom(long seedAdd) {
        super(seedAdd*System.currentTimeMillis()+seedAdd);
    }

    /* (non-Javadoc)
     * @see java.util.Random#nextInt()
     */
    public int nextInt() {
        return Math.abs(super.nextInt());
    }

    /**
     * Next long.
     *
     * @param mod the mod
     *
     * @return the random long
     */
    public long nextLong(long mod) {
        return Math.abs((super.nextLong() + System.currentTimeMillis()) % mod);
    }
}
