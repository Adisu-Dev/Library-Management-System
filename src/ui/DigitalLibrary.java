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
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// 🚀 ከ "extends Stage" ወደ Component ተቀይሯል
public class DigitalLibrary {
    private TableView<EBook> eBookTable;
    private ObservableList<EBook> masterData;
    private FilteredList<EBook> filteredData;
    private Pagination pagination;
    private ComboBox<Integer> cmbPageSize;

    private TextField txtSearch;
    private HBox categoriesBox;
    private String currentCategory = "All Books";

    // 🚀 አሁን ሜተዱ VBox ነው የሚመልሰው
    public VBox getView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f4f7f6;");

        // ── 1. Compact header bar ─────────────────────────────────────
        HBox headerPanel = new HBox(16);
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerPanel.setMinHeight(56); headerPanel.setMaxHeight(56);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 20, 0, 20));
        headerPanel.setEffect(new DropShadow(4, Color.rgb(0,0,0,0.05)));

        Label lblHeader = new Label("📱 Digital E-Library");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeader.setTextFill(Color.web("#1e293b"));

        // Compact stat pills
        HBox statPills = new HBox(10);
        statPills.setAlignment(Pos.CENTER);
        statPills.getChildren().addAll(
            createStatPill("📘", "12,450 E-Books",   "#3b82f6"),
            createStatPill("🌟", "124 New Arrivals",  "#3b82f6"),
            createStatPill("⬇️", "8,942 Downloads",   "#3b82f6")
        );

        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label lblNote = new Label("✅ Free Access — no due dates, no fines");
        lblNote.setFont(Font.font("Segoe UI", 11));
        lblNote.setTextFill(Color.web("#065f46"));
        lblNote.setStyle(
            "-fx-background-color: #d1fae5;" +
            "-fx-border-color: #6ee7b7;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-padding: 5 12;"
        );

        headerPanel.getChildren().addAll(lblHeader, statPills, hSpacer, lblNote);

        // ── 2. Compact toolbar: search + chips + page size ────────────
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 20, 10, 20));
        toolbar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search by title, author, or category...");
        txtSearch.setPrefHeight(36);
        txtSearch.setPrefWidth(280);
        txtSearch.setStyle(
            "-fx-border-color: #bdc3c7; -fx-border-radius: 18;" +
            "-fx-background-radius: 18; -fx-padding: 0 14; -fx-font-size: 13px;"
        );
        txtSearch.textProperty().addListener((obs, o, nv) -> updatePagination());

        Button btnSearch = new Button("Search");
        btnSearch.setPrefHeight(36);
        btnSearch.setStyle(
            "-fx-background-color: #2980b9; -fx-text-fill: white;" +
            "-fx-border-radius: 18; -fx-background-radius: 18;" +
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 16;"
        );

        // Category chips — compact
        categoriesBox = new HBox(6);
        categoriesBox.setAlignment(Pos.CENTER_LEFT);
        Button btnAll        = createCategoryChip("All",        true);
        Button btnFiction    = createCategoryChip("Fiction",    false);
        Button btnLiterature = createCategoryChip("Literature", false);
        Button btnReligion   = createCategoryChip("Religion",   false);
        Button btnPsychology = createCategoryChip("Psychology", false);
        categoriesBox.getChildren().addAll(btnAll, btnFiction, btnLiterature, btnReligion, btnPsychology);

        Region toolSpacer = new Region(); HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        Label lblRows = new Label("Rows:");
        lblRows.setFont(Font.font("Segoe UI", 12));
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(5);
        cmbPageSize.setPrefHeight(32);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        cmbPageSize.setOnAction(e -> updatePagination());

        toolbar.getChildren().addAll(txtSearch, btnSearch, categoriesBox, toolSpacer, lblRows, cmbPageSize);

        // ── 3. Table fills ALL space, pagination pinned at bottom ────
        eBookTable = new TableView<>();
        setupTableColumns();
        eBookTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        pagination = new Pagination();
        pagination.setMaxHeight(36);
        pagination.setMinHeight(36);
        pagination.setPageFactory(this::createPage);

        // ── Refresh button + auto-refresh label ───────────────────────
        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 14;"
        );
        btnRefresh.setOnAction(e -> refreshData());

        Label lblAutoRefresh = new Label("● Auto-refresh: 30s");
        lblAutoRefresh.setFont(Font.font("Segoe UI", 11));
        lblAutoRefresh.setTextFill(Color.web("#3b82f6"));

        HBox refreshBar = new HBox(10, btnRefresh, lblAutoRefresh);
        refreshBar.setAlignment(Pos.CENTER_LEFT);
        refreshBar.setPadding(new Insets(6, 20, 4, 20));
        refreshBar.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-width: 0 0 1 0;");

        // Auto-refresh every 30 seconds
        javafx.animation.Timeline autoRefresh = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), ev -> refreshData())
        );
        autoRefresh.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autoRefresh.play();
        // Stop auto-refresh when the node is removed from scene
        refreshBar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) autoRefresh.stop();
        });

        // BorderPane: table grows in center, pagination fixed at bottom
        BorderPane tableSection = new BorderPane();
        tableSection.setCenter(eBookTable);
        tableSection.setBottom(pagination);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        // ── Chip actions ──────────────────────────────────────────────
        btnAll.setOnAction(e        -> { updateChipStyles(btnAll);        filterByCategory("All Books");   });
        btnFiction.setOnAction(e    -> { updateChipStyles(btnFiction);    filterByCategory("Fiction");     });
        btnLiterature.setOnAction(e -> { updateChipStyles(btnLiterature); filterByCategory("Literature"); });
        btnReligion.setOnAction(e   -> { updateChipStyles(btnReligion);   filterByCategory("Religion");   });
        btnPsychology.setOnAction(e -> { updateChipStyles(btnPsychology); filterByCategory("Psychology"); });

        root.getChildren().addAll(headerPanel, toolbar, refreshBar, tableSection);
        VBox.setVgrow(root, Priority.ALWAYS);

        masterData  = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(masterData, p -> true);
        loadDummyData();
        updatePagination();

        return root;
    }

    // ── Compact stat pill ─────────────────────────────────────────────
    private Label createStatPill(String icon, String text, String color) {
        Label l = new Label(icon + "  " + text);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        l.setTextFill(Color.web(color));
        l.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-padding: 4 12;" +
            "-fx-border-width: 1.5;"
        );
        return l;
    }

    // ==========================================
    // 🚀 DataGrid Features: Pagination & Filtering
    // ==========================================

    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize.getValue();
        int fromIndex = pageIndex * pageSize;
        int toIndex   = Math.min(fromIndex + pageSize, filteredData.size());

        ObservableList<EBook> pageItems = fromIndex < filteredData.size()
            ? FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList();

        eBookTable.setItems(pageItems);
        // Force all cells to re-render so Read/Download buttons always appear
        javafx.application.Platform.runLater(() -> eBookTable.refresh());
        return new VBox();
    }

    private void updatePagination() {
        filteredData.setPredicate(book -> {
            boolean matchesCategory = currentCategory.equals("All Books")
                || book.getTopic().equalsIgnoreCase(currentCategory);
            String searchText = txtSearch.getText();
            boolean matchesSearch = searchText == null || searchText.isEmpty()
                || book.getTitle().toLowerCase().contains(searchText.toLowerCase())
                || book.getAuthor().toLowerCase().contains(searchText.toLowerCase())
                || book.getTopic().toLowerCase().contains(searchText.toLowerCase());
            return matchesCategory && matchesSearch;
        });

        int pageSize  = cmbPageSize.getValue();
        int total     = filteredData.size();
        int pageCount = (int) Math.ceil((double) total / pageSize);
        if (pageCount < 1) pageCount = 1;

        // Temporarily remove factory, update count, then restore — forces page 0 to re-render
        pagination.setPageFactory(null);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(this::createPage);
    }

    /** Reload data from DB and refresh the table. */
    private void refreshData() {
        masterData.clear();
        loadDummyData();          // reloads from DB (or fallback)
        updatePagination();
    }

    public void filterByCategory(String category) {
        this.currentCategory = category;
        updatePagination();
    }

    // ==========================================
    // UI Helpers & Table Columns
    // ==========================================
    private void setupTableColumns() {
        TableColumn<EBook, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setPrefWidth(300);

        TableColumn<EBook, String> colAuthor = new TableColumn<>("Author Name");
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colAuthor.setPrefWidth(250);

        TableColumn<EBook, String> colTopic = new TableColumn<>("Category");
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));
        colTopic.setPrefWidth(150);

        // Actions column — Read & Download buttons only (Free Access label removed)
        TableColumn<EBook, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(200);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnRead     = new Button("📖 Read");
            private final Button btnDownload = new Button("⬇ Download");
            private final HBox   pane        = new HBox(8, btnRead, btnDownload);
            {
                btnRead.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
                btnDownload.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
                pane.setAlignment(Pos.CENTER_LEFT);
                btnRead.setOnAction(e -> openPDF(getTableView().getItems().get(getIndex()).getFilePath()));
                btnDownload.setOnAction(e -> downloadPDF(
                    getTableView().getItems().get(getIndex()).getFilePath(),
                    getTableView().getItems().get(getIndex()).getTitle()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        // Right-click context menu — also shows Read & Download on any row
        eBookTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<EBook> row = new javafx.scene.control.TableRow<>();
            ContextMenu ctx = new ContextMenu();
            MenuItem mRead = new MenuItem("📖  Read Now");
            MenuItem mDown = new MenuItem("⬇  Download");
            mRead.setOnAction(e -> { if (!row.isEmpty()) openPDF(row.getItem().getFilePath()); });
            mDown.setOnAction(e -> { if (!row.isEmpty()) downloadPDF(row.getItem().getFilePath(), row.getItem().getTitle()); });
            ctx.getItems().addAll(mRead, mDown);
            row.setOnContextMenuRequested(e -> { if (!row.isEmpty()) ctx.show(row, e.getScreenX(), e.getScreenY()); });
            return row;
        });

        eBookTable.getColumns().addAll(colTitle, colAuthor, colTopic, colAction);
        eBookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        eBookTable.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 15px;");
        VBox.setVgrow(eBookTable, Priority.ALWAYS);
    }

    private Button createCategoryChip(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", 12));
        String active   = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 5 14;";
        String inactive = "-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-text-fill: #374151; -fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 5 14;";
        btn.setStyle(isActive ? active : inactive);
        return btn;
    }

    private void updateChipStyles(Button selectedBtn) {
        String active   = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 5 14;";
        String inactive = "-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-text-fill: #374151; -fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 5 14;";
        for (javafx.scene.Node node : categoriesBox.getChildren()) {
            if (node instanceof Button) node.setStyle(inactive);
        }
        selectedBtn.setStyle(active);
    }

    private void openPDF(String filePath) {
        File pdfFile = new File(filePath);
        // Extract a friendly book name from the path
        String bookName = pdfFile.getName().replace(".pdf", "").replace(".PDF", "");
        if (pdfFile.exists()) {
            showAlert(Alert.AlertType.INFORMATION, "Opening E-Book", "📖 Loading eBook...");
            try {
                Desktop.getDesktop().open(pdfFile);
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Cannot Open", "Could not open the PDF: " + ex.getMessage());
            }
        } else {
            // File not at stored path — let user browse for it
            showAlert(Alert.AlertType.INFORMATION, "File Not Found",
                "📖 Opening " + bookName + "...\n\n" +
                "The e-book file was not found. Please locate it manually.\n" +
                "Make sure PDF files are placed in: C:\\LMS_EBooks\\");

            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Locate E-Book: " + bookName);
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File chosen = fc.showOpenDialog(eBookTable.getScene() != null ? eBookTable.getScene().getWindow() : null);
            if (chosen != null) {
                try {
                    Desktop.getDesktop().open(chosen);
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Cannot Open", "Could not open the PDF: " + ex.getMessage());
                }
            }
        }
    }

    private void downloadPDF(String sourcePath, String title) {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            // File not at stored path — let user browse for it
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Locate E-Book PDF to Download");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            sourceFile = fc.showOpenDialog(eBookTable.getScene() != null ? eBookTable.getScene().getWindow() : null);
            if (sourceFile == null) return;
        }
        try {
            String userHome = System.getProperty("user.home");
            File destFile = new File(userHome + "\\Downloads\\" + title + ".pdf");
            showAlert(Alert.AlertType.INFORMATION, "Downloading", "📖 Loading eBook...");
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert(Alert.AlertType.INFORMATION, "Download Complete",
                "✅ " + title + " saved to your Downloads folder.");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Download Failed", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void loadDummyData() {
        // Load e-books from the same Books table used by Admin's Manage Catalog
        // Books uploaded via UploadEBook are stored there
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT Title, Author, Category FROM Books ORDER BY Title ASC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                while (rs.next()) {
                    String title    = rs.getString("Title");
                    String author   = rs.getString("Author");
                    String category = rs.getString("Category");
                    // Build expected file path — Librarian should place PDFs in C:\LMS_EBooks\
                    String filePath = "C:\\LMS_EBooks\\" + title + ".pdf";
                    masterData.add(new EBook(title, author, category, filePath));
                }
            }
        } catch (java.sql.SQLException ex) {
            System.out.println("DigitalLibrary DB load error: " + ex.getMessage());
        }

        // If DB is empty or connection failed, fall back to sample data
        if (masterData.isEmpty()) {
            masterData.addAll(
                new EBook("Dertogada",         "Yismiake Worku", "Fiction",    "C:\\LMS_EBooks\\Dertogada.pdf"),
                new EBook("Sememen",           "Sisay Nigusu",   "Fiction",    "C:\\LMS_EBooks\\Sememen.pdf"),
                new EBook("Chibo",             "Alemayehu",      "Literature", "C:\\LMS_EBooks\\Chibo.pdf"),
                new EBook("Yebirhan Enat",     "Dn Henok",       "Religion",   "C:\\LMS_EBooks\\Yebirhan Enat.pdf"),
                new EBook("Mistire Silasse",   "EOTC",           "Religion",   "C:\\LMS_EBooks\\Mistre Silasse.pdf")
            );
        }
    }

    public static class EBook {
        private String title, author, topic, filePath;
        public EBook(String title, String author, String topic, String filePath) {
            this.title = title; this.author = author; this.topic = topic; this.filePath = filePath;
        }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getTopic() { return topic; }
        public String getFilePath() { return filePath; }
    }
}