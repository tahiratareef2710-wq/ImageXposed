package displayPackage.services;

import displayPackage.core.BaseService;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton service that manages user notifications and alerts.
 * Queues notification messages and dispatches them to the UI.
 *
 * GoF: Singleton — one shared instance across all controllers.
 */
public class NotificationService extends BaseService {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static NotificationService instance;

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
            instance.initialise();
        }
        return instance;
    }

    private NotificationService() {
        super();
    }

    private final List<String> notificationQueue = new ArrayList<>();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onInitialise() {
        notificationQueue.clear();
        System.out.println("[NotificationService] Initialised.");
    }

    @Override
    protected void onExecute() {
        // Process and display all queued notifications
        for (String msg : notificationQueue) {
            System.out.println("[NOTIFICATION] " + msg);
        }
        notificationQueue.clear();
    }

    @Override
    protected void onShutdown() {
        notificationQueue.clear();
        System.out.println("[NotificationService] Shutdown.");
    }

    // ── Notification Operations (from class diagram) ───────────────────────

    /**
     * Notifies the user that an image upload was successful.
     *
     * @param imageId the ID of the uploaded image
     */
    public void notifyUploadSuccess(String imageId) {
        String msg = "Image uploaded successfully. ID: " + imageId;
        notificationQueue.add(msg);
        System.out.println("[NotificationService] " + msg);
    }

    /**
     * Notifies the user of a validation result.
     *
     * @param result the validation result description
     */
    public void notifyValidationResult(String result) {
        String msg = "Validation result: " + result;
        notificationQueue.add(msg);
        System.out.println("[NotificationService] " + msg);
    }

    /**
     * Notifies the user that a report export is complete.
     *
     * @param path the file path where the report was exported
     */
    public void notifyExportComplete(String path) {
        String msg = "Report exported to: " + path;
        notificationQueue.add(msg);
        System.out.println("[NotificationService] " + msg);
    }

    /**
     * Adds a custom notification to the queue.
     *
     * @param message the notification message
     */
    public void notify(String message) {
        notificationQueue.add(message);
    }

    /**
     * @return the current notification queue (unmodifiable view)
     */
    public List<String> getNotificationQueue() {
        return new ArrayList<>(notificationQueue);
    }
}
