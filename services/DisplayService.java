package displayPackage.services;

import displayPackage.core.BaseService;
import java.util.Map;

/**
 * Singleton service that handles navigation and display coordination
 * between views. Acts as the central point for switching screens
 * and showing previews/notifications within the UI.
 *
 * GoF: Singleton — one shared instance across all controllers.
 */
public class DisplayService extends BaseService {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static DisplayService instance;

    public static synchronized DisplayService getInstance() {
        if (instance == null) {
            instance = new DisplayService();
            instance.initialise();
        }
        return instance;
    }

    private DisplayService() {
        super();
    }

    private String currentView = "";

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onInitialise() {
        currentView = "login";
        System.out.println("[DisplayService] Initialised.");
    }

    @Override
    protected void onExecute() {
        // No-op
    }

    @Override
    protected void onShutdown() {
        currentView = "";
        System.out.println("[DisplayService] Shutdown.");
    }

    // ── Display Operations (from class diagram) ────────────────────────────

    /**
     * Switches the main window to the Dashboard view.
     */
    public void displayDashboard() {
        currentView = "dashboard";
        System.out.println("[DisplayService] Navigating to Dashboard.");
    }

    /**
     * Displays the current user profile data.
     *
     * @param data profile data map containing username, email, etc.
     */
    public void displayCurrentProfile(Map<String, Object> data) {
        currentView = "profile";
        System.out.println("[DisplayService] Showing profile: " + data);
    }

    /**
     * Shows a success message after a successful image upload.
     *
     * @param imageId the ID of the uploaded image
     */
    public void displayUploadSuccess(String imageId) {
        System.out.println("[DisplayService] Upload success for image: " + imageId);
    }

    /**
     * Shows a preview of the generated report.
     *
     * @param data the report data to preview
     */
    public void displayReportPreview(Map<String, Object> data) {
        currentView = "report";
        System.out.println("[DisplayService] Report preview: " + data);
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String getCurrentView() {
        return currentView;
    }

    public void setCurrentView(String view) {
        this.currentView = view;
    }
}
