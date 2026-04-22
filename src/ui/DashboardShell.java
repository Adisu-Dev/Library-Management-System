package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;

/**
 * Shared factory for the fixed top header and fixed footer
 * used across Admin, Librarian, and Student dashboards.
 *
 * Header layout:
 *   [Logo + "SmartLMS"]  |  "Smart Library Management System"  |  [Avatar + Name + Role]
 *
 * Footer:
 *   Developed by Adisu, Dawit, and Dagnachew.
 */
public class DashboardShell {

    // ── Gradient colours ─────────────────────────────────────────────
    private static final String HEADER_BG =
        "linear-gradient(to right, #0f172a 0%, #1e3a5f 50%, #0f172a 100%)";
    private static final String FOOTER_BG =
        "linear-gradient(to right, #0f172a 0%, #1e293b 100%)";
    private static final String ACCENT = "#38bdf8";

    // ─────────────────────────────────────────────────────────────────
    // TOP HEADER  (with clickable profile popup)
    // ─────────────────────────────────────────────────────────────────
    public static HBox buildHeader(String userName, String userRole, String roleEmoji) {
        HBox header = new HBox();
        header.setMinHeight(64);
        header.setMaxHeight(64);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 28, 0, 20));
        header.setStyle("-fx-background-color: " + HEADER_BG + ";");
        header.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.45)));

        // ── LEFT: Logo + brand name ───────────────────────────────────
        HBox leftBox = new HBox(12);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.setMinWidth(280);
        leftBox.setMaxWidth(280);

        ImageView logoView = new ImageView();
        try {
            logoView.setImage(new Image(new File("src/images/logo.png").toURI().toString()));
            logoView.setFitWidth(38);
            logoView.setPreserveRatio(true);
        } catch (Exception ignored) {}

        StackPane logoWrap = new StackPane(logoView);
        logoWrap.setStyle(
            "-fx-background-color: rgba(56,189,248,0.12);" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 5;"
        );

        VBox brandText = new VBox(1);
        brandText.setAlignment(Pos.CENTER_LEFT);
        Label lblBrand = new Label("SmartLMS");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblBrand.setTextFill(Color.WHITE);
        Label lblBrandSub = new Label("BDU Library");
        lblBrandSub.setFont(Font.font("Segoe UI", 10));
        lblBrandSub.setTextFill(Color.web("#64748b"));
        brandText.getChildren().addAll(lblBrand, lblBrandSub);
        leftBox.getChildren().addAll(logoWrap, brandText);

        // ── CENTER: Project title ─────────────────────────────────────
        VBox centerBox = new VBox(2);
        centerBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        Label lblTitle = new Label("Smart Library Management System");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.WHITE);
        lblTitle.setEffect(new DropShadow(8, Color.web(ACCENT, 0.4)));

        Label lblSubtitle = new Label("Bahir Dar University · 2026");
        lblSubtitle.setFont(Font.font("Segoe UI", 11));
        lblSubtitle.setTextFill(Color.web("#64748b"));
        centerBox.getChildren().addAll(lblTitle, lblSubtitle);

        // ── RIGHT: Avatar + name + role (clickable) ───────────────────
        HBox rightBox = new HBox(12);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(280);
        rightBox.setMaxWidth(280);
        rightBox.setStyle("-fx-cursor: hand;");

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        Label lblUserName = new Label(userName != null ? userName : "");
        lblUserName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblUserName.setTextFill(Color.WHITE);
        Label lblUserRole = new Label(roleEmoji + "  " + userRole);
        lblUserRole.setFont(Font.font("Segoe UI", 11));
        lblUserRole.setTextFill(Color.web(ACCENT));
        userInfo.getChildren().addAll(lblUserName, lblUserRole);

        // Avatar circle with initials
        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.web(ACCENT, 0.18));
        circle.setStroke(Color.web(ACCENT, 0.65));
        circle.setStrokeWidth(1.8);

        String initials = userName != null && !userName.isEmpty()
            ? String.valueOf(userName.charAt(0)).toUpperCase() : "?";
        Label lblInitials = new Label(initials);
        lblInitials.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblInitials.setTextFill(Color.web(ACCENT));
        avatar.getChildren().addAll(circle, lblInitials);

        rightBox.getChildren().addAll(userInfo, avatar);

        // ── Click on profile → show profile popup ────────────────────
        rightBox.setOnMouseEntered(e -> {
            circle.setFill(Color.web(ACCENT, 0.30));
            circle.setStroke(Color.web(ACCENT, 1.0));
        });
        rightBox.setOnMouseExited(e -> {
            circle.setFill(Color.web(ACCENT, 0.18));
            circle.setStroke(Color.web(ACCENT, 0.65));
        });
        rightBox.setOnMouseClicked(e -> showProfilePopup(userName, userRole, roleEmoji));

        header.getChildren().addAll(leftBox, centerBox, rightBox);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────
    // PROFILE POPUP
    // ─────────────────────────────────────────────────────────────────
    private static void showProfilePopup(String userName, String userRole, String roleEmoji) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("My Profile");
        popup.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPrefWidth(380);

        // ── Header banner ─────────────────────────────────────────────
        VBox banner = new VBox(10);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(30, 20, 24, 20));
        banner.setStyle("linear-gradient(to bottom, #1e3a5f, #0f172a);");
        banner.setStyle("-fx-background-color: linear-gradient(to bottom, #1e3a5f, #0f172a);");

        // Large avatar
        StackPane bigAvatar = new StackPane();
        Circle bigCircle = new Circle(40);
        bigCircle.setFill(Color.web(ACCENT, 0.20));
        bigCircle.setStroke(Color.web(ACCENT, 0.80));
        bigCircle.setStrokeWidth(2.5);
        String initials = userName != null && !userName.isEmpty()
            ? String.valueOf(userName.charAt(0)).toUpperCase() : "?";
        Label bigInitials = new Label(initials);
        bigInitials.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        bigInitials.setTextFill(Color.web(ACCENT));
        bigAvatar.getChildren().addAll(bigCircle, bigInitials);

        Label lblName = new Label(userName != null ? userName : "Unknown");
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblName.setTextFill(Color.WHITE);

        Label lblRole = new Label(roleEmoji + "  " + userRole);
        lblRole.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lblRole.setTextFill(Color.web(ACCENT));
        lblRole.setPadding(new Insets(4, 14, 4, 14));
        lblRole.setStyle(
            "-fx-background-color: rgba(56,189,248,0.12);" +
            "-fx-border-color: rgba(56,189,248,0.35);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );

        banner.getChildren().addAll(bigAvatar, lblName, lblRole);

        // ── Info rows ─────────────────────────────────────────────────
        VBox infoBox = new VBox(0);
        infoBox.setPadding(new Insets(8, 24, 8, 24));
        infoBox.getChildren().addAll(
            buildProfileRow("🏛️", "Institution",  "Bahir Dar University"),
            buildProfileRow("🎭", "Role",          userRole),
            buildProfileRow("🔐", "Account",       "Active & Secured"),
            buildProfileRow("📅", "System",        "Smart LMS 2026"),
            buildProfileRow("🌐", "Access Level",  getRoleDescription(userRole))
        );

        // ── Close button ──────────────────────────────────────────────
        VBox btnBox = new VBox();
        btnBox.setPadding(new Insets(16, 24, 24, 24));
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("Close");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setPrefHeight(42);
        btnClose.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnClose.setStyle(
            "-fx-background-color: linear-gradient(to right,#38bdf8,#818cf8);" +
            "-fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnClose.setOnAction(ev -> popup.close());
        btnBox.getChildren().add(btnClose);

        root.getChildren().addAll(banner, infoBox, btnBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        popup.setScene(scene);
        popup.centerOnScreen();

        // Fade in
        root.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(280), root);
        ft.setFromValue(0); ft.setToValue(1);
        popup.setOnShown(ev -> ft.play());
        popup.show();
    }

    private static HBox buildProfileRow(String icon, String label, String value) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 0 1 0;");

        Label lIcon = new Label(icon);
        lIcon.setFont(Font.font(16));
        lIcon.setMinWidth(24);

        VBox text = new VBox(2);
        Label lLabel = new Label(label);
        lLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lLabel.setTextFill(Color.web("#64748b"));
        Label lValue = new Label(value);
        lValue.setFont(Font.font("Segoe UI", 13));
        lValue.setTextFill(Color.WHITE);
        text.getChildren().addAll(lLabel, lValue);

        row.getChildren().addAll(lIcon, text);
        return row;
    }

    private static String getRoleDescription(String role) {
        switch (role.toLowerCase()) {
            case "admin":      return "Full system access";
            case "librarian":  return "Circulation & catalog management";
            default:           return "Browse library & view borrows";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // BOTTOM FOOTER
    // ─────────────────────────────────────────────────────────────────
    public static HBox buildFooter() {
        HBox footer = new HBox();
        footer.setMinHeight(40);
        footer.setMaxHeight(40);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 24, 0, 24));
        footer.setStyle(
            "-fx-background-color: linear-gradient(to right, #0f172a 0%, #1e3a5f 50%, #0f172a 100%);" +
            "-fx-border-color: rgba(56,189,248,0.20); -fx-border-width: 1 0 0 0;"
        );
        footer.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.45)));

        // Left: version — bright white
        Label lblLeft = new Label("Smart LMS v2026");
        lblLeft.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblLeft.setTextFill(Color.web("#cbd5e1"));

        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);

        // Center: credits — bright, bold, cyan accent
        Label lblCenter = new Label("✦  Developed by Adisu, Dawit, and Dagnachew  ✦");
        lblCenter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblCenter.setTextFill(Color.web("#e2e8f0"));
        lblCenter.setEffect(new DropShadow(6, Color.web("#38bdf8", 0.35)));

        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);

        // Right: institution — bright white
        Label lblRight = new Label("Bahir Dar University Poly Campus © 2026");
        lblRight.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblRight.setTextFill(Color.web("#cbd5e1"));

        footer.getChildren().addAll(lblLeft, spacerL, lblCenter, spacerR, lblRight);
        return footer;
    }
}
