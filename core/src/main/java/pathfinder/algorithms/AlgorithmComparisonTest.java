package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Eseguibile con:
 *   mvn -pl core exec:java -Dexec.mainClass="pathfinder.algorithms.AlgorithmComparisonTest"
 */
public class AlgorithmComparisonTest {

    private static final int ROWS = 60;
    private static final int COLS = 80;
    private static final int NUM_GRIDS = 100;
    private static final int PAIRS_PER_GRID = 5;

    private static final double[] WALL_DENSITIES = {0.0, 0.10, 0.20, 0.30};
    private static final double MIN_REACHABLE_FRACTION = 0.4;

    private static final long SEED = 2026L;

    public static void main(String[] args) {
        System.out.println("=== ALGORITHM COMPARISON TEST (costo uniforme + muri) ===\n");

        for (double density : WALL_DENSITIES) {
            System.out.printf("%n########## Densità muri: %.0f%% ##########%n%n", density * 100);
            runBenchmark(density);
        }
    }

    private static void runBenchmark(double wallDensity) {
        Random seedRnd = new Random(SEED + (long)(wallDensity * 1000));

        long expBFS = 0, expDFS = 0, expUCS = 0, expGreedy = 0, expAStar = 0, expHill = 0;
        int optBFS = 0, optDFS = 0, optGreedy = 0, optAStar = 0, optHill = 0;
        int noPathDFS = 0, noPathGreedy = 0, noPathHill = 0;

        double sumOvhBFS = 0; int cntSubOptBFS = 0;
        double sumOvhDFS = 0; int cntSubOptDFS = 0;
        double sumOvhGreedy = 0; int cntSubOptGreedy = 0;
        double sumOvhHill = 0; int cntSubOptHill = 0;

        long timeBFS = 0, timeDFS = 0, timeUCS = 0, timeGreedy = 0, timeAStar = 0, timeHill = 0;

        int totalPairs = 0;

        for (int g = 0; g < NUM_GRIDS; g++) {
            Grid grid = generateWallGrid(ROWS, COLS, wallDensity, seedRnd.nextLong());

            List<int[]> free = new ArrayList<>();
            for (int r = 0; r < ROWS; r++)
                for (int c = 0; c < COLS; c++)
                    if (!grid.get(r, c).isWall()) free.add(new int[]{r, c});
            if (free.size() < 2) continue;

            for (int p = 0; p < PAIRS_PER_GRID; p++) {
                int si = seedRnd.nextInt(free.size());
                int gi = seedRnd.nextInt(free.size());
                if (si == gi) continue;

                grid.setStart(free.get(si)[0], free.get(si)[1]);
                grid.setGoal(free.get(gi)[0], free.get(gi)[1]);

                Grid gBfs    = copyGridWithStartGoal(grid);
                Grid gDfs    = copyGridWithStartGoal(grid);
                Grid gUcs    = copyGridWithStartGoal(grid);
                Grid gGreedy = copyGridWithStartGoal(grid);
                Grid gAStar  = copyGridWithStartGoal(grid);
                Grid gHill   = copyGridWithStartGoal(grid);

                SearchResult rUcs = new UniformCostSearch().search(gUcs);
                if (!rUcs.isPathFound()) continue; // coppia non raggiungibile, skip

                SearchResult rBfs    = new BFS().search(gBfs);
                SearchResult rDfs    = new DFS().search(gDfs);
                SearchResult rGreedy = new GreedyBestFirst().search(gGreedy);
                SearchResult rAStar  = new AStar().search(gAStar);
                SearchResult rHill   = new HillClimbing().search(gHill);

                double optimalCost = rUcs.getTotalCost();

                boolean bfsOpt    = rBfs.isPathFound()    && Math.abs(rBfs.getTotalCost()    - optimalCost) < 1e-6;
                boolean dfsOpt    = rDfs.isPathFound()    && Math.abs(rDfs.getTotalCost()    - optimalCost) < 1e-6;
                boolean greedyOpt = rGreedy.isPathFound() && Math.abs(rGreedy.getTotalCost() - optimalCost) < 1e-6;
                boolean astarOpt  = rAStar.isPathFound()  && Math.abs(rAStar.getTotalCost()  - optimalCost) < 1e-6;
                boolean hillOpt   = rHill.isPathFound()   && Math.abs(rHill.getTotalCost()   - optimalCost) < 1e-6;

                if (rBfs.isPathFound() && !bfsOpt) {
                    sumOvhBFS += (rBfs.getTotalCost() - optimalCost) / optimalCost * 100;
                    cntSubOptBFS++;
                }

                if (!rDfs.isPathFound()) noPathDFS++;
                else if (!dfsOpt) { sumOvhDFS += (rDfs.getTotalCost() - optimalCost) / optimalCost * 100; cntSubOptDFS++; }

                if (!rGreedy.isPathFound()) noPathGreedy++;
                else if (!greedyOpt) { sumOvhGreedy += (rGreedy.getTotalCost() - optimalCost) / optimalCost * 100; cntSubOptGreedy++; }

                if (!rHill.isPathFound()) noPathHill++;
                else if (!hillOpt) { sumOvhHill += (rHill.getTotalCost() - optimalCost) / optimalCost * 100; cntSubOptHill++; }

                totalPairs++;
                expBFS    += rBfs.getNodesExpanded();
                expDFS    += rDfs.getNodesExpanded();
                expUCS    += rUcs.getNodesExpanded();
                expGreedy += rGreedy.getNodesExpanded();
                expAStar  += rAStar.getNodesExpanded();
                expHill   += rHill.getNodesExpanded();

                timeBFS    += rBfs.getExecutionTimeMs();
                timeDFS    += rDfs.getExecutionTimeMs();
                timeUCS    += rUcs.getExecutionTimeMs();
                timeGreedy += rGreedy.getExecutionTimeMs();
                timeAStar  += rAStar.getExecutionTimeMs();
                timeHill   += rHill.getExecutionTimeMs();

                if (bfsOpt)    optBFS++;
                if (dfsOpt)    optDFS++;
                if (greedyOpt) optGreedy++;
                if (astarOpt)  optAStar++;
                if (hillOpt)   optHill++;
            }
        }

        if (totalPairs == 0) {
            System.out.println("Nessuna coppia valida generata a questa densità.");
            return;
        }

        System.out.printf("Coppie testate: %d%n%n", totalPairs);
        System.out.printf("%-12s %10s %8s %10s %10s %12s%n",
                "Algoritmo", "Nodi med.", "Tempo", "Ottimale", "%Ottim.", "Overhead med.");
        System.out.println("-".repeat(70));

        printRow("UCS",       expUCS,    timeUCS,    totalPairs, totalPairs, totalPairs, 0, 0);
        printRow("BFS",       expBFS,    timeBFS,    totalPairs, optBFS,     totalPairs, sumOvhBFS, cntSubOptBFS);
        printRow("DFS",       expDFS,    timeDFS,    totalPairs, optDFS,     totalPairs, sumOvhDFS, cntSubOptDFS);
        printRow("Greedy BF", expGreedy, timeGreedy, totalPairs, optGreedy,  totalPairs, sumOvhGreedy, cntSubOptGreedy);
        printRow("A* Manh.",  expAStar,  timeAStar,  totalPairs, optAStar,   totalPairs, 0, 0);
        printRow("Hill Climb",expHill,   timeHill,   totalPairs, optHill,    totalPairs, sumOvhHill, cntSubOptHill);

        System.out.println("\nTasso di fallimento completo (nessun percorso trovato):");
        System.out.printf("  DFS:        %d/%d (%.1f%%)%n", noPathDFS, totalPairs, 100.0 * noPathDFS / totalPairs);
        System.out.printf("  Greedy BF:  %d/%d (%.1f%%)%n", noPathGreedy, totalPairs, 100.0 * noPathGreedy / totalPairs);
        System.out.printf("  Hill Climb: %d/%d (%.1f%%)%s%n", noPathHill, totalPairs, 100.0 * noPathHill / totalPairs,
                wallDensity == 0.0 ? "  (atteso 0% senza muri: niente minimi locali)" : "");
    }

