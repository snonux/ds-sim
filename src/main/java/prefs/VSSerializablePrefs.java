package prefs;

//import java.util.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import serialize.VSSerializable;
import serialize.VSSerialize;

/**
 * The class VSSerializablePrefs, this class is used if the VSPrefs should
 * be serializable.
 *
 * @author Paul C. Buetow
 */
public class VSSerializablePrefs extends VSPrefs implements VSSerializable {
    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize_(serialize, objectOutputStream);
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#deserialize(serialize.VSSerialize,
     *	java.io.ObjectInputStream)
     */
    public synchronized void deserialize(VSSerialize serialize,
                                         ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException {
        super.deserialize_(serialize, objectInputStream);
    }
}
