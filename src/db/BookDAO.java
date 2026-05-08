package db;

import model.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public static List<Book> getAllBooks() {
        List<Book> bookList = new ArrayList<>();
        // SSMS ላይ ያሉትን ኮለም ስሞች በግልጽ መጥራት ስህተትን ይከላከላል
        String query = "SELECT BookID, Title, Author, ISBN, Category, Quantity, AvailableQuantity FROM Books";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("BookID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getString("ISBN"),
                        rs.getString("Category"),
                        rs.getInt("Quantity"),
                        rs.getInt("AvailableQuantity")
                );
                bookList.add(book);
            }
        } catch (SQLException e) {
            System.out.println("Get Books Error: " + e.getMessage());
        }
        return bookList;
    }

    public static boolean addBook(Book book) {
        String query = "INSERT INTO Books (Title, Author, ISBN, Category, Quantity, AvailableQuantity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getIsbn());
            pstmt.setString(4, book.getCategory());
            pstmt.setInt(5, book.getQuantity());
            pstmt.setInt(6, book.getAvailableQuantity());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Add Book Error: " + e.getMessage());
            return false;
        }
    }

    public static List<Book> searchBooks(String keyword) {
        List<Book> bookList = new ArrayList<>();
        String query = "SELECT BookID, Title, Author, ISBN, Category, Quantity, AvailableQuantity FROM Books WHERE Title LIKE ? OR Author LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookList.add(new Book(rs.getInt("BookID"), rs.getString("Title"), rs.getString("Author"),
                            rs.getString("ISBN"), rs.getString("Category"), rs.getInt("Quantity"), rs.getInt("AvailableQuantity")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Search Books Error: " + e.getMessage());
        }
        return bookList;
    }

    public static boolean updateBook(Book book) {
        String query = "UPDATE Books SET Title=?, Author=?, ISBN=?, Category=?, Quantity=?, AvailableQuantity=? WHERE BookID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getIsbn());
            pstmt.setString(4, book.getCategory());
            pstmt.setInt(5, book.getQuantity());
            pstmt.setInt(6, book.getAvailableQuantity());
            pstmt.setInt(7, book.getBookID());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update Book Error: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteBook(int id) {
        // First check if this book has any active (unreturned) borrow records
        String checkActive = "SELECT COUNT(*) FROM BorrowRecords WHERE BookID = ? AND ReturnDate IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement chk = conn.prepareStatement(checkActive)) {
            chk.setInt(1, id);
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // Cannot delete — book is currently borrowed
                throw new SQLException("ACTIVE_BORROWS");
            }
        } catch (SQLException e) {
            if (e.getMessage().equals("ACTIVE_BORROWS")) throw new RuntimeException("ACTIVE_BORROWS");
            System.out.println("Delete check error: " + e.getMessage());
            return false;
        }

        // Delete borrow history first (past records where book was returned)
        String deleteHistory = "DELETE FROM BorrowRecords WHERE BookID = ? AND ReturnDate IS NOT NULL";
        // Delete reservations
        String deleteReservations = "DELETE FROM Reservations WHERE BookID = ?";
        // Then delete the book
        String deleteBook = "DELETE FROM Books WHERE BookID = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement p1 = conn.prepareStatement(deleteHistory)) {
                p1.setInt(1, id); p1.executeUpdate();
            }
            try (PreparedStatement p2 = conn.prepareStatement(deleteReservations)) {
                p2.setInt(1, id); p2.executeUpdate();
            }
            try (PreparedStatement p3 = conn.prepareStatement(deleteBook)) {
                p3.setInt(1, id);
                int rows = p3.executeUpdate();
                conn.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.out.println("Delete Book Error: " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    // 🚀 በ Category (ዘርፍ) ለይቶ ማምጫ ሜተድ
    public static java.util.List<model.Book> getBooksByCategory(String category) {
        java.util.List<model.Book> bookList = new java.util.ArrayList<>();
        String query = "SELECT BookID, Title, Author, ISBN, Category, Quantity, AvailableQuantity FROM Books WHERE Category = ?";

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, category);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookList.add(new model.Book(
                            rs.getInt("BookID"), rs.getString("Title"), rs.getString("Author"),
                            rs.getString("ISBN"), rs.getString("Category"),
                            rs.getInt("Quantity"), rs.getInt("AvailableQuantity")
                    ));
                }
            }
        } catch (java.sql.SQLException e) {
            System.out.println("Filter Error: " + e.getMessage());
        }
        return bookList;
    }
}