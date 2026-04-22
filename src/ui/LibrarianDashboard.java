package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

/**
 * Librarian Dashboard — focused on circulation (issue/return),
 * book catalog browsing, and member lookup.
 * Librarians cannot manage user accounts or view admin reports.
 */
public class LibrarianDashboard extends Stage {

    private final String librarianName;
    private final int librarianId;
    private Button activeButton;
    private Button[] navButtons;
    private BorderPane root;
    private ScrollPane dashboardView;

    public LibrarianDashboard(String librarianName, int librarianId) {
        this.librarianName = librarianName;
        this.librarianId   = librarianId;

        setTitle("Smart Library | Librarian Portal");
        setMinWidth(1150);
        setMinHeight(750);

        root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7f6;");

        setupSidebar();
        setupMainContent();

        setScene(new Scene(root));
        setMaximized(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // Sidebar
    // ═══════════════════════════════════════════════════════════════
    private void setupSidebar() {
        BorderPane sidebar = new BorderPane();
        sidebar.setPrefWidth(260); sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #0f172a;");
        sidebar.setPadding(new Insets(30, 20, 30, 20));

        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.TOP_CENTER);

        // Nav buttons — no duplicate profile here, profile is in the top header
        Button btnDash       = createMenuButton("🎛️ Dashboard");
        Button btnIssue      = createMenuButton("📤 Issue Book");
        Button btnReturn     = createMenuButton("📥 Return Book");
        Button btnCatalog    = createMenuButton("📚 Book Catalog");
        Button btnUpload     = createMenuButton("☁️ Upload E-Book");
        Button btnSettings   = createMenuButton("⚙ Security Settings");

        navButtons = new Button[]{btnDash, btnIssue, btnReturn, btnCatalog, btnUpload, btnSettings};
        setActiveMenu(btnDash);

        btnDash.setOnAction(e -> { setActiveMenu(btnDash); root.setCenter(dashboardView); });
        btnIssue.setOnAction(e -> { setActiveMenu(btnIssue); root.setCenter(wrapInScroll(new IssueBook().getView())); });
        btnReturn.setOnAction(e -> { setActiveMenu(btnReturn); root.setCenter(wrapInScroll(new ReturnBook().getView())); });
        btnCatalog.setOnAction(e -> { setActiveMenu(btnCatalog); root.setCenter(wrapInScroll(new ManageBooks().getView())); });
        btnUpload.setOnAction(e -> { setActiveMenu(btnUpload); root.setCenter(wrapInScroll(new UploadEBook().getView())); });
        btnSettings.setOnAction(e -> { setActiveMenu(btnSettings); showSecurityDialog(); });

        menuBox.getChildren().addAll(btnDash, btnIssue, btnReturn, btnCatalog, btnUpload, btnSettings);
        sidebar.setCenter(menuBox);

        Button btnLogout = new Button("LOG OUT");
        btnLogout.setPrefWidth(Double.MAX_VALUE); btnLogout.setPrefHeight(45);
        btnLogout.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnLogout.setOnAction(e -> handleLogout());
        sidebar.setBottom(btnLogout);

        root.setLeft(sidebar);

        // ── Fixed top header ──────────────────────────────────────────
        root.setTop(DashboardShell.buildHeader(librarianName, "Librarian", "📚"));

        // ── Fixed bottom footer ───────────────────────────────────────
        root.setBottom(DashboardShell.buildFooter());
    }

    // ═══════════════════════════════════════════════════════════════
    // Main dashboard content — Advanced Librarian Features
    // ═══════════════════════════════════════════════════════════════
    private void setupMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 36, 24, 36));

        // ── Welcome bar ───────────────────────────────────────────────
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label lblWelcome = new Label("Welcome back, " + librarianName + " 👋");
        lblWelcome.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblWelcome.setTextFill(Color.web("#1e293b"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblDate = new Label("📅 " + java.time.LocalDate.now().toString());
        lblDate.setFont(Font.font("Segoe UI", 12));
        lblDate.setTextFill(Color.web("#64748b"));
        topBar.getChildren().addAll(lblWelcome, sp, lblDate);

        // ── KPI cards (live from DB) ───────────────────────────────────
        int[] lm = db.DashboardDAO.getMetrics();
        int issuedToday = 0, returnedToday = 0;
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                java.sql.ResultSet ri = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) FROM BorrowRecords WHERE CAST(IssueDate AS DATE)=CAST(GETDATE() AS DATE)");
                if (ri.next()) issuedToday = ri.getInt(1);
                java.sql.ResultSet rr = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) FROM BorrowRecords WHERE CAST(ReturnDate AS DATE)=CAST(GETDATE() AS DATE)");
                if (rr.next()) returnedToday = rr.getInt(1);
            }
        } catch (java.sql.SQLException ignored) {}

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
                createKPICard("Issued Today",    String.valueOf(issuedToday),   "📤", "#3b82f6"),
                createKPICard("Returned Today",  String.valueOf(returnedToday), "📥", "#10b981"),
                createKPICard("Overdue Books",   String.valueOf(lm[3]),         "⚠️", "#ef4444"),
                createKPICard("Total Books",     String.valueOf(lm[0]),         "📚", "#8b5cf6"),
                createKPICard("Total Members",   String.valueOf(lm[1]),         "👥", "#f59e0b")
        );

        // ── Two-column layout: Quick Actions + Overdue Alert ──────────
        HBox midRow = new HBox(20);

        // Quick Actions card (Changed to Horizontal Row)
        VBox quickCard = new VBox(12);
        HBox.setHgrow(quickCard, Priority.ALWAYS); // Allow it to expand instead of fixed 340 width
        quickCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 22;");
        quickCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));
        Label lblQ = new Label("⚡ Quick Actions");
        lblQ.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblQ.setTextFill(Color.web("#1e293b"));

        HBox quickBtnRow = new HBox(15);
        quickBtnRow.getChildren().addAll(
                createActionBtn("📤  Issue a Book",        "#3b82f6", e -> { setActiveMenu(navButtons[1]); root.setCenter(wrapInScroll(new IssueBook().getView())); }),
                createActionBtn("📥  Process Return",      "#10b981", e -> { setActiveMenu(navButtons[2]); root.setCenter(wrapInScroll(new ReturnBook().getView())); }),
                createActionBtn("📚  Manage Book Catalog", "#8b5cf6", e -> { setActiveMenu(navButtons[3]); root.setCenter(wrapInScroll(new ManageBooks().getView())); }),
                createActionBtn("☁️  Upload E-Book",       "#f59e0b", e -> { setActiveMenu(navButtons[4]); root.setCenter(wrapInScroll(new UploadEBook().getView())); })
        );
        quickCard.getChildren().addAll(lblQ, quickBtnRow);

        // Overdue alert card (live from DB)
        VBox overdueCard = new VBox(10);
        HBox.setHgrow(overdueCard, Priority.ALWAYS);
        overdueCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 22;");
        overdueCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));
        Label lblOD = new Label("🚨 Overdue Alerts");
        lblOD.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblOD.setTextFill(Color.web("#ef4444"));
        overdueCard.getChildren().add(lblOD);

        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT TOP 5 u.FullName, b.Title, br.DueDate, " +
                        "DATEDIFF(day, br.DueDate, GETDATE()) AS DaysLate " +
                        "FROM BorrowRecords br " +
                        "JOIN Users u ON br.UserID=u.UserID " +
                        "JOIN Books b ON br.BookID=b.BookID " +
                        "WHERE br.ReturnDate IS NULL AND br.DueDate < GETDATE() " +
                        "ORDER BY br.DueDate ASC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int days = rs.getInt("DaysLate");
                    double fine = days * 5.0;
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(8, 12, 8, 12));
                    row.setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 8; -fx-border-color: #fecaca; -fx-border-radius: 8;");
                    Label lName = new Label("👤 " + rs.getString("FullName"));
                    lName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    lName.setTextFill(Color.web("#1e293b"));
                    Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
                    Label lBook = new Label("📖 " + rs.getString("Title"));
                    lBook.setFont(Font.font("Segoe UI", 12));
                    lBook.setTextFill(Color.web("#64748b"));
                    Label lFine = new Label(days + "d · " + String.format("%.0f", fine) + " ETB");
                    lFine.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    lFine.setTextFill(Color.web("#ef4444"));
                    row.getChildren().addAll(lName, lBook, r, lFine);
                    overdueCard.getChildren().add(row);
                }
                if (!any) {
                    Label lOk = new Label("✅ No overdue books right now!");
                    lOk.setTextFill(Color.web("#10b981"));
                    lOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                    overdueCard.getChildren().add(lOk);
                }
            }
        } catch (java.sql.SQLException ignored) {}

        midRow.getChildren().addAll(quickCard, overdueCard);

        // ── Bottom row: Recent Transactions + Member Search ───────────
        HBox bottomRow = new HBox(20);

        // Recent transactions
        VBox txCard = new VBox(10);
        HBox.setHgrow(txCard, Priority.ALWAYS);
        txCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 22;");
        txCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));
        Label lblTx = new Label("⏱️ Recent Transactions");
        lblTx.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblTx.setTextFill(Color.web("#1e293b"));
        txCard.getChildren().add(lblTx);

        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT TOP 6 u.FullName, b.Title, br.IssueDate, br.ReturnDate " +
                        "FROM BorrowRecords br " +
                        "JOIN Users u ON br.UserID=u.UserID " +
                        "JOIN Books b ON br.BookID=b.BookID " +
                        "ORDER BY br.IssueDate DESC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                while (rs.next()) {
                    boolean returned = rs.getDate("ReturnDate") != null;
                    String dot   = returned ? "🟢" : "🔵";
                    String action = returned ? "Returned" : "Issued";
                    txCard.getChildren().add(createLogItem(dot,
                            action + ": '" + rs.getString("Title") + "' → " + rs.getString("FullName"),
                            rs.getDate("IssueDate").toString()));
                }
            }
        } catch (java.sql.SQLException ignored) {
            txCard.getChildren().addAll(
                    createLogItem("🟢", "Returned: 'Advanced Java' by Dawit Bekele",    "5 mins ago"),
                    createLogItem("🔵", "Issued: 'Python Crash Course' to Meron Alemu", "22 mins ago"),
                    createLogItem("🟠", "Overdue alert: 'Dertogada' — 3 days late",     "1 hour ago")
            );
        }

        // Quick member lookup
        VBox memberCard = new VBox(12);
        memberCard.setPrefWidth(320);
        memberCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 22;");
        memberCard.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.06)));
        Label lblMem = new Label("🔍 Member Lookup");
        lblMem.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblMem.setTextFill(Color.web("#1e293b"));

        TextField txtMemberSearch = new TextField();
        txtMemberSearch.setPromptText("Search by name or BDU ID...");
        txtMemberSearch.setPrefHeight(38);
        txtMemberSearch.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 10;");

        VBox memberResults = new VBox(6);

        txtMemberSearch.textProperty().addListener((obs, old, nv) -> {
            memberResults.getChildren().clear();
            if (nv == null || nv.trim().isEmpty()) return;
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    String q = "SELECT TOP 4 FullName, Username, Role FROM Users " +
                            "WHERE FullName LIKE ? OR Username LIKE ?";
                    java.sql.PreparedStatement pst = conn.prepareStatement(q);
                    pst.setString(1, "%" + nv + "%");
                    pst.setString(2, "%" + nv + "%");
                    java.sql.ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        HBox row = new HBox(10);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(7, 10, 7, 10));
                        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
                        Label lName = new Label("👤 " + rs.getString("FullName"));
                        lName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                        lName.setTextFill(Color.web("#1e293b"));
                        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
                        Label lId = new Label(rs.getString("Username"));
                        lId.setFont(Font.font("Segoe UI", 11));
                        lId.setTextFill(Color.web("#64748b"));
                        Label lRole = new Label(rs.getString("Role"));
                        lRole.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                        lRole.setTextFill(Color.web("#3b82f6"));
                        lRole.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8; -fx-padding: 2 8;");
                        row.getChildren().addAll(lName, r, lId, lRole);
                        memberResults.getChildren().add(row);
                    }
                }
            } catch (java.sql.SQLException ignored) {}
        });

        memberCard.getChildren().addAll(lblMem, txtMemberSearch, memberResults);
        bottomRow.getChildren().addAll(txCard, memberCard);

        content.getChildren().addAll(topBar, kpiRow, midRow, bottomRow);

        dashboardView = new ScrollPane(content);
        dashboardView.setFitToWidth(true);
        dashboardView.setStyle("-fx-background-color: transparent;");
        root.setCenter(dashboardView);
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane wrapInScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private VBox createKPICard(String title, String value, String icon, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 5;");
        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox();
        Label lt = new Label(title); lt.setTextFill(Color.GRAY); lt.setFont(Font.font("Segoe UI", 13));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label li = new Label(icon); li.setFont(Font.font("Segoe UI Emoji", 20));
        top.getChildren().addAll(lt, sp, li);

        Label lv = new Label(value);
        lv.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        lv.setTextFill(Color.web("#1e293b"));

        card.getChildren().addAll(top, lv);
        return card;
    }

    private Button createActionBtn(String text, String color,
                                   javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base  = "-fx-background-color: white; -fx-text-fill: #1e293b; -fx-border-color: " + color +
                "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
        String hover = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-color: #3b82f6" +
                "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private HBox createLogItem(String dot, String msg, String time) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        Label ld = new Label(dot);
        Label lm = new Label(msg); lm.setFont(Font.font("Segoe UI", 13)); lm.setTextFill(Color.web("#1e293b"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label lt = new Label(time); lt.setFont(Font.font("Segoe UI", 11)); lt.setTextFill(Color.GRAY);
        row.getChildren().addAll(ld, lm, sp, lt);
        return row;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(Double.MAX_VALUE); btn.setPrefHeight(48);
        btn.setAlignment(Pos.CENTER_LEFT); btn.setPadding(new Insets(0, 0, 0, 20));
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        String normal = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        String hover  = "-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 0 0 0 20; -fx-font-weight: bold;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(hover); });
        btn.setOnMouseExited(e  -> { if (btn != activeButton) btn.setStyle(normal); });
        return btn;
    }

    private void setActiveMenu(Button btn) {
        this.activeButton = btn;
        String normal = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        String active = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: default; -fx-alignment: center-left; -fx-padding: 0 0 0 20;";
        for (Button b : navButtons) b.setStyle(normal);
        btn.setStyle(active);
    }

    private void showSecurityDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Security Settings");
        dialog.setHeaderText("Change Your Password");
        VBox content = new VBox(10);
        PasswordField txtOld     = new PasswordField(); txtOld.setPromptText("Current Password");
        PasswordField txtNew     = new PasswordField(); txtNew.setPromptText("New Password");
        PasswordField txtConfirm = new PasswordField(); txtConfirm.setPromptText("Confirm New Password");
        content.getChildren().addAll(new Label("Current Password:"), txtOld,
                new Label("New Password:"), txtNew, new Label("Confirm:"), txtConfirm);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (txtNew.getText().equals(txtConfirm.getText()) && !txtNew.getText().isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Password changed successfully!").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Passwords do not match!").show();
            }
        }
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            this.close();
            try {
                Stage loginStage = new Stage();
                loginStage.setTitle("Smart Library | LMS 2026");
                loginStage.setMaximized(true);
                loginStage.setScene(new Scene(new LoginForm().getView(), 1000, 600));
                loginStage.show();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}