/**
 * Smooths values over time using averages and weights.
 * Runs in time proportional to n (could be better).
 * 
 * @author Elan Ronen
 */
public class WeightedMASmoother implements Smoother {

    private final int size;         // The number of measurements to store
    private final double[] values;  // Circular array
    private final double[] weights; // The weights
    private int index = -1;         // Index of the most recent item
    private double sum;             // Sum of numbers in the array
    private int items;              // Number of items in the array
    private double average;         // The average

    /**
     * The constructor
     * @param n - the number of values to remember
     * @param weights - the weights for the values (most recent first)
     */
    public WeightedMASmoother(int n, double[] weights) {
        size = n;
        values = new double[size];
        this.weights = new double[size + 1];
        for (int i = 0; i < size; i++) {
            this.weights[i] = i < weights.length ? weights[i] : 1;
        }
    }

    @Override
    public double push(double v) {
        var sumOfWeights = weights[0];
        for (int i = 0; i < items; i++) {
            sum += (weights[i + 1] - weights[i]) * values[(index - i + size) % size];
            sumOfWeights += weights[i + 1];
        }
        sum += weights[0] * v;
        index = (index + 1) % size;
        values[index] = v;

        if (items != size) items++;

        return average = sum / sumOfWeights;
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
