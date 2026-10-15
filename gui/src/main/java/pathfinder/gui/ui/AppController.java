package pathfinder.gui.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pathfinder.algorithms.UniformCostSearch;
import pathfinder.gui.components.GridCanvas;
import pathfinder.gui.components.StatsPanel;
import pathfinder.ml.HeuristicTrainer;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

public class AppController {

    private int rows = GridCanvas.DEFAULT_ROWS;
    private int cols = GridCanvas.DEFAULT_COLS;
    private int cell = GridCanvas.DEFAULT_CELL;

    private Grid                   grid    = new Grid(rows, cols);
    private final HeuristicTrainer trainer = new HeuristicTrainer();
    private final AlgorithmRunner  runner  = new AlgorithmRunner(trainer);

    private GridCanvas     canvas;
    private StatsPanel     stats;
    private SearchAnimator animator;
    private Label          lblStatus;

    private ToggleGroup algoGroup;
    private ToggleGroup editGroup;
    private Button      btnPlay;
    private Button      btnStep;
    private Button      btnTrain;
    private RadioButton rbOLS;

    private boolean hasRunBefore = false;

    public BorderPane buildUI() {
        canvas   = new GridCanvas(rows, cols, cell);
        stats    = new StatsPanel();
        animator = new SearchAnimator(canvas);

        grid.setStart(rows / 2, 2);
        grid.setGoal(rows / 2, cols - 3);
        canvas.setGrid(grid);
        canvas.setOnChanged(this::onGridChanged);

        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPannable(false);

        lblStatus = new Label("Pronto.");

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(scroll);
        root.setRight(stats);
        root.setBottom(lblStatus);

        BorderPane.setMargin(scroll, new Insets(4));
        BorderPane.setMargin(stats,  new Insets(4));
        BorderPane.setMargin(lblStatus, new Insets(2, 6, 2, 6));

        return root;
    }

    // ── toolbar ──────────────────────────────────────────────────────────

    private VBox buildToolbar() {
        VBox bar = new VBox(4);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.getChildren().addAll(
                buildGridSizeRow(),
                buildEditRow(),
                buildTerrainRow(),
                new Separator(Orientation.HORIZONTAL),
                buildAlgoRow(),
                new Separator(Orientation.HORIZONTAL),
                buildActionRow()
        );
        return bar;
    }

