package events.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import events.VSAbstractEvent;
import serialize.VSSerialize;

/**
 * The class VSAbstractInternalEvent, this class if for destinguishing between
 * internal and non-internal events. Internal usage only.
 *
 * @author Paul C. Buetow
 */
abstract public class VSAbstractInternalEvent extends VSAbstractEvent {
    /* (non-Javadoc)
     * @see events.VSAbstractEvent#createShortname()()
     */
    protected String createShortname(String savedShortname) {
        return savedShortname;
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize(serialize, objectOutputStream);

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#deserialize(serialize.VSSerialize,
     *	java.io.ObjectInputStream)
     */
    public synchronized void deserialize(VSSerialize serialize,
                                         ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException {
        super.deserialize(serialize, objectInputStream);

        if (VSSerialize.DEBUG)
            System.out.println("Deserializing: VSAbstractInternalEvent");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
