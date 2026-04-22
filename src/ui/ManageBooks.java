package ui;

import db.BookDAO;
import model.Book;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;

// 🚀 ከ "extends Stage" ወደ Component ተቀይሯል
public class ManageBooks {
    private TextField txtTitle, txtAuthor, txtISBN, txtQuantity, txtSearch;
    private ComboBox<String> cmbCategory;
    private TableView<Book> bookTable;

    private Pagination pagination;
    private ObservableList<Book> masterData = FXCollections.observableArrayList();
    private FilteredList<Book> filteredData;
    private javafx.collections.transformation.SortedList<Book> sortedData;

    private ComboBox<Integer> cmbPageSize;
    private int selectedBookId = -1;

    // 🚀 አሁን ሜተዱ VBox ነው የሚመልሰው (ለ SPA አሰራር)
    public VBox getView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f4f7f6;");

        // --- 1. Header Panel ---
        HBox headerPanel = new HBox(20);
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerPanel.setMinHeight(70);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 30, 0, 30));
        headerPanel.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        Label lblHeader = new Label("📚 Manage Books Catalog");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblHeader.setTextFill(Color.web("#1e293b"));
        headerPanel.getChildren().add(lblHeader);

        // --- Main Content Splitter ---
        HBox mainSplit = new HBox(30);
        mainSplit.setPadding(new Insets(30));

        // --- 2. Input Card (Left Panel) ---
        VBox leftPanel = new VBox(15);
        leftPanel.setPrefWidth(360);

        VBox formCard = new VBox(15); // ክፍተቱን ትንሽ ከፈት አደረግኩት
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        formCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));

        txtTitle = createStyledField("Book Title");
        txtAuthor = createStyledField("Author Name");
        txtISBN = createStyledField("ISBN Number");

        cmbCategory = new ComboBox<>();
        cmbCategory.getItems().addAll("Programming", "Fiction", "Science", "Business", "Psychology", "Literature", "Religion");
        cmbCategory.setPromptText("Select Category");
        cmbCategory.setPrefHeight(40); cmbCategory.setMaxWidth(Double.MAX_VALUE);
        cmbCategory.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        txtQuantity = createStyledField("Stock Quantity");

        Button btnAdd = createActionBtn("ADD TO CATALOG", "#64748b");
        Button btnUpdate = createActionBtn("UPDATE RECORD", "#64748b");
        Button btnDelete = createActionBtn("DELETE BOOK", "#64748b");
        Button btnClear = createActionBtn("CLEAR FIELDS", "#64748b");

        btnAdd.setOnAction(e -> addBook());
        btnUpdate.setOnAction(e -> updateBook());
        btnDelete.setOnAction(e -> deleteBook());
        btnClear.setOnAction(e -> clearFields());

        VBox buttonsBox = new VBox(10, btnAdd, btnUpdate, btnDelete, btnClear);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));

        formCard.getChildren().addAll(
                new Label("Book Information"){{setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); setTextFill(Color.web("#1e293b"));}},
                new Label("Title"), txtTitle,
                new Label("Author"), txtAuthor,
                new Label("ISBN"), txtISBN,
                new Label("Category"), cmbCategory,
                new Label("Quantity"), txtQuantity,
                buttonsBox
        );

        // Wrap left panel in ScrollPane so buttons are always visible
        ScrollPane leftScroll = new ScrollPane(formCard);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        leftScroll.setPrefWidth(360);
        leftScroll.setMinWidth(360);
        leftPanel.getChildren().add(leftScroll);

        // --- 3. Table Area (Right Panel) ---
        VBox centerArea = new VBox(20);
        HBox.setHgrow(centerArea, Priority.ALWAYS); // ሙሉ ቦታ እንዲይዝ

        VBox tableCard = new VBox(15);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        tableCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        // 🚀 Table Controls
        HBox tableControls = new HBox(15);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Live Search: Find by title, author, or category...");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5; -fx-padding: 0 15; -fx-font-size: 14px;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePagination());

        Button btnExportExcel = new Button("📊 Export to CSV");
        btnExportExcel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btnExportExcel.setPrefHeight(40);
        btnExportExcel.setOnAction(e -> exportData("Excel"));

        Button btnExportPDF = new Button("📄 Export Text");
        btnExportPDF.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btnExportPDF.setPrefHeight(40);
        btnExportPDF.setOnAction(e -> exportData("PDF"));

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize, btnExportExcel, btnExportPDF);

        bookTable = new TableView<>();
        setupTableColumns();
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        pagination.setMaxHeight(40);
        pagination.setMinHeight(40);
        HBox paginationBox = new HBox(pagination);
        paginationBox.setAlignment(Pos.CENTER);

        tableCard.getChildren().addAll(
                new Label("📋 Book Directory"){{setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); setTextFill(Color.web("#1e293b"));}},
                tableControls,
                bookTable,
                paginationBox
        );
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        centerArea.getChildren().add(tableCard);

        mainSplit.getChildren().addAll(leftPanel, centerArea);

        // Header እና Main Content ወደ root ይገባሉ
        root.getChildren().addAll(headerPanel, mainSplit);

        setupSearchAndSortLogic();
        loadBooks();

        return root;
    }

    // =========================================================
    // DataGrid & Pagination Logic
    // =========================================================
    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize.getValue();
        int fromIndex = pageIndex * pageSize;
        javafx.collections.ObservableList<Book> source =
            sortedData != null ? FXCollections.observableArrayList(sortedData) :
                                 FXCollections.observableArrayList(masterData);
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        bookTable.setItems(fromIndex < source.size()
            ? FXCollections.observableArrayList(source.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());
        return new VBox();
    }

    private void setupTableColumns() {
        TableColumn<Book, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("bookID"));
        TableColumn<Book, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Book, String> colAuthor = new TableColumn<>("Author");
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        TableColumn<Book, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Book, Integer> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        bookTable.getColumns().addAll(colId, colTitle, colAuthor, colCat, colQty);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
            if (newSel != null) {
                selectedBookId = newSel.getBookID();
                txtTitle.setText(newSel.getTitle());
                txtAuthor.setText(newSel.getAuthor());
                txtISBN.setText(newSel.getIsbn());
                cmbCategory.setValue(newSel.getCategory());
                txtQuantity.setText(String.valueOf(newSel.getQuantity()));
            }
        });
    }

    private void setupSearchAndSortLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData   = new javafx.collections.transformation.SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(bookTable.comparatorProperty());

        txtSearch.textProperty().addListener((obs, old, nv) -> {
            filteredData.setPredicate(book -> {
                if (nv == null || nv.isEmpty()) return true;
                String filter = nv.toLowerCase();
                return book.getTitle().toLowerCase().contains(filter) ||
                        book.getAuthor().toLowerCase().contains(filter) ||
                        book.getCategory().toLowerCase().contains(filter);
            });
            updatePagination();
        });
    }

    private void updatePagination() {
        if (filteredData == null) filteredData = new FilteredList<>(masterData, p -> true);
        if (sortedData   == null) {
            sortedData = new javafx.collections.transformation.SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(bookTable.comparatorProperty());
        }
        int pageSize  = cmbPageSize.getValue();
        int pageCount = (int) Math.ceil((double) sortedData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        int toIndex = Math.min(pageSize, sortedData.size());
        bookTable.setItems(FXCollections.observableArrayList(sortedData.subList(0, toIndex)));
    }

    // =========================================================
    // Database Operations
    // =========================================================
    private void loadBooks() {
        try {
            masterData.setAll(BookDAO.getAllBooks());
            updatePagination();
            bookTable.refresh(); // force cell re-render
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Load Error"); }
    }

    private void addBook() {
        if (!validateInputs()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please fill all required fields!");
            return;
        }
        try {
            int q = Integer.parseInt(txtQuantity.getText());
            Book b = new Book(0, txtTitle.getText(), txtAuthor.getText(), txtISBN.getText(), cmbCategory.getValue(), q, q);
            if (BookDAO.addBook(b)) {
                loadBooks();   // ← auto-refresh table
                clearFields();
                showAlert(Alert.AlertType.INFORMATION, "Success!", "Book added to catalog.");
            }
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Check Quantity! Must be a number."); }
    }

    private void updateBook() {
        if (selectedBookId == -1) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a book from the table first.");
            return;
        }
        try {
            int q = Integer.parseInt(txtQuantity.getText());
            Book b = new Book(selectedBookId, txtTitle.getText(), txtAuthor.getText(), txtISBN.getText(), cmbCategory.getValue(), q, q);
            if (BookDAO.updateBook(b)) {
                loadBooks();   // ← auto-refresh table
                clearFields();
                showAlert(Alert.AlertType.INFORMATION, "Updated successfully!");
            }
        } catch (Exception e) { }
    }

    private void deleteBook() {
        if (selectedBookId == -1) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a book from the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this book completely?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().get() == ButtonType.YES) {
            if (BookDAO.deleteBook(selectedBookId)) {
                loadBooks();   // ← auto-refresh table
                clearFields();
                showAlert(Alert.AlertType.INFORMATION, "Book deleted.");
            }
        }
    }

    private void clearFields() {
        txtTitle.clear(); txtAuthor.clear(); txtISBN.clear(); txtQuantity.clear(); cmbCategory.setValue(null);
        selectedBookId = -1;
        bookTable.getSelectionModel().clearSelection();
        // Don't call loadBooks() here — callers already do it
    }

    private boolean validateInputs() {
        return !txtTitle.getText().isEmpty() && !txtAuthor.getText().isEmpty() && cmbCategory.getValue() != null && !txtQuantity.getText().isEmpty();
    }

    // =========================================================
    // Export Data
    // =========================================================
    private void exportData(String format) {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();

        if (format.equals("Excel")) {
            // ── CSV export ────────────────────────────────────────────
            File file = new File(dir, "Library_Catalog_" + LocalDate.now() + ".csv");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Book ID,Book Title,Author,ISBN,Category,Quantity");
                for (Book book : masterData) {
                    writer.printf("%d,%s,%s,%s,%s,%d\n",
                            book.getBookID(),
                            book.getTitle().replace(",", " "),
                            book.getAuthor().replace(",", " "),
                            book.getIsbn(), book.getCategory(), book.getQuantity());
                }
                showAlert(Alert.AlertType.INFORMATION, "Export Success", "CSV saved to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Export Error", ex.getMessage());
            }

        } else {
            // ── PDF export via java.awt.print ─────────────────────────
            exportAsPDF(dir);
        }
    }

    private void exportAsPDF(File dir) {
        // Build lines to print
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("SMART LIBRARY LMS — OFFICIAL CATALOG REPORT");
        lines.add("Generated: " + LocalDate.now());
        lines.add("=".repeat(70));
        lines.add(String.format("%-6s %-30s %-20s %-12s %-10s", "ID", "Title", "Author", "Category", "Qty"));
        lines.add("-".repeat(70));
        for (Book b : masterData) {
            lines.add(String.format("%-6d %-30s %-20s %-12s %-10d",
                b.getBookID(),
                truncate(b.getTitle(), 28),
                truncate(b.getAuthor(), 18),
                truncate(b.getCategory(), 10),
                b.getQuantity()));
        }
        lines.add("=".repeat(70));
        lines.add("Total books: " + masterData.size());

        printToPDF(dir, "Library_Catalog_" + LocalDate.now() + ".pdf", lines);
    }

    private void printToPDF(File dir, String filename, java.util.List<String> lines) {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        java.awt.print.PageFormat pf = job.defaultPage();
        pf.setOrientation(java.awt.print.PageFormat.LANDSCAPE);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            int linesPerPage = 50;
            int totalPages   = (int) Math.ceil((double) lines.size() / linesPerPage);
            if (pageIndex >= totalPages) return java.awt.print.Printable.NO_SUCH_PAGE;

            java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics;
            g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int lineHeight = fm.getHeight();

            double x = pageFormat.getImageableX();
            double y = pageFormat.getImageableY() + lineHeight;

            int start = pageIndex * linesPerPage;
            int end   = Math.min(start + linesPerPage, lines.size());
            for (int i = start; i < end; i++) {
                g2.drawString(lines.get(i), (float) x, (float) y);
                y += lineHeight;
            }
            return java.awt.print.Printable.PAGE_EXISTS;
        }, pf);

        // Save to file via print-to-file stream
        File pdfFile = new File(dir, filename);
        try {
            // Use print dialog — user can choose "Print to PDF" or Microsoft Print to PDF
            job.setJobName("Library Catalog Report");
            boolean doPrint = job.printDialog();
            if (doPrint) {
                job.print();
                showAlert(Alert.AlertType.INFORMATION, "Print Sent",
                    "Report sent to printer/PDF.\nTo save as PDF, choose 'Microsoft Print to PDF' or 'Save as PDF' in the print dialog.");
            }
        } catch (java.awt.print.PrinterException ex) {
            // Fallback: save formatted text file with .pdf extension
            try (PrintWriter writer = new PrintWriter(new FileWriter(pdfFile))) {
                lines.forEach(writer::println);
                showAlert(Alert.AlertType.INFORMATION, "Export Success",
                    "PDF report saved to:\n" + pdfFile.getAbsolutePath());
            } catch (Exception e2) {
                showAlert(Alert.AlertType.ERROR, "Export Error", e2.getMessage());
            }
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }

    // =========================================================
    // UI Styling Helpers
    // =========================================================
    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type); a.setContentText(msg); a.setHeaderText(type == Alert.AlertType.INFORMATION ? "Success" : "Error"); a.show();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private TextField createStyledField(String p) {
        TextField t = new TextField(); t.setPromptText(p); t.setPrefHeight(40);
        t.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 10;");
        return t;
    }

    private Button createActionBtn(String t, String c) {
        Button b = new Button(t); b.setMaxWidth(Double.MAX_VALUE); b.setPrefHeight(45);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base  = "-fx-background-color: " + c + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;";
        String hover = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }
}