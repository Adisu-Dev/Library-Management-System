package ui;

import client.LibraryClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  DashboardView — Main UI for Distributed Library Client             ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * DISTRIBUTED CONCEPTS DEMONSTRATED IN THIS VIEW:
 * ─────────────────────────────────────────────────────────────────────
 * • All data operations go through LibraryClient → TCP → LibraryServer
 * • No direct database access from the client
 * • Demonstrates real-time shared state: borrow one book from Client A,
 *   then refresh Client B — the book shows as unavailable
 * • Error messages from the server (e.g., "already borrowed") are
 *   displayed directly to the user
 */
public class DashboardView {

    private BorderPane view;
    private final LibraryClient client;
    private final String userName;

    // Table data
    private TableView<BookRow> bookTable;
    private ObservableList<BookRow> bookData;

    // Status bar
    private Label lblStatus;

    /**
     * @param client   the connected LibraryClient
     * @param userName the display name of the current user
     */
    public DashboardView(LibraryClient client, String userName) {
        this.client   = client;
        this.userName = userName;
        buildUI();
        loadBooks(); // initial load
    }

    // ─────────────────────────────────────────────────────────────────
    // UI Construction
    // ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        view = new BorderPane();
        view.setStyle("-fx-background-color: #0f172a;");

        // ── Top bar ───────────────────────────────────────────────────
        view.setTop(buildTopBar());

        // ── Left sidebar ──────────────────────────────────────────────
        view.setLeft(buildSidebar());

        // ── Center: books table ───────────────────────────────────────
        view.setCenter(buildTableArea());

