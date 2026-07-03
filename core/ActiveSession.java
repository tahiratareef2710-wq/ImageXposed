package displayPackage.core;

import displayPackage.models.User;

/**
 * Singleton class to hold the currently authenticated user's session data.
 * Allows components across the application to know who is logged in
 * without passing the username string through every method signature.
 */
public class ActiveSession {

    private static ActiveSession instance;
    private User loggedInUser;

    private ActiveSession() {}

    public static synchronized ActiveSession getInstance() {
        if (instance == null) {
            instance = new ActiveSession();
        }
        return instance;
    }

    public void login(User user) {
        this.loggedInUser = user;
    }

    public void logout() {
        this.loggedInUser = null;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
}
