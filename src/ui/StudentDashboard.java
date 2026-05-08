package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import db.DatabaseConnection;

import java.io.File;
import java.util.Optional;

public class StudentDashboard extends Stage {

    private final String studentName;
    private final int studentId;

    private BorderPane root;
    private ScrollPane dashboardView;
    private Button activeButton;
    private Button[] navButtons;

    private TableView<BorrowedBook> borrowedTable;
    private ObservableList<BorrowedBook> masterData;
    private Pagination pagination;
    private ComboBox<Integer> cmbPageSize;

    public StudentDashboard(String studentName, int studentId) {
        this.studentName = studentName;
        this.studentId = studentId;

        setTitle("LMS | Pro Student Portal 2026");
        setMinWidth(1150);
        setMinHeight(750);

        root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7f6;");

        // 1. Sidebar & Header Setup
        setupSidebar();

        // 2. Main Dashboard Content
        setupMainContent();

        Scene scene = new Scene(root);
        setScene(scene);
        setMaximized(true);
    }

    // ==========================================
    // 🚀 Sidebar & Header (Refactored & Added Logo)
    // ==========================================
    private void setupSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(260); sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #0f172a;");
        sidebar.setPadding(new Insets(20, 20, 20, 20));
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Navigation buttons only — profile is shown in the top header
        Button btnDashboard = createMenuButton("🏠 My Dashboard");
        Button btnBrowse    = createMenuButton("📚 Digital Library");
        Button btnHistory   = createMenuButton("⏳ Reading History");
        Button btnRequest   = createMenuButton("📬 Request to Borrow");
        Button btnSettings  = createMenuButton("⚙ Security & Alerts");

        navButtons = new Button[]{btnDashboard, btnBrowse, btnHistory, btnRequest, btnSettings};
        setActiveMenu(btnDashboard);

        btnDashboard.setOnAction(e -> { setActiveMenu(btnDashboard); refreshStudentDashboard(); });
        btnBrowse.setOnAction(e -> { setActiveMenu(btnBrowse); root.setCenter(new DigitalLibrary().getView()); });
        btnHistory.setOnAction(e -> { setActiveMenu(btnHistory); root.setCenter(new ReadingHistory(studentId).getView()); });
        btnRequest.setOnAction(e -> { setActiveMenu(btnRequest); showBorrowRequestDialog(); });
        btnSettings.setOnAction(e -> { setActiveMenu(btnSettings); DashboardShell.showProfilePopup(studentName, "Student", "🎓", studentId); });

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("LOG OUT");
        btnLogout.setMaxWidth(Double.MAX_VALUE); btnLogout.setPrefHeight(45);
        btnLogout.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnLogout.setOnAction(e -> handleLogout());

        sidebar.getChildren().addAll(btnDashboard, btnBrowse, btnHistory, btnRequest, btnSettings, spacer, btnLogout);
        root.setLeft(sidebar);

        // ── Fixed top header ──────────────────────────────────────────
        root.setTop(DashboardShell.buildHeader(studentName, "Student", "🎓", studentId));

