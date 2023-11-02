import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.function.DoubleSupplier;

/**
 * Test class for the different smoothers.
 * 
 * @author Elan Ronen
 */
public class SmootherTest {

    /**
     * If 'y' is chosen, automatically copies output to the computer's clipboard.
     * @param args - not used
     */
    public static void main(String[] args) {

        // smoothers to test (comment out a line to remove the smoother)
        final Smoother[] SMOOTHERS = new Smoother[] {
            new MASmoother(9),
            new WeightedMASmoother(9, new double[]{5, 3}),
            new MMSmoother(9),
            new PSmoother(0.2),
            new FallSmoother(0.005, true),
            new FallSmoother(0.005, false),
        };

        // number of doubles to generate
        final int INPUT_SIZE = 300;
        // random generator seed (keep the same number to generate the same doubles)
        final long RANDOM_SEED = System.currentTimeMillis();

        // catch printed data
        final var defaultOut = System.out;
        final var output = new TextCollector();
        System.setOut(new PrintStream(output));

        // generate data input
        final var randData = normalize(new RandomMover(RANDOM_SEED)::next, INPUT_SIZE);
        printPoints(randData);

        // feed the data into the smoothers
        for (var smoother : SMOOTHERS) {
            System.out.println();
            printPoints(feed(randData, smoother));
        }

        // print data to console
        System.setOut(defaultOut);
        // ask if to copy to clipboard
        final var scanner = new Scanner(System.in);
        System.out.print("Copy points to clipboard? [y/n]:");
        final var choice = scanner.nextLine();
        scanner.close();
        if (choice.toLowerCase().strip().equals("y")) {
            // copy to clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(output.get()), null);
            System.out.println("Copied points to clipboard");
        } else {
            // print points
            System.out.println(output.get());
            System.out.println("\nCopy the above points");
        }
        System.out.println("Paste in desmos: https://www.desmos.com/calculator");
        System.out.println("Click the wrench (Graph Settings)");
        System.out.println("Set the X-Axis to -5 < x < " + (INPUT_SIZE + 5));
        System.out.println("Set the Y-Axis to -1.1 < y < 1.1");
        System.out.println("Click the gear (Edit List)");
        System.out.println("Click each colored circle and disable Points and enable Lines");
        System.out.println("Click Done");
        System.out.println("Click the colored circles to show/hide the graphs");
    }

    public static double[] feed(double[] inputs, Smoother smoother) {
        var outputs = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            outputs[i] = smoother.push(inputs[i]);
        }
        return outputs;
    }

    public static void printPoints(double[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            System.out.printf("(%d, %f), ", i, data[i]);
        }
        System.out.printf("(%d, %f)\n", data.length - 1, data[data.length - 1]);
    }

    public static double[] normalize(DoubleSupplier getter, int n) {
        var data = new double[n];
        for (int i = 0; i < n; i++) {
            data[i] = getter.getAsDouble();
        }
        return normalize(data);
    }

    public static double[] normalize(double[] data) {
        var max = Math.abs(data[0]);
        for (var num : data) {
            max = Math.max(max, Math.abs(num));
        }
        var normal = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            normal[i] = data[i] / max;
        }
        return normal;
    }

}

/**
 * Generates doubles that "move" randomly.
 */
class RandomMover {

    private double v;
    private final Random random;

    RandomMover(long seed) {
        random = new Random(seed);
    }

    public double next() {
        return v += random.nextDouble() * 2 - 1;
    }

}

/**
 * Catcher for printed text.
 */
class TextCollector extends OutputStream {

    private final List<String> lines = new ArrayList<>();
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void write(int b) {
        if (b == '\n') {
            lines.add(buffer.toString());
            buffer.setLength(0);
        } else {
            buffer.append((char) b);
        }
    }

    public String get() {
        return String.join("\n", lines);
    }

}