        // ── Bottom: status bar ────────────────────────────────────────
        view.setBottom(buildStatusBar());
    }

    // ── Top Bar ───────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(16, 24, 16, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        Label lblTitle = new Label("📚 Distributed Library System");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Connection indicator
        Label lblConn = new Label("🟢 Connected to " + client.getServerHost() + ":" + client.getServerPort());
        lblConn.setFont(Font.font("Segoe UI", 13));
        lblConn.setTextFill(Color.web("#10b981"));

        Label lblUser = new Label("👤 " + userName);
        lblUser.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblUser.setTextFill(Color.web("#94a3b8"));

        bar.getChildren().addAll(lblTitle, spacer, lblConn, lblUser);
        return bar;
    }

    // ── Sidebar ───────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setStyle("-fx-background-color: #1e293b;");

        Label lblMenu = new Label("OPERATIONS");
        lblMenu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblMenu.setTextFill(Color.web("#475569"));

        // ── Search ────────────────────────────────────────────────────
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search books...");
        txtSearch.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 8 12;"
        );
        txtSearch.setOnAction(e -> searchBooks(txtSearch.getText()));

        Button btnSearch = sidebarButton("🔍  Search", "#3b82f6");
        btnSearch.setOnAction(e -> searchBooks(txtSearch.getText()));

        Button btnRefresh = sidebarButton("🔄  Refresh All Books", "#0ea5e9");
        btnRefresh.setOnAction(e -> loadBooks());

        // ── Add Book ──────────────────────────────────────────────────
        Separator sep1 = styledSep();

        Label lblAdd = sectionLabel("ADD BOOK");

        TextField txtTitle  = styledField("Title");
        TextField txtAuthor = styledField("Author");
        TextField txtCat    = styledField("Category");

        Button btnAdd = sidebarButton("➕  Add Book", "#10b981");
        btnAdd.setOnAction(e -> {
            String t = txtTitle.getText().trim();
            String a = txtAuthor.getText().trim();
            String c = txtCat.getText().trim();
            if (t.isEmpty() || a.isEmpty() || c.isEmpty()) {
                setStatus("⚠ Fill in title, author, and category.", "#f59e0b");
                return;
            }
            runAsync(() -> {
                String resp = client.addBook(t, a, c);
                Platform.runLater(() -> {
                    handleResponse(resp);
                    if (resp.startsWith("SUCCESS")) {
                        txtTitle.clear(); txtAuthor.clear(); txtCat.clear();
                        loadBooks();
                    }
                });
            });
        });

        // ── Borrow Book ───────────────────────────────────────────────
        Separator sep2 = styledSep();
        Label lblBorrow = sectionLabel("BORROW / RETURN");

        TextField txtBookId   = styledField("Book ID");
        TextField txtUserId   = styledField("User ID");
        TextField txtDays     = styledField("Days (default 14)");

        Button btnBorrow = sidebarButton("📤  Borrow Book", "#8b5cf6");
        btnBorrow.setOnAction(e -> {
            try {
                int bookId = Integer.parseInt(txtBookId.getText().trim());
                int userId = Integer.parseInt(txtUserId.getText().trim());
                String daysStr = txtDays.getText().trim();
                int days = daysStr.isEmpty() ? 14 : Integer.parseInt(daysStr);

                runAsync(() -> {
                    // DISTRIBUTED CONCEPT — Mutual Exclusion:
                    // This call goes to LibraryServer.borrowBook() which is
                    // synchronized. If two clients call this simultaneously
                    // for the same book, only one will succeed.
                    String resp = client.borrowBook(bookId, userId, userName, days);
                    Platform.runLater(() -> {
                        handleResponse(resp);
                        loadBooks();
                    });
                });
            } catch (NumberFormatException ex) {
                setStatus("⚠ Book ID and User ID must be numbers.", "#f59e0b");
            }
        });

        // ── Return Book ───────────────────────────────────────────────
        TextField txtRecordId = styledField("Record ID");

        Button btnReturn = sidebarButton("📥  Return Book", "#f59e0b");
        btnReturn.setOnAction(e -> {
            try {
                int recordId = Integer.parseInt(txtRecordId.getText().trim());
                int bookId   = Integer.parseInt(txtBookId.getText().trim());
                runAsync(() -> {
                    String resp = client.returnBook(recordId, bookId);
                    Platform.runLater(() -> {
                        handleResponse(resp);
                        loadBooks();
                    });
                });
            } catch (NumberFormatException ex) {
                setStatus("⚠ Record ID and Book ID must be numbers.", "#f59e0b");
            }
        });

        // ── Concepts panel ────────────────────────────────────────────
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox conceptsBox = buildConceptsPanel();

        sidebar.getChildren().addAll(
            lblMenu, txtSearch, btnSearch, btnRefresh,
            sep1, lblAdd, txtTitle, txtAuthor, txtCat, btnAdd,
            sep2, lblBorrow,
            txtBookId, txtUserId, txtDays, btnBorrow,
            txtRecordId, btnReturn,
            spacer, conceptsBox
        );
        return sidebar;
    }

    // ── Table Area ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildTableArea() {
        VBox area = new VBox(12);
        area.setPadding(new Insets(24, 24, 0, 24));

        Label lblHeading = new Label("📖 Library Books");
        lblHeading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeading.setTextFill(Color.WHITE);

        Label lblHint = new Label(
            "Multi-Client Demo: Open run_client.bat twice and try to borrow the same book simultaneously.");
        lblHint.setFont(Font.font("Segoe UI", 12));
        lblHint.setTextFill(Color.web("#64748b"));

        bookData  = FXCollections.observableArrayList();
        bookTable = new TableView<>(bookData);
        bookTable.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        TableColumn<BookRow, Integer> colId   = col("ID",        "bookId",   60);
        TableColumn<BookRow, String>  colTitle = col("Title",     "title",    200);
        TableColumn<BookRow, String>  colAuth  = col("Author",    "author",   150);
        TableColumn<BookRow, String>  colIsbn  = col("ISBN",      "isbn",     120);
        TableColumn<BookRow, String>  colCat   = col("Category",  "category", 120);
        TableColumn<BookRow, Integer> colQty   = col("Qty",       "quantity", 60);
        TableColumn<BookRow, Integer> colAvail = col("Available", "available",80);

        bookTable.getColumns().addAll(colId, colTitle, colAuth, colIsbn, colCat, colQty, colAvail);

        // Style header
        bookTable.setStyle(bookTable.getStyle() +
            "-fx-table-header-background: #0f172a;");

        area.getChildren().addAll(lblHeading, lblHint, bookTable);
        return area;
    }

    // ── Status Bar ────────────────────────────────────────────────────

    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(10, 24, 10, 24));
        bar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1 0 0 0;");

        lblStatus = new Label("Ready — connected to server.");
        lblStatus.setFont(Font.font("Segoe UI", 12));
        lblStatus.setTextFill(Color.web("#94a3b8"));

        bar.getChildren().add(lblStatus);
        return bar;
    }

    // ── Concepts Panel ────────────────────────────────────────────────

    private VBox buildConceptsPanel() {
        VBox box = new VBox(4);
        box.setStyle(
            "-fx-background-color: rgba(56,189,248,0.06);" +
            "-fx-border-color: rgba(56,189,248,0.20);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 10 12;"
        );
        Label lbl = new Label(
            "📡 TCP Sockets\n" +
            "🧵 Thread Pool\n" +
            "🔒 Mutual Exclusion\n" +
            "📊 Shared State\n" +
            "📨 Message Protocol\n" +
            "🖥 Client-Server Model"
        );
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web("#7dd3fc"));
        box.getChildren().add(lbl);
        return box;
    }

    // ─────────────────────────────────────────────────────────────────
    // Data Operations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Loads all books from the server and populates the table.
     *
     * DISTRIBUTED CONCEPT — Message Passing:
     * Sends "LIST" command → receives "BOOKS:..." response → parses and displays.
     * Runs in a background thread to avoid blocking the JavaFX UI thread.
     */
    private void loadBooks() {
        setStatus("⏳ Loading books from server...", "#94a3b8");
        runAsync(() -> {
            String response = client.listBooks();
            Platform.runLater(() -> parseAndDisplayBooks(response));
        });
    }

    private void searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            loadBooks();
            return;
        }
        setStatus("🔍 Searching for: " + keyword, "#94a3b8");
        runAsync(() -> {
            String response = client.searchBooks(keyword);
            Platform.runLater(() -> parseAndDisplayBooks(response));
        });
    }

    /**
     * Parses the "BOOKS:..." protocol response and fills the TableView.
     *
     * Protocol format:
     *   BOOKS:id|title|author|isbn|category|qty|available\n...
     */
    private void parseAndDisplayBooks(String response) {
        bookData.clear();

        if (response == null || response.startsWith("ERROR:")) {
            setStatus("❌ " + (response != null ? response.replace("ERROR:", "") : "No response"), "#ef4444");
            return;
        }
        if ("BOOKS:EMPTY".equals(response)) {
            setStatus("ℹ No books found.", "#94a3b8");
            return;
        }

        // Strip the "BOOKS:" prefix from the first line
        String data = response.startsWith("BOOKS:") ? response.substring(6) : response;
        String[] lines = data.split("\n");
        int count = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length >= 7) {
                try {
                    bookData.add(new BookRow(
                        Integer.parseInt(f[0].trim()),
                        f[1].trim(), f[2].trim(), f[3].trim(), f[4].trim(),
                        Integer.parseInt(f[5].trim()),
                        Integer.parseInt(f[6].trim())
                    ));
                    count++;
                } catch (NumberFormatException ignored) {}
            }
        }

        setStatus("✅ Loaded " + count + " book(s) from server.", "#10b981");
    }

    private void handleResponse(String response) {
        if (response == null) {
            setStatus("❌ No response from server.", "#ef4444");
        } else if (response.startsWith("SUCCESS:")) {
            setStatus("✅ " + response.replace("SUCCESS:", ""), "#10b981");
        } else if (response.startsWith("ERROR:")) {
            setStatus("❌ " + response.replace("ERROR:", ""), "#ef4444");
        } else {
            setStatus(response, "#94a3b8");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private void setStatus(String text, String color) {
        if (lblStatus != null) {
            lblStatus.setText(text);
            lblStatus.setTextFill(Color.web(color));
        }
    }

    /** Runs a task on a daemon background thread. */
    private void runAsync(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private Button sidebarButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(38);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 6; -fx-background-radius: 6;" +
            "-fx-font-size: 12px; -fx-padding: 6 10;"
        );
        return tf;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#475569"));
        return lbl;
    }

    private Separator styledSep() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");
        return sep;
    }

    private <T> TableColumn<BookRow, T> col(String header, String property, double minWidth) {
        TableColumn<BookRow, T> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setMinWidth(minWidth);
        col.setStyle("-fx-text-fill: white; -fx-alignment: CENTER-LEFT;");
        return col;
    }

    public BorderPane getView() { return view; }

    // ─────────────────────────────────────────────────────────────────
    // Inner Model — TableView row
    // ─────────────────────────────────────────────────────────────────

    /**
     * Simple JavaFX-friendly model for a row in the books TableView.
     * Uses plain fields (not JavaFX properties) for simplicity.
     */
    public static class BookRow {
        private final int    bookId;
        private final String title;
        private final String author;
        private final String isbn;
        private final String category;
        private final int    quantity;
        private final int    available;

        public BookRow(int bookId, String title, String author, String isbn,
                       String category, int quantity, int available) {
            this.bookId    = bookId;
            this.title     = title;
            this.author    = author;
            this.isbn      = isbn;
            this.category  = category;
            this.quantity  = quantity;
            this.available = available;
        }

        public int    getBookId()    { return bookId; }
        public String getTitle()     { return title; }
        public String getAuthor()    { return author; }
        public String getIsbn()      { return isbn; }
        public String getCategory()  { return category; }
        public int    getQuantity()  { return quantity; }
        public int    getAvailable() { return available; }
    }
}
