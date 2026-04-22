package db;

import model.UserRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for User Management operations.
 * Handles CRUD operations and authentication for the LMS system.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // 1. User Authentication (Login)
    public static String authenticateUser(String username, String password) {
        String role = null;
        String query = "SELECT Role FROM Users WHERE Username = ? AND PasswordHash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);

            // Hash the provided password to compare with the stored hash
            String encryptedPassword = PasswordUtil.hashPassword(password);
            pstmt.setString(2, encryptedPassword);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    role = rs.getString("Role");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login authentication failed: ", e);
        }
        return role;
    }

    // 2. Fetch All Users (For TableView)
    public static List<UserRecord> getAllUsers() {
        List<UserRecord> userList = new ArrayList<>();
        String query = "SELECT UserID, FullName, Username, Role, Phone FROM Users";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                userList.add(new UserRecord(
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Username"),
                        rs.getString("Role"),
                        rs.getString("Phone")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch users: ", e);
        }
        return userList;
    }

    // 3. Register a New User (Create) — with optional IsBlocked flag
    public static boolean registerUser(String name, String user, String pass, String role, String phone) {
        return registerUser(name, user, pass, role, phone, false); // default: not blocked
    }

    public static boolean registerUser(String name, String user, String pass, String role, String phone, boolean blocked) {
        // Check if IsBlocked column exists
        boolean hasIsBlocked = false;
        try (Connection testConn = DatabaseConnection.getConnection()) {
            if (testConn != null) {
                java.sql.DatabaseMetaData meta = testConn.getMetaData();
                try (ResultSet colRs = meta.getColumns(null, null, "Users", "IsBlocked")) {
                    hasIsBlocked = colRs.next();
                }
            }
        } catch (SQLException ignored) {}

        String sql = hasIsBlocked
            ? "INSERT INTO Users (FullName, Username, PasswordHash, Role, Phone, IsBlocked) VALUES (?, ?, ?, ?, ?, ?)"
            : "INSERT INTO Users (FullName, Username, PasswordHash, Role, Phone) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, user);
            pst.setString(3, PasswordUtil.hashPassword(pass));
            pst.setString(4, role);
            pst.setString(5, phone);
            if (hasIsBlocked) pst.setInt(6, blocked ? 1 : 0);

            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "User registration failed: ", e);
            return false;
        }
    }

    // 4. Update Existing User Information
    public static boolean updateUser(int id, String name, String user, String pass, String role, String phone) {
        // 🚀 PRO FEATURE: Check if the admin typed a new password.
        boolean updatePassword = (pass != null && !pass.trim().isEmpty());
        String sql;

        // Dynamically build the query based on whether we are updating the password or not
        if (updatePassword) {
            sql = "UPDATE Users SET FullName=?, Username=?, PasswordHash=?, Role=?, Phone=? WHERE UserID=?";
        } else {
            sql = "UPDATE Users SET FullName=?, Username=?, Role=?, Phone=? WHERE UserID=?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, user);

            if (updatePassword) {
                // If a new password is provided, hash it and set all parameters
                pst.setString(3, PasswordUtil.hashPassword(pass));
                pst.setString(4, role);
                pst.setString(5, phone);
                pst.setInt(6, id);
            } else {
                // Skip the PasswordHash column and shift the parameter indexes
                pst.setString(3, role);
                pst.setString(4, phone);
                pst.setInt(5, id);
            }

            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "User update failed: ", e);
            return false;
        }
    }

    // 5. Delete User
    public static boolean deleteUser(int id) {
        String sql = "DELETE FROM Users WHERE UserID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "User deletion failed: ", e);
            return false;
        }
    }

    // =========================================================
    // 🚀 6. NEW: Check Duplicate Username
    // =========================================================
    public static boolean isUsernameTaken(String username, int excludeUserId) {
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND UserID != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setInt(2, excludeUserId);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking duplicate username: ", e);
        }
        return false;
    }

    // =========================================================
    // 7. Block / Unblock a Student account
    // =========================================================
    public static boolean setUserBlocked(int userId, boolean blocked) {
        // Uses an IsBlocked column (BIT) in the Users table.
        // If your DB doesn't have it yet, run:
        //   ALTER TABLE Users ADD IsBlocked BIT NOT NULL DEFAULT 0;
        String sql = "UPDATE Users SET IsBlocked = ? WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, blocked ? 1 : 0);
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating block status: ", e);
            return false;
        }
    }

    // =========================================================
    // 8. Fetch All Users including block status
    // =========================================================
    public static List<UserRecord> getAllUsersWithStatus() {
        List<UserRecord> userList = new ArrayList<>();
        // Check if IsBlocked column exists before using it
        String query;
        boolean hasIsBlocked = false;
        try (Connection testConn = DatabaseConnection.getConnection()) {
            if (testConn != null) {
                java.sql.DatabaseMetaData meta = testConn.getMetaData();
                try (ResultSet colRs = meta.getColumns(null, null, "Users", "IsBlocked")) {
                    hasIsBlocked = colRs.next();
                }
            }
        } catch (SQLException ignored) {}

        if (hasIsBlocked) {
            query = "SELECT UserID, FullName, Username, Role, Phone, IsBlocked FROM Users";
        } else {
            query = "SELECT UserID, FullName, Username, Role, Phone FROM Users";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                boolean blocked = hasIsBlocked && rs.getInt("IsBlocked") == 1;
                userList.add(new UserRecord(
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Username"),
                        rs.getString("Role"),
                        rs.getString("Phone"),
                        blocked
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch users with status: ", e);
            return getAllUsers();
        }
        return userList;
    }
}
