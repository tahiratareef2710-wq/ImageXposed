package displayPackage.core;

import displayPackage.services.DatabaseHandler;
import displayPackage.services.DisplayService;

/**
 * Abstract base class for all controllers in the system.
 * Each controller holds references to the shared DatabaseHandler
 * and DisplayService instances (obtained via Singleton pattern).
 *
 * Subclasses: LoginController, RegistrationController,
 *             UploadImageController, AnalyzeImageController, etc.
 */
public abstract class BaseController {

    protected DatabaseHandler dbHandler;
    protected DisplayService displayService;

    /**
     * Default constructor — initialises references to shared services.
     */
    protected BaseController() {
        this.dbHandler = DatabaseHandler.getInstance();
        this.displayService = DisplayService.getInstance();
    }

    /**
     * Entry point for processing a request.
     * Concrete controllers override this to implement their specific flow.
     */
    public abstract void handleRequest();

    /**
     * Validates preconditions before processing a request.
     * Concrete controllers override this to check input validity.
     *
     * @return true if validation passes, false otherwise
     */
    public abstract boolean validate();

    /**
     * Provides access to the DatabaseHandler for subclasses
     * that need to pass it around.
     *
     * @return the shared DatabaseHandler instance
     */
    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }
}
