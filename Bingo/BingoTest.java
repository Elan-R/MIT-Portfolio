import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Little test class to work with the Bingo class.
 * @author Elan Ronen
 */
public class BingoTest {

    /** Number of boards to test */
    static int BOARD_COUNT = 10000;
    /** Width of each board */
    static int WIDTH = 5;
    /** Height of each board */
    static int HEIGHT = WIDTH;
    /** Pool of numbers to pick from */
    static int SET = WIDTH * HEIGHT;
    /** Marks in a line to win each board */
    static int CONNECT = 4;

    public static void main(String[] args) {
        // Create an ArrayList of the state (win or lose) of every board at each pick
        final ArrayList<ArrayList<Boolean>> data = feed((Bingo board, Integer value) -> board.cross(value), BOARD_COUNT, SET, WIDTH, HEIGHT, CONNECT);
        // Find the percent of boards won at each pick
        final var relativeFrequencies = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            final var row = data.get(i);
            relativeFrequencies[i] = (double) Collections.frequency(row, true) / row.size();
        }

        // Display!
        System.out.println(Arrays.toString(relativeFrequencies));
    }

    /**
     * Creates `boardCount` Bingo boards with the parameters given and feeds them to `function`,
     * returning an ArrayList of its results. `function` is also given every integer from 0 to
     * `numberCount` in a random order.
     * @param <Result> - return type of `function`
     * @param function - function that accepts a bingo and an integer
     * @param boardCount - number of boards to create and feed into `function`
     * @param numberCount - maximum number that can appear on the boards (0 indexed)
     * @param boardWidth - width of the boards
     * @param boardHeight - height of the boards
     * @param connect - number of marks in a line to win
     * @return an ArrayList of size `boardCount` of the return type of `function` with its results
     */
    public static <Result> ArrayList<ArrayList<Result>> feed(BiFunction<Bingo, Integer, Result> function, int boardCount, int numberCount, int boardWidth, int boardHeight, int connect) {
        // Create the boards
        final Bingo[] boards = new Bingo[boardCount];
        for (int i = 0; i < boardCount; i++) {
            boards[i] = new Bingo(boardWidth, boardHeight, connect, numberCount, new Random());
        }

        final var numberIterator = Bingo.generateNumbers(numberCount, new Random());
        final var results = new ArrayList<ArrayList<Result>>(numberCount);

        // For every integer to be picked...
        for (int i = 0; i < numberCount; i++) {
            // Pick one
            final var draw = numberIterator.next();
            // Apply `function` to every row with the picked integer and store its results
            final var row = new ArrayList<Result>(boards.length);
            for (int j = 0; j < boards.length; j++) {
                row.add(function.apply(boards[j], draw));
            }
            // Add the results to the final ArrayList
            results.add(row);
        }

        return results;
    }

}
