import java.util.ArrayList;
import java.util.List;

/**
 * Smooths values over time using medians.
 * Runs in time proportional to n.
 * 
 * @author Elan Ronen
 */
public class MMSmoother implements Smoother {

    /**
     * A container class to link values across data structures.
     */
    private class Value {

        double value;

    }

    private final int size;         // The number of measurements to store
    private final Value[] values;   // Circular array
    private int index = -1;         // Index of the most recent item
    private int items;              // Number of items in the array
    private List<Value> sorted;     // List of sorted values
    private double median;          // The median

    /**
     * The constructor
     * @param n - the number of values to remember
     */
    public MMSmoother(int n) {
        size = n;
        values = new Value[size];
        for (int i = 0; i < size; i++) {
            values[i] = new Value();
        }
        sorted = new ArrayList<>(size);
    }

    @Override
    public double push(double v) {
        index = (index + 1) % size;
        // Get the Value object with the oldest value
        final var value = values[index];
        value.value = v;
        if (items != size) {
            sorted.add(value);
            items++;
        }

        sorted.sort((Value o1, Value o2) -> (int) Math.signum(o1.value - o2.value));
        return median = sorted.get(sorted.size() / 2).value;
    }

    @Override
    public double get() {
        return median;
    }

    @Override
    public void reset() {
        if (index == -1) return;

        index = -1;
        median = items = 0;
        sorted.clear();
    }

}