        // ── Fixed bottom footer ───────────────────────────────────────
        root.setBottom(DashboardShell.buildFooter());
    }

    // ==========================================
    // 🚀 Main Content (Refactored)
    // ==========================================
    private void setupMainContent() {
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24, 36, 24, 36));

        // ── Welcome + date ────────────────────────────────────────────
        HBox welcomeBar = new HBox(15);
        welcomeBar.setAlignment(Pos.CENTER_LEFT);
        Label lblWelcome = new Label("Welcome back, " + studentName + " 👋");
        lblWelcome.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblWelcome.setTextFill(Color.web("#1e293b"));
        Region sp0 = new Region(); HBox.setHgrow(sp0, Priority.ALWAYS);
        Label lblDate = new Label("📅 " + java.time.LocalDate.now().toString());
        lblDate.setFont(Font.font("Segoe UI", 12));
        lblDate.setTextFill(Color.web("#64748b"));
        welcomeBar.getChildren().addAll(lblWelcome, sp0, lblDate);

        // ── KPI cards (live from DB) ───────────────────────────────────
        int booksHeld = 0, overdueCount = 0;
        double totalFine = 0;
        try {
            int[] metrics = db.StudentDAO.getMemberMetrics(studentId);
            booksHeld    = metrics[0];
            overdueCount = metrics[1];
            // Calculate total fine
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    java.sql.ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT SUM(DATEDIFF(day, DueDate, GETDATE()) * 5) AS TotalFine " +
                        "FROM BorrowRecords WHERE UserID=" + studentId +
                        " AND ReturnDate IS NULL AND DueDate < GETDATE()");
                    if (rs.next() && rs.getObject(1) != null) totalFine = rs.getDouble(1);
                }
            }
        } catch (Exception ignored) {}

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            createStudentKPI("🪪", "Your Library ID",  "#" + studentId,              "#3b82f6"),
            createStudentKPI("📚", "Books Borrowed",  String.valueOf(booksHeld),    "#1e293b"),
            createStudentKPI("⚠️", "Overdue Books",   String.valueOf(overdueCount), overdueCount > 0 ? "#ef4444" : "#10b981"),
            createStudentKPI("💰", "Total Fine",      String.format("%.0f ETB", totalFine), "#1e293b")
        );

        // ── Two-column: Active Borrows table + Quick Actions ──────────
        HBox midRow = new HBox(20);

        // Active borrows table
        VBox borrowCard = new VBox(12);
        HBox.setHgrow(borrowCard, Priority.ALWAYS);
        borrowCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 20;");
        borrowCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));

        Label lblBorrow = new Label("📋 My Active Borrows");
        lblBorrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblBorrow.setTextFill(Color.web("#1e293b"));

        borrowedTable = new TableView<>();

        // Status column with color
        TableColumn<BorrowedBook, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<BorrowedBook, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colDue.setPrefWidth(110);

        TableColumn<BorrowedBook, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setTextFill(item.equals("Overdue")
                    ? javafx.scene.paint.Color.web("#ef4444")
                    : javafx.scene.paint.Color.web("#10b981"));
                setStyle("-fx-font-weight: bold;");
            }
        });

        borrowedTable.getColumns().addAll(colTitle, colDue, colStatus);
        borrowedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        borrowedTable.setPrefHeight(220);

        // Load from DB
        masterData = FXCollections.observableArrayList();
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT b.Title, br.DueDate, " +
                    "CASE WHEN br.DueDate < GETDATE() THEN 'Overdue' ELSE 'On Time' END AS Status " +
                    "FROM BorrowRecords br JOIN Books b ON br.BookID=b.BookID " +
                    "WHERE br.UserID=? AND br.ReturnDate IS NULL ORDER BY br.DueDate ASC";
                java.sql.PreparedStatement pst = conn.prepareStatement(q);
                pst.setInt(1, studentId);
                java.sql.ResultSet rs = pst.executeQuery();
                while (rs.next()) masterData.add(new BorrowedBook(
                    rs.getString("Title"), rs.getDate("DueDate").toString(), rs.getString("Status")));
            }
        } catch (Exception ignored) {}

        if (masterData.isEmpty()) {
            borrowedTable.setPlaceholder(new Label("✅ No active borrows — you're all clear!"));
        }
        borrowedTable.setItems(masterData);

        borrowCard.getChildren().addAll(lblBorrow, borrowedTable);

        // Quick actions + fine info card
        VBox rightCol = new VBox(14);
        rightCol.setPrefWidth(280);

        // Quick actions
        VBox quickCard = new VBox(10);
        quickCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 18;");
        quickCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));
        Label lblQ = new Label("⚡ Quick Actions");
        lblQ.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblQ.setTextFill(Color.web("#1e293b"));
        quickCard.getChildren().addAll(lblQ,
            createQuickBtn("📚 Browse Digital Library", "#3b82f6",
                e -> { setActiveMenu(navButtons[1]); root.setCenter(new DigitalLibrary().getView()); }),
            createQuickBtn("⏳ View Reading History", "#8b5cf6",
                e -> { setActiveMenu(navButtons[2]); root.setCenter(new ReadingHistory(studentId).getView()); })
        );

        // Fine summary card
        VBox fineCard = new VBox(8);
        fineCard.setStyle(totalFine > 0
            ? "-fx-background-color: #fff5f5; -fx-background-radius: 14; -fx-padding: 18; -fx-border-color: #fecaca; -fx-border-radius: 14;"
            : "-fx-background-color: #f0fdf4; -fx-background-radius: 14; -fx-padding: 18; -fx-border-color: #bbf7d0; -fx-border-radius: 14;");
        Label lblFineTitle = new Label(totalFine > 0 ? "⚠️ Outstanding Fine" : "✅ No Outstanding Fines");
        lblFineTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblFineTitle.setTextFill(Color.web("#1e293b"));
        Label lblFineAmt = new Label(totalFine > 0
            ? String.format("%.0f ETB — Please return overdue books", totalFine)
            : "All your books are returned on time.");
        lblFineAmt.setFont(Font.font("Segoe UI", 12));
        lblFineAmt.setTextFill(Color.web("#64748b"));
        lblFineAmt.setWrapText(true);
        fineCard.getChildren().addAll(lblFineTitle, lblFineAmt);

        rightCol.getChildren().addAll(quickCard, fineCard);
        midRow.getChildren().addAll(borrowCard, rightCol);

        // ── Recommended books row ─────────────────────────────────────
        Label lblRec = new Label("🤖 Recommended for You");
        lblRec.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblRec.setTextFill(Color.web("#1e293b"));

        HBox recRow = new HBox(16);
        recRow.getChildren().addAll(
            createBookCard("Advanced Java",  "4.9", "#3b82f6"),
            createBookCard("Flutter Dev",    "4.8", "#10b981"),
            createBookCard("Dertogada",      "4.9", "#f59e0b"),
            createBookCard("Python AI",      "4.7", "#8b5cf6"),
            createBookCard("Clean Code",     "4.8", "#ef4444")
        );

        mainContent.getChildren().addAll(welcomeBar, kpiRow, midRow, lblRec, recRow);

        dashboardView = new ScrollPane(mainContent);
        dashboardView.setFitToWidth(true);
        dashboardView.setStyle("-fx-background-color: transparent;");
        root.setCenter(dashboardView);
    }

    /** Rebuilds the student dashboard center with fresh data from DB. */
    private void refreshStudentDashboard() {
        setupMainContent();
    }

    private VBox createStudentKPI(String icon, String title, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 4;");
        card.setEffect(new DropShadow(8, Color.rgb(0,0,0,0.05)));
        HBox.setHgrow(card, Priority.ALWAYS);
        HBox top = new HBox();
        Label lt = new Label(title); lt.setTextFill(Color.GRAY); lt.setFont(Font.font("Segoe UI", 12));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label li = new Label(icon); li.setFont(Font.font(18));
        top.getChildren().addAll(lt, sp, li);
        Label lv = new Label(value);
        lv.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lv.setTextFill(Color.web(color));
        card.getChildren().addAll(top, lv);
        return card;
    }

    private Button createQuickBtn(String text, String color,
            javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(38);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        String base  = "-fx-background-color: white; -fx-text-fill: #1e293b; -fx-border-color: " + color +
                       "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        String hover = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-color: #3b82f6" +
                       "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private void setActiveMenu(Button clickedButton) {
        this.activeButton = clickedButton;
        String normal = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        String active = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: default; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        for (Button btn : navButtons) btn.setStyle(normal);
        clickedButton.setStyle(active);
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(Double.MAX_VALUE, 48);
        btn.setAlignment(Pos.CENTER_LEFT); btn.setPadding(new Insets(0, 0, 0, 20));
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        String normal = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        String hover  = "-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 0 0 0 20; -fx-font-weight: bold;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(hover); });
        btn.setOnMouseExited(e  -> { if (btn != activeButton) btn.setStyle(normal); });
        return btn;
    }

    private VBox createBookCard(String title, String rating, String color) {
        VBox card = new VBox(8, new Label("📚"){{setFont(Font.font(36)); setStyle("-fx-text-fill: "+color+";");}},
            new Label(title){{setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); setWrapText(true); setMaxWidth(130);}},
            new Label("⭐ "+rating){{setTextFill(Color.web("#f59e0b"));}});
        card.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-cursor: hand;");
        card.setAlignment(Pos.CENTER); card.setPrefWidth(140);
        card.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.05)));
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8fafc; -fx-padding: 14; -fx-background-radius: 12; -fx-border-color: " + color + "; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-cursor: hand;"));
        return card;
    }

    /**
     * Feature 2 — Student side: "Request to Borrow" dialog.
     * Submits a record to BorrowRequests with Status='Pending'.
     * The librarian then sees it in BorrowRequestView and can Approve/Reject.
     */
    private void showBorrowRequestDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("📬 Request to Borrow a Book");
        dialog.setResizable(false);

        VBox box = new VBox(16);
        box.setPadding(new Insets(28, 32, 28, 32));
        box.setStyle("-fx-background-color: #f8fafc;");
        box.setPrefWidth(400);

        Label lblTitle = new Label("📬 Request to Borrow");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#1e293b"));

        Label lblSub = new Label("Enter the Book ID you want to borrow.\nA librarian will review and approve your request.");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web("#64748b"));
        lblSub.setWrapText(true);

        Label lblBookIdLabel = new Label("Book ID:");
        lblBookIdLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        TextField txtBookId = new TextField();
        txtBookId.setPromptText("e.g. 42");
        txtBookId.setPrefHeight(42);
        txtBookId.setStyle(
            "-fx-border-color: #e2e8f0; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-padding: 0 12; -fx-font-size: 14px;");

        // Live book-title lookup
        Label lblBookInfo = new Label();
        lblBookInfo.setFont(Font.font("Segoe UI", 12));
        lblBookInfo.setWrapText(true);
        txtBookId.textProperty().addListener((obs, o, nv) -> {
            if (nv == null || nv.trim().isEmpty()) { lblBookInfo.setText(""); return; }
            try {
                int bid = Integer.parseInt(nv.trim());
                try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                    if (conn != null) {
                        java.sql.PreparedStatement pst = conn.prepareStatement(
                            "SELECT Title, AvailableQuantity FROM Books WHERE BookID=?");
                        pst.setInt(1, bid);
                        java.sql.ResultSet rs = pst.executeQuery();
                        if (rs.next()) {
                            int avail = rs.getInt("AvailableQuantity");
                            lblBookInfo.setText("📚 " + rs.getString("Title") +
                                (avail > 0 ? "  ✅ Available" : "  ❌ Out of stock"));
                            lblBookInfo.setTextFill(avail > 0
                                ? Color.web("#10b981") : Color.web("#ef4444"));
                        } else {
                            lblBookInfo.setText("❌ Book ID not found");
                            lblBookInfo.setTextFill(Color.web("#ef4444"));
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
                lblBookInfo.setText("⚠️ Enter a numeric Book ID");
                lblBookInfo.setTextFill(Color.web("#f59e0b"));
            } catch (java.sql.SQLException ex) {
                lblBookInfo.setText("⚠️ Database error");
                lblBookInfo.setTextFill(Color.web("#f59e0b"));
            }
        });

        Label lblResult = new Label();
        lblResult.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblResult.setWrapText(true);

        Button btnSubmit = new Button("📬 Submit Request");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setPrefHeight(44);
        btnSubmit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnSubmit.setStyle(
            "-fx-background-color: #f59e0b; -fx-text-fill: #0f172a;" +
            "-fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> {
            String raw = txtBookId.getText().trim();
            if (raw.isEmpty()) {
                lblResult.setText("⚠️ Please enter a Book ID.");
                lblResult.setTextFill(Color.web("#f59e0b"));
                return;
            }
            try {
                int bookId = Integer.parseInt(raw);
                btnSubmit.setDisable(true);
                lblResult.setText("⏳ Submitting…");
                lblResult.setTextFill(Color.web("#64748b"));

                // Run DB call off the FX thread
                java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
                    String result = db.BorrowDAO.submitBorrowRequest(bookId, studentId);
                    javafx.application.Platform.runLater(() -> {
                        if ("Success".equals(result)) {
                            lblResult.setText("✅ Request submitted! A librarian will review it shortly.");
                            lblResult.setTextFill(Color.web("#10b981"));
                            // Feature 3: log the action with timestamp
                            db.ActivityLog.log(studentId,
                                "Submitted borrow request for BookID=" + bookId);
                            txtBookId.clear();
                            lblBookInfo.setText("");
                        } else {
                            lblResult.setText("❌ " + result);
                            lblResult.setTextFill(Color.web("#ef4444"));
                        }
                        btnSubmit.setDisable(false);
                    });
                });
            } catch (NumberFormatException ex) {
                lblResult.setText("⚠️ Book ID must be a number.");
                lblResult.setTextFill(Color.web("#f59e0b"));
            }
        });

        Button btnClose = new Button("Close");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setPrefHeight(38);
        btnClose.setStyle(
            "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b;" +
            "-fx-background-radius: 8; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());

        box.getChildren().addAll(
            lblTitle, lblSub,
            lblBookIdLabel, txtBookId, lblBookInfo,
            btnSubmit, lblResult, btnClose);

        dialog.setScene(new javafx.scene.Scene(box));
        dialog.centerOnScreen();
        dialog.show();
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Logout?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            AppNavigator.goToLanding(this);
        }
    }

    public static class BorrowedBook {
        private String title, dueDate, status;
        public BorrowedBook(String t, String d, String s) { title=t; dueDate=d; status=s; }
        public String getTitle() { return title; } public String getDueDate() { return dueDate; } public String getStatus() { return status; }
    }
}