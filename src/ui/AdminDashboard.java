package ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

// 🚀 ለ System Tray የሚያስፈልጉ የ AWT ላይብረሪዎች
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Toolkit;
import java.awt.AWTException;

import java.io.File;
import java.util.Optional;

public class AdminDashboard extends Stage {
    private final String adminName;
    private final int adminId;
    private TrayIcon trayIcon;
    private Button activeButton;
    private BorderPane root;
    private Button[] navButtons;
    // Nav buttons as fields so refreshDashboard() can reference them
    private Button btnBooks, btnUsers, btnReports;

    public AdminDashboard(String adminName, int adminId) {
        this.adminName = adminName;
        this.adminId   = adminId;
        setTitle("Smart Library | Enterprise Admin Control Center");
        setMinWidth(1150);
        setMinHeight(750);

        root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7f6;");

        setupSystemTray();

        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260); sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #0f172a;");

        VBox menuBox = new VBox(15);
        menuBox.setPadding(new Insets(20, 20, 20, 20));
        menuBox.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(menuBox, Priority.ALWAYS);

        Button btnDash     = createMenuButton("🎛️ Control Center");
        btnBooks    = createMenuButton("📚 Manage Catalog");
        btnUsers    = createMenuButton("👥 Manage Staff");
        btnReports  = createMenuButton("📈 Analytics & Reports");
        Button btnSettings = createMenuButton("⚙ Security Settings");

        navButtons = new Button[]{btnDash, btnBooks, btnUsers, btnReports, btnSettings};
        setActiveMenu(btnDash, navButtons);

        menuBox.getChildren().addAll(btnDash, btnBooks, btnUsers, btnReports, btnSettings);

        VBox logoutBox = new VBox();
        logoutBox.setPadding(new Insets(10, 20, 20, 20));
        Button btnLogout = new Button("LOG OUT");
        btnLogout.setPrefWidth(Double.MAX_VALUE);
        btnLogout.setPrefHeight(45);
        btnLogout.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnLogout.setOnAction(e -> handleLogout());
        logoutBox.getChildren().add(btnLogout);

        sidebar.getChildren().addAll(menuBox, logoutBox);
        root.setLeft(sidebar);
        root.setTop(DashboardShell.buildHeader(adminName, "Administrator", "👑", adminId));
        root.setBottom(DashboardShell.buildFooter());

        // Build initial dashboard content
        refreshDashboard();

        // Wire navigation buttons
        btnDash.setOnAction(e -> { setActiveMenu(btnDash, navButtons); refreshDashboard(); });
        btnBooks.setOnAction(e -> { setActiveMenu(btnBooks, navButtons); root.setCenter(new ManageBooks().getView()); });
        btnUsers.setOnAction(e -> { setActiveMenu(btnUsers, navButtons); root.setCenter(new ManageUsers().getView()); });
        btnReports.setOnAction(e -> { setActiveMenu(btnReports, navButtons); root.setCenter(new Reports().getView()); });
        btnSettings.setOnAction(e -> { setActiveMenu(btnSettings, navButtons); DashboardShell.showProfilePopup(adminName, "Administrator", "👑", adminId); });

        Scene scene = new Scene(root);
        setScene(scene);
        setMaximized(true);

