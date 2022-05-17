package src;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.ArrayList;

import states.AutoState;
import states.GuidedState;
import states.ManualState;

import static src.KnightsTourGUI.*;

public class KnightsTour {
    // Set constant attributes
    public static final int BOARD_SIZE = 8;
    public static final int KNIGHT_MOVES = 8;

    // Holds a reference to the GUI frame or window
    public static KnightsTourGUI app;

    // Stores the knight's movements relative to its origin
    private static int moveOffsets[][] = {
        {1, -2}, {2, -1}, {2, 1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {-1, -2}
    };

    // Move orders for a chessboard of size m congruent to 0 mod 8
    private static int moveOrders[][] = {
        {3, 4, 2, 6, 1, 5, 7, 8},
        {8, 7, 6, 4, 2, 1, 3, 5},
        {5, 1, 8, 6, 7, 3, 4, 2},
        {5, 1, 3, 4, 2, 6, 7, 8},
        {2, 1, 4, 3, 5, 6, 7, 8}
    };

    // Keeps track of the current move order
    public static int moveSet = 0;

    // Keeps track of the knight's current position on the chessboard
    public static Cell currentPos;

    // Stores the knight's valid moves
    public static List<Cell> neighborCells = new ArrayList<Cell>();

    // Keeps track of visited cells
    public static List<Cell> visitedCells = new ArrayList<Cell>();

    // Stores the number of neighbors of the knight's valid moves
    public static int[] futureNeighbors = new int[KNIGHT_MOVES];

    // Define program states
    static ManualState manual = new ManualState();
    static AutoState auto = new AutoState();
    static GuidedState guided = new GuidedState();

    // A StateMachine class that handles the program's state transitions and behavior
    static StateMachine sm = new StateMachine();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            app = new KnightsTourGUI();
        });
    }

    // Loop through all of the cells in the chessboard and attempt to find a tour in each
    static void evaluateAlgorithm() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Cell btn = app.cellArray[col][row];
                System.out.println("Evaluating btn: " + btn.locate());
                btn.doClick();

                try {
                    if (auto.tour != null)
                        auto.tour.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sm.change(auto);
            }
        }
    }

    /**
     * Populate the chessboard with cells/squares
     * 
     * @param frame a reference to the instance variable JFrame
     */
    static void generateButtons(KnightsTourGUI frame) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                // Instantiates a new cell and stores its location via constructor
                Cell btn = new Cell(String.valueOf(col) + String.valueOf(row));

                // Adds a mouseListener that will trigger if the user clicks on any of the cells
                btn.addActionListener(e -> sm.processBtnEvent((Cell) e.getSource()));

                // Decorates the button accordingly
                designBtn(btn);

                // Store a reference of the cell and add it as component
                frame.cellArray[col][row] = btn;
                frame.chessBoard.add(btn);
            }
        }
    }

    /**
     * Moves the knight to the cell/button provided as argument
     * 
     * @param btn the cell to be occupied by the knight
     */
    public static void moveKnight(Cell btn) {
        // Update the gui and mark cell/button as visited
        designCurrentCell(btn);
        btn.setVisited(true);
        visitedCells.add(btn);
        
        // Keep track of the knight's new position
        currentPos = btn;
    }

    /**
     * Resets every variable used for finding tours
     */
    public static void resetAll() {
        currentPos = null;
        resetNeighbors();
        resetVisited();
    }

    /**
     * Resets the chessboard back to its original empty state and lets go of visited cells
     */
    private static void resetVisited() {
        for (int i = 0; i < visitedCells.size(); i++) {
            Cell btn = visitedCells.get(i);
            designBtn(btn);
            btn.setVisited(false);
        }

        visitedCells.clear();
    }

    /**
     * Resets the decorative states of neighbor cells
     */
    public static void resetNeighbors() {
        for (int i = 0; i < neighborCells.size(); i++) {
            Cell button = neighborCells.get(i);
            
            if (button != null)
                designBtn(button);
        }

        neighborCells.clear();
    }

    /**
     * Updates the move order based on the position of the knight on the chessboard
     * 
     * @param btn the current position of the knight
     */
    public static void updateMoveOrder(Cell btn) {
        String pos = btn.locate();

        if (pos.equals("76") && moveSet == 0)
            moveSet = 1;
        else if (pos.equals("22") && moveSet == 1)
            moveSet = 2;
        else if (pos.equals("01") && moveSet == 2)
            moveSet = 3;
        else if (pos.equals("75") && moveSet == 3)
            moveSet = 4;
    }

    /**
     * Finds valid moves/neighbors for the knight
     * 
     * @param btn cell where the knight is currently located
     */
    public static void findNeighbors(Cell btn) {
        // Get the location of the current cell
        String pos = btn.locate();

        // Checks for valid neighbors
        for (int i = 0; i < KNIGHT_MOVES; i++) {
            // Get the corresponding row and column of current cell
            String parts[] = pos.split("");

            // Calculates the neighbor's presumed position on the board
            int tempCol = Integer.parseInt(parts[0]) + moveOffsets[i][0];
            int tempRow = Integer.parseInt(parts[1]) + moveOffsets[i][1];

            // Verifies the neighbor's position by checking if it's on the board
            if (isOnBoard(tempCol, tempRow)) {
                Cell button = app.cellArray[tempCol][tempRow];

                // If button is not visited, add it to the list of neighbors and set its color
                if (!button.isVisited()) {
                    neighborCells.add(button);

                    // Attempts to recursively find future neighbors on the current valid neighbor
                    sm.findFutureNeighs(button, i);
                    
                    designNeighbor(button);
                    continue;
                }
            }

            // Mark the cell in the list of neighbors as null if it is not a valid neighbor
            neighborCells.add(null);
        }
    }

    /**
     * Checks to see if the cell denoted by <code>tempCol</code> and <code>tempRow</code> is on the board
     * and does not exceed outside its boundaries
     * 
     * @param tempCol the column where the cell is located
     * @param tempRow the row where the cell is located
     * @return whether or not the cell is on the chessboard
     */
    private static boolean isOnBoard(int tempCol, int tempRow) {
        return tempRow >= 0 && tempRow < BOARD_SIZE && tempCol >= 0 && tempCol < BOARD_SIZE;
    }

    /**
     * Counts the number of neighbors of a knight's valid neighbor cell
     * 
     * @param btn a valid neighbor cell
     * @param index the index of the neighbor cell/button based on move order
     */
    public static void cntFutureNeighs(Cell btn, int index) {
        // Get the position/location of the cell and initialize neighbor count
        String pos = btn.locate();
        int neighborCnt = 0;

        // Checks for valid neighbors
        for (int i = 0; i < KNIGHT_MOVES; i++) {
            String parts[] = pos.split("");

            int tempCol = Integer.parseInt(parts[0]) + moveOffsets[i][0];
            int tempRow = Integer.parseInt(parts[1]) + moveOffsets[i][1];

            if (isOnBoard(tempCol, tempRow)) {
                Cell button = app.cellArray[tempCol][tempRow];

                // If button is not visited, count it as one valid neighbor cell
                if (!button.isVisited())
                    neighborCnt++;
            }
        }

        // Stores the number of neighbors of the current cell
        futureNeighbors[index] = neighborCnt;
    }

    /**
     * Selects the cell to be marked as the knight's destination on tour progressions
     * 
     * @return the designated cell
     */
    public static Cell selectCell() {
        // Stores the indexes which correspond to the cells in the neighborCells array list with the least
        // neighbor count
        List<Integer> btnIndexes = new ArrayList<Integer>();

        // Stores the least neighbor count among the neighbors
        int leastNeighborCnt = KNIGHT_MOVES;

        // Finds neighbor cells with the least neighbor count
        for (int i = 0; i < KNIGHT_MOVES; i++) {
            // Skip iteration if cell is null
            if (neighborCells.get(i) == null)
                continue;

            // Get neighbor count of the current neighbor cell
            int neighborCnt = futureNeighbors[i];
            
            // If the neighbor count is less than the least neighbor count; update the least neighbor count,
            // reset the stored indexes, and store the new index
            if (neighborCnt < leastNeighborCnt) {
                leastNeighborCnt = neighborCnt;
                btnIndexes.clear();
                btnIndexes.add(i);
                continue;
            }

            // If neighbor count is equal to least neighbour count, add it to the list of indexes
            if (neighborCnt == leastNeighborCnt)
                btnIndexes.add(i);
        }

        // Reset neighbor counts
        futureNeighbors = new int[KNIGHT_MOVES];
        
        // Return the corresponding cell of the index stored in btnIndexes if its length is one (it means
        // there are no ties)
        if (btnIndexes.size() == 1)
            return neighborCells.get(btnIndexes.get(0));
        
        // Otherwise, handle the tie using Sam Ganzfried's algorithm
        return handleTies(btnIndexes);
    }

    /**
     * Handles the ties between neighbor cells with the least neighbor count if there are any
     * 
     * @param btnIndexes the cells/buttons that are tied
     * @return the designated cell of the knight in tour progressions
     */
    private static Cell handleTies(List<Integer> btnIndexes) {
        int index = 0;

        // Iterate through the move order
        for (int i = 0; i < KNIGHT_MOVES; i++) {
            index = moveOrders[moveSet][i] - 1;

            // If the current move/index is found in the list of valid indexes, break out of the loop
            if (btnIndexes.contains(index))
                break;
        }

        // Return the corresponding neighbor cell of the final index/move
        return neighborCells.get(index);
    }
}
