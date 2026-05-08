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

            // Validate student exists
            PreparedStatement checkUser = conn.prepareStatement(
                "SELECT FullName FROM Users WHERE UserID = ?");
            checkUser.setInt(1, userId);
            if (!checkUser.executeQuery().next())
                return "Error: Student ID not found in database!";

            // Validate book exists
            PreparedStatement checkBook = conn.prepareStatement(
                "SELECT Title, AvailableQuantity FROM Books WHERE BookID = ?");
            checkBook.setInt(1, bookId);
            ResultSet rsBook = checkBook.executeQuery();
            if (!rsBook.next()) return "Error: Book ID not found in database!";
            if (rsBook.getInt("AvailableQuantity") <= 0)
                return "Error: Book is currently out of stock!";

            conn.setAutoCommit(false);

            // Insert borrow record
            PreparedStatement pstIssue = conn.prepareStatement(
                "INSERT INTO BorrowRecords (BookID, UserID, DueDate) " +
                "VALUES (?, ?, DATEADD(day, ?, GETDATE()))");
            pstIssue.setInt(1, bookId);
            pstIssue.setInt(2, userId);
            pstIssue.setInt(3, days);
            pstIssue.executeUpdate();

            // Atomic decrement — only succeeds if AvailableQuantity is still > 0
            // This prevents race conditions where two transactions pass the check simultaneously
            PreparedStatement pstUpdate = conn.prepareStatement(
                "UPDATE Books SET AvailableQuantity = AvailableQuantity - 1 " +
                "WHERE BookID = ? AND AvailableQuantity > 0");
            pstUpdate.setInt(1, bookId);
            int rowsUpdated = pstUpdate.executeUpdate();

            if (rowsUpdated == 0) {
                // Another transaction already took the last copy — rollback
                conn.rollback();
                return "Error: Book just went out of stock. Please try again.";
            }

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

    // =========================================================
    // Feature 1: Dynamic Reading History — live from DB
    // Returns all borrow records (returned + active) for a student
    // =========================================================
    public static List<String[]> getReadingHistory(int userId) {
        List<String[]> list = new ArrayList<>();
        String q = "SELECT b.Title, " +
                   "CONVERT(VARCHAR(10), br.IssueDate, 120) AS IssueDate, " +
                   "CONVERT(VARCHAR(10), br.ReturnDate, 120) AS ReturnDate, " +
                   "CASE WHEN br.ReturnDate IS NOT NULL THEN '✅ Returned' " +
                   "     WHEN br.DueDate < GETDATE() THEN '⚠️ Overdue' " +
                   "     ELSE '📖 Active' END AS Status " +
                   "FROM BorrowRecords br " +
                   "JOIN Books b ON br.BookID = b.BookID " +
                   "WHERE br.UserID = ? " +
                   "ORDER BY br.IssueDate DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(q)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("Title"),
                    rs.getString("IssueDate"),
                    rs.getString("ReturnDate") != null ? rs.getString("ReturnDate") : "—",
                    rs.getString("Status")
                });
            }
        } catch (SQLException e) {
            System.out.println("Reading History Error: " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // Feature 2: Borrow Request system
    // Ensures BorrowRequests table exists, then inserts a request
    // =========================================================
    public static String submitBorrowRequest(int bookId, int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return "Error: Cannot connect to database.";

            // Auto-create BorrowRequests table if it doesn't exist
            conn.createStatement().execute(
                "IF OBJECT_ID('BorrowRequests','U') IS NULL " +
                "CREATE TABLE BorrowRequests (" +
                "  RequestID   INT PRIMARY KEY IDENTITY(1,1)," +
                "  BookID      INT NOT NULL FOREIGN KEY REFERENCES Books(BookID)," +
                "  UserID      INT NOT NULL FOREIGN KEY REFERENCES Users(UserID)," +
                "  RequestedAt DATETIME DEFAULT GETDATE()," +
                "  Status      NVARCHAR(20) DEFAULT 'Pending'" +
                ")");

            // Check book exists and is available
            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT Title, AvailableQuantity FROM Books WHERE BookID=?")) {
                chk.setInt(1, bookId);
                ResultSet rs = chk.executeQuery();
                if (!rs.next()) return "Error: Book not found.";
                if (rs.getInt("AvailableQuantity") <= 0)
                    return "Error: Book is out of stock. You can reserve it instead.";
            }

            // Prevent duplicate pending request
            try (PreparedStatement dup = conn.prepareStatement(
                    "SELECT COUNT(*) FROM BorrowRequests WHERE BookID=? AND UserID=? AND Status='Pending'")) {
                dup.setInt(1, bookId); dup.setInt(2, userId);
                ResultSet rs = dup.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    return "Error: You already have a pending request for this book.";
            }

            // Insert request
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO BorrowRequests (BookID, UserID) VALUES (?, ?)")) {
                ins.setInt(1, bookId); ins.setInt(2, userId);
                ins.executeUpdate();
            }

            // Log the activity
            ActivityLog.log(userId, "Requested to borrow BookID=" + bookId);
            return "Success";
        } catch (SQLException e) {
            return "Database Error: " + e.getMessage();
        }
    }

    /** Returns all pending borrow requests for the librarian view */
    public static List<String[]> getPendingRequests() {
        List<String[]> list = new ArrayList<>();
        String q = "SELECT rq.RequestID, b.Title, u.FullName, u.UserID, b.BookID, " +
                   "CONVERT(VARCHAR(16), rq.RequestedAt, 120) AS RequestedAt, rq.Status " +
                   "FROM BorrowRequests rq " +
                   "JOIN Books b ON rq.BookID = b.BookID " +
                   "JOIN Users u ON rq.UserID = u.UserID " +
                   "WHERE rq.Status = 'Pending' " +
                   "ORDER BY rq.RequestedAt ASC";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return list;
            // Table may not exist yet
            try (ResultSet rs = conn.createStatement().executeQuery(q)) {
                while (rs.next()) {
                    list.add(new String[]{
                        String.valueOf(rs.getInt("RequestID")),
                        rs.getString("Title"),
                        rs.getString("FullName"),
                        String.valueOf(rs.getInt("UserID")),
                        String.valueOf(rs.getInt("BookID")),
                        rs.getString("RequestedAt"),
                        rs.getString("Status")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Get Pending Requests Error: " + e.getMessage());
        }
        return list;
    }

    /** Librarian approves a request — issues the book and updates request status */
    public static String approveRequest(int requestId, int bookId, int userId, int days) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return "Error: Cannot connect.";
            conn.setAutoCommit(false);

            // Issue the book (reuse existing logic)
            PreparedStatement pstIssue = conn.prepareStatement(
                "INSERT INTO BorrowRecords (BookID, UserID, DueDate) " +
                "VALUES (?, ?, DATEADD(day, ?, GETDATE()))");
            pstIssue.setInt(1, bookId); pstIssue.setInt(2, userId); pstIssue.setInt(3, days);
            pstIssue.executeUpdate();

            // Decrement availability atomically
            PreparedStatement pstDecr = conn.prepareStatement(
                "UPDATE Books SET AvailableQuantity = AvailableQuantity - 1 " +
                "WHERE BookID = ? AND AvailableQuantity > 0");
            pstDecr.setInt(1, bookId);
            if (pstDecr.executeUpdate() == 0) { conn.rollback(); return "Error: Book out of stock."; }

            // Mark request as Approved
            PreparedStatement pstReq = conn.prepareStatement(
                "UPDATE BorrowRequests SET Status='Approved' WHERE RequestID=?");
            pstReq.setInt(1, requestId);
            pstReq.executeUpdate();

            conn.commit();
            ActivityLog.log(userId, "Request #" + requestId + " APPROVED — BookID=" + bookId);
            return "Success";
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return "Database Error: " + e.getMessage();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /** Librarian rejects a request */
    public static String rejectRequest(int requestId, int userId, int bookId) {
        String q = "UPDATE BorrowRequests SET Status='Rejected' WHERE RequestID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(q)) {
            pst.setInt(1, requestId);
            pst.executeUpdate();
            ActivityLog.log(userId, "Request #" + requestId + " REJECTED — BookID=" + bookId);
            return "Success";
        } catch (SQLException e) {
            return "Database Error: " + e.getMessage();
        }
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