        Platform.runLater(() -> showSystemTrayNotification("Welcome Admin", "LMS Enterprise System is securely running."));
    }

    /** Rebuilds the entire dashboard center with fresh data from DB. */
    private void refreshDashboard() {
        VBox mainContent = new VBox(30);
        mainContent.setPadding(new Insets(30, 40, 30, 40));
        mainContent.setAlignment(Pos.TOP_LEFT);

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Global Search: Find books, users, or transactions...");
        txtSearch.setPrefHeight(45);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setMaxWidth(800);
        txtSearch.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 25; -fx-background-radius: 25; -fx-padding: 0 20; -fx-font-size: 15px;");

        Region topSpacer = new Region(); HBox.setHgrow(topSpacer, Priority.ALWAYS);

        // ── Real server status — actually pings the DB ─────────────────
        boolean dbOnline = db.DatabaseConnection.getConnection() != null;
        Label lblServerStatus = new Label(dbOnline ? "🟢 Server: Online" : "🔴 Server: Offline");
        lblServerStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblServerStatus.setTextFill(Color.web(dbOnline ? "#10b981" : "#ef4444"));
        lblServerStatus.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 20;");
        lblServerStatus.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));
        // Tooltip explains what it means
        Tooltip.install(lblServerStatus, new Tooltip(
            dbOnline ? "SQL Server is connected and responding." : "Cannot reach SQL Server. Check if the service is running."));

        // ── Real overdue count — queries DB ───────────────────────────
        int overdueCount = 0;
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                java.sql.ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM BorrowRecords WHERE ReturnDate IS NULL AND DueDate < GETDATE()");
                if (rs.next()) overdueCount = rs.getInt(1);
            }
        } catch (java.sql.SQLException ignored) {}

        final int finalOverdue = overdueCount;
        Label lblAlert = new Label(overdueCount == 0 ? "🔔" : "🔔 " + overdueCount);
        lblAlert.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 16));
        lblAlert.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 20; -fx-text-fill: "
            + (overdueCount > 0 ? "#ef4444" : "#64748b") + "; -fx-cursor: hand;");
        lblAlert.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));
        Tooltip.install(lblAlert, new Tooltip(
            finalOverdue == 0 ? "No overdue books." : finalOverdue + " book(s) are overdue and need attention."));

        lblAlert.setOnMouseClicked(e -> {
            if (finalOverdue == 0) {
                showSystemTrayNotification("All Clear", "No overdue books at this time.");
            } else {
                showSystemTrayNotification("Overdue Alert",
                    finalOverdue + " book(s) are overdue.\nGo to Analytics & Reports to view details.");
                // Navigate to reports
                setActiveMenu(btnReports, navButtons);
                root.setCenter(new Reports().getView());
            }
        });

        topBar.getChildren().addAll(txtSearch, topSpacer, lblServerStatus, lblAlert);

        // --- Quick Actions (🚀 አሁን SPA ይጠቀማሉ) ---
        Label lblQuick = new Label("⚡ Quick Actions");
        lblQuick.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblQuick.setTextFill(Color.web("#1e293b"));

        HBox quickActionsBox = new HBox(15);
        quickActionsBox.getChildren().addAll(
                createActionBtn("+ Add New Book", "#3b82f6", e -> {
                    setActiveMenu(btnBooks, navButtons);
                    root.setCenter(new ManageBooks().getView());
                }),
                createActionBtn("+ Register Librarian", "#8b5cf6", e -> {
                    setActiveMenu(btnUsers, navButtons);
                    root.setCenter(new ManageUsers().getView());
                }),
                createActionBtn("🚫 Manage Student Access", "#ef4444", e -> {
                    setActiveMenu(btnUsers, navButtons);
                    root.setCenter(new ManageUsers().getView());
                }),
                createActionBtn("📈 View Reports", "#10b981", e -> {
                    setActiveMenu(btnReports, navButtons);
                    root.setCenter(new Reports().getView());
                })
        );

        Label lblStats = new Label("📊 System Overview");
        lblStats.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblStats.setTextFill(Color.web("#1e293b"));

        // Load live metrics from database
        int[] m = db.DashboardDAO.getMetrics();
        String totalBooks     = String.valueOf(m[0]);
        String totalMembers   = String.valueOf(m[1]);
        String totalBorrowed  = String.valueOf(m[2]);
        String totalOverdue   = String.valueOf(m[3]);

        HBox statsBox = new HBox(20);
        statsBox.getChildren().addAll(
                createKPICard("Total Catalog",       totalBooks,    "📈 Live count",       "📚", "#3b82f6"),
                createKPICard("Active Members",      totalMembers,  "📈 Registered users", "👥", "#8b5cf6"),
                createKPICard("Currently Borrowed",  totalBorrowed, "⚖️ Active loans",     "📤", "#f59e0b"),
                createKPICard("Overdue & Fines",     totalOverdue,  "🚨 Action Required",  "⚠️", "#ef4444")
        );

        HBox bottomSplit = new HBox(30);
        bottomSplit.setAlignment(Pos.TOP_LEFT);

        // ── Live Monthly Circulation Chart ────────────────────────────
        VBox chartBox = new VBox(10);
        HBox.setHgrow(chartBox, Priority.ALWAYS);
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");
        chartBox.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        Label lblChart = new Label("📊 Monthly Circulation Trends (Live)");
        lblChart.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setBarGap(4);
        barChart.setCategoryGap(20);
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Always show last 6 months — fill missing months with 0
        java.util.LinkedHashMap<String, Integer> monthMap = new java.util.LinkedHashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            String label = today.minusMonths(i).format(fmt);
            monthMap.put(label, 0);
        }

        // Load real monthly borrow counts from DB and fill into the map
        int maxVal = 1;
        try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
            if (conn != null) {
                String q = "SELECT FORMAT(IssueDate, 'MMM yyyy') AS Month, COUNT(*) AS Total " +
                           "FROM BorrowRecords " +
                           "WHERE IssueDate >= DATEADD(month, -6, GETDATE()) " +
                           "GROUP BY FORMAT(IssueDate, 'MMM yyyy'), YEAR(IssueDate), MONTH(IssueDate) " +
                           "ORDER BY YEAR(IssueDate) ASC, MONTH(IssueDate) ASC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);
                while (rs.next()) {
                    String month = rs.getString("Month");
                    int total   = rs.getInt("Total");
                    if (monthMap.containsKey(month)) monthMap.put(month, total);
                    if (total > maxVal) maxVal = total;
                }
            }
        } catch (java.sql.SQLException ignored) {}

        // Add all 6 months to the series (0 for empty months)
        for (java.util.Map.Entry<String, Integer> entry : monthMap.entrySet())
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));

        // Set upper bound — at least 5 so bars look proportional
        yAxis.setUpperBound(Math.max(maxVal + 2, 5));
        yAxis.setTickUnit(Math.max(1, (int) Math.ceil(yAxis.getUpperBound() / 5.0)));

        barChart.getData().add(series);
        barChart.setPrefHeight(300);
        chartBox.getChildren().addAll(lblChart, barChart);

        // ── Feature 3: Live Activity Log — bound to ActivityLog.getEntries() ──
        VBox feedBox = new VBox(10);
        feedBox.setMinWidth(350);
        feedBox.setPrefWidth(400);
        feedBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");
        feedBox.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        Label lblFeed = new Label("⏱️ Live Activity Log");
        lblFeed.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        // ListView bound directly to the observable list — auto-updates via Platform.runLater
        ListView<String> logView = new ListView<>(db.ActivityLog.getEntries());
        logView.setPrefHeight(280);
        logView.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-background-color: #f8fafc;");
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-padding: 6 10; -fx-border-color: transparent transparent #e2e8f0 transparent;");
                // Color-code by action type
                if (item.contains("APPROVED"))      setTextFill(Color.web("#10b981"));
                else if (item.contains("REJECTED")) setTextFill(Color.web("#ef4444"));
                else if (item.contains("borrowed")) setTextFill(Color.web("#3b82f6"));
                else if (item.contains("returned")) setTextFill(Color.web("#8b5cf6"));
                else                                setTextFill(Color.web("#1e293b"));
            }
        });
        logView.setPlaceholder(new Label("No activity yet"));

        // Load recent entries from DB on first open
        db.ActivityLog.loadRecent();

        feedBox.getChildren().addAll(lblFeed, logView);
        bottomSplit.getChildren().addAll(chartBox, feedBox);

        mainContent.getChildren().addAll(topBar, lblQuick, quickActionsBox, lblStats, statsBox, bottomSplit);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);
    }  // end refreshDashboard()

    private void setActiveMenu(Button clickedButton, Button[] allButtons) {
        this.activeButton = clickedButton;

        String normal = "-fx-background-color: transparent;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-cursor: hand;" +
                        "-fx-alignment: center-left;" +
                        "-fx-padding: 0 0 0 20;";
        String active = "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: default;" +
                        "-fx-alignment: center-left;" +
                        "-fx-padding: 0 0 0 20;";

        for (Button btn : allButtons) btn.setStyle(normal);
        clickedButton.setStyle(active);
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.io.InputStream s = AdminDashboard.class.getResourceAsStream("/images/logo.png");
            java.awt.Image image = s != null
                ? Toolkit.getDefaultToolkit().createImage(s.readAllBytes())
                : Toolkit.getDefaultToolkit().getImage("logo.png");
            trayIcon = new TrayIcon(image, "LMS Admin System");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Smart Library LMS");
            tray.add(trayIcon);
        } catch (Exception e) { System.out.println("TrayIcon error."); }
    }

    private void showSystemTrayNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setPrefHeight(48);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 0, 0, 20));
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));

        // Bright white-grey text — clearly readable on dark sidebar
        String normal = "-fx-background-color: transparent;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-cursor: hand;" +
                        "-fx-alignment: center-left;" +
                        "-fx-padding: 0 0 0 20;";
        String hover  = "-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 8;" +
                        "-fx-alignment: center-left;" +
                        "-fx-padding: 0 0 0 20;" +
                        "-fx-font-weight: bold;";

        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(hover); });
        btn.setOnMouseExited(e  -> { if (btn != activeButton) btn.setStyle(normal); });
        return btn;
    }

    private Button createActionBtn(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base  = "-fx-background-color: white; -fx-text-fill: #1e293b; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;";
        String hover = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(event);
        return btn;
    }

    private VBox createKPICard(String title, String value, String trend, String icon, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 5;");
        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox();
        Label lblTitle = new Label(title);
        lblTitle.setTextFill(Color.GRAY);
        lblTitle.setFont(Font.font("Segoe UI", 14));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblIcon = new Label(icon);
        lblIcon.setFont(Font.font("Segoe UI Emoji", 20));
        top.getChildren().addAll(lblTitle, spacer, lblIcon);

        Label lblVal = new Label(value);
        lblVal.setTextFill(Color.web("#1e293b"));
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));

        Label lblTrend = new Label(trend);
        lblTrend.setFont(Font.font("Segoe UI", 12));
        lblTrend.setTextFill(trend.contains("+") ? Color.web("#10b981") : (trend.contains("Action") ? Color.web("#ef4444") : Color.GRAY));

        card.getChildren().addAll(top, lblVal, lblTrend);
        return card;
    }

    /** Converts a DB timestamp to a human-readable "X mins ago" string. */
    private String formatTimeAgo(java.sql.Timestamp ts) {
        if (ts == null) return "—";
        long diffMs = System.currentTimeMillis() - ts.getTime();
        long mins  = diffMs / 60_000;
        long hours = mins  / 60;
        long days  = hours / 24;
        if (mins  < 1)   return "Just now";
        if (mins  < 60)  return mins  + " min" + (mins  == 1 ? "" : "s") + " ago";
        if (hours < 24)  return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        if (days  < 7)   return days  + " day"  + (days  == 1 ? "" : "s") + " ago";
        return ts.toString().substring(0, 10);
    }

    private HBox createLogItem(String dot, String msg, String time) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        Label lblDot = new Label(dot);
        Label lblMsg = new Label(msg);
        lblMsg.setFont(Font.font("Segoe UI", 13));
        lblMsg.setTextFill(Color.web("#1e293b"));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblTime = new Label(time);
        lblTime.setFont(Font.font("Segoe UI", 11));
        lblTime.setTextFill(Color.GRAY);

        row.getChildren().addAll(lblDot, lblMsg, spacer, lblTime);
        return row;
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            if (SystemTray.isSupported() && trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            // Return to the full LandingPage with the login modal on top
            AppNavigator.goToLanding(this);
        }
    }
}