    private HBox buildGridSizeRow() {
        TextField tfRows = new TextField(String.valueOf(rows));
        TextField tfCols = new TextField(String.valueOf(cols));
        TextField tfCell = new TextField(String.valueOf(cell));
        tfRows.setPrefWidth(50);
        tfCols.setPrefWidth(50);
        tfCell.setPrefWidth(40);

        Button btnResize = new Button("Applica");
        btnResize.setOnAction(e -> {
            try {
                int newRows = Integer.parseInt(tfRows.getText().trim());
                int newCols = Integer.parseInt(tfCols.getText().trim());
                int newCell = Integer.parseInt(tfCell.getText().trim());

                if (newRows < 5 || newRows > 200) { lblStatus.setText("Righe: 5-200"); return; }
                if (newCols < 5 || newCols > 300) { lblStatus.setText("Colonne: 5-300"); return; }
                if (newCell < 4 || newCell > 32)  { lblStatus.setText("Cella px: 4-32"); return; }

                rows = newRows; cols = newCols; cell = newCell;
                grid = canvas.resize(rows, cols, cell);
                grid.setStart(rows / 2, 2);
                grid.setGoal(rows / 2, cols - 3);
                canvas.setGrid(grid);
                resetAnimUI();
                lblStatus.setText("Griglia " + rows + "x" + cols + ", cella " + cell + "px");
            } catch (NumberFormatException ex) {
                lblStatus.setText("Inserisci numeri validi.");
            }
        });

        HBox row = new HBox(6,
                new Label("Righe:"), tfRows,
                new Label("Colonne:"), tfCols,
                new Label("Cella px:"), tfCell,
                btnResize);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildEditRow() {
        editGroup = new ToggleGroup();

        RadioButton rbWall  = rb("Muri",  GridCanvas.EditMode.WALL,  editGroup);
        RadioButton rbStart = rb("Start", GridCanvas.EditMode.START, editGroup);
        RadioButton rbGoal  = rb("Goal",  GridCanvas.EditMode.GOAL,  editGroup);
        rbWall.setSelected(true);

        editGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n instanceof RadioButton r)
                canvas.setEditMode((GridCanvas.EditMode) r.getUserData());
        });

        Button btnClear = new Button("Pulisci tutto");
        btnClear.setOnAction(e -> {
            grid.clearAll();
            grid.setStart(rows / 2, 2);
            grid.setGoal(rows / 2, cols - 3);
            canvas.setGrid(grid);
            resetAnimUI();
            stats.setStatus("Griglia pulita.");
        });

        Button btnClearWalls = new Button("Rimuovi muri");
        btnClearWalls.setOnAction(e -> {
            grid.clearWalls();
            canvas.clearOverlay();
            canvas.redraw();
            resetAnimUI();
        });

        HBox row = new HBox(8,
                new Label("Struttura:"), rbWall, rbStart, rbGoal,
                new Separator(Orientation.VERTICAL),
                btnClear, btnClearWalls);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildTerrainRow() {
        RadioButton rbRoad  = rb("Strada (x1)", GridCanvas.EditMode.ROAD,  editGroup);
        RadioButton rbGrass = rb("Erba (x3)",   GridCanvas.EditMode.GRASS, editGroup);
        RadioButton rbWater = rb("Acqua (x8)",  GridCanvas.EditMode.WATER, editGroup);

        Label legend = new Label(
                "  [ bianco=strada  verde=erba  azzurro=acqua  giallo=visitato  arancio=percorso ]");
        legend.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        HBox row = new HBox(8,
                new Label("Terreno:"), rbRoad, rbGrass, rbWater,
                new Separator(Orientation.VERTICAL), legend);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildAlgoRow() {
        algoGroup = new ToggleGroup();
        HBox row = new HBox(8, new Label("Algoritmo:"));
        row.setAlignment(Pos.CENTER_LEFT);

        for (String name : runner.getAlgorithmNames()) {
            RadioButton r = new RadioButton(name);
            r.setToggleGroup(algoGroup);
            r.setUserData(name);
            row.getChildren().add(r);
        }

        rbOLS = new RadioButton(runner.getMLOLSName());
        rbOLS.setToggleGroup(algoGroup);
        rbOLS.setUserData(runner.getMLOLSName());
        rbOLS.setDisable(true);
        row.getChildren().add(rbOLS);

        algoGroup.getToggles().stream()
                .filter(t -> ((RadioButton) t).getText().equals("A* (Manhattan)"))
                .findFirst()
                .ifPresent(t -> t.setSelected(true));

        algoGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (o != null && n != null && o != n) {
                animator.stop();
                animator = new SearchAnimator(canvas);
                canvas.clearOverlay();
                canvas.redraw();
                btnPlay.setText("Play");
                hasRunBefore = false;
            }
        });

        return row;
    }

    private HBox buildActionRow() {
        btnPlay  = new Button("Play");
        btnStep  = new Button("Passo");
        Button btnSkip  = new Button("Salta tutto");
        Button btnReset = new Button("Reset visualizzazione");

        btnPlay.setOnAction(e  -> onPlay());
        btnStep.setOnAction(e  -> onStep());
        btnSkip.setOnAction(e  -> { if (!ensureReady()) return; animator.skipToEnd(); });
        btnReset.setOnAction(e -> { animator.stop(); resetAnimUI(); });

        Label lblGrids = new Label("Griglie training:");
        Spinner<Integer> spGrids = new Spinner<>(5, 2000, 500);
        spGrids.setPrefWidth(80);
        spGrids.setEditable(true);

        Label lblSamples = new Label("Coppie/griglia:");
        Spinner<Integer> spSamples = new Spinner<>(5, 200, 5);
        spSamples.setPrefWidth(70);
        spSamples.setEditable(true);

        btnTrain = new Button("Addestra euristica ML");
        btnTrain.setOnAction(e -> trainML(spGrids.getValue(), spSamples.getValue()));

        HBox row = new HBox(8,
                btnPlay, btnStep, btnSkip, btnReset,
                new Separator(Orientation.VERTICAL),
                lblGrids, spGrids, lblSamples, spSamples, btnTrain);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── azioni ───────────────────────────────────────────────────────────

    private void onPlay() {
        if (animator.isPlaying()) { animator.stop(); btnPlay.setText("Play"); return; }

        if (hasRunBefore && animator.isDone()) {
            resetAnimUI();
        }

        if (animator.getCursor() == 0 && animator.getTotal() == 0) {
            runAlgorithmThenAnimate(true);
        } else {
            animator.play();
            btnPlay.setText("Pausa");
        }
    }

    private void onStep() {
        if (animator.isPlaying()) { animator.stop(); btnPlay.setText("Play"); }

        if (hasRunBefore && animator.isDone()) {
            resetAnimUI();
        }

        if (animator.getTotal() == 0) {
            runAlgorithmThenAnimate(false);
        } else {
            animator.step();
            lblStatus.setText("Nodo " + animator.getCursor() + " / " + animator.getTotal());
        }
    }

    private void runAlgorithmThenAnimate(boolean autoPlay) {
        String name = selectedAlgo();
        if (name == null) { stats.setStatus("Scegli un algoritmo."); return; }
        if (grid.getStartNode() == null || grid.getGoalNode() == null) {
            stats.setStatus("Imposta Start e Goal.");
            return;
        }

        setControlsDisabled(true);
        lblStatus.setText("Esecuzione " + name + "...");
        hasRunBefore = true;

        runner.run(name, grid,
                result -> {
                    animator.load(result, r -> onAnimDone(r));
                    setControlsDisabled(false);
                    lblStatus.setText("Nodi da mostrare: " + result.getVisitedNodes().size()
                            + (result.isPathFound()
                            ? " | costo: " + String.format("%.1f", result.getTotalCost())
                            : " | nessun percorso"));
                    if (autoPlay) { animator.play(); btnPlay.setText("Pausa"); }
                    else          { animator.step(); lblStatus.setText("Nodo 1 / " + animator.getTotal()); }
                },
                err -> {
                    setControlsDisabled(false);
                    stats.setStatus("Errore: " + err.getMessage());
                }
        );
    }

    private void onAnimDone(SearchResult r) {
        btnPlay.setText("Play");
        if (r == null) return;

        if (!r.isPathFound()) {
            stats.showResult(r, null, 0);
            lblStatus.setText("Nessun percorso trovato.");
            return;
        }

        Grid refGrid = copyGridWithStartGoal(grid);
        SearchResult ucsResult = new UniformCostSearch().search(refGrid);
        double optimalCost = ucsResult.isPathFound() ? ucsResult.getTotalCost() : r.getTotalCost();
        double overheadPct = optimalCost > 0
                ? (r.getTotalCost() - optimalCost) / optimalCost * 100
                : 0;

        stats.showResult(r, optimalCost, overheadPct);
        lblStatus.setText("Fine. Costo: " + String.format("%.1f", r.getTotalCost())
                + " | nodi espansi: " + r.getNodesExpanded());
    }

    private Grid copyGridWithStartGoal(Grid original) {
        Grid copy = original.copyWalls();
        Node s = original.getStartNode();
        Node g = original.getGoalNode();
        if (s != null) copy.setStart(s.getRow(), s.getCol());
        if (g != null) copy.setGoal(g.getRow(), g.getCol());
        return copy;
    }

    private void resetAnimUI() {
        animator.stop();
        animator = new SearchAnimator(canvas);
        canvas.clearOverlay();
        canvas.redraw();
        btnPlay.setText("Play");
        lblStatus.setText("Pronto.");
        hasRunBefore = false;
    }

    private void onGridChanged() {
        animator.stop();
        animator = new SearchAnimator(canvas);
        btnPlay.setText("Play");
        hasRunBefore = false;
    }

    // ── ML ───────────────────────────────────────────────────────────────

    private void trainML(int numGrids, int samplesPerGrid) {
        btnTrain.setDisable(true);
        btnTrain.setText("Training...");
        lblStatus.setText("Generazione " + numGrids + " griglie Perlin e training in corso...");

        Grid referenceGrid = grid;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                trainer.train(referenceGrid, numGrids, samplesPerGrid);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            btnTrain.setDisable(false);
            btnTrain.setText("Addestra euristica ML");
            rbOLS.setDisable(false);

            HeuristicTrainer.TrainingStats s = trainer.getLastStats();
            stats.showMLStats(formatMLStats(s));
            lblStatus.setText("Training completato su " + numGrids + " griglie generate.");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnTrain.setDisable(false);
            btnTrain.setText("Addestra euristica ML");
            lblStatus.setText("Training fallito: " + task.getException().getMessage());
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String formatMLStats(HeuristicTrainer.TrainingStats s) {
        StringBuilder sb = new StringBuilder();

        sb.append(s.numGrids()).append(" griglie generate, ")
                .append(s.totalSamples()).append(" esempi estratti\n");
        sb.append("(ogni percorso trovato fornisce piu' esempi,\n");
        sb.append("uno ogni ~20 celle del cammino)\n\n");

        sb.append("MAE predizione costo residuo:\n");
        sb.append("quanto l'euristica sbaglia, in media,\n");
        sb.append("nel prevedere il costo residuo fino al goal.\n");
        sb.append("Piu' basso = euristica piu' precisa.\n\n");

        sb.append("  \"train\" = errore sui dati usati per imparare\n");
        sb.append("  \"test\"  = errore su dati mai visti prima\n");
        sb.append("           (indica quanto generalizza)\n\n");

        sb.append(String.format("  OLS:  train %.2f  |  test %.2f\n",
                s.olsTrainMAE(), s.olsValMAE()));

        return sb.toString();
    }

    // ── utility ──────────────────────────────────────────────────────────

    private String selectedAlgo() {
        Toggle t = algoGroup.getSelectedToggle();
        return t == null ? null : (String) t.getUserData();
    }

    private boolean ensureReady() {
        if (animator.getTotal() == 0) { runAlgorithmThenAnimate(false); return false; }
        return true;
    }

    private void setControlsDisabled(boolean v) {
        btnPlay.setDisable(v);
        btnStep.setDisable(v);
        btnTrain.setDisable(v);
    }

    private RadioButton rb(String text, Object data, ToggleGroup g) {
        RadioButton r = new RadioButton(text);
        r.setToggleGroup(g);
        r.setUserData(data);
        return r;
    }
}