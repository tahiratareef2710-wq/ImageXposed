package displayPackage.services;

import displayPackage.core.BaseService;
import displayPackage.models.*;

import java.sql.*;
import java.util.*;

/**
 * Singleton service that handles all data persistence.
 * Now using Microsoft SQL Server JDBC.
 */
public class DatabaseHandler extends BaseService implements IRepository {

    private static DatabaseHandler instance;

    public static synchronized DatabaseHandler getInstance() {
        if (instance == null) {
            instance = new DatabaseHandler();
            instance.initialise();
        }
        return instance;
    }

    private DatabaseHandler() {
        super();
    }

    @Override
    protected void onInitialise() {
        System.out.println("[DatabaseHandler] Initialised with SQL Server connection.");
    }

    @Override
    protected void onExecute() { }

    @Override
    protected void onShutdown() {
        DBConnection.closeConnection();
        System.out.println("[DatabaseHandler] Shutdown complete.");
    }

    // ── User Operations ────────────────────────────────────────────────────

    public User lookupUser(String username) {
        String query = "SELECT username, email, password_hash FROM Users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("username"), rs.getString("email"), rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error looking up user: " + e.getMessage());
        }
        return null;
    }

    public boolean checkUniqueness(String username, String email) {
        String query = "SELECT 1 FROM Users WHERE username = ? OR email = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return !rs.next(); // True if unique (no results found)
            }
        } catch (SQLException e) {
            System.err.println("DB Error checking uniqueness: " + e.getMessage());
            return false;
        }
    }

    public void saveUser(User user) {
        String query = "INSERT INTO Users (username, email, password_hash) VALUES (?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error saving user: " + e.getMessage());
        }
    }

    public void updateUser(String username, String email, String passHash) {
        String query = "UPDATE Users SET email = COALESCE(?, email), password_hash = COALESCE(?, password_hash), updated_at = GETDATE() WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, (email != null && !email.isEmpty()) ? email : null);
            stmt.setString(2, (passHash != null && !passHash.isEmpty()) ? passHash : null);
            stmt.setString(3, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error updating user: " + e.getMessage());
        }
    }

    // ── Image Operations ───────────────────────────────────────────────────

    public void recordImageEntry(Image image) {
        String query = "INSERT INTO Images (id, file_name, file_format, file_size, file_path, tracking_id) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, image.getId());
            stmt.setString(2, image.getFileName());
            stmt.setString(3, image.getFileFormat());
            stmt.setLong(4, image.getFileSize());
            stmt.setString(5, image.getFilePath());
            stmt.setString(6, image.getTrackingId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error recording image: " + e.getMessage());
        }
    }

    public Image getImage(String imageId) {
        String query = "SELECT * FROM Images WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, imageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Image img = new Image(rs.getString("file_name"), rs.getString("file_format"),
                                          rs.getLong("file_size"), rs.getString("file_path"));
                    img.setId(rs.getString("id"));
                    return img;
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving image: " + e.getMessage());
        }
        return null;
    }

    // ── Scan Operations ────────────────────────────────────────────────────

    public void persistScan(Scan scan) {
        String checkQuery = "SELECT 1 FROM Scans WHERE id = ?";
        boolean exists = false;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setString(1, scan.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                exists = rs.next();
            }
        } catch (SQLException e) { }

        try {
            if (exists) {
                String update = "UPDATE Scans SET validation_result=?, analysis_verdict=?, analysis_confidence=?, md5_hash=?, sha256_hash=?, metadata_info=?, ela_result=? WHERE id=?";
                try (PreparedStatement stmt = conn.prepareStatement(update)) {
                    stmt.setString(1, scan.getValidationResult());
                    stmt.setString(2, (String) scan.getScanData().get("verdict"));
                    stmt.setString(3, (String) scan.getScanData().get("confidence"));
                    stmt.setString(4, (String) scan.getScanData().get("md5Hash"));
                    stmt.setString(5, (String) scan.getScanData().get("sha256"));
                    stmt.setString(6, (String) scan.getScanData().get("metadata"));
                    stmt.setString(7, (String) scan.getScanData().get("elaResult"));
                    stmt.setString(8, scan.getId());
                    stmt.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO Scans (id, user_id, image_id, validation_result, analysis_verdict, analysis_confidence, md5_hash, sha256_hash, metadata_info, ela_result) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setString(1, scan.getId());
                    stmt.setString(2, scan.getUserId());
                    stmt.setString(3, scan.getImageId());
                    stmt.setString(4, scan.getValidationResult());
                    stmt.setString(5, (String) scan.getScanData().get("verdict"));
                    stmt.setString(6, (String) scan.getScanData().get("confidence"));
                    stmt.setString(7, (String) scan.getScanData().get("md5Hash"));
                    stmt.setString(8, (String) scan.getScanData().get("sha256"));
                    stmt.setString(9, (String) scan.getScanData().get("metadata"));
                    stmt.setString(10, (String) scan.getScanData().get("elaResult"));
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error persisting scan: " + e.getMessage());
        }
    }

    public List<Scan> loadScanHistory(String userId) {
        List<Scan> scans = new ArrayList<>();
        String query = "SELECT * FROM Scans WHERE user_id = ? ORDER BY scan_timestamp DESC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Scan s = new Scan(rs.getString("user_id"), rs.getString("image_id"));
                    s.setId(rs.getString("id"));
                    s.setValidationResult(rs.getString("validation_result"));
                    s.addScanData("verdict", rs.getString("analysis_verdict"));
                    s.addScanData("confidence", rs.getString("analysis_confidence"));
                    // Read the actual scan timestamp from DB instead of using constructor's new Date()
                    Timestamp ts = rs.getTimestamp("scan_timestamp");
                    if (ts != null) {
                        s.setTimestamp(new java.util.Date(ts.getTime()));
                    }
                    scans.add(s);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error loading scan history: " + e.getMessage());
        }
        return scans;
    }

    public void deleteScan(String scanId) {
        String query = "DELETE FROM Scans WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, scanId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error deleting scan: " + e.getMessage());
        }
    }

    public Scan getScan(String scanId) {
        String query = "SELECT * FROM Scans WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, scanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Scan s = new Scan(rs.getString("user_id"), rs.getString("image_id"));
                    s.setId(rs.getString("id"));
                    s.setValidationResult(rs.getString("validation_result"));
                    s.addScanData("verdict", rs.getString("analysis_verdict"));
                    s.addScanData("confidence", rs.getString("analysis_confidence"));
                    s.addScanData("md5Hash", rs.getString("md5_hash"));
                    s.addScanData("sha256", rs.getString("sha256_hash"));
                    s.addScanData("metadata", rs.getString("metadata_info"));
                    s.addScanData("elaResult", rs.getString("ela_result"));
                    // Read the actual scan timestamp from DB instead of using constructor's new Date()
                    Timestamp ts = rs.getTimestamp("scan_timestamp");
                    if (ts != null) {
                        s.setTimestamp(new java.util.Date(ts.getTime()));
                    }
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving scan: " + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> fetchAnalysisResults(String scanId) {
        Scan scan = getScan(scanId);
        return scan != null ? scan.packageScanData() : new HashMap<>();
    }

    // ── Report Operations ──────────────────────────────────────────────────

    public void persistReport(Report report) {
        boolean exists = false;
        String checkQuery = "SELECT id FROM Reports WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setString(1, report.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) exists = true;
            }
        } catch (SQLException e) { }

        String content = (String) report.getReportData().getOrDefault("reportText", "");

        if (exists) {
            String update = "UPDATE Reports SET scan_id=?, report_type=?, content=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(update)) {
                stmt.setString(1, report.getScanId());
                stmt.setString(2, report.getExportFormat());
                stmt.setString(3, content);
                stmt.setString(4, report.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("DB Error updating report: " + e.getMessage());
            }
        } else {
            String insert = "INSERT INTO Reports (id, scan_id, report_type, content) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                stmt.setString(1, report.getId());
                stmt.setString(2, report.getScanId());
                stmt.setString(3, report.getExportFormat());
                stmt.setString(4, content);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("DB Error inserting report: " + e.getMessage());
            }
        }
    }

    public Report getReport(String reportId) {
        String query = "SELECT * FROM Reports WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Report r = new Report(rs.getString("scan_id"));
                    r.setExportFormat(rs.getString("report_type"));
                    r.setId(rs.getString("id"));
                    r.getReportData().put("reportText", rs.getString("content"));
                    return r;
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving report: " + e.getMessage());
        }
        return null;
    }

    // ── Feedback Operations ────────────────────────────────────────────────

    public void saveFeedback(Feedback feedback) {
        String query = "INSERT INTO Feedbacks (id, user_id, rating, comments) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, feedback.getId());
            stmt.setString(2, feedback.getUserId());
            stmt.setInt(3, 5); // Default rating
            stmt.setString(4, feedback.getCategory() + ": " + feedback.getSubject()); // Combine mapping
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error saving feedback: " + e.getMessage());
        }
    }

    public List<Feedback> getAllFeedback() {
        List<Feedback> list = new ArrayList<>();
        String query = "SELECT * FROM Feedbacks ORDER BY created_at DESC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Feedback f = new Feedback(rs.getString("user_id"), "General", rs.getString("comments"));
                    f.setId(rs.getString("id"));
                    list.add(f);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving feedback: " + e.getMessage());
        }
        return list;
    }

    public void persist(Map<String, Object> data) {
        String id = (String) data.getOrDefault("id", UUID.randomUUID().toString());
        System.out.println("[DatabaseHandler] Persisting data with ID: " + id);
    }
}
