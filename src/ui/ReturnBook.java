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
import java.time.temporal.ChronoUnit;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

// 1. extends Stage የሚለውን አጥፍተነዋል
public class ReturnBook {
    private TextField txtRecordID;
    private Label lblPenaltyDisplay;
    private Label lblDaysInfo;
    private TableView<BorrowRecord> borrowedTable;

    private Pagination pagination;
    private ObservableList<BorrowRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<BorrowRecord> filteredData;
    private javafx.collections.transformation.SortedList<BorrowRecord> sortedData;
    private ComboBox<Integer> cmbPageSize;
    private TextField txtSearch;

    private double currentPenalty = 0.0;
    private BorrowRecord selectedRecord = null;

    // 2. ዋናውን ኮንቴነር የሚይዝ Variable
    private BorderPane view;

    public ReturnBook() {
        // 3. Stage ላይ ይሰሩ የነበሩትን አጥፍተን BorderPane እንፈጥራለን
        view = new BorderPane();
        view.setStyle("-fx-background-color: #f4f7f6;");

        // --- 1. Header Panel ---
        HBox headerPanel = new HBox(20);
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerPanel.setPrefHeight(75);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 30, 0, 30));
        headerPanel.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        // ማስታወሻ: SPA ስለሆነ 'Back' በተን አያስፈልግም፣ በ AdminDashboard ሜኑ ነው የሚመለሱት

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label lblHeader = new Label("📥 Book Return & Penalty Processing");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        headerPanel.getChildren().addAll(lblHeader, headerSpacer); // Back በተን ወጥቷል
        view.setTop(headerPanel); // root ወደ view ተቀይሯል

        // --- 2. Main Layout ---
        HBox mainContainer = new HBox(30);
        mainContainer.setPadding(new Insets(30));

        // --- Left Panel: Return Processing Card ---
        VBox returnCard = new VBox(20);
        returnCard.setPrefWidth(350);
        returnCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 30;");
        returnCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));

        txtRecordID = new TextField();
        txtRecordID.setPromptText("Select from table...");
        txtRecordID.setEditable(false);
        txtRecordID.setPrefHeight(45);
        txtRecordID.setStyle("-fx-background-color: #f1f3f5; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-font-weight: bold;");

        VBox penaltyCard = new VBox(5);
        penaltyCard.setAlignment(Pos.CENTER);
        penaltyCard.setPadding(new Insets(20));
        penaltyCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-border-color: #dee2e6;");

        lblPenaltyDisplay = new Label("0.00 ETB");
        lblPenaltyDisplay.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblPenaltyDisplay.setTextFill(Color.web("#27ae60"));
        lblDaysInfo = new Label("No record selected");

        penaltyCard.getChildren().addAll(new Label("Calculated Penalty:"), lblPenaltyDisplay, lblDaysInfo);

        Button btnReturn = new Button("📥 CONFIRM RETURN");
        btnReturn.setPrefWidth(Double.MAX_VALUE);
        btnReturn.setPrefHeight(50);
        btnReturn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnReturn.setOnMouseEntered(e -> btnReturn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnReturn.setOnMouseExited(e  -> btnReturn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnReturn.setOnAction(e -> returnBookFunction());

        returnCard.getChildren().addAll(new Label("Return Details"){{setFont(Font.font("System", FontWeight.BOLD, 18));}},
                new Label("Transaction Record ID:"), txtRecordID, new Region(), penaltyCard, new Region(), btnReturn);

        // --- Right Panel: Table with Pagination & Filtering ---
        VBox tableCard = new VBox(15);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        tableCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        HBox tableControls = new HBox(15);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Live Search: Find by Title or Student...");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePagination());

        Button btnExport = new Button("📊 Export List");
        btnExport.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        btnExport.setPrefHeight(40);
        btnExport.setOnAction(e -> exportBorrowedBooks());

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize, btnExport);

        borrowedTable = new TableView<>();
        setupTableColumns();

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData   = new javafx.collections.transformation.SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(borrowedTable.comparatorProperty());
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
        VBox.setVgrow(pagination, Priority.ALWAYS);

        tableCard.getChildren().addAll(
                new Label("📋 Books Currently Issued"){{setFont(Font.font("System", FontWeight.BOLD, 18));}},
                tableControls,
                pagination
        );

        mainContainer.getChildren().addAll(returnCard, tableCard);

        view.setCenter(mainContainer); // root ወደ view ተቀይሯል

        loadBorrowedBooks();
    }

    // 4. ለ AdminDashboard ቪውውን አሳልፎ የሚሰጥ Method (SPA Logic)
    public BorderPane getView() {
        return view;
    }

    // ==========================================
    // UI Helpers & Logic (No Changes Here)
    // ==========================================
    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int fromIndex = pageIndex * pageSize;
        ObservableList<BorrowRecord> source = sortedData != null
            ? FXCollections.observableArrayList(sortedData)
            : FXCollections.observableArrayList(masterData);
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        borrowedTable.setItems(fromIndex < source.size()
            ? FXCollections.observableArrayList(source.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());
        return new VBox(borrowedTable);
    }

    private void updatePagination() {
        if (filteredData == null) filteredData = new FilteredList<>(masterData, p -> true);
        if (sortedData   == null) {
            sortedData = new javafx.collections.transformation.SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(borrowedTable.comparatorProperty());
        }
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int pageCount = (int) Math.ceil((double) sortedData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        int toIndex = Math.min(pageSize, sortedData.size());
        borrowedTable.setItems(FXCollections.observableArrayList(sortedData.subList(0, toIndex)));
    }

    private void setupTableColumns() {
        TableColumn<BorrowRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        TableColumn<BorrowRecord, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        TableColumn<BorrowRecord, String> colStudent = new TableColumn<>("Student Name");
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<BorrowRecord, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        borrowedTable.getColumns().addAll(colId, colTitle, colStudent, colDue);
        borrowedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        borrowedTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedRecord = newSel;
                txtRecordID.setText(String.valueOf(newSel.getRecordId()));
                calculatePenaltyPreview(newSel.getDueDate().split(" ")[0]);
            }
        });
    }

    private void calculatePenaltyPreview(String dueDateStr) {
        try {
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            LocalDate today = LocalDate.now();
            long daysLate = ChronoUnit.DAYS.between(dueDate, today);

            if (daysLate > 0) {
                currentPenalty = daysLate * 5.0;
                lblPenaltyDisplay.setText(String.format("%.2f ETB", currentPenalty));
                lblPenaltyDisplay.setTextFill(Color.web("#e74c3c"));
                lblDaysInfo.setText("⚠️ " + daysLate + " days overdue");
            } else {
                currentPenalty = 0.0;
                lblPenaltyDisplay.setText("0.00 ETB");
                lblPenaltyDisplay.setTextFill(Color.web("#27ae60"));
                lblDaysInfo.setText("✅ On time return");
            }
        } catch (Exception ex) {
            lblPenaltyDisplay.setText("0.00 ETB");
            lblDaysInfo.setText("Ready");
        }
    }

    private void loadBorrowedBooks() {
        masterData.setAll(BorrowDAO.getIssuedBooks());
        updatePagination();
    }

    private void returnBookFunction() {
        if (txtRecordID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a record from the table first!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Complete return process for this book?\nPenalty to be paid: " + currentPenalty + " ETB", ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().get() == ButtonType.OK) {
            int recordId = Integer.parseInt(txtRecordID.getText());
            if (BorrowDAO.returnBook(recordId, currentPenalty).equals("Success")) {

                generateReturnReceipt(recordId, currentPenalty);
                showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Book Returned Successfully!\nPenalty Collected: " + currentPenalty + " ETB");

                // Auto-refresh: reload table and reset all fields
                loadBorrowedBooks();
                txtRecordID.clear();
                lblPenaltyDisplay.setText("0.00 ETB");
                lblPenaltyDisplay.setTextFill(Color.web("#27ae60"));
                lblDaysInfo.setText("No record selected");
                currentPenalty = 0.0;
                selectedRecord = null;
                borrowedTable.getSelectionModel().clearSelection();
            }
        }
    }

    private void generateReturnReceipt(int recordId, double penaltyPaid) {
        if (selectedRecord == null) return;

        File dir = new File("C:\\LMS_Receipts");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Return_Receipt_" + recordId + ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("==========================================");
            writer.println("         BAHIR DAR UNIVERSITY LMS         ");
            writer.println("          OFFICIAL RETURN RECEIPT         ");
            writer.println("==========================================");
            writer.println("Transaction ID: " + recordId);
            writer.println("Return Date   : " + LocalDate.now().toString());
            writer.println("------------------------------------------");
            writer.println("Student Name  : " + selectedRecord.getStudentName());
            writer.println("Book Title    : " + selectedRecord.getBookTitle());
            writer.println("Due Date Was  : " + selectedRecord.getDueDate());
            writer.println("------------------------------------------");
            writer.println("Penalty Paid  : " + penaltyPaid + " ETB");
            writer.println("Status        : CLEARED");
            writer.println("==========================================");
            writer.println("       Thank you for using the LMS!       ");
        } catch (IOException ex) {
            System.out.println("Could not generate receipt.");
        }
    }

    private void exportBorrowedBooks() {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Active_Borrows_" + LocalDate.now() + ".csv");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Record ID,Book Title,Student Name,Due Date");

            ObservableList<BorrowRecord> dataToExport = filteredData != null ? filteredData : masterData;

            for (BorrowRecord record : dataToExport) {
                writer.printf("%d,%s,%s,%s\n",
                        record.getRecordId(),
                        record.getBookTitle().replace(",", " "),
                        record.getStudentName().replace(",", " "),
                        record.getDueDate()
                );
            }
            showAlert(Alert.AlertType.INFORMATION, "Export Success", "List exported successfully to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}