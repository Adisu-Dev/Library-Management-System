package model;

/**
 * Advanced Model class representing a Book in the Library System.
 * Supports CRUD operations and DataGrid editing features.
 */
public class Book {
    private int bookID;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private int quantity;
    private int availableQuantity;

    // Constructor for retrieving books from the Database (includes ID)
    public Book(int bookID, String title, String author, String isbn, String category, int quantity, int availableQuantity) {
        this.bookID = bookID;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
    }

    // Constructor for registering a new book (Database generates ID)
    public Book(String title, String author, String isbn, String category, int quantity, int availableQuantity) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
    }

    // ==========================================
    // Getters and Setters (Required for Editing/Updating)
    // ==========================================

    public int getBookID() { return bookID; }
    public void setBookID(int bookID) { this.bookID = bookID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }

    // Pro Feature: Logic to ensure quantity is never negative
    public void setQuantity(int quantity) {
        if(quantity >= 0) this.quantity = quantity;
    }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) {
        if(availableQuantity >= 0) this.availableQuantity = availableQuantity;
    }

    // ==========================================
    // Advanced Overrides (Optional but Professional)
    // ==========================================

    @Override
    public String toString() {
        return title + " by " + author; // Helpful for debugging and Dropdowns
    }
}