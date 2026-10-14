package pathfinder.gui.ui;

import javafx.animation.AnimationTimer;
import pathfinder.gui.components.GridCanvas;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gestisce la riproduzione animata di un risultato di ricerca sulla griglia.
 *
 * Mostra i nodi visitati uno per volta nell'ordine in cui l'algoritmo
 * li ha espansi, poi disegna il percorso finale.
 *
 * Modalità:
 *   play()  — avanza automaticamente a velocità fissa
 *   step()  — avanza di un nodo per chiamata
 *   skipToEnd() — mostra tutto immediatamente
 */
public class SearchAnimator {

    private static final long DELAY_NS = 8_000_000L; // ~8ms per nodo

    private final GridCanvas canvas;

    private List<Node>   visitedNodes;
    private List<Node>   path;
    private SearchResult result;
    private int          cursor = 0;
    private boolean      done   = false;

    private AnimationTimer          timer;
    private long                    lastTime = 0;
    private Consumer<SearchResult>  onDone;

    public SearchAnimator(GridCanvas canvas) {
        this.canvas = canvas;
    }

    public void load(SearchResult r, Consumer<SearchResult> onDone) {
        stop();
        this.result       = r;
        this.visitedNodes = r.getVisitedNodes();
        this.path         = r.getPath();
        this.cursor       = 0;
        this.done         = false;
        this.onDone       = onDone;
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
        while (cursor < visitedNodes.size())
            canvas.markVisited(visitedNodes.get(cursor++));
        finish();
    }

    private boolean advance() {
        if (cursor >= visitedNodes.size()) return false;
        canvas.markVisited(visitedNodes.get(cursor++));
        return cursor < visitedNodes.size();
    }

    private void finish() {
        done = true;
        if (path != null && !path.isEmpty()) canvas.showPath(path);
        if (onDone != null) onDone.accept(result);
    }

    public boolean isDone()    { return done; }
    public int     getCursor() { return cursor; }
    public int     getTotal()  { return visitedNodes == null ? 0 : visitedNodes.size(); }
}
