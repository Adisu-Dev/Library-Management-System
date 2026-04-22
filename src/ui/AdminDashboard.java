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
    // 🚀 Warning ለማጥፋት 'final' ተጨምሯል
    private final String adminName;
    private TrayIcon trayIcon;

    private Button activeButton;

    public AdminDashboard(String adminName) {
        this.adminName = adminName;
        setTitle("Smart Library | Enterprise Admin Control Center");
        setMinWidth(1150);
        setMinHeight(750);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7f6;");

        setupSystemTray();

        // ==========================================
        // 1. Sidebar — Logout always pinned at bottom
        // ==========================================
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260); sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #0f172a;");

        // ── Top section: nav buttons only (profile is in the top header) ──
        VBox menuBox = new VBox(15);
        menuBox.setPadding(new Insets(20, 20, 20, 20));
        menuBox.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(menuBox, Priority.ALWAYS);

        Button btnDash     = createMenuButton("🎛️ Control Center");
        Button btnBooks    = createMenuButton("📚 Manage Catalog");
        Button btnUsers    = createMenuButton("👥 Manage Staff");
        Button btnReports  = createMenuButton("📈 Analytics & Reports");
        Button btnSettings = createMenuButton("⚙ Security Settings");

        Button[] navButtons = {btnDash, btnBooks, btnUsers, btnReports, btnSettings};
        setActiveMenu(btnDash, navButtons);

        menuBox.getChildren().addAll(btnDash, btnBooks, btnUsers, btnReports, btnSettings);

        // ── Logout pinned at bottom ───────────────────────────────────
        VBox logoutBox = new VBox();
        logoutBox.setPadding(new Insets(10, 20, 20, 20));

        Button btnLogout = new Button("LOG OUT");
        btnLogout.setPrefWidth(Double.MAX_VALUE);
        btnLogout.setPrefHeight(45);
        btnLogout.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnLogout.setOnAction(e -> handleLogout());
        logoutBox.getChildren().add(btnLogout);

        sidebar.getChildren().addAll(menuBox, logoutBox);
        root.setLeft(sidebar);

        // ── Fixed top header ──────────────────────────────────────────
        root.setTop(DashboardShell.buildHeader(adminName, "Administrator", "👑"));

        // ── Fixed bottom footer ───────────────────────────────────────
        root.setBottom(DashboardShell.buildFooter());

        // ==========================================
        // 2. Main Content Area (Responsive)
        // ==========================================
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

        Label lblServerStatus = new Label("🟢 Server: Online");
        lblServerStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblServerStatus.setTextFill(Color.web("#10b981"));
        lblServerStatus.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 20;");
        lblServerStatus.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        Label lblAlert = new Label("🔔 2");
        lblAlert.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 16));
        lblAlert.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 20; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        lblAlert.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        lblAlert.setOnMouseClicked(e -> showSystemTrayNotification("System Alert", "You have 2 pending return approvals."));

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

        VBox chartBox = new VBox(10);
        HBox.setHgrow(chartBox, Priority.ALWAYS);
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");
        chartBox.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        Label lblChart = new Label("Monthly Circulation Trends");
        lblChart.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 120));
        series.getData().add(new XYChart.Data<>("Feb", 185));
        series.getData().add(new XYChart.Data<>("Mar", 150));
        series.getData().add(new XYChart.Data<>("Apr", 210));
        barChart.getData().add(series);
        barChart.setPrefHeight(300);

        chartBox.getChildren().addAll(lblChart, barChart);

        VBox feedBox = new VBox(15);
        feedBox.setMinWidth(350);
        feedBox.setPrefWidth(400);
        feedBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");
        feedBox.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        Label lblFeed = new Label("⏱️ Live Activity Log");
        lblFeed.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        VBox logs = new VBox(10);
        logs.getChildren().addAll(
                createLogItem("🟢", "Adisu Dereje returned 'Dertogada'", "10 mins ago"),
                createLogItem("🔵", "New student 'Aweke Goshu' registered", "1 hour ago"),
                createLogItem("🟠", "Dawit borrowed 'Advanced Java'", "2 hours ago"),
                createLogItem("🔴", "System backed up automatically", "Yesterday")
        );

        feedBox.getChildren().addAll(lblFeed, logs);
        bottomSplit.getChildren().addAll(chartBox, feedBox);

        mainContent.getChildren().addAll(topBar, lblQuick, quickActionsBox, lblStats, statsBox, bottomSplit);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        // ==========================================
        // 🚀 Menu Actions (SPA Logic Implementation)
        // ==========================================
        btnDash.setOnAction(e -> {
            setActiveMenu(btnDash, navButtons);
            root.setCenter(scrollPane);
        });

        btnBooks.setOnAction(e -> {
            setActiveMenu(btnBooks, navButtons);
            root.setCenter(new ManageBooks().getView());
        });

        btnUsers.setOnAction(e -> {
            setActiveMenu(btnUsers, navButtons);
            root.setCenter(new ManageUsers().getView());
        });

        btnReports.setOnAction(e -> {
            setActiveMenu(btnReports, navButtons);
            root.setCenter(new Reports().getView());
        });

        btnSettings.setOnAction(e -> {
            setActiveMenu(btnSettings, navButtons);
            showSecuritySettingsDialog();
        });

        Scene scene = new Scene(root);
        setScene(scene);
        setMaximized(true);

        Platform.runLater(() -> showSystemTrayNotification("Welcome Admin", "LMS Enterprise System is securely running."));
    }

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
            java.awt.Image image = Toolkit.getDefaultToolkit().getImage("src/images/logo.png");
            trayIcon = new TrayIcon(image, "LMS Admin System");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Smart Library LMS");
            tray.add(trayIcon);
        } catch (AWTException e) { System.out.println("TrayIcon error."); }
    }

    private void showSystemTrayNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private void showSecuritySettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Admin Security Settings");
        dialog.setHeaderText("Change Administrator Password");
        VBox content = new VBox(10);
        PasswordField txtOld = new PasswordField(); txtOld.setPromptText("Current Password");
        PasswordField txtNew = new PasswordField(); txtNew.setPromptText("New Strong Password");
        PasswordField txtConfirm = new PasswordField(); txtConfirm.setPromptText("Confirm New Password");
        content.getChildren().addAll(new Label("Current Password:"), txtOld, new Label("New Password:"), txtNew, new Label("Confirm Password:"), txtConfirm);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (txtNew.getText().equals(txtConfirm.getText()) && !txtNew.getText().isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Admin Password changed successfully!").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Passwords do not match!").show();
            }
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
        String base  = "-fx-background-color: white; -fx-text-fill: #1e293b; -fx-border-color: " + color + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20;";
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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 5;");
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

    // 🚀 የ Start ኤረር የጠፋበት አዲሱ የ Logout ሎጂክ
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            if (SystemTray.isSupported() && trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            this.close(); // የአድሚን ዳሽቦርዱን ይዘጋዋል

            // 🚀 አዲሱን የ LoginForm SPA ቪው ይከፍታል
            try {
                Stage loginStage = new Stage();
                loginStage.setTitle("Smart Library | LMS 2026");
                loginStage.setMaximized(true);
                loginStage.setScene(new Scene(new LoginForm().getView(), 1000, 600));
                loginStage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}