package exceptions;

/**
 * The Interface VSEventNotCopyableException, this exception is thrown if
 * the someone tried to copy a not copyable event!
 *
 * @author Paul C. Buetow
 */
public class VSEventNotCopyableException extends Exception {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    public VSEventNotCopyableException(String descr) {
        super(descr);
    }
}
