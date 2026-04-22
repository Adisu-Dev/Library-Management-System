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
        String query = "DELETE FROM Books WHERE BookID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Delete Book Error: " + e.getMessage());
            return false;
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