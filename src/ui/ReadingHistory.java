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

// 1. extends Stage የሚለውን አጥፍተነዋል
public class ReadingHistory {

    private TableView<HistoryRecord> table;
    private Pagination pagination;
    private ObservableList<HistoryRecord> masterData;
    private FilteredList<HistoryRecord> filteredData;
    private ComboBox<Integer> cmbPageSize;
    private TextField txtSearch;

    // 2. ዋናውን ኮንቴነር የሚይዝ Variable (በ SPA የዊንዶው መጎተቻዎች አያስፈልጉንም)
    private BorderPane view;

    public ReadingHistory() {
        // 3. Stage ሎጂኮችን አጥፍተን BorderPane እንፈጥራለን
        view = new BorderPane();
        view.setStyle("-fx-background-color: #f4f7f6;"); // ከዳሽቦርዱ ጋር እንዲመሳሰል

        // ==========================================
        // Main Content Area (Custom Title Bar ሙሉ በሙሉ ወጥቷል)
        // ==========================================
        VBox contentBox = new VBox(25);
        contentBox.setPadding(new Insets(30, 35, 35, 35));

        Label lblHeader = new Label("⏳ Your Reading Journey");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblHeader.setTextFill(Color.web("#2c3e50"));

        HBox tableControls = new HBox(15);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search history by Book Title...");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-padding: 0 15; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        lblRows.setFont(Font.font("Segoe UI", 14));

        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(5);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePagination());

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize);

        table = new TableView<>();
        table.setStyle("-fx-background-radius: 10; -fx-overflow-x: hidden; -fx-font-size: 14px; -fx-font-family: 'Segoe UI';");
        table.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        TableColumn<HistoryRecord, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        colBook.setPrefWidth(350);

        TableColumn<HistoryRecord, String> colDate = new TableColumn<>("Last Read Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("lastRead"));
        colDate.setPrefWidth(200);

        TableColumn<HistoryRecord, String> colStatus = new TableColumn<>("Completion Level");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("completion"));
        colStatus.setPrefWidth(200);

        table.getColumns().addAll(colBook, colDate, colStatus);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        masterData = FXCollections.observableArrayList(
                new HistoryRecord("Advanced Java & JavaFX", "2026-04-12", "🟢 65% Completed"),
                new HistoryRecord("Introduction to Algorithms", "2026-04-10", "✅ 100% Finished"),
                new HistoryRecord("Flutter Development Guide", "2026-04-08", "🟡 15% Started"),
                new HistoryRecord("Dertogada", "2026-04-01", "✅ 100% Finished"),
                new HistoryRecord("Database Management Systems", "2026-03-25", "🟠 40% Reading"),
                new HistoryRecord("Yefikir Chemistry", "2026-03-20", "✅ 100% Finished")
        );

        filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(rec -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return rec.getBookTitle().toLowerCase().contains(newVal.toLowerCase());
            });
            updatePagination();
        });

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        updatePagination();

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnExportExcel = new Button("📊 Export to Excel");
        btnExportExcel.setPrefHeight(40);
        btnExportExcel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnExportExcel.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 0 20;");
        btnExportExcel.setOnAction(e -> exportData("Excel"));

        Button btnExportPDF = new Button("📄 Export to PDF");
        btnExportPDF.setPrefHeight(40);
        btnExportPDF.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnExportPDF.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 0 20;");
        btnExportPDF.setOnAction(e -> exportData("PDF"));

        // ማስታወሻ: "Close History" የሚለው በተን ወጥቷል (SPA ላይ ሜኑ ነው የሚነካው)

        footer.getChildren().addAll(btnExportExcel, btnExportPDF);

        contentBox.getChildren().addAll(lblHeader, tableControls, pagination, footer);

        view.setCenter(contentBox);
    }

    // 4. ለ Dashboard ቪውውን አሳልፎ የሚሰጥ Method
    public BorderPane getView() {
        return view;
    }

    private VBox createPage(int pageIndex) {
        int pageSize = cmbPageSize != null ? cmbPageSize.getValue() : 5;
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredData != null ? filteredData.size() : masterData.size());

        ObservableList<HistoryRecord> sourceList = filteredData != null ? filteredData : masterData;

        if (fromIndex < sourceList.size()) {
            table.setItems(FXCollections.observableArrayList(sourceList.subList(fromIndex, toIndex)));
        } else {
            table.setItems(FXCollections.emptyObservableList());
        }
        return new VBox(table);
    }

    private void updatePagination() {
        if (filteredData == null) filteredData = new FilteredList<>(masterData, p -> true);
        int pageSize = cmbPageSize != null ? cmbPageSize.getValue() : 5;
        int pageCount = (int) Math.ceil((double) filteredData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);

        int toIndex = Math.min(pageSize, filteredData.size());
        table.setItems(FXCollections.observableArrayList(filteredData.subList(0, toIndex)));
    }

    private void exportData(String format) {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();

        String ext = format.equals("Excel") ? ".csv" : ".txt";
        File file = new File(dir, "My_Reading_History_" + LocalDate.now() + ext);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            ObservableList<HistoryRecord> dataToExport = filteredData != null ? filteredData : masterData;

            if (format.equals("Excel")) {
                writer.println("Book Title,Last Read Date,Completion Level");
                for (HistoryRecord record : dataToExport) {
                    writer.printf("%s,%s,%s\n",
                            record.getBookTitle().replace(",", " "),
                            record.getLastRead(),
                            record.getCompletion()
                    );
                }
            } else {
                writer.println("======================================================");
                writer.println("               BAHIR DAR UNIVERSITY LMS               ");
                writer.println("              PERSONAL READING HISTORY (PDF)          ");
                writer.println("======================================================");
                writer.printf("%-35s | %-15s | %-15s\n", "Book Title", "Last Read Date", "Status");
                writer.println("------------------------------------------------------");
                for (HistoryRecord record : dataToExport) {
                    writer.printf("%-35s | %-15s | %-15s\n",
                            (record.getBookTitle().length() > 32 ? record.getBookTitle().substring(0, 30) + "..." : record.getBookTitle()),
                            record.getLastRead(),
                            record.getCompletion());
                }
                writer.println("======================================================");
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Export Success");
            a.setHeaderText(null);
            a.setContentText(format + " exported successfully to:\n" + file.getAbsolutePath());
            a.showAndWait();
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Could not export history: " + ex.getMessage());
            a.showAndWait();
        }
    }

    public static class HistoryRecord {
        private final String bookTitle;
        private final String lastRead;
        private final String completion;

        public HistoryRecord(String bookTitle, String lastRead, String completion) {
            this.bookTitle = bookTitle;
            this.lastRead = lastRead;
            this.completion = completion;
        }

        public String getBookTitle() { return bookTitle; }
        public String getLastRead() { return lastRead; }
        public String getCompletion() { return completion; }
    }
}