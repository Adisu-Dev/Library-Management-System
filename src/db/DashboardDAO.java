package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardDAO {

    private static final Logger LOGGER = Logger.getLogger(DashboardDAO.class.getName());

    /**
     * ዳሽቦርዱ ላይ ያሉትን 4 ቁልፍ መረጃዎች ያመጣል
     * Index 0: Total Books (Catalog)
     * Index 1: Active Members
     * Index 2: Currently Borrowed
     * Index 3: Overdue & Fines
     */
    public static int[] getMetrics() {
        int[] metrics = new int[4]; // ወደ 4 አሳድገነዋል
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();

            if (conn != null) {
                // 1. Total Catalog (የመጽሐፍት ብዛት)
                String query1 = "SELECT COUNT(*) AS Total FROM Books";
                try (PreparedStatement stmt = conn.prepareStatement(query1);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics[0] = rs.getInt(1);
                }

                // 2. Active Members (የተማሪዎች ብዛት) [cite: 31]
                String query2 = "SELECT COUNT(*) AS Total FROM Users";
                try (PreparedStatement stmt = conn.prepareStatement(query2);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics[1] = rs.getInt(1);
                }

                // 3. Currently Borrowed (ያልተመለሱ መጽሐፍት) [cite: 33]
                String query3 = "SELECT COUNT(*) AS Total FROM BorrowRecords WHERE ReturnDate IS NULL";
                try (PreparedStatement stmt = conn.prepareStatement(query3);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics[2] = rs.getInt(1);
                }

                // 4. Overdue & Fines (ከመመለሻ ቀን ያለፉ)
                String query4 = "SELECT COUNT(*) AS Total FROM BorrowRecords WHERE ReturnDate IS NULL AND DueDate < GETDATE()";
                try (PreparedStatement stmt = conn.prepareStatement(query4);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics[3] = rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving dashboard metrics: ", e);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return metrics;
    }
}