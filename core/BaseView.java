package displayPackage.core;

import javax.swing.*;
import java.awt.*;

/**
 * Abstract base class for all view panels in the system.
 * Provides a common contract (render, handleInput, show, hide)
 * and shared UI theming constants so every view looks consistent.
 *
 * Note: Your existing views (LoginView, DashboardView, etc.) extend
 * JFrame or compose JPanel directly. Those can remain as-is for now.
 * New views should extend BaseView for consistency going forward.
 */
public abstract class BaseView {

    // ── Shared theme colours ───────────────────────────────────────────────
    protected static final Color BG_DARK        = new Color(26, 26, 46);
    protected static final Color ACCENT          = new Color(233, 69, 96);
    protected static final Color FIELD_BG        = new Color(15, 52, 96);
    protected static final Color CARD_BG         = new Color(22, 33, 62);
    protected static final Color TEXT_PRIMARY     = Color.WHITE;
    protected static final Color TEXT_SECONDARY   = new Color(140, 140, 170);
    protected static final Color TEXT_MUTED       = new Color(100, 100, 130);
    protected static final Color SUCCESS          = new Color(80, 200, 120);
    protected static final Color WARNING          = new Color(255, 130, 60);
    protected static final Color ERROR            = new Color(220, 80, 80);

    protected String title;
    protected boolean isVisible;
    protected JPanel rootPanel;

    protected BaseView(String title) {
        this.title = title;
        this.isVisible = false;
    }

    /**
     * Builds and returns the root panel for this view.
     * Called once during construction; result is cached in rootPanel.
     */
    public abstract void render();

    /**
     * Wires up event listeners and input handling for this view.
     */
    public abstract void handleInput();

    /**
     * Makes this view visible (e.g., switches a CardLayout to show it).
     */
    public void show() {
        if (rootPanel != null) {
            rootPanel.setVisible(true);
            isVisible = true;
        }
    }

    /**
     * Hides this view.
     */
    public void hide() {
        if (rootPanel != null) {
            rootPanel.setVisible(false);
            isVisible = false;
        }
    }

    /**
     * @return the root JPanel representing this view
     */
    public JPanel getPanel() {
        return rootPanel;
    }

    public String getTitle() {
        return title;
    }

    public boolean isVisible() {
        return isVisible;
    }
}