    private static void printRow(String name, long expanded, long timeMs, int total,
                                 int optimal, int optimalDenom, double sumOvh, int cntSubOpt) {
        double avgExp = (double) expanded / total;
        double avgTime = (double) timeMs / total;
        double optPct = 100.0 * optimal / optimalDenom;
        String ovhStr = cntSubOpt > 0 ? String.format("%.1f%%", sumOvh / cntSubOpt) : "-";
        System.out.printf("%-12s %10.1f %7.2fms %5d/%-4d %9.1f%% %12s%n",
                name, avgExp, avgTime, optimal, optimalDenom, optPct, ovhStr);
    }

    private static Grid generateWallGrid(int rows, int cols, double wallDensity, long seed) {
        int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Grid grid = new Grid(rows, cols);
            Random rnd = new Random(seed + attempt);
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    if (rnd.nextDouble() < wallDensity)
                        grid.get(r, c).setType(Node.Type.WALL);

            if (reachableFraction(grid) >= MIN_REACHABLE_FRACTION) return grid;
        }
        return new Grid(rows, cols);
    }

    private static double reachableFraction(Grid grid) {
        int rows = grid.getRows(), cols = grid.getCols();
        Node start = null;
        outer:
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!grid.get(r, c).isWall()) { start = grid.get(r, c); break outer; }
        if (start == null) return 0;

        boolean[][] visited = new boolean[rows][cols];
        java.util.Queue<Node> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        visited[start.getRow()][start.getCol()] = true;
        int reached = 0, total = 0;

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!grid.get(r, c).isWall()) total++;

        while (!queue.isEmpty()) {
            Node n = queue.poll();
            reached++;
            for (Node nb : grid.getNeighbors(n)) {
                if (!visited[nb.getRow()][nb.getCol()]) {
                    visited[nb.getRow()][nb.getCol()] = true;
                    queue.add(nb);
                }
            }
        }
        return total == 0 ? 0 : (double) reached / total;
    }

    private static Grid copyGridWithStartGoal(Grid original) {
        Grid copy = original.copyWalls();
        Node s = original.getStartNode();
        Node g = original.getGoalNode();
        if (s != null) copy.setStart(s.getRow(), s.getCol());
        if (g != null) copy.setGoal(g.getRow(), g.getCol());
        return copy;
    }
}