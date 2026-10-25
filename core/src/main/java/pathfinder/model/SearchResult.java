package pathfinder.model;

import java.util.List;

/**
 * Risultato di un'esecuzione di un algoritmo di ricerca.
 * Raccoglie il percorso trovato (se esiste), le statistiche di esecuzione
 * e la sequenza di SearchStep per la visualizzazione/animazione.
 */
public class SearchResult {

    private final String algorithmName;
    private final List<Node> path;
    private final List<SearchStep> steps;
    private final double totalCost;
    private final int nodesExpanded;
    private final int maxNodesInMemory;
    private final double executionTimeMs;

    public SearchResult(
            String algorithmName,
            List<Node> path,
            List<SearchStep> steps,
            double totalCost,
            int nodesExpanded,
            int maxNodesInMemory,
            double executionTimeMs) {
        this.algorithmName = algorithmName;
        this.path = path;
        this.steps = steps;
        this.totalCost = totalCost;
        this.nodesExpanded = nodesExpanded;
        this.maxNodesInMemory = maxNodesInMemory;
        this.executionTimeMs = executionTimeMs;
    }

    public String getAlgorithmName() { return algorithmName; }
    public List<Node> getPath() { return path; }
    public List<SearchStep> getSteps() { return steps; }
    public double getTotalCost() { return totalCost; }
    public int getNodesExpanded() { return nodesExpanded; }
    public int getMaxNodesInMemory() { return maxNodesInMemory; }
    public double getExecutionTimeMs() { return executionTimeMs; }
    public boolean isPathFound() { return path != null && !path.isEmpty(); }

    @Override
    public String toString() {
        return String.format(
                "[%s] path=%s cost=%.1f nodes=%d maxMem=%d steps=%d time=%.2fms",
                algorithmName,
                isPathFound() ? path.size() + " steps" : "NOT FOUND",
                totalCost,
                nodesExpanded,
                maxNodesInMemory,
                steps != null ? steps.size() : 0,
                executionTimeMs
        );
    }
}