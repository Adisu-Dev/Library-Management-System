package model;

/**
 * Advanced Model for User Management.
 * Supports CRUD operations and DataGrid editing features.
 */
public class UserRecord {
    private int userId;
    private String fullName;
    private String username;
    private String role;
    private String phone;
    private String encryptedPassword;
    private boolean blocked;

    // Constructor for TableView and general use
    public UserRecord(int userId, String fullName, String username, String role, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.phone = phone;
        this.blocked = false;
    }

    // Constructor with blocked status
    public UserRecord(int userId, String fullName, String username, String role, String phone, boolean blocked) {
        this(userId, fullName, username, role, phone);
        this.blocked = blocked;
    }

    // ==========================================
    // Getters and Setters (Enables DataGrid Editing)
    // ==========================================

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public String getStatusLabel() { return blocked ? "🚫 Blocked" : "✅ Active"; }
}