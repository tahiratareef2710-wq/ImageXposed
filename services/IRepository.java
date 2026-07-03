package displayPackage.services;

import displayPackage.models.*;

import java.util.List;

/**
 * IRepository interface for Protected Variation (GRASP pattern).
 * This interface protects the system from changes in database implementation.
 */
public interface IRepository {

    // User operations
    User lookupUser(String username);
    boolean checkUniqueness(String username, String email);
    void saveUser(User user);
    void updateUser(String username, String email, String passHash);

    // Image operations
    void recordImageEntry(Image image);
    Image getImage(String imageId);

    // Scan operations
    void persistScan(Scan scan);
    List<Scan> loadScanHistory(String userId);
    void deleteScan(String scanId);
    Scan getScan(String scanId);

    // Report operations
    void persistReport(Report report);
    Report getReport(String reportId);

    // Feedback operations
    void saveFeedback(Feedback feedback);
}