package utils;

/**
 * A generic 3-tuple record that holds three values of potentially different types.
 * This is an immutable value object that provides automatic implementations of
 * {@code equals()}, {@code hashCode()}, and {@code toString()}.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * VS3Tupel<String, Integer, Boolean> tuple = new VS3Tupel<>("Hello", 42, true);
 * String first = tuple.a();
 * Integer second = tuple.b();
 * Boolean third = tuple.c();
 * }</pre>
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param a the first element
 * @param b the second element
 * @param c the third element
 * 
 * @author Paul C. Buetow
 */
public record VS3Tupel<A, B, C>(A a, B b, C c) {
    
    /**
     * Gets the first element (provided for backward compatibility).
     *
     * @return the first element
     * @deprecated Use {@link #a()} instead
     */
    @Deprecated
    public A getA() {
        return a;
    }
    
    /**
     * Gets the second element (provided for backward compatibility).
     *
     * @return the second element
     * @deprecated Use {@link #b()} instead
     */
    @Deprecated
    public B getB() {
        return b;
    }
    
    /**
     * Gets the third element (provided for backward compatibility).
     *
     * @return the third element
     * @deprecated Use {@link #c()} instead
     */
    @Deprecated
    public C getC() {
        return c;
    }
}