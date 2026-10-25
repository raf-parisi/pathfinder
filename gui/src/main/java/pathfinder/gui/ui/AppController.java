package pathfinder.gui.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pathfinder.algorithms.UniformCostSearch;
import pathfinder.gui.components.GridCanvas;
import pathfinder.gui.components.StatsPanel;
import pathfinder.ml.PerlinGridGenerator;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

public class AppController {

    private int rows = 50;
    private int cols = 80;
    private int cell = 15;

    private Grid grid = new Grid(rows, cols);
    private final AlgorithmRunner runner = new AlgorithmRunner();

    private GridCanvas canvas;
    private StatsPanel stats;
    private SearchAnimator animator;
    private Label lblStatus;

    private ComboBox<String> cbAlgorithms;
    private ToggleGroup editGroup;
    private Button btnPlay;
    private Button btnStep;

    private boolean hasRunBefore = false;

    public BorderPane buildUI() {
        canvas = new GridCanvas(rows, cols, cell);
        stats = new StatsPanel();
        animator = new SearchAnimator(canvas);

        grid.setStart(rows * 3 / 4, cols / 4);
        grid.setGoal(rows / 4, cols * 3 / 4);

        canvas.setGrid(grid);
        canvas.setOnChanged(this::onGridChanged);

        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPannable(false);

        lblStatus = new Label("Pronto. Trascina Start/Goal o disegna muri e terreni.");

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(scroll);
        root.setRight(stats);
        root.setBottom(lblStatus);

        BorderPane.setMargin(scroll, new Insets(4));
        BorderPane.setMargin(stats, new Insets(4));
        BorderPane.setMargin(lblStatus, new Insets(2, 6, 2, 6));

        return root;
    }

    // ── toolbar ──────────────────────────────────────────────────────────

    private VBox buildToolbar() {
        VBox bar = new VBox(6);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.getChildren().addAll(
                buildTopControlRow(),
                new Separator(Orientation.HORIZONTAL),
                buildActionRow()
        );
        return bar;
    }

