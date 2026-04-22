package ui;

import db.BorrowDAO;
import model.BorrowRecord;
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

import java.time.LocalDate;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

// 🚀 ከ "extends Stage" ወደ አዲስ Component (VBox አምጪ) ተቀይሯል
public class IssueBook {
    private TextField txtBookID, txtUserID, txtDays;
    private TableView<BorrowRecord> issueTable;
    private Pagination pagination;
    private ObservableList<BorrowRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<BorrowRecord> filteredData;
    private javafx.collections.transformation.SortedList<BorrowRecord> sortedData;
    private ComboBox<Integer> cmbPageSize;
    private TextField txtSearch;
    private Label lblDueDatePreview;

    // 🚀 አሁን ሜተዱ የሚያስረክበው VBox ነው (ይህ VBox ዳሽቦርዱ መሃል ላይ ይገባል)
    public VBox getView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f4f7f6;");
        // ማስታወሻ: የ BorderPane ሎጂኩን ወደ VBox ቀይሬዋለሁ ምክንያቱም SPA ውስጥ ስንገባ ScrollPane ውስጥ ስለሚቀመጥ

        // --- 1. Header Panel ---
        HBox headerPanel = new HBox(20);
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerPanel.setMinHeight(70);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 30, 0, 30));
        headerPanel.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        Label lblHeader = new Label("📤 Issue Book to Student");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        headerPanel.getChildren().add(lblHeader);

        // --- 2. Main Layout ---
        HBox mainContainer = new HBox(30);
        mainContainer.setPadding(new Insets(30));

        // --- Left Panel: Input Form ---
        VBox formCard = new VBox(20);
        formCard.setPrefWidth(380);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 30;");
        formCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));

        txtBookID = createStyledField("📚 Scan or Type Book ID", "Scan barcode or type ID + Enter");
        // Smart lookup: search DB and show book title below field
        Label lblBookInfo = new Label();
        lblBookInfo.setFont(Font.font("Segoe UI", 12));
        lblBookInfo.setTextFill(Color.web("#3b82f6"));
        txtBookID.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !txtBookID.getText().isEmpty()) {
                lookupBookInfo(txtBookID.getText().trim(), lblBookInfo);
                txtUserID.requestFocus();
            }
        });
        HBox bookIdBox = new HBox(10, txtBookID);
        HBox.setHgrow(txtBookID, Priority.ALWAYS);
        bookIdBox.setAlignment(Pos.CENTER_LEFT);

        txtUserID = createStyledField("🎓 Scan or Type Student ID", "Scan barcode or type ID + Enter");
        // Smart lookup: search DB and show student name below field
        Label lblStudentInfo = new Label();
        lblStudentInfo.setFont(Font.font("Segoe UI", 12));
        lblStudentInfo.setTextFill(Color.web("#3b82f6"));
        txtUserID.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !txtUserID.getText().isEmpty()) {
                lookupStudentInfo(txtUserID.getText().trim(), lblStudentInfo);
                txtDays.requestFocus();
            }
        });
        HBox userIdBox = new HBox(10, txtUserID);
        HBox.setHgrow(txtUserID, Priority.ALWAYS);
        userIdBox.setAlignment(Pos.CENTER_LEFT);

        txtDays = createStyledField("⏳ Days to Borrow", "7");
        txtDays.setText("7");

        lblDueDatePreview = new Label("Expected Return: " + LocalDate.now().plusDays(7).toString());
        lblDueDatePreview.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblDueDatePreview.setTextFill(Color.web("#27ae60"));

        txtDays.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int days = Integer.parseInt(newVal);
                lblDueDatePreview.setText("Expected Return: " + LocalDate.now().plusDays(days).toString());
                lblDueDatePreview.setTextFill(Color.web("#27ae60"));
            } catch (NumberFormatException ex) {
                lblDueDatePreview.setText("Invalid number of days!");
                lblDueDatePreview.setTextFill(Color.web("#ef4444"));
            }
        });

        VBox daysBox = new VBox(5, txtDays, lblDueDatePreview);

        Button btnIssue = new Button("📤 CONFIRM ISSUE");
        btnIssue.setPrefWidth(Double.MAX_VALUE);
        btnIssue.setPrefHeight(50);
        btnIssue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnIssue.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnIssue.setOnMouseEntered(e -> btnIssue.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnIssue.setOnMouseExited(e  -> btnIssue.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnIssue.setOnAction(e -> issueBookToUser());

        formCard.getChildren().addAll(
                new Label("Issue Details"){{setFont(Font.font("System", FontWeight.BOLD, 18));}},
                createLabel("Book ID:"), bookIdBox, lblBookInfo,
                createLabel("Student ID:"), userIdBox, lblStudentInfo,
                createLabel("Duration (Days):"), daysBox,
                new Region(), btnIssue
        );

        // --- Right Panel: Table ---
        VBox tableCard = new VBox(15);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        tableCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        HBox tableControls = new HBox(15);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search by Book Title or Student Name...");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 5;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setOnAction(e -> updatePagination());

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize);

        issueTable = new TableView<>();
        setupTableColumns();
        VBox.setVgrow(issueTable, Priority.ALWAYS);

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData   = new javafx.collections.transformation.SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(issueTable.comparatorProperty());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(rec -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return rec.getBookTitle().toLowerCase().contains(lower) ||
                        rec.getStudentName().toLowerCase().contains(lower);
            });
            updatePagination();
        });

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        pagination.setMaxHeight(40);
        pagination.setMinHeight(40);
        HBox paginationBox = new HBox(pagination);
        paginationBox.setAlignment(Pos.CENTER);

        tableCard.getChildren().addAll(
                new Label("📋 Recent Issuance Records"){{setFont(Font.font("System", FontWeight.BOLD, 18));}},
                tableControls,
                issueTable,
                paginationBox
        );

        mainContainer.getChildren().addAll(formCard, tableCard);

        // 🚀 Header እና Main Content ን ወደ Root VBox እናስገባለን
        root.getChildren().addAll(headerPanel, mainContainer);

        loadIssuedBooks();
        return root;
    }

    // =========================================================
    // DataGrid Logic
    // =========================================================
    private void updatePagination() {
        int pageSize  = cmbPageSize.getValue();
        int pageCount = (int) Math.ceil((double) sortedData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        int toIndex = Math.min(pageSize, sortedData.size());
        issueTable.setItems(FXCollections.observableArrayList(sortedData.subList(0, toIndex)));
    }

    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize.getValue();
        int fromIndex = pageIndex * pageSize;
        ObservableList<BorrowRecord> source = FXCollections.observableArrayList(sortedData);
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        issueTable.setItems(fromIndex < source.size()
            ? FXCollections.observableArrayList(source.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());
        return new VBox();
    }

    private void setupTableColumns() {
        TableColumn<BorrowRecord, Integer> colId = new TableColumn<>("Record ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        TableColumn<BorrowRecord, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        TableColumn<BorrowRecord, String> colStudent = new TableColumn<>("Student Name");
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<BorrowRecord, String> colDueDate = new TableColumn<>("Due Date");
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        TableColumn<BorrowRecord, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPrint = new Button("🖨️ Print Slip");
            {
                btnPrint.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
                btnPrint.setOnAction(e -> generateIssueReceipt(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnPrint);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        issueTable.getColumns().addAll(colId, colTitle, colStudent, colDueDate, colAction);
        issueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(issueTable, Priority.ALWAYS);
    }

    private void loadIssuedBooks() {
        masterData.setAll(BorrowDAO.getIssuedBooks());
        updatePagination();
    }

    private void issueBookToUser() {
        try {
            int bookId = Integer.parseInt(txtBookID.getText());
            int userId = Integer.parseInt(txtUserID.getText());
            int days   = Integer.parseInt(txtDays.getText());

            String result = BorrowDAO.issueBook(bookId, userId, days);
            if (result.equals("Success")) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Book Issued Successfully!");
                // Clear inputs and auto-refresh table
                txtBookID.clear();
                txtUserID.clear();
                txtDays.setText("7");
                lblDueDatePreview.setText("Expected Return: " + LocalDate.now().plusDays(7).toString());
                lblDueDatePreview.setTextFill(Color.web("#27ae60"));
                loadIssuedBooks();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", result);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Check your inputs! Book ID and Student ID must be numbers.");
        }
    }

    private Button createScanButton() {
        Button btn = new Button("📷 Scan");
        btn.setPrefHeight(45);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
        return btn;
    }

    private void lookupBookInfo(String bookIdStr, Label lblBookInfo) {
        try {
            int bookId = Integer.parseInt(bookIdStr);
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    java.sql.PreparedStatement pst = conn.prepareStatement(
                        "SELECT Title, AvailableQuantity FROM Books WHERE BookID = ?");
                    pst.setInt(1, bookId);
                    java.sql.ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        String title = rs.getString("Title");
                        int avail = rs.getInt("AvailableQuantity");
                        lblBookInfo.setText("📚 Book: " + title + (avail > 0 ? "  ✅ Available" : "  ❌ Out of Stock"));
                        lblBookInfo.setTextFill(avail > 0 ? Color.web("#10b981") : Color.web("#ef4444"));
                    } else {
                        lblBookInfo.setText("❌ Book ID not found");
                        lblBookInfo.setTextFill(Color.web("#ef4444"));
                    }
                }
            }
        } catch (NumberFormatException ex) {
            lblBookInfo.setText("⚠️ Book ID must be a number");
            lblBookInfo.setTextFill(Color.web("#f59e0b"));
        } catch (java.sql.SQLException ex) {
            lblBookInfo.setText("⚠️ Database error");
            lblBookInfo.setTextFill(Color.web("#f59e0b"));
        }
    }

    private void lookupStudentInfo(String userIdStr, Label lblStudentInfo) {
        try {
            int userId = Integer.parseInt(userIdStr);
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    java.sql.PreparedStatement pst = conn.prepareStatement(
                        "SELECT FullName, Role FROM Users WHERE UserID = ?");
                    pst.setInt(1, userId);
                    java.sql.ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        String name = rs.getString("FullName");
                        String role = rs.getString("Role");
                        lblStudentInfo.setText("👤 Student: " + name + " (" + role + ")");
                        lblStudentInfo.setTextFill(Color.web("#3b82f6"));
                    } else {
                        lblStudentInfo.setText("❌ Student ID not found");
                        lblStudentInfo.setTextFill(Color.web("#ef4444"));
                    }
                }
            }
        } catch (NumberFormatException ex) {
            lblStudentInfo.setText("⚠️ Student ID must be a number");
            lblStudentInfo.setTextFill(Color.web("#f59e0b"));
        } catch (java.sql.SQLException ex) {
            lblStudentInfo.setText("⚠️ Database error");
            lblStudentInfo.setTextFill(Color.web("#f59e0b"));
        }
    }

    private void simulateBarcodeScan(TextField targetField, String dummyId, String type) {
        targetField.setText(dummyId);
        new Alert(Alert.AlertType.INFORMATION, "📷 " + type + " Barcode scanned successfully!").show();
    }

    private void generateIssueReceipt(BorrowRecord record) {
        File dir = new File("C:\\LMS_Receipts");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Receipt_" + record.getRecordId() + ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("==========================================");
            writer.println("           SMART LIBRARY LMS              ");
            writer.println("          BOOK ISSUE RECEIPT              ");
            writer.println("==========================================");
            writer.println("Receipt No: " + record.getRecordId());
            writer.println("Date Issued: " + LocalDate.now().toString());
            writer.println("------------------------------------------");
            writer.println("Student Name : " + record.getStudentName());
            writer.println("Book Title   : " + record.getBookTitle());
            writer.println("Return Due   : " + record.getDueDate());
            writer.println("------------------------------------------");
            writer.println("==========================================");
            showAlert(Alert.AlertType.INFORMATION, "Receipt Generated", "Receipt saved at:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Print Error", "Could not generate receipt.");
        }
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); return lbl;
    }

    private TextField createStyledField(String prompt, String tooltipText) {
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.setPrefHeight(45);
        tf.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        return tf;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }
}