package core.time;

import java.util.ArrayList;

/**
 * The class VSVectorTime, defined how the vector timestamps are represented.
 *
 * @author Paul C. Buetow
 */
public class VSVectorTime extends ArrayList<Long> implements VSTime {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The global time. */
    private long globalTime;

    /**
     * Instantiates a new vector time.
     *
     * @param globalTime the global time
     */
    public VSVectorTime(long globalTime) {
        this.globalTime = globalTime;
    }

    /**
     * To long array.
     *
     * @return the long[]
     */
    public long[] toLongArray() {
        final int size = super.size();
        final long[] arr = new long[size];

        for (int i = 0; i < size; ++i)
            arr[i] = super.get(i).longValue();

        return arr;
    }

    /**
     * Sets the global time.
     *
     * @param globalTime the new global time
     */
    public void setGlobalTime(long globalTime) {
        this.globalTime = globalTime;
    }

    /* (non-Javadoc)
     * @see core.time.VSTime#getGlobalTime()
     */
    public long getGlobalTime() {
        return globalTime;
    }

    /**
     * Gets the copy.
     *
     * @return the copy
     */
    public VSVectorTime getCopy() {
        final VSVectorTime vectorTime = new VSVectorTime(globalTime);
        final int size = super.size();

        for (int i = 0; i < size; ++i)
            vectorTime.add(super.get(i));

        return vectorTime;
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#toString()
     */
    public String toString() {
        final int size = super.size();
        final StringBuffer buffer = new StringBuffer();
        buffer.append("(");

        for (int i = 0; i < size-1; ++i)
            buffer.append(super.get(i)+",");
        buffer.append(super.get(size-1)+")");

        return buffer.toString();
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#get(int)
     */
    public Long get(int index) {
        if (index >= super.size())
            return Long.valueOf(0);

        return super.get(index);
    }
}
