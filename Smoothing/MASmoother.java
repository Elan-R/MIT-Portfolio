/**
 * Smooths values over time using averages.
 * Runs in constant time.
 *
 * @author Elan Ronen
 */
public class MASmoother implements Smoother {

    private final int size;         // The number of measurements to store
    private final double[] values;  // Circular array
    private int index = -1;         // Index of the most recent item
    private double sum;             // Sum of numbers in the array
    private int items;              // Number of items in the array
    private double average;         // The average

    /**
     * The constructor
     * @param n - the number of values to remember
     */
    public MASmoother(int n) {
        size = n;
        values = new double[size];
    }

    @Override
    public double push(double v) {
        index = (index + 1) % size;
        // Replace the oldest with the newest value in the sum and array
        sum += v - values[index];
        values[index] = v;

        if (items != size) items++;

        return average = sum / items;
    }

    @Override
    public double get() {
        return average;
    }

    @Override
    public void reset() {
        if (index == -1) return;

        for (int i = 0; i < size; i++) {
            values[i] = 0;
        }
        sum = average = items = 0;
        index = -1;
    }

}
