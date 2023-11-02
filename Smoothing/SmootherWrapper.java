import java.util.function.DoubleSupplier;

/**
 * A wrapper around a supplier function and a smoother for convenience.
 * 
 * @author Elan Ronen
 */
public class SmootherWrapper {

    private final DoubleSupplier getter;    // The function
    private final Smoother smoother;        // The smoother

    /**
     * The constructor
     * @param getter - the function to get data from
     * @param smoother - the smoother to feed data to
     */
    public SmootherWrapper(DoubleSupplier getter, Smoother smoother) {
        this.getter = getter;
        this.smoother = smoother;
    }

    /**
     * Get data from the function and push it to the smoother
     * @return the smoother's output
     */
    public double update() {
        return smoother.push(getter.getAsDouble());
    }

    /**
     * Get the most recent smoother's output
     * @return the smoother's output
     */
    public double get() {
        return smoother.get();
    }

}
