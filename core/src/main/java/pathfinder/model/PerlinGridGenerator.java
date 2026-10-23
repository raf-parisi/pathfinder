package pathfinder.ml;

import pathfinder.model.Grid;
import pathfinder.model.Node;

import java.util.Random;

/**
 * Generatore di griglie procedurali tramite Perlin noise.
 */
public class PerlinGridGenerator {

    private static final double MIN_REACHABLE_FRACTION = 0.4;

    private final double scale;
    private final double waterThreshold;
    private final double grassThreshold;
    private final double wallDensity;

    public PerlinGridGenerator() {
        this(0.1, 0.3, 0.0, 0.1);
    }

    public PerlinGridGenerator(double scale, double waterThreshold,
                               double grassThreshold, double wallDensity) {
        this.scale          = scale;
        this.waterThreshold = waterThreshold;
        this.grassThreshold = grassThreshold;
        this.wallDensity    = wallDensity;
    }

    public Grid generate(int rows, int cols, long seed) {
        int MAX_ATTEMPTS = 10;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Grid grid = tryGenerate(rows, cols, seed + attempt);
            if (reachableFraction(grid) >= MIN_REACHABLE_FRACTION)
                return grid;
        }
        return generateFallback(rows, cols, seed);
    }

    private Grid tryGenerate(int rows, int cols, long seed) {
        Grid grid = new Grid(rows, cols);

        double[][] terrainNoise = perlin(rows, cols, scale, seed);
        double[][] wallNoise    = perlin(rows, cols, scale * 3, seed + 1);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double tv = terrainNoise[r][c];
                double wv = wallNoise[r][c];

                if (wv > (1.0 - wallDensity * 2)) {
                    grid.get(r, c).setType(Node.Type.WALL);
                    continue;
                }

                if (tv > waterThreshold) {
                    grid.setTerrain(r, c, Node.Terrain.WATER);
                } else if (tv > grassThreshold) {
                    grid.setTerrain(r, c, Node.Terrain.GRASS);
                }
            }
        }

        return grid;
    }

    private Grid generateFallback(int rows, int cols, long seed) {
        Grid grid = new Grid(rows, cols);
        Random rnd = new Random(seed);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (rnd.nextDouble() < 0.3)
                    grid.setTerrain(r, c, Node.Terrain.WATER);
        return grid;
    }

    private double[][] perlin(int rows, int cols, double scale, long seed) {
        Random rnd = new Random(seed);

        int gRows = (int)(rows * scale) + 2;
        int gCols = (int)(cols * scale) + 2;

        double[][] gx = new double[gRows][gCols];
        double[][] gy = new double[gRows][gCols];

        for (int r = 0; r < gRows; r++) {
            for (int c = 0; c < gCols; c++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                gx[r][c] = Math.cos(angle);
                gy[r][c] = Math.sin(angle);
            }
        }

        double[][] noise = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double xf = c * scale;
                double yf = r * scale;

                int x0 = (int) xf;
                int y0 = (int) yf;
                int x1 = x0 + 1;
                int y1 = y0 + 1;

                double dx = xf - x0;
                double dy = yf - y0;

                double n00 = dot(gx[y0][x0], gy[y0][x0], dx,     dy);
                double n10 = dot(gx[y0][x1], gy[y0][x1], dx - 1, dy);
                double n01 = dot(gx[y1][x0], gy[y1][x0], dx,     dy - 1);
                double n11 = dot(gx[y1][x1], gy[y1][x1], dx - 1, dy - 1);

                double sx = smoothstep(dx);
                double sy = smoothstep(dy);

                double nx0 = lerp(n00, n10, sx);
                double nx1 = lerp(n01, n11, sx);
                noise[r][c] = lerp(nx0, nx1, sy);
            }
        }
        return noise;
    }

    private double dot(double gx, double gy, double dx, double dy) {
        return gx * dx + gy * dy;
    }

    private double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private double reachableFraction(Grid grid) {
        int rows = grid.getRows(), cols = grid.getCols();
        int startR = rows / 2, startC = cols / 2;

        Node start = null;
        outer:
        for (int dr = 0; dr < rows / 2; dr++) {
            for (int dc = 0; dc < cols / 2; dc++) {
                for (int[] d : new int[][]{{dr,dc},{dr,-dc},{-dr,dc},{-dr,-dc}}) {
                    int r = startR + d[0], c = startC + d[1];
                    if (r >= 0 && r < rows && c >= 0 && c < cols
                            && !grid.get(r, c).isWall()) {
                        start = grid.get(r, c);
                        break outer;
                    }
                }
            }
        }
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

    public static String toAscii(Grid grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getCols(); c++) {
                Node n = grid.get(r, c);
                if (n.isWall()) sb.append('#');
                else sb.append(switch (n.getTerrain()) {
                    case ROAD  -> '.';
                    case GRASS -> 'G';
                    case WATER -> 'W';
                });
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}