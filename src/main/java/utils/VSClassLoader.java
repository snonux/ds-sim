package utils;

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
     */
    public Object newInstance(String classname) {
        Object object = null;

        try {
            object = super.loadClass(classname, true).getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            System.out.println(e + "; Classname " + classname);
        }

        return object;
    }
}
