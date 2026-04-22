package model;

/**
 * Advanced Model for Borrowing Transactions.
 * Supports Master-Detail view and Penalty tracking.
 */
public class BorrowRecord {
    private int recordId;
    private String bookTitle;
    private String studentName;
    private String issueDate;
    private String dueDate;
    private String returnDate; // 🚀 New: To track when it was returned
    private double penaltyAmount; // 🚀 New: For Requirement #34 (Penalty)

    // Constructor for TableView summary
    public BorrowRecord(int recordId, String bookTitle, String studentName, String issueDate, String dueDate) {
        this.recordId = recordId;
        this.bookTitle = bookTitle;
        this.studentName = studentName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    // Full Constructor for detailed records and reporting
    public BorrowRecord(int recordId, String bookTitle, String studentName, String issueDate, String dueDate, String returnDate, double penaltyAmount) {
        this.recordId = recordId;
        this.bookTitle = bookTitle;
        this.studentName = studentName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.penaltyAmount = penaltyAmount;
    }

    // ==========================================
    // Getters and Setters (Enables DataGrid Editing)
    // ==========================================

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public double getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(double penaltyAmount) { this.penaltyAmount = penaltyAmount; }
}