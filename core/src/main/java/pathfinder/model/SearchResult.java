package pathfinder.model;

import java.util.List;

/**
 * Risultato di un'esecuzione di un algoritmo di ricerca.
 * Raccoglie il percorso trovato (se esiste), le statistiche di esecuzione
 * e la sequenza di SearchStep (eventi atomici) per l'animazione.
 */
public class SearchResult {

    private final String algorithmName;
    private final List<Node> path;
    private final List<SearchStep> steps;
    private final double totalCost;
    private final int nodesExpanded;
    private final long executionTimeMs;
    private final boolean optimal;

    public SearchResult(
            String algorithmName,
            List<Node> path,
            List<SearchStep> steps,
            double totalCost,
            int nodesExpanded,
            long executionTimeMs,
            boolean optimal) {
        this.algorithmName = algorithmName;
        this.path = path;
        this.steps = steps;
        this.totalCost = totalCost;
        this.nodesExpanded = nodesExpanded;
        this.executionTimeMs = executionTimeMs;
        this.optimal = optimal;
    }

    public String getAlgorithmName() { return algorithmName; }
    public List<Node> getPath() { return path; }
    public List<SearchStep> getSteps() { return steps; }
    public double getTotalCost() { return totalCost; }
    public int getNodesExpanded() { return nodesExpanded; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public boolean isOptimal() { return optimal; }
    public boolean isPathFound() { return !path.isEmpty(); }

    @Override
    public String toString() {
        return String.format(
                "[%s] path=%s cost=%.1f nodes=%d steps=%d time=%dms optimal=%b",
                algorithmName,
                isPathFound() ? path.size() + " steps" : "NOT FOUND",
                totalCost,
                nodesExpanded,
                steps != null ? steps.size() : 0,
                executionTimeMs,
                optimal
        );
    }
}