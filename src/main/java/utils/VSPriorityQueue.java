package utils;

import java.util.PriorityQueue;

/**
 * The class VSPriorityQueue. This class is the same like the standard
 * VSPriorityQueue of the Java API. It only overrides the get(int) method.
 *
 * @author Paul C. Buetow
 */
public final class VSPriorityQueue<T> extends PriorityQueue<T> {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /**
     * Gets the specific element. If the index is out of bounds, it will return
     * null.
     *
     * @param index the index
     *
     * @return the element, or null, if out of bounds
     */
    public T get(int index) {
        int i = 0;

        for (T t : this)
            if (i++ == index)
                return t;

        return null;
    }
}
