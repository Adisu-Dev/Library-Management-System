package db;

import model.BorrowRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BorrowDAO {

    // 1. የተወሰዱ መጽሐፍትን ማምጫ (ለ TableView)
    public static List<BorrowRecord> getIssuedBooks() {
        List<BorrowRecord> records = new ArrayList<>();
        String query = "SELECT br.RecordID, b.Title, u.FullName, br.IssueDate, br.DueDate " +
                "FROM BorrowRecords br " +
                "JOIN Books b ON br.BookID = b.BookID " +
                "JOIN Users u ON br.UserID = u.UserID " +
                "WHERE br.ReturnDate IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                records.add(new BorrowRecord(
                        rs.getInt("RecordID"),
                        rs.getString("Title"),
                        rs.getString("FullName"),
                        rs.getDate("IssueDate").toString(),
                        rs.getDate("DueDate").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Get Issued Books Error: " + e.getMessage());
        }
        return records;
    }

    // 2. መጽሐፍ ማዋሻ (Issue Book with Transaction)
    public static String issueBook(int bookId, int userId, int days) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            PreparedStatement checkUser = conn.prepareStatement("SELECT FullName FROM Users WHERE UserID = ?");
            checkUser.setInt(1, userId);
            if (!checkUser.executeQuery().next()) return "Error: Student ID not found in database!";

            PreparedStatement checkBook = conn.prepareStatement("SELECT AvailableQuantity FROM Books WHERE BookID = ?");
            checkBook.setInt(1, bookId);
            ResultSet rsBook = checkBook.executeQuery();

            if (rsBook.next()) {
                if (rsBook.getInt("AvailableQuantity") <= 0) return "Error: Book is currently out of stock!";
            } else {
                return "Error: Book ID not found in database!";
            }

            conn.setAutoCommit(false);

            String insertIssue = "INSERT INTO BorrowRecords (BookID, UserID, DueDate) VALUES (?, ?, DATEADD(day, ?, GETDATE()))";
            PreparedStatement pstIssue = conn.prepareStatement(insertIssue);
            pstIssue.setInt(1, bookId);
            pstIssue.setInt(2, userId);
            pstIssue.setInt(3, days);
            pstIssue.executeUpdate();

            String updateBook = "UPDATE Books SET AvailableQuantity = AvailableQuantity - 1 WHERE BookID = ?";
            PreparedStatement pstUpdate = conn.prepareStatement(updateBook);
            pstUpdate.setInt(1, bookId);
            pstUpdate.executeUpdate();

            conn.commit();
            return "Success";

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return "Database Error: " + e.getMessage();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // =========================================================
    // Book Reservation — students can reserve out-of-stock books
    // SQL: CREATE TABLE Reservations (
    //   ReservationID INT IDENTITY PRIMARY KEY,
    //   BookID INT FOREIGN KEY REFERENCES Books(BookID),
    //   UserID INT FOREIGN KEY REFERENCES Users(UserID),
    //   ReservedAt DATETIME DEFAULT GETDATE(),
    //   Status NVARCHAR(20) DEFAULT 'Pending'  -- Pending / Fulfilled / Cancelled
    // );
    // =========================================================
    public static String reserveBook(int bookId, int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return "Error: Cannot connect to database.";

            // Check if book exists
            try (PreparedStatement chkBook = conn.prepareStatement(
                    "SELECT Title, AvailableQuantity FROM Books WHERE BookID=?")) {
                chkBook.setInt(1, bookId);
                ResultSet rs = chkBook.executeQuery();
                if (!rs.next()) return "Error: Book ID not found.";
                if (rs.getInt("AvailableQuantity") > 0)
                    return "Error: Book is available — please issue it directly instead of reserving.";
            }

            // Check for duplicate reservation
            try (PreparedStatement chkDup = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Reservations WHERE BookID=? AND UserID=? AND Status='Pending'")) {
                chkDup.setInt(1, bookId);
                chkDup.setInt(2, userId);
                ResultSet rs = chkDup.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    return "Error: You already have a pending reservation for this book.";
            }

            // Insert reservation
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO Reservations (BookID, UserID) VALUES (?, ?)")) {
                ins.setInt(1, bookId);
                ins.setInt(2, userId);
                ins.executeUpdate();
                return "Success";
            }
        } catch (SQLException e) {
            System.out.println("Reserve Book Error: " + e.getMessage());
            return "Database Error: " + e.getMessage();
        }
    }

    public static List<String[]> getStudentReservations(int userId) {
        List<String[]> list = new ArrayList<>();
        String q = "SELECT r.ReservationID, b.Title, b.Author, r.ReservedAt, r.Status " +
                   "FROM Reservations r JOIN Books b ON r.BookID=b.BookID " +
                   "WHERE r.UserID=? ORDER BY r.ReservedAt DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(q)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("ReservationID")),
                    rs.getString("Title"),
                    rs.getString("Author"),
                    rs.getTimestamp("ReservedAt").toString().substring(0, 10),
                    rs.getString("Status")
                });
            }
        } catch (SQLException e) {
            System.out.println("Get Reservations Error: " + e.getMessage());
        }
        return list;
    }
    public static String returnBook(int recordId, double penaltyAmount) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int bookId = -1;
            PreparedStatement getBook = conn.prepareStatement("SELECT BookID FROM BorrowRecords WHERE RecordID = ?");
            getBook.setInt(1, recordId);
            ResultSet rs = getBook.executeQuery();
            if (rs.next()) {
                bookId = rs.getInt("BookID");
            }

            if (bookId == -1) return "Error: Record not found.";

            // የውሰት ሪከርዱን ማዘመን (ReturnDate ዛሬን ያደርገዋል፣ ቅጣቱንም ይመዘግባል)
            String updateRecord = "UPDATE BorrowRecords SET ReturnDate = GETDATE(), PenaltyAmount = ? WHERE RecordID = ?";
            PreparedStatement pstReturn = conn.prepareStatement(updateRecord);
            pstReturn.setDouble(1, penaltyAmount);
            pstReturn.setInt(2, recordId);
            pstReturn.executeUpdate();

            // የመጽሐፉን AvailableQuantity በ 1 መጨመር (ወደ ላይብረሪ ስለተመለሰ)
            String updateBook = "UPDATE Books SET AvailableQuantity = AvailableQuantity + 1 WHERE BookID = ?";
            PreparedStatement pstUpdate = conn.prepareStatement(updateBook);
            pstUpdate.setInt(1, bookId);
            pstUpdate.executeUpdate();

            conn.commit();
            return "Success";

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return "Database Error: " + e.getMessage();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}