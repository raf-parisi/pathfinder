package pathfinder.gui.ui;

import javafx.animation.AnimationTimer;
import pathfinder.gui.components.GridCanvas;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;
import pathfinder.model.NodeState;

import java.util.List;
import java.util.function.Consumer;

public class SearchAnimator {

    private static final long DELAY_NS = 8_000_000L; // ~8ms per step

    private final GridCanvas canvas;

    private List<SearchStep> steps;
    private List<Node>       path;
    private SearchResult     result;
    private Node             startNode;
    private int              startClosedCount = 0;
    private int              cursor = 0;
    private boolean          done   = false;

    private AnimationTimer         timer;
    private long                   lastTime = 0;
    private Consumer<SearchResult> onDone;

    public SearchAnimator(GridCanvas canvas) {
        this.canvas = canvas;
    }

    public void load(SearchResult r, Consumer<SearchResult> onDone) {
        stop();
        this.result = r;
        this.steps  = r.getSteps();
        this.path   = r.getPath();
        this.cursor = 0;
        this.done   = false;
        this.onDone = onDone;

        this.startNode        = (steps != null && !steps.isEmpty()) ? steps.get(0).node() : null;
        this.startClosedCount = 0;

        canvas.clearOverlay();
        canvas.redraw();
    }

    public void play() {
        if (done || timer != null) return;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now - lastTime < DELAY_NS) return;
                lastTime = now;
                if (!advance()) { stop(); finish(); }
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; }
    }

    public boolean isPlaying() { return timer != null; }

    public boolean step() {
        if (done) return false;
        stop();
        boolean more = advance();
        if (!more) finish();
        return more;
    }

    public void skipToEnd() {
        stop();
        if (steps != null) {
            while (cursor < steps.size()) {
                SearchStep step = steps.get(cursor++);
                if (startNode != null && step.node().equals(startNode) && step.state() == NodeState.CLOSED) {
                    startClosedCount++;
                    if (startClosedCount > 1) {
                        canvas.clearOverlay();
                        canvas.redraw();
                    }
                }
                canvas.applyStep(step);
            }
        }
        finish();
    }

    private boolean advance() {
        if (steps == null || cursor >= steps.size()) return false;

        SearchStep step = steps.get(cursor++);

        // Ogni volta che l'IDDFS inizia una nuova iterazione, chiama dls sullo start
        // aggiungendo un nuovo step (start, CLOSED). Dalla seconda volta in poi, azzeriamo la grafica!
        if (startNode != null && step.node().equals(startNode) && step.state() == NodeState.CLOSED) {
            startClosedCount++;
            if (startClosedCount > 1) {
                canvas.clearOverlay();
                canvas.redraw();
            }
        }

        canvas.applyStep(step);
        return cursor < steps.size();
    }

    private void finish() {
        done = true;
        if (path != null && !path.isEmpty()) canvas.showPath(path);
        if (onDone != null) onDone.accept(result);
    }

    public boolean isDone()    { return done; }
    public int     getCursor() { return cursor; }
    public int     getTotal()  { return steps == null ? 0 : steps.size(); }
}