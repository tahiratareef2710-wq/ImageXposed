package displayPackage.core;

/**
 * Abstract base class for all services in the system.
 * Defines a standard lifecycle: initialise → execute → shutdown.
 *
 * Subclasses: DatabaseHandler, DisplayService, NotificationService
 */
public abstract class BaseService {

    protected boolean isInitialised = false;

    /**
     * Performs one-time setup for the service (e.g., open DB connection,
     * load config). Should be called before execute().
     * Sets isInitialised to true upon successful completion.
     */
    public void initialise() {
        if (!isInitialised) {
            onInitialise();
            isInitialised = true;
        }
    }

    /**
     * Hook for subclasses to provide their specific initialisation logic.
     */
    protected abstract void onInitialise();

    /**
     * Performs the service's main action. Can be called repeatedly.
     * Automatically calls initialise() if not yet initialised.
     */
    public void execute() {
        if (!isInitialised) {
            initialise();
        }
        onExecute();
    }

    /**
     * Hook for subclasses to provide their specific execution logic.
     */
    protected abstract void onExecute();

    /**
     * Gracefully shuts down the service (e.g., close connections, flush buffers).
     * Resets isInitialised so the service can be re-initialised if needed.
     */
    public void shutdown() {
        if (isInitialised) {
            onShutdown();
            isInitialised = false;
        }
    }

    /**
     * Hook for subclasses to provide their specific shutdown logic.
     */
    protected abstract void onShutdown();

    /**
     * @return true if the service has been initialised and is ready to use
     */
    public boolean isInitialised() {
        return isInitialised;
    }
}
