import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Models a bingo board.
 * @author Elan Ronen
 */
class Bingo {

    /** width of the baord */
    private int width;
    /** height of the board */
    private int height;
    /** number of marks in a line to win */
    private int connect;
    /** the board holding the numbers */
    private int[][] board;
    /** the board holding the marked spaces */
    private boolean[][] marks;
    /** whether the board is won */
    private boolean bingo;

    /**
     * Constructor.
     * @param width - width of the board
     * @param height - height of the board
     * @param connect - number of marks in a line to win
     * @param numbers - the maximum number that can appear on the board (0 indexed)
     * @param random - a source of randomness
     */
    public Bingo(int width, int height, int connect, int numbers, Random random) {
        this.width = width;
        this.height = height;
        this.connect = connect;
        // Create a shuffled iterator of the integers 0 to `numbers`
        final var randomNumbersIterator = generateNumbers(numbers, random);
        board = new int[width][];
        marks = new boolean[width][];
        for (int i = 0; i < width; i++) {
            final var boardRow = new int[height];
            board[i] = boardRow;
            for (int j = 0; j < height; j++) {
                boardRow[j] = randomNumbersIterator.next();
            }
            marks[i] = new boolean[height];
        }
    }

    /**
     * Creates a shuffled iterator of the integers 0 to `length`.
     * @param length - the max number (excluded and 0 indexed)
     * @param random - a source of randomness
     * @return an iterator of the shuffled integers
     */
    public static Iterator<Integer> generateNumbers(int length, Random random) {
        // Fill an array with numbers 0 to `length`
        final var array = new Integer[length];
        for (int i = 0; i < length; i++) {
            array[i] = i;
        }
        // Shuffle the array and return an iterator
        final var list = Arrays.asList(array);
        Collections.shuffle(list, random);
        return list.iterator();
    }

    /**
     * Crosses off a space on the board if it exists.
     * Continues to mark spaces once won.
     * @param value - the number to mark
     * @return whether the board is won
     */
    public boolean cross(int value) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (board[i][j] == value) {
                    marks[i][j] = true;
                    // Don't check for a bingo if one was already found
                    return bingo ? bingo : (bingo = hasBingo());
                }
            }
        }
        return bingo;
    }

    /**
     * Checks if the board is won.
     * Implementation is not perfect, but is cool.
     * @return whether the board has a bingo
     */
    public boolean hasBingo() {
        // Scan along top and bottom of the board in the direction:
        for (int x = 0; x < width; x++) {
            if (
                nConsecutive(x, 0, 0, 1) ||      // vertical
                nConsecutive(x, 0, 1, 1) ||      // top-left to bottom-right
                nConsecutive(x, 0, -1, 1) ||           // top-right to bottom-left
                nConsecutive(x, height - 1, 1, -1) ||    // bottom-left to top-right
                nConsecutive(x, height - 1, -1, -1)            // bottom-right to top-left
            ) {
                return true;
            }
        }

        // Scan along left and right of board in the direction:
        for (int y = 0; y < height; y++) {
            if (
                nConsecutive(0, y, 1, 0) ||      // horizontal
                nConsecutive(0, y, 1, 1) ||      // top-left to bottom-right
                nConsecutive(0, y, 1, -1) ||           // bottom-left to top-right
                nConsecutive(width - 1, y, -1, 1) ||     // top-right to bottom-left
                nConsecutive(width - 1, y, -1, -1)             // bottom-right to top-left
            ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Starting form `x` and `y`, changes x by `xStep` and y by `yStep` each until
     * `connect` marks are found in a row or the end of the board is hit.
     * @param x - the starting x position on the board [0, width)
     * @param y - the starting y position on the board [0, height]
     * @param xStep - the change in x
     * @param yStep - the change in y
     * @return whether `connect` marks in a row were found
     */
    private boolean nConsecutive(int x, int y, int xStep, int yStep) {
        var counter = 0;

        while (x >= 0 && x < width && y >= 0 && y < height) {
            // If there is a mark, increase the counter, else set it to zero
            counter = marks[x][y] ? counter + 1 : 0;
            if (counter == connect) return true;
            x += xStep;
            y += yStep;
        }
        return false;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // Apply padding
                sb.append(String.format(" %1$3s", board[i][j]));
                // * indicates a mark
                sb.append(marks[i][j] ? '*' : ' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

}
