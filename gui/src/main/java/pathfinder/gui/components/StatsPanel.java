package pathfinder.gui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import pathfinder.model.SearchResult;

/**
 * Pannello testuale con le statistiche dell'ultima ricerca.
 */
public class StatsPanel extends VBox {

    private final Label lblAlgo      = new Label("Algoritmo: -");
    private final Label lblCosto     = new Label("Costo percorso: -");
    private final Label lblOttimale  = new Label("Costo ottimale (UCS): -");
    private final Label lblOverhead  = new Label("Overhead vs ottimo: -");
    private final Label lblNodi      = new Label("Nodi espansi: -");
    private final Label lblTempo     = new Label("Tempo: -");
    private final Label lblStatus    = new Label("");
    private final Label lblML        = new Label();

    public StatsPanel() {
        setSpacing(6);
        setPadding(new Insets(10));
        setPrefWidth(240);

        Label title = new Label("Statistiche");
        title.setStyle("-fx-font-weight: bold;");

        Label titleML = new Label("Euristica ML");
        titleML.setStyle("-fx-font-weight: bold;");

        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(220);
        lblML.setWrapText(true);
        lblML.setMaxWidth(220);

        getChildren().addAll(
                title,
                new Separator(),
                lblAlgo, lblCosto, lblOttimale, lblOverhead, lblNodi, lblTempo,
                new Separator(),
                lblStatus,
                new Separator(),
                titleML,
                lblML
        );
    }

    public void setStatus(String s) { lblStatus.setText(s); }

    /**
     * Mostra il risultato di un algoritmo, includendo il confronto con
     * il costo ottimo (UCS) quando disponibile.
     *
     * @param r            risultato dell'algoritmo eseguito
     * @param optimalCost  costo ottimo di riferimento (da UCS), null se
     *                     non calcolato o se r non ha trovato un percorso
     * @param overheadPct  percentuale di overhead rispetto all'ottimo
     *                     (0 se il risultato e' gia' ottimale o se
     *                     optimalCost e' null)
     */
    public void showResult(SearchResult r, Double optimalCost, double overheadPct) {
        lblAlgo.setText("Algoritmo: " + r.getAlgorithmName());
        lblNodi.setText("Nodi espansi: " + r.getNodesExpanded());
        lblTempo.setText("Tempo: " + r.getExecutionTimeMs() + " ms");

        if (!r.isPathFound()) {
            lblCosto.setText("Costo percorso: nessun percorso");
            lblOttimale.setText("Costo ottimale (UCS): -");
            lblOverhead.setText("Overhead vs ottimo: -");
            return;
        }

        lblCosto.setText(String.format("Costo percorso: %.1f", r.getTotalCost()));

        if (optimalCost == null) {
            lblOttimale.setText("Costo ottimale (UCS): -");
            lblOverhead.setText("Overhead vs ottimo: -");
            return;
        }

        lblOttimale.setText(String.format("Costo ottimale (UCS): %.1f", optimalCost));

        boolean isOptimal = Math.abs(r.getTotalCost() - optimalCost) < 1e-6;
        if (isOptimal) {
            lblOverhead.setText("Overhead vs ottimo: 0% (ottimale)");
        } else {
            lblOverhead.setText(String.format("Overhead vs ottimo: +%.1f%%", overheadPct));
        }
    }

    public void showMLStats(String s) { lblML.setText(s); }
    public void clearMLStats()        { lblML.setText(""); }
}