package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Student Portal operations.
 * Provides summary metrics for individual student dashboards.
 */
public class StudentDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    /**
     * Retrieves personal metrics for a specific student.
     * Index 0: Books currently borrowed and not yet returned.
     * Index 1: Books that are past their due date (Overdue).
     */
    public static int[] getMemberMetrics(int userId) {
        int[] metrics = new int[2];
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {

                // 1. Count books currently held by the student (Not returned)
                String queryCurrent = "SELECT COUNT(*) AS TotalHeld FROM BorrowRecords WHERE UserID = ? AND ReturnDate IS NULL";
                try (PreparedStatement pst1 = conn.prepareStatement(queryCurrent)) {
                    pst1.setInt(1, userId);
                    ResultSet rs1 = pst1.executeQuery();
                    if (rs1.next()) {
                        metrics[0] = rs1.getInt("TotalHeld");
                    }
                }

                // 2. Count overdue books (ReturnDate is NULL and DueDate is before today)
                String queryOverdue = "SELECT COUNT(*) AS OverdueCount FROM BorrowRecords " +
                        "WHERE UserID = ? AND ReturnDate IS NULL AND DueDate < GETDATE()";
                try (PreparedStatement pst2 = conn.prepareStatement(queryOverdue)) {
                    pst2.setInt(1, userId);
                    ResultSet rs2 = pst2.executeQuery();
                    if (rs2.next()) {
                        metrics[1] = rs2.getInt("OverdueCount");
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve student dashboard metrics: ", e);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return metrics;
    }
}