package prefs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

/**
 * The class VSPrefsRestriction.
 */
abstract public class VSPrefsRestriction implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * The class VSIntegerPrefsRestriction.
     */
    public static class VSIntegerPrefsRestriction extends VSPrefsRestriction {
        private static final long serialVersionUID = 2L;
        /** The min value. */
        private int minValue;

        /** The max value. */
        private int maxValue;

        /**
         * Instantiates a new integer setting restriction.
         *
         * @param minValue the min value
         * @param maxValue the max value
         */
        public VSIntegerPrefsRestriction(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        /**
         * Gets the min value.
         *
         * @return the min value
         */
        public int getMinValue() {
            return minValue;
        }

        /**
         * Gets the max value.
         *
         * @return the max value
         */
        public int getMaxValue() {
            return maxValue;
        }

        /* (non-Javadoc)
         * @see prefs.VSPrefsRestriction#writeObject(java.io.ObjectOutputStream)
         */
        public void writeObject(ObjectOutputStream out)
        throws IOException {
            out.writeObject(Integer.valueOf(minValue));
            out.writeObject(Integer.valueOf(maxValue));
        }

        /* (non-Javadoc)
         * @see prefs.VSPrefsRestriction#readObject(java.io.ObjectInputStream)
         */
        public void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
            minValue = ((Integer) in.readObject()).intValue();
            maxValue = ((Integer) in.readObject()).intValue();
        }
    }

    /**
     * The class VSStringPrefsRestriction.
     */
    public static class VSStringPrefsRestriction extends VSPrefsRestriction {
        private static final long serialVersionUID = 2L;

        /** The possible selections. */
        Vector<String> possibleSelections;

        /**
         * Instantiates a new string setting restriction.
         *
         * @param possibleSelections the possible selections
         */
        public VSStringPrefsRestriction(String [] possibleSelections) {
            this.possibleSelections = new Vector<String>();

            for (String elem : possibleSelections)
                this.possibleSelections.add(elem);
        }

        /**
         * Gets the possible selections.
         *
         * @return the possible selections
         */
        public Vector<String> getPossibleSelections() {
            return possibleSelections;
        }

        /* (non-Javadoc)
         * @see prefs.VSPrefsRestriction#writeObject(java.io.ObjectOutputStream)
         */
        public void writeObject(ObjectOutputStream out)
        throws IOException {
            out.writeObject(possibleSelections);
        }

        /* (non-Javadoc)
         * @see prefs.VSPrefsRestriction#readObject(java.io.ObjectInputStream)
         */
        @SuppressWarnings("unchecked")
        public void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
            possibleSelections = (Vector<String>) in.readObject();
        }
    }

    /**
     * Serializes the object.
     *
     * @param out The output stream
     */
    abstract public void writeObject(ObjectOutputStream out)
    throws IOException;

    /**
     * Deserializes the object.
     *
     * @param in The input stream
     */
    abstract public void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException;
}
