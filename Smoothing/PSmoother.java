/**
 * Smooths values over time using proportions.
 * Runs in constant time.
 * 
 * @author Elan Ronen
 */
public class PSmoother implements Smoother {

    private final double P; // The proportion constant
    private double output;  // The previous output

    /**
     * The constructor
     * @param p - the proportion constant (multipied by the error)
     */
    public PSmoother(double p) {
        P = p;
    }

    @Override
    public double push(double v) {
        // Change the output by the error times the proportion constant
        return output += P * (v - output);
    }

    @Override
    public double get() {
        return output;
    }

    @Override
    public void reset() {
        output = 0;
    }

}