    private HBox buildTopControlRow() {
        TextField tfRows = new TextField(String.valueOf(rows));
        TextField tfCols = new TextField(String.valueOf(cols));
        TextField tfCell = new TextField(String.valueOf(cell));
        tfRows.setPrefWidth(45);
        tfCols.setPrefWidth(45);
        tfCell.setPrefWidth(35);

        Button btnResize = new Button("Ridimensiona");
        btnResize.setOnAction(e -> {
            try {
                int newRows = Integer.parseInt(tfRows.getText().trim());
                int newCols = Integer.parseInt(tfCols.getText().trim());
                int newCell = Integer.parseInt(tfCell.getText().trim());

                if (newRows < 5 || newRows > 250) { lblStatus.setText("Righe: 5-250"); return; }
                if (newCols < 5 || newCols > 350) { lblStatus.setText("Colonne: 5-350"); return; }
                if (newCell < 4 || newCell > 32)  { lblStatus.setText("Cella px: 4-32"); return; }

                rows = newRows; cols = newCols; cell = newCell;
                grid = canvas.resize(rows, cols, cell);
                grid.setStart(rows * 3 / 4, cols / 4);
                grid.setGoal(rows / 4, cols * 3 / 4);
                canvas.setGrid(grid);
                resetAnimUI();
                lblStatus.setText("Griglia " + rows + "x" + cols + ", cella " + cell + "px");
            } catch (NumberFormatException ex) {
                lblStatus.setText("Inserisci numeri validi.");
            }
        });

        editGroup = new ToggleGroup();
        RadioButton rbWall  = rb("Muri",  GridCanvas.EditMode.WALL,  editGroup);
        RadioButton rbRoad  = rb("Strada", GridCanvas.EditMode.ROAD,  editGroup);
        RadioButton rbGrass = rb("Erba",   GridCanvas.EditMode.GRASS, editGroup);
        RadioButton rbWater = rb("Acqua",  GridCanvas.EditMode.WATER, editGroup);
        rbWall.setSelected(true);

        editGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n instanceof RadioButton r)
                canvas.setEditMode((GridCanvas.EditMode) r.getUserData());
        });

        Button btnClear = new Button("Pulisci");
        btnClear.setOnAction(e -> {
            grid.clearAll();
            grid.setStart(rows * 3 / 4, cols / 4);
            grid.setGoal(rows / 4, cols * 3 / 4);
            canvas.setGrid(grid);
            resetAnimUI();
            stats.setStatus("Griglia pulita.");
        });

        // Pulsante per la generazione della griglia casuale
        Button btnRandomGrid = new Button("🎲 Griglia Casuale (Muri Fitti)");
        btnRandomGrid.setOnAction(e -> {
            // Parametri: scala(0.1), acqua(0.3), erba(0.0), densità muri aumentata a 0.35 (35%)
            PerlinGridGenerator generator = new PerlinGridGenerator(0.1, 0.3, 0.0, 0.35);
            long randomSeed = System.currentTimeMillis();

            grid = generator.generate(rows, cols, randomSeed);
            grid.setStart(rows * 3 / 4, cols / 4);
            grid.setGoal(rows / 4, cols * 3 / 4);

            canvas.setGrid(grid);
            resetAnimUI();
            stats.setStatus("Griglia con muri fitti e terreni misti generata.");
        });

        cbAlgorithms = new ComboBox<>();
        cbAlgorithms.getItems().addAll(
                "--- NON INFORMATA ---",
                "BFS", "DFS", "UCS", "IDDFS (Iterative Deepening DFS)", "Bidirectional Search",
                "--- INFORMATA ---",
                "Greedy Best-First", "A* (Manhattan)", "A* (Euclidea)", "IDA*",
                "--- LOCALE ---",
                "Hill Climbing"
        );
        cbAlgorithms.setValue("A* (Manhattan)");
        cbAlgorithms.setPrefWidth(180);

        styleComboBox(cbAlgorithms);

        cbAlgorithms.valueProperty().addListener((obs, o, n) -> {
            if (n != null && !n.startsWith("---")) {
                animator.stop();
                animator = new SearchAnimator(canvas);
                canvas.clearOverlay();
                canvas.redraw();
                btnPlay.setText("▶ Play");
                hasRunBefore = false;
            } else if (n != null && n.startsWith("---")) {
                cbAlgorithms.setValue(o);
            }
        });

        Button btnHelp = new Button("❓ Aiuto");
        btnHelp.setOnAction(e -> showHelpDialog());

        HBox row = new HBox(8,
                new Label("Griglia:"), tfRows, tfCols, new Label("px:"), tfCell, btnResize,
                new Separator(Orientation.VERTICAL),
                rbWall, rbRoad, rbGrass, rbWater, btnClear, btnRandomGrid,
                new Separator(Orientation.VERTICAL),
                new Label("Algo:"), cbAlgorithms,
                new Separator(Orientation.VERTICAL),
                btnHelp
        );
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildActionRow() {
        btnPlay  = new Button("▶ Play");
        btnStep  = new Button("⏭ Passo");
        Button btnSkip  = new Button("⏩ Salta tutto");
        Button btnReset = new Button("🔄 Reset");

        btnPlay.setOnAction(e  -> onPlay());
        btnStep.setOnAction(e  -> onStep());
        btnSkip.setOnAction(e  -> { if (!ensureReady()) return; animator.skipToEnd(); });
        btnReset.setOnAction(e -> { animator.stop(); resetAnimUI(); });

        HBox row = new HBox(10, btnPlay, btnStep, btnSkip, btnReset);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("---")) {
                        setDisable(true);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #555555; -fx-background-color: #e8e8e8;");
                    } else {
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guida - AI Pathfinder");
        alert.setHeaderText("Guida rapida all'interfaccia");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        content.getChildren().addAll(
                createHelpItem("🖱️ Trascina Start / Goal:", "Clicca e trascina il nodo Start (verde 'S') o Goal (rosso 'G') per spostarli."),
                createHelpItem("✏️ Disegno e Terreni:", "Seleziona Muri, Strada, Erba o Acqua in alto e trascina il mouse sulla griglia per dipingere."),
                createHelpItem("🎨 Colori Animazione:", "• Giallo Dorato: Frontiera \n• Arancione chiaro: Nodi esplorati\n• Arancione scuro: Percorso finale")
        );

        alert.getDialogPane().setContent(content);
        alert.setResizable(false);
        alert.showAndWait();
    }

    private VBox createHelpItem(String title, String desc) {
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #222222;");
        Label lblDesc = new Label(desc);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #555555;");
        return new VBox(2, lblTitle, lblDesc);
    }

    // ── azioni ───────────────────────────────────────────────────────────

    private void onPlay() {
        if (animator.isPlaying()) { animator.stop(); btnPlay.setText("▶ Play"); return; }

        if (hasRunBefore && animator.isDone()) {
            resetAnimUI();
        }

        if (animator.getCursor() == 0 && animator.getTotal() == 0) {
            runAlgorithmThenAnimate(true);
        } else {
            animator.play();
            btnPlay.setText("⏸ Pausa");
        }
    }

    private void onStep() {
        if (animator.isPlaying()) { animator.stop(); btnPlay.setText("▶ Play"); }

        if (hasRunBefore && animator.isDone()) {
            resetAnimUI();
        }

        if (animator.getTotal() == 0) {
            runAlgorithmThenAnimate(false);
        } else {
            animator.step();
            lblStatus.setText("Step " + animator.getCursor() + " / " + animator.getTotal());
        }
    }

    private void runAlgorithmThenAnimate(boolean autoPlay) {
        String name = selectedAlgo();
        if (name == null || name.startsWith("---")) { stats.setStatus("Scegli un algoritmo valido."); return; }
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
                    lblStatus.setText("Step totali: " + result.getSteps().size()
                            + (result.isPathFound()
                            ? " | costo: " + String.format("%.1f", result.getTotalCost())
                            : " | nessun percorso"));
                    if (autoPlay) { animator.play(); btnPlay.setText("⏸ Pausa"); }
                    else          { animator.step(); lblStatus.setText("Step 1 / " + animator.getTotal()); }
                },
                err -> {
                    setControlsDisabled(false);
                    stats.setStatus("Errore: " + err.getMessage());
                }
        );
    }

    private void onAnimDone(SearchResult r) {
        btnPlay.setText("▶ Play");
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
        btnPlay.setText("▶ Play");
        lblStatus.setText("Pronto.");
        hasRunBefore = false;
    }

    private void onGridChanged() {
        animator.stop();
        animator = new SearchAnimator(canvas);
        btnPlay.setText("▶ Play");
        hasRunBefore = false;
    }

    private String selectedAlgo() {
        return cbAlgorithms.getValue();
    }

    private boolean ensureReady() {
        if (animator.getTotal() == 0) { runAlgorithmThenAnimate(false); return false; }
        return true;
    }

    private void setControlsDisabled(boolean v) {
        btnPlay.setDisable(v);
        btnStep.setDisable(v);
    }

    private RadioButton rb(String text, Object data, ToggleGroup g) {
        RadioButton r = new RadioButton(text);
        r.setToggleGroup(g);
        r.setUserData(data);
        return r;
    }
}