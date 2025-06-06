package serialize;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The Interface VSSerializable, all classes which take part of the serialize/
 * deserialize proces are implementing this interface. It is preferred over the
 * standard Serializable interface of Java because we want not serialize the
 * whole class tree of each class but certain variables only!
 *
 * @author Paul C. Buetow
 */
public interface VSSerializable {
    /**
     * Serializes
     *
     * @param serialize The serialize object
     * @param objectOutputStream The object output stream
     */
    public void serialize(VSSerialize serialize,
                          ObjectOutputStream objectOutputStream)
    throws IOException;
    /**
     * Deserializes
     *
     * @param serialize The serialize object
     * @param objectInputStream The object input stream
     */
    public void deserialize(VSSerialize serialize,
                            ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException;
}
