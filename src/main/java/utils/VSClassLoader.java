package utils;

import exceptions.VSErrorHandler;
import exceptions.VSSimulatorRuntimeException;

/**
 * The class VSClassLoader. This class is used in order to create new objects
 * by its classnames.
 *
 * @author Paul C. Buetow
 */
public class VSClassLoader extends ClassLoader {
    /**
     * Creates a new instance of the given classname.
     *
     * @param classname the classname
     *
     * @return the object
     * @throws VSSimulatorRuntimeException if the class cannot be loaded or instantiated
     */
    public Object newInstance(String classname) {
        if (classname == null || classname.trim().isEmpty()) {
            VSErrorHandler.warning("Attempted to load null or empty classname");
            return null;
        }

        try {
            return super.loadClass(classname, true).getDeclaredConstructor().newInstance();

        } catch (ClassNotFoundException e) {
            VSErrorHandler.handle("Class not found: " + classname, e, false);
            return null;
        } catch (NoSuchMethodException e) {
            VSErrorHandler.handle("No default constructor for class: " + classname, e, false);
            return null;
        } catch (Exception e) {
            VSErrorHandler.handle("Failed to instantiate class: " + classname, e, false);
            return null;
        }
    }
}
