package pathfinder.gui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import pathfinder.model.SearchResult;

public class StatsPanel extends VBox {

    private final Label lblAlgo      = new Label("Algoritmo: -");
    private final Label lblCosto     = new Label("Costo: -");
    private final Label lblOttimale  = new Label("Costo ottimo: -");
    private final Label lblOverhead  = new Label("Overhead: -");
    private final Label lblNodi      = new Label("Nodi espansi: -");
    private final Label lblTempo     = new Label("Tempo: -");

    // Sezione per il confronto con l'esecuzione precedente
    private final Label titlePrev    = new Label("🔄 Esecuzione precedente");
    private final Label lblPrevInfo  = new Label("Nessuna esecuzione precedente.");

    private SearchResult lastResult = null;

    public StatsPanel() {
        setSpacing(8);
        setPadding(new Insets(12));
        setPrefWidth(240);
        setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 0 1;");

        Label title = new Label("📊 Statistiche");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333333;");

        // Stesso font e stile per allineare le due sezioni
        titlePrev.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333333;");
        lblPrevInfo.setWrapText(true);
        lblPrevInfo.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");

        getChildren().addAll(
                title,
                new Separator(),
                lblAlgo,
                lblCosto,
                lblOttimale,
                lblOverhead,
                lblNodi,
                lblTempo,
                new Separator(),
                titlePrev,
                lblPrevInfo
        );
    }

    public void setStatus(String s) {
        lblPrevInfo.setText(s);
    }

    public void showResult(SearchResult r, Double optimalCost, double overheadPct) {
        if (this.lastResult != null && !this.lastResult.getAlgorithmName().equals(r.getAlgorithmName())) {
            formatComparison(this.lastResult, r);
        } else {
            lblPrevInfo.setText("Esegui un altro algoritmo per confrontarlo.");
        }

        this.lastResult = r;

        lblAlgo.setText("Algoritmo: " + r.getAlgorithmName());
        lblNodi.setText("Nodi espansi: " + r.getNodesExpanded());
        lblTempo.setText("Tempo: " + r.getExecutionTimeMs() + " ms");

        if (!r.isPathFound()) {
            lblCosto.setText("Costo: Nessun percorso");
            lblOttimale.setText("Costo ottimo: -");
            lblOverhead.setText("Overhead: -");
            return;
        }

        lblCosto.setText(String.format("Costo: %.1f", r.getTotalCost()));

        if (optimalCost == null) {
            lblOttimale.setText("Costo ottimo: -");
            lblOverhead.setText("Overhead: -");
            return;
        }

        lblOttimale.setText(String.format("Costo ottimo: %.1f", optimalCost));

        boolean isOptimal = Math.abs(r.getTotalCost() - optimalCost) < 1e-6;
        if (isOptimal) {
            lblOverhead.setText("Overhead: 0% (Ottimale)");
        } else {
            lblOverhead.setText(String.format("Overhead: +%.1f%%", overheadPct));
        }
    }

    private void formatComparison(SearchResult prev, SearchResult curr) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Precedente (%s):\n", prev.getAlgorithmName()));
        sb.append(String.format("• Nodi: %d | Costo: %.1f\n\n", prev.getNodesExpanded(), prev.isPathFound() ? prev.getTotalCost() : 0));

        sb.append(String.format("Attuale (%s):\n", curr.getAlgorithmName()));

        int diffNodi = curr.getNodesExpanded() - prev.getNodesExpanded();
        String diffNodiStr = diffNodi > 0 ? "+" + diffNodi : String.valueOf(diffNodi);
        sb.append(String.format("• Δ Nodi: %s nodi\n", diffNodiStr));

        if (curr.isPathFound() && prev.isPathFound()) {
            double diffCosto = curr.getTotalCost() - prev.getTotalCost();
            String diffCostoStr = diffCosto > 0 ? String.format("+%.1f", diffCosto) : String.format("%.1f", diffCosto);
            sb.append(String.format("• Δ Costo: %s", diffCostoStr));
        }

        lblPrevInfo.setText(sb.toString());
    }
}