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
    // 6b. Update own profile (username + optional password + bio)
    //     Verifies current password before making any change.
    //     Returns: 0=success, 1=wrong current password, 2=username taken, 3=db error
    // =========================================================
    public static int updateOwnProfile(int userId, String currentPassword,
                                       String newUsername, String newPassword) {
        return updateOwnProfile(userId, currentPassword, newUsername, newPassword, null);
    }

    public static int updateOwnProfile(int userId, String currentPassword,
                                       String newUsername, String newPassword, String bio) {
        // 1. Verify current password
        String verifySql = "SELECT PasswordHash FROM Users WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(verifySql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) return 3;
            String storedHash = rs.getString("PasswordHash");
            if (!storedHash.equals(PasswordUtil.hashPassword(currentPassword))) {
                return 1; // wrong current password
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Profile update - verify failed: ", e);
            return 3;
        }

        // 2. Check username uniqueness (if username is being changed)
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            if (isUsernameTaken(newUsername.trim(), userId)) return 2;
        }

        // 3. Build and run update
        boolean changePass = (newPassword != null && !newPassword.trim().isEmpty());
        boolean changeUser = (newUsername != null && !newUsername.trim().isEmpty());
        boolean changeBio  = (bio != null);

        if (!changePass && !changeUser && !changeBio) return 0;

        // Check if Bio column exists
        boolean hasBio = false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet colRs = meta.getColumns(null, null, "Users", "Bio")) {
                    hasBio = colRs.next();
                }
            }
        } catch (SQLException ignored) {}

        StringBuilder sql = new StringBuilder("UPDATE Users SET ");
        if (changeUser) sql.append("Username = ?, ");
        if (changePass) sql.append("PasswordHash = ?, ");
        if (changeBio && hasBio) sql.append("Bio = ?, ");
        String query = sql.toString().replaceAll(", $", "") + " WHERE UserID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            int idx = 1;
            if (changeUser) pst.setString(idx++, newUsername.trim());
            if (changePass) pst.setString(idx++, PasswordUtil.hashPassword(newPassword));
            if (changeBio && hasBio) pst.setString(idx++, bio);
            pst.setInt(idx, userId);
            pst.executeUpdate();
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Profile update failed: ", e);
            return 3;
        }
    }

    // =========================================================
    // 6c. Update bio only (no password verification needed for bio alone)
    // =========================================================
    public static boolean updateBio(int userId, String bio) {
        // Ensure Bio column exists
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet colRs = meta.getColumns(null, null, "Users", "Bio")) {
                if (!colRs.next()) {
                    // Create the column
                    conn.createStatement().execute(
                        "ALTER TABLE Users ADD Bio NVARCHAR(300) NULL");
                }
            }
            try (PreparedStatement pst = conn.prepareStatement(
                    "UPDATE Users SET Bio = ? WHERE UserID = ?")) {
                pst.setString(1, bio);
                pst.setInt(2, userId);
                return pst.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Bio update failed: ", e);
            return false;
        }
    }

    // =========================================================
    // 6d. Fetch bio for a user
    // =========================================================
    public static String getBio(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return "";
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet colRs = meta.getColumns(null, null, "Users", "Bio")) {
                if (!colRs.next()) return "";
            }
            try (PreparedStatement pst = conn.prepareStatement(
                    "SELECT Bio FROM Users WHERE UserID = ?")) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next() && rs.getString("Bio") != null) return rs.getString("Bio");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Bio fetch failed: ", e);
        }
        return "";
    }

    // =========================================================
    // 6e. Fetch username by userId
    // =========================================================
    public static String getUsernameById(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT Username FROM Users WHERE UserID = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("Username");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Username fetch failed: ", e);
        }
        return "";
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
