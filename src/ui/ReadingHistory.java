package ui;

import javafx.application.Platform;
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
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Feature 1 — Dynamic Reading History
 *
 * Fetches borrow records from the DB on a background thread, then binds
 * the result to an ObservableList<HistoryRecord> that drives a TableView.
 * Any future call to refresh() re-fetches and the UI updates automatically
 * because the TableView is bound to the same ObservableList.
 *
 * Threading contract:
 *   - DB work runs on a single-thread executor (never blocks the FX thread).
 *   - All ObservableList mutations happen inside Platform.runLater().
 */
public class ReadingHistory {

    // ── Observable data source — TableView is bound to this list ─────
    private final ObservableList<HistoryRecord> masterData =
            FXCollections.observableArrayList();

    private FilteredList<HistoryRecord> filteredData;
    private TableView<HistoryRecord> table;
    private Pagination pagination;
    private ComboBox<Integer> cmbPageSize;
    private TextField txtSearch;
    private Label lblStatus;

    private final int studentId;
    private final BorderPane view;

    // ── Background executor — daemon so it doesn't block JVM shutdown ─
    private static final java.util.concurrent.ExecutorService BG =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ReadingHistory-Loader");
                t.setDaemon(true);
                return t;
            });

    public ReadingHistory() { this(0); }

    public ReadingHistory(int studentId) {
        this.studentId = studentId;
        view = new BorderPane();
        view.setStyle("-fx-background-color: #f4f7f6;");
        view.setCenter(buildContent());

        // Kick off the initial load
        loadFromDatabase();
    }

    public BorderPane getView() { return view; }

    // ─────────────────────────────────────────────────────────────────
    // UI construction
    // ─────────────────────────────────────────────────────────────────
    private VBox buildContent() {
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(30, 35, 35, 35));

        // ── Header row ────────────────────────────────────────────────
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label lblHeader = new Label("⏳ Your Reading Journey");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblHeader.setTextFill(Color.web("#2c3e50"));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Live status indicator
        lblStatus = new Label("⏳ Loading…");
        lblStatus.setFont(Font.font("Segoe UI", 12));
        lblStatus.setTextFill(Color.web("#64748b"));

        // Refresh button — re-fetches from DB on demand
        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
        btnRefresh.setOnAction(e -> loadFromDatabase());

        headerRow.getChildren().addAll(lblHeader, sp, lblStatus, btnRefresh);

        // ── Toolbar: search + page-size ───────────────────────────────
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search by Book Title or Status…");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle(
            "-fx-background-color: white; -fx-background-radius: 5;" +
            "-fx-padding: 0 15; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        lblRows.setFont(Font.font("Segoe UI", 14));

        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setStyle(
            "-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePagination());

        toolbar.getChildren().addAll(txtSearch, lblRows, cmbPageSize);

        // ── TableView ─────────────────────────────────────────────────
        table = new TableView<>();
        table.setStyle(
            "-fx-background-radius: 10; -fx-font-size: 14px; -fx-font-family: 'Segoe UI';");
        table.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.05)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("⏳ Loading reading history…"));

        // Book Title column
        TableColumn<HistoryRecord, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        colBook.setPrefWidth(320);

        // Issue Date column
        TableColumn<HistoryRecord, String> colIssue = new TableColumn<>("Borrowed On");
        colIssue.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        colIssue.setPrefWidth(130);

        // Return Date column
        TableColumn<HistoryRecord, String> colReturn = new TableColumn<>("Returned On");
        colReturn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        colReturn.setPrefWidth(130);

        // Status column — color-coded
        TableColumn<HistoryRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(160);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.contains("Returned"))
                    setTextFill(Color.web("#10b981"));
                else if (item.contains("Overdue"))
                    setTextFill(Color.web("#ef4444"));
                else
                    setTextFill(Color.web("#3b82f6"));
                setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            }
        });

        table.getColumns().addAll(colBook, colIssue, colReturn, colStatus);

        // ── FilteredList bound to masterData ──────────────────────────
        filteredData = new FilteredList<>(masterData, p -> true);

        // Live search — predicate updates whenever the text field changes
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(rec -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return rec.getBookTitle().toLowerCase().contains(lower)
                    || rec.getStatus().toLowerCase().contains(lower);
            });
            updatePagination();
        });

        // ── Pagination ────────────────────────────────────────────────
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        // ── Footer: export buttons ────────────────────────────────────
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnExportCSV = new Button("📊 Export to CSV");
        styleExportBtn(btnExportCSV);
        btnExportCSV.setOnAction(e -> exportData("CSV"));

        Button btnExportTxt = new Button("📄 Export to TXT");
        styleExportBtn(btnExportTxt);
        btnExportTxt.setOnAction(e -> exportData("TXT"));

        footer.getChildren().addAll(btnExportCSV, btnExportTxt);

        contentBox.getChildren().addAll(headerRow, toolbar, pagination, footer);
        return contentBox;
    }

    // ─────────────────────────────────────────────────────────────────
    // Feature 1 core: background DB fetch → Platform.runLater UI update
    // ─────────────────────────────────────────────────────────────────
    private void loadFromDatabase() {
        // Show loading state on FX thread immediately
        Platform.runLater(() -> {
            lblStatus.setText("⏳ Loading…");
            table.setPlaceholder(new Label("⏳ Fetching reading history from database…"));
        });

        BG.submit(() -> {
            // --- runs on background thread ---
            List<String[]> rows = db.BorrowDAO.getReadingHistory(studentId);

            // --- back to FX thread to update UI ---
            Platform.runLater(() -> {
                masterData.clear();
                for (String[] r : rows) {
                    // r[0]=Title, r[1]=IssueDate, r[2]=ReturnDate (or "—"), r[3]=Status
                    masterData.add(new HistoryRecord(r[0], r[1], r[2], r[3]));
                }

                if (masterData.isEmpty()) {
                    table.setPlaceholder(new Label("📚 No borrow history yet — start reading!"));
                    lblStatus.setText("✅ No records found");
                } else {
                    table.setPlaceholder(new Label("No results match your search."));
                    lblStatus.setText("✅ " + masterData.size() + " record(s) loaded");
                }

                updatePagination();
            });

            // Log the view action (safe — ActivityLog uses Platform.runLater internally)
            db.ActivityLog.log(studentId, "Viewed reading history");
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Pagination helpers
    // ─────────────────────────────────────────────────────────────────
    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int fromIndex = pageIndex * pageSize;
        int toIndex   = Math.min(fromIndex + pageSize, filteredData.size());

        table.setItems(fromIndex < filteredData.size()
            ? FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());

        return new VBox(table);
    }

    private void updatePagination() {
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int pageCount = (int) Math.ceil((double) filteredData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);

        int toIndex = Math.min(pageSize, filteredData.size());
        table.setItems(FXCollections.observableArrayList(filteredData.subList(0, toIndex)));
    }

    // ─────────────────────────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────────────────────────
    private void exportData(String format) {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();

        String ext  = format.equals("CSV") ? ".csv" : ".txt";
        File   file = new File(dir, "Reading_History_" + LocalDate.now() + ext);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            ObservableList<HistoryRecord> data =
                filteredData != null ? filteredData : masterData;

            if (format.equals("CSV")) {
                writer.println("Book Title,Borrowed On,Returned On,Status");
                for (HistoryRecord r : data) {
                    writer.printf("%s,%s,%s,%s%n",
                        r.getBookTitle().replace(",", " "),
                        r.getIssueDate(), r.getReturnDate(), r.getStatus());
                }
            } else {
                writer.println("=".repeat(70));
                writer.println("         BAHIR DAR UNIVERSITY LMS — PERSONAL READING HISTORY");
                writer.println("=".repeat(70));
                writer.printf("%-36s | %-12s | %-12s | %-16s%n",
                    "Book Title", "Borrowed On", "Returned On", "Status");
                writer.println("-".repeat(70));
                for (HistoryRecord r : data) {
                    String title = r.getBookTitle().length() > 33
                        ? r.getBookTitle().substring(0, 31) + "…" : r.getBookTitle();
                    writer.printf("%-36s | %-12s | %-12s | %-16s%n",
                        title, r.getIssueDate(), r.getReturnDate(), r.getStatus());
                }
                writer.println("=".repeat(70));
            }

            showAlert(Alert.AlertType.INFORMATION, "Export Success",
                format + " exported to:\n" + file.getAbsolutePath());

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", ex.getMessage());
        }
    }

    private void styleExportBtn(Button btn) {
        btn.setPrefHeight(40);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
            "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 0 20;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 0 20;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
            "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 0 20;"));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────
    // Model — immutable value object for each row
    // ─────────────────────────────────────────────────────────────────
    public static class HistoryRecord {
        private final String bookTitle;
        private final String issueDate;
        private final String returnDate;
        private final String status;

        public HistoryRecord(String bookTitle, String issueDate,
                             String returnDate, String status) {
            this.bookTitle  = bookTitle;
            this.issueDate  = issueDate;
            this.returnDate = returnDate != null ? returnDate : "—";
            this.status     = status;
        }

        public String getBookTitle()  { return bookTitle; }
        public String getIssueDate()  { return issueDate; }
        public String getReturnDate() { return returnDate; }
        public String getStatus()     { return status; }
    }
}
