/**
 * An interface to take data and produce smoothed data in real time.
 * 
 * @author Elan Ronen
 */
public interface Smoother {

    /**
     * Add an input value
     * @param v - the value to add
     * @return the smoothed output value
    */
    public double push(double v);

    /**
     * Get the smoothed output value after the most recent {@link #push(double)}
     * @return the smoothed output
     */
    public double get();

    /**
     * Reset the internal state.
     */
    public void reset();

}
