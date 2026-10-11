package pathfinder.model;

import java.util.List;

/**
 * Risultato di un'esecuzione di un algoritmo di ricerca.
 * Raccoglie il percorso trovato (se esiste), le statistiche di esecuzione
 * e i nodi esplorati (per la visualizzazione).
 */
public class SearchResult {

    private final String algorithmName;

    // Percorso ottimale dal nodo start al nodo goal (inclusi). Vuoto se non trovato.
    private final List<Node> path;

    // tutti i nodi visitati nell'ordine di visita
    private final List<Node> visitedNodes;

    // Double.MAX_VALUE se non trovato.
    private final double totalCost;

    private final int nodesExpanded;

    private final long executionTimeMs;

    // true se l'algoritmo garantisce ottimalità per questa istanza
    private final boolean optimal;

    public SearchResult(
            String algorithmName,
            List<Node> path,
            List<Node> visitedNodes,
            double totalCost,
            int nodesExpanded,
            long executionTimeMs,
            boolean optimal) {
        this.algorithmName  = algorithmName;
        this.path           = path;
        this.visitedNodes   = visitedNodes;
        this.totalCost      = totalCost;
        this.nodesExpanded  = nodesExpanded;
        this.executionTimeMs = executionTimeMs;
        this.optimal        = optimal;
    }

    public String       getAlgorithmName()   { return algorithmName;   }
    public List<Node>   getPath()            { return path;            }
    public List<Node>   getVisitedNodes()    { return visitedNodes;    }
    public double       getTotalCost()       { return totalCost;       }
    public int          getNodesExpanded()   { return nodesExpanded;   }
    public long         getExecutionTimeMs() { return executionTimeMs; }
    public boolean      isOptimal()          { return optimal;         }
    public boolean      isPathFound()        { return !path.isEmpty(); }

    @Override
    public String toString() {
        return String.format(
            "[%s] path=%s cost=%.1f nodes=%d time=%dms optimal=%b",
            algorithmName,
            isPathFound() ? path.size() + " steps" : "NOT FOUND",
            totalCost,
            nodesExpanded,
            executionTimeMs,
            optimal
        );
    }
}
