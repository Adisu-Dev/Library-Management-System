package ui;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 1. extends Stage የሚለውን አጥፍተነዋል
public class Reports {

    private TableView<ReportRecord> overdueTable;
    private Pagination pagination;
    private ObservableList<ReportRecord> overdueData;
    private FilteredList<ReportRecord> filteredData;
    private ComboBox<Integer> cmbPageSize;
    private TextField txtSearch;

    // 2. ዋናውን ኮንቴነር የሚይዝ Variable
    private BorderPane view;

    public Reports() {
        // 3. Stage ላይ ይሰሩ የነበሩትን (setTitle, setScene, ወዘተ) አጥፍተን BorderPane እንፈጥራለን
        view = new BorderPane();
        view.setStyle("-fx-background-color: #f4f7f6;");

        // ── Compact header bar ────────────────────────────────────────
        HBox headerPanel = new HBox(16);
        headerPanel.setStyle("-fx-background-color: #2c3e50;");
        headerPanel.setMinHeight(52); headerPanel.setMaxHeight(52);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 20, 0, 20));

        Label lblHeader = new Label("📈 Library Reports & Performance");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeader.setTextFill(Color.WHITE);

        // Compact stat pills inline with header
        Region hSp = new Region(); HBox.setHgrow(hSp, Priority.ALWAYS);

        overdueData = FXCollections.observableArrayList();
        // Load ALL active borrows — both overdue and on-time
        // The ReportRecord class calculates penalty only if overdue
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                // First: overdue records (for the Overdue tab)
                String qOverdue = "SELECT u.FullName, b.Title, br.DueDate " +
                           "FROM BorrowRecords br " +
                           "JOIN Users u ON br.UserID = u.UserID " +
                           "JOIN Books b ON br.BookID = b.BookID " +
                           "WHERE br.ReturnDate IS NULL AND br.DueDate < GETDATE() " +
                           "ORDER BY br.DueDate ASC";
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(qOverdue)) {
                    while (rs.next()) {
                        overdueData.add(new ReportRecord(
                            rs.getString("FullName"),
                            rs.getString("Title"),
                            rs.getDate("DueDate").toLocalDate()
                        ));
                    }
                }
            }
        } catch (java.sql.SQLException ex) {
            System.out.println("Reports overdue load error: " + ex.getMessage());
        }

        int totalOverdueBooks = overdueData.size();
        double totalFines = 0.0;
        for (ReportRecord record : overdueData) totalFines += record.getPenaltyAmount();

        int activeLoanCount = 0;
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                java.sql.ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM BorrowRecords WHERE ReturnDate IS NULL");
                if (rs.next()) activeLoanCount = rs.getInt(1);
            }
        } catch (java.sql.SQLException ignored) {}

        Label pill1 = createStatPill("🚨 " + totalOverdueBooks + " Overdue",   "#c0392b");
        Label pill2 = createStatPill("💰 " + String.format("%.0f", totalFines) + " ETB Fines", "#27ae60");
        Label pill3 = createStatPill("📚 " + activeLoanCount + " Active Loans", "#2980b9");

        headerPanel.getChildren().addAll(lblHeader, hSp, pill1, pill2, pill3);
        view.setTop(headerPanel);

        // ── Main content: TabPane fills all space ─────────────────────
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ── Tab 1: Overdue & Penalties ────────────────────────────────
        Tab tabOverdue = new Tab("🚨 Overdue & Penalties");

        // Compact toolbar
        HBox tableControls = new HBox(10);
        tableControls.setAlignment(Pos.CENTER_LEFT);
        tableControls.setPadding(new Insets(10, 16, 6, 16));
        tableControls.setStyle("-fx-background-color: white;");

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search by Student Name or Book...");
        txtSearch.setPrefHeight(34);
        txtSearch.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-font-size: 13px;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows:");
        lblRows.setFont(Font.font("Segoe UI", 12));
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setPrefHeight(32);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePaginationCount());

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize);

        // Table
        overdueTable = new TableView<>();
        setupOverdueTableColumns();
        overdueTable.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        filteredData = new FilteredList<>(overdueData, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(rec -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return rec.getStudentName().toLowerCase().contains(lower) ||
                        rec.getBookTitle().toLowerCase().contains(lower);
            });
            updatePaginationCount();
        });

        // Pagination pinned at bottom via BorderPane
        pagination = new Pagination();
        pagination.setMaxHeight(36); pagination.setMinHeight(36);
        pagination.setPageFactory(this::createOverduePage);
        updatePaginationCount();

        BorderPane overduePane = new BorderPane();
        overduePane.setTop(tableControls);
        overduePane.setCenter(overdueTable);
        overduePane.setBottom(pagination);
        tabOverdue.setContent(overduePane);

        // ── Tab 2: Issued Books (live from DB) ────────────────────────
        Tab tabIssued = new Tab("📤 Issued Books");
        tabIssued.setContent(buildIssuedBooksTab());

        // ── Tab 3: Returned Books (live from DB) ──────────────────────
        Tab tabReturned = new Tab("📥 Returned Books");
        tabReturned.setContent(buildReturnedBooksTab());

        tabPane.getTabs().addAll(tabOverdue, tabIssued, tabReturned);
        view.setCenter(tabPane);

        // ==========================================
        // 3. Bottom Export Panel
        // ==========================================
        HBox bottomPanel = new HBox(15);
        bottomPanel.setAlignment(Pos.CENTER_RIGHT);
        bottomPanel.setPadding(new Insets(20, 40, 20, 40));
        bottomPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        Button btnExcel = createExportBtn("📊 Export to Excel", "#27ae60");
        btnExcel.setOnAction(e -> exportRealData("Excel"));

        Button btnPDF = createExportBtn("📄 Export Report", "#c0392b");
        btnPDF.setOnAction(e -> exportRealData("Document"));

        bottomPanel.getChildren().addAll(btnExcel, btnPDF);

        // 5. root.setBottom የነበረውን ወደ view.setBottom ቀይረነዋል
        view.setBottom(bottomPanel);
    }

    // 6. ለ AdminDashboard ቪውውን አሳልፎ የሚሰጥ Method (እጅግ ጠቃሚው ክፍል)
    public BorderPane getView() {
        return view;
    }

    // ==========================================
    // UI Helpers
    // ==========================================

    private VBox createOverduePage(int pageIndex) {
        int pageSize = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredData != null ? filteredData.size() : overdueData.size());
        ObservableList<ReportRecord> sourceList = filteredData != null ? filteredData : overdueData;
        overdueTable.setItems(fromIndex < sourceList.size()
            ? FXCollections.observableArrayList(sourceList.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());
        return new VBox(); // table is in BorderPane center, not inside pagination
    }

    private void updatePaginationCount() {
        if (filteredData == null) filteredData = new FilteredList<>(overdueData, p -> true);
        int pageSize = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int pageCount = (int) Math.ceil((double) filteredData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
    }

    private void setupOverdueTableColumns() {
        overdueTable.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI';");

        TableColumn<ReportRecord, String> colStudent = new TableColumn<>("Student Name");
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<ReportRecord, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        TableColumn<ReportRecord, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        TableColumn<ReportRecord, Integer> colDays = new TableColumn<>("Days Overdue");
        colDays.setCellValueFactory(new PropertyValueFactory<>("daysOverdue"));
        TableColumn<ReportRecord, String> colPenalty = new TableColumn<>("Penalty (ETB)");
        colPenalty.setCellValueFactory(new PropertyValueFactory<>("penaltyStr"));

        // ── Mark as Paid action column ────────────────────────────────
        TableColumn<ReportRecord, Void> colAction = new TableColumn<>("Fine Status");
        colAction.setPrefWidth(160);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPaid = new Button("✅ Mark as Paid");

            {
                btnPaid.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                btnPaid.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;"
                );
                btnPaid.setOnMouseEntered(e -> btnPaid.setStyle(
                    "-fx-background-color: #059669; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;"
                ));
                btnPaid.setOnMouseExited(e -> btnPaid.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;"
                ));

                btnPaid.setOnAction(e -> {
                    ReportRecord rec = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Mark fine of " + rec.getPenaltyStr() + " for " + rec.getStudentName() + " as PAID and remove from list?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Confirm Payment");
                    confirm.setHeaderText("Fine Settlement");
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        overdueData.remove(rec);
                        updatePaginationCount();
                        showAlert(Alert.AlertType.INFORMATION, "Fine Cleared",
                            "Fine for " + rec.getStudentName() + " (" + rec.getPenaltyStr() + ") has been marked as paid and removed.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReportRecord rec = getTableView().getItems().get(getIndex());
                    // Show "Paid" label if no penalty, button if overdue
                    if (rec.getPenaltyAmount() <= 0) {
                        Label lbl = new Label("✅ No Fine");
                        lbl.setTextFill(Color.web("#10b981"));
                        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                        setGraphic(lbl);
                    } else {
                        setGraphic(btnPaid);
                    }
                }
            }
        });

        overdueTable.getColumns().addAll(colStudent, colBook, colDue, colDays, colPenalty, colAction);
        overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Label createStatPill(String text, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        l.setTextFill(Color.web(color));
        l.setStyle(
            "-fx-background-color: rgba(255,255,255,0.12);" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-padding: 4 14; -fx-border-width: 1.5;"
        );
        return l;
    }

    private Button createExportBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: white; -fx-text-fill: " + color + "; -fx-border-color: " + color + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;"));
        return btn;
    }

    private void exportRealData(String format) {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();

        String ext = format.equals("Excel") ? ".csv" : ".txt";
        File file = new File(dir, "Overdue_Report_" + LocalDate.now() + ext);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            ObservableList<ReportRecord> dataToExport = filteredData != null ? filteredData : overdueData;

            if (format.equals("Excel")) {
                writer.println("Student Name,Book Title,Due Date,Days Overdue,Penalty (ETB)");
            } else {
                writer.println("===========================================");
                writer.println("        LMS OVERDUE PENALTY REPORT         ");
                writer.println("===========================================");
            }

            for (ReportRecord record : dataToExport) {
                if (format.equals("Excel")) {
                    writer.printf("%s,%s,%s,%d,%s\n", record.getStudentName(), record.getBookTitle(), record.getDueDate(), record.getDaysOverdue(), record.getPenaltyStr());
                } else {
                    writer.printf("%s | %s | %s | %d days | %s\n", record.getStudentName(), record.getBookTitle(), record.getDueDate(), record.getDaysOverdue(), record.getPenaltyStr());
                }
            }
            showAlert(Alert.AlertType.INFORMATION, "Export Success", "Report created at: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ── Issued Books tab — live from DB ──────────────────────────────
    private VBox buildIssuedBooksTab() {
        TableView<String[]> table = new TableView<>();
        table.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        String[] headers = {"Student Name", "Book Title", "Issue Date", "Due Date", "Status"};
        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<String[], String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[col]));
            if (i == headers.length - 1) {
                tc.setCellFactory(c -> new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); return; }
                        setText(item);
                        setStyle(item.equals("Overdue")
                            ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                            : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    }
                });
            }
            table.getColumns().add(tc);
        }

        javafx.collections.ObservableList<String[]> data = FXCollections.observableArrayList();
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT u.FullName, b.Title, " +
                           "CONVERT(VARCHAR(10), br.IssueDate, 120) AS IssueDate, " +
                           "CONVERT(VARCHAR(10), br.DueDate, 120) AS DueDate, " +
                           "CASE WHEN br.DueDate < GETDATE() THEN 'Overdue' ELSE 'Active' END AS Status " +
                           "FROM BorrowRecords br " +
                           "JOIN Users u ON br.UserID = u.UserID " +
                           "JOIN Books b ON br.BookID = b.BookID " +
                           "WHERE br.ReturnDate IS NULL " +
                           "ORDER BY br.IssueDate DESC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                while (rs.next()) {
                    data.add(new String[]{
                        rs.getString("FullName"),
                        rs.getString("Title"),
                        rs.getString("IssueDate"),
                        rs.getString("DueDate"),
                        rs.getString("Status")
                    });
                }
            }
        } catch (java.sql.SQLException ex) {
            System.out.println("Issued books load error: " + ex.getMessage());
        }

        if (data.isEmpty()) table.setPlaceholder(new Label("No books are currently issued."));
        table.setItems(data);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(table);
        box.setPadding(new Insets(16));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ── Returned Books tab — live from DB ────────────────────────────
    private VBox buildReturnedBooksTab() {
        TableView<String[]> table = new TableView<>();
        table.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        String[] headers = {"Student Name", "Book Title", "Issue Date", "Return Date", "Fine Paid"};
        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<String[], String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[col]));
            if (i == headers.length - 1) {
                tc.setCellFactory(c -> new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); return; }
                        setText(item);
                        setStyle(item.equals("0.00 ETB") || item.equals("No Fine")
                            ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                            : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                });
            }
            table.getColumns().add(tc);
        }

        javafx.collections.ObservableList<String[]> data = FXCollections.observableArrayList();
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT u.FullName, b.Title, " +
                           "CONVERT(VARCHAR(10), br.IssueDate, 120) AS IssueDate, " +
                           "CONVERT(VARCHAR(10), br.ReturnDate, 120) AS ReturnDate, " +
                           "ISNULL(br.PenaltyAmount, 0) AS Penalty " +
                           "FROM BorrowRecords br " +
                           "JOIN Users u ON br.UserID = u.UserID " +
                           "JOIN Books b ON br.BookID = b.BookID " +
                           "WHERE br.ReturnDate IS NOT NULL " +
                           "ORDER BY br.ReturnDate DESC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                while (rs.next()) {
                    double penalty = rs.getDouble("Penalty");
                    data.add(new String[]{
                        rs.getString("FullName"),
                        rs.getString("Title"),
                        rs.getString("IssueDate"),
                        rs.getString("ReturnDate"),
                        penalty > 0 ? String.format("%.2f ETB", penalty) : "0.00 ETB"
                    });
                }
            }
        } catch (java.sql.SQLException ex) {
            System.out.println("Returned books load error: " + ex.getMessage());
        }

        if (data.isEmpty()) table.setPlaceholder(new Label("No books have been returned yet."));
        table.setItems(data);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(table);
        box.setPadding(new Insets(16));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
    private VBox buildSimpleReportTab(String[][] rows, String[] headers) {
        TableView<String[]> table = new TableView<>();
        table.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<String[], String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[col]));
            // Color the last column (status/fine) for visual clarity
            if (i == headers.length - 1) {
                tc.setCellFactory(c -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); return; }
                        setText(item);
                        if (item.contains("Overdue") || (item.contains("ETB") && !item.startsWith("0"))) {
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        } else if (item.equals("Active") || item.startsWith("0")) {
                            setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                });
            }
            table.getColumns().add(tc);
        }

        ObservableList<String[]> data = FXCollections.observableArrayList(rows);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(10, table);
        box.setPadding(new Insets(20));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    public static class ReportRecord {
        private String studentName;
        private String bookTitle;
        private LocalDate dueDate;
        private long daysOverdue;
        private double penaltyAmount;

        public ReportRecord(String studentName, String bookTitle, LocalDate dueDate) {
            this.studentName = studentName; this.bookTitle = bookTitle; this.dueDate = dueDate;
            LocalDate today = LocalDate.now();
            if (today.isAfter(dueDate)) {
                this.daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
                this.penaltyAmount = this.daysOverdue * 5.0;
            } else {
                this.daysOverdue = 0; this.penaltyAmount = 0.0;
            }
        }
        public String getStudentName() { return studentName; }
        public String getBookTitle() { return bookTitle; }
        public String getDueDate() { return dueDate.toString(); }
        public long getDaysOverdue() { return daysOverdue; }
        public String getPenaltyStr() { return penaltyAmount > 0 ? penaltyAmount + " ETB" : "No Penalty"; }
        public double getPenaltyAmount() { return penaltyAmount; }
    }
}