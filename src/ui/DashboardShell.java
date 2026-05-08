
package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardShell {

    private static final String HEADER_BG =
        "linear-gradient(to right, #0f172a 0%, #1e3a5f 50%, #0f172a 100%)";
    private static final String ACCENT = "#38bdf8";

    // ── buildHeader overloads ─────────────────────────────────────────
    public static HBox buildHeader(String userName, String userRole, String roleEmoji) {
        return buildHeader(userName, userRole, roleEmoji, -1);
    }

    public static HBox buildHeader(String userName, String userRole, String roleEmoji, int userId) {
        HBox header = new HBox();
        header.setMinHeight(64); header.setMaxHeight(64);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 28, 0, 20));
        header.setStyle("-fx-background-color: " + HEADER_BG + ";");
        header.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.45)));

        HBox leftBox = new HBox(12);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.setMinWidth(280); leftBox.setMaxWidth(280);
        ImageView logoView = new ImageView();
        try {
            java.io.InputStream s = DashboardShell.class.getResourceAsStream("/images/logo.png");
            if (s != null) { logoView.setImage(new Image(s)); }
            logoView.setFitWidth(38); logoView.setPreserveRatio(true);
        } catch (Exception ignored) {}
        StackPane logoWrap = new StackPane(logoView);
        logoWrap.setStyle("-fx-background-color: rgba(56,189,248,0.12); -fx-background-radius: 10; -fx-padding: 5;");
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

        HBox rightBox = new HBox(12);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(280); rightBox.setMaxWidth(280);
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
        rightBox.setOnMouseEntered(e -> { circle.setFill(Color.web(ACCENT, 0.30)); circle.setStroke(Color.web(ACCENT, 1.0)); });
        rightBox.setOnMouseExited(e  -> { circle.setFill(Color.web(ACCENT, 0.18)); circle.setStroke(Color.web(ACCENT, 0.65)); });
        rightBox.setOnMouseClicked(e -> showProfilePopup(userName, userRole, roleEmoji, userId));

        header.getChildren().addAll(leftBox, centerBox, rightBox);
        return header;
    }

    // ── PROFILE POPUP — public so dashboards can open it directly ────
    public static void showProfilePopup(String userName, String userRole, String roleEmoji, int userId) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("My Profile");
        popup.setResizable(true);
        popup.setMinWidth(480); popup.setMinHeight(560);

        final String[] themes        = {"Dark Navy", "Midnight Blue", "Forest Green", "Deep Purple", "Slate Grey"};
        final String[] themeBgColors = {"#0f172a",   "#0a1628",       "#0a1f0a",      "#1a0a2e",     "#1a1f2e"};
        final String[] themeAccents  = {"#38bdf8",   "#60a5fa",       "#34d399",      "#a78bfa",     "#94a3b8"};
        final String[] themeIcons    = {"🌊",         "🌙",            "🌿",           "🔮",          "🪨"};
        final int[] themeIdx = {0};

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + themeBgColors[0] + ";");

        ScrollPane scrollRoot = new ScrollPane(root);
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        // Resolve admin userId by name FIRST before building any tabs
        final int[] resolvedId = {userId};
        if (userId <= 0 && userName != null) {
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    java.sql.PreparedStatement pst = conn.prepareStatement(
                        "SELECT UserID FROM Users WHERE FullName = ? AND Role = ?");
                    pst.setString(1, userName); pst.setString(2, userRole);
                    java.sql.ResultSet rs = pst.executeQuery();
                    if (rs.next()) resolvedId[0] = rs.getInt("UserID");
                }
            } catch (java.sql.SQLException ignored) {}
        }
        // Also try lookup by username if FullName lookup failed
        if (resolvedId[0] <= 0 && userName != null) {
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection()) {
                if (conn != null) {
                    java.sql.PreparedStatement pst = conn.prepareStatement(
                        "SELECT UserID FROM Users WHERE Username = ? OR FullName = ?");
                    pst.setString(1, userName); pst.setString(2, userName);
                    java.sql.ResultSet rs = pst.executeQuery();
                    if (rs.next()) resolvedId[0] = rs.getInt("UserID");
                }
            } catch (java.sql.SQLException ignored) {}
        }

        // Tab bar
        HBox tabBar = new HBox(0);
        tabBar.setStyle("-fx-background-color: rgba(0,0,0,0.30); -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 0 1 0;");
        javafx.scene.control.Button tabProfile  = tabBtn("👤  Profile",  true);
        javafx.scene.control.Button tabSecurity = tabBtn("🔐  Security", false);
        javafx.scene.control.Button tabTheme    = tabBtn("🎨  Theme",    false);
        tabBar.getChildren().addAll(tabProfile, tabSecurity, tabTheme);

        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(24, 28, 24, 28));

        VBox profileTab  = buildProfileTab(userName, userRole, roleEmoji, resolvedId[0]);
        VBox securityTab = buildSecurityTab(resolvedId[0]);
        VBox themeTab    = buildThemeTab(themes, themeBgColors, themeAccents, themeIcons, themeIdx, root);

        contentArea.getChildren().add(profileTab);

        tabProfile.setOnAction(e  -> { setTabActive(tabProfile,  tabSecurity, tabTheme); contentArea.getChildren().setAll(profileTab);  });
        tabSecurity.setOnAction(e -> { setTabActive(tabSecurity, tabProfile,  tabTheme); contentArea.getChildren().setAll(securityTab); });
        tabTheme.setOnAction(e    -> { setTabActive(tabTheme,    tabProfile,  tabSecurity); contentArea.getChildren().setAll(themeTab); });

        // Close button
        HBox closeBar = new HBox();
        closeBar.setPadding(new Insets(12, 28, 20, 28));
        closeBar.setStyle("-fx-background-color: rgba(0,0,0,0.20); -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1 0 0 0;");
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("✕  Close & Return to Dashboard");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setPrefHeight(42);
        btnClose.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String closeBase  = "-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        String closeHover = "-fx-background-color: rgba(239,68,68,0.18); -fx-text-fill: #ef4444; -fx-border-color: rgba(239,68,68,0.40); -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        btnClose.setStyle(closeBase);
        btnClose.setOnMouseEntered(ev -> btnClose.setStyle(closeHover));
        btnClose.setOnMouseExited(ev  -> btnClose.setStyle(closeBase));
        btnClose.setOnAction(ev -> popup.close());
        HBox.setHgrow(btnClose, Priority.ALWAYS);
        closeBar.getChildren().add(btnClose);

        root.getChildren().addAll(tabBar, contentArea, closeBar);

        javafx.scene.Scene scene = new javafx.scene.Scene(scrollRoot, 480, 620);
        popup.setScene(scene);
        popup.centerOnScreen();
        root.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), root);
        ft.setFromValue(0); ft.setToValue(1);
        popup.setOnShown(ev -> ft.play());
        popup.show();
    }

    // ── PROFILE TAB ───────────────────────────────────────────────────
    private static VBox buildProfileTab(String userName, String userRole, String roleEmoji, int userId) {
        VBox tab = new VBox(0);

        // Banner
        VBox banner = new VBox(10);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(20, 0, 20, 0));
        banner.setStyle("-fx-background-color: rgba(56,189,248,0.05); -fx-border-color: rgba(56,189,248,0.10); -fx-border-width: 0 0 1 0;");

        StackPane bigAvatar = new StackPane();
        Circle bigCircle = new Circle(44);
        bigCircle.setFill(Color.web(ACCENT, 0.15));
        bigCircle.setStroke(Color.web(ACCENT, 0.80));
        bigCircle.setStrokeWidth(2.5);
        String initials = userName != null && !userName.isEmpty() ? String.valueOf(userName.charAt(0)).toUpperCase() : "?";
        Label bigInitials = new Label(initials);
        bigInitials.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        bigInitials.setTextFill(Color.web(ACCENT));
        bigAvatar.getChildren().addAll(bigCircle, bigInitials);

        Label lblName = new Label(userName != null ? userName : "Unknown");
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblName.setTextFill(Color.WHITE);

        Label lblRoleBadge = new Label(roleEmoji + "  " + userRole);
        lblRoleBadge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lblRoleBadge.setTextFill(Color.web(ACCENT));
        lblRoleBadge.setPadding(new Insets(4, 14, 4, 14));
        lblRoleBadge.setStyle("-fx-background-color: rgba(56,189,248,0.12); -fx-border-color: rgba(56,189,248,0.35); -fx-border-radius: 20; -fx-background-radius: 20;");

        // Username — fetched and displayed clearly
        String currentUsername = userId > 0 ? db.UserDAO.getUsernameById(userId) : "";
        HBox usernameRow = new HBox(6);
        usernameRow.setAlignment(Pos.CENTER);
        Label lblULabel = new Label("Username:");
        lblULabel.setFont(Font.font("Segoe UI", 11));
        lblULabel.setTextFill(Color.web("#64748b"));
        Label lblUValue = new Label(currentUsername.isEmpty() ? "—" : currentUsername);
        lblUValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblUValue.setTextFill(Color.web("#e2e8f0"));
        usernameRow.getChildren().addAll(lblULabel, lblUValue);

        banner.getChildren().addAll(bigAvatar, lblName, lblRoleBadge, usernameRow);
        if (userId > 0) {
            Label lblId = new Label("System ID  #" + userId);
            lblId.setFont(Font.font("Segoe UI", 10));
            lblId.setTextFill(Color.web("#475569"));
            lblId.setPadding(new Insets(2, 8, 2, 8));
            lblId.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;");
            banner.getChildren().add(lblId);
        }

        // Info rows
        VBox infoBox = new VBox(0);
        infoBox.setPadding(new Insets(8, 0, 8, 0));
        infoBox.getChildren().addAll(
            buildProfileRow("🏛️", "Institution",  "Bahir Dar University"),
            buildProfileRow("🎭", "Role",          userRole),
            buildProfileRow("🔐", "Account",       "Active & Secured"),
            buildProfileRow("🌐", "Access Level",  getRoleDescription(userRole))
        );

        // Bio section
        VBox bioSection = new VBox(8);
        bioSection.setPadding(new Insets(16, 0, 0, 0));

        HBox bioHeader = new HBox(8);
        bioHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblBioIcon = new Label("📝"); lblBioIcon.setFont(Font.font(14));
        Label lblBioTitle = new Label("Bio");
        lblBioTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblBioTitle.setTextFill(Color.WHITE);
        bioHeader.getChildren().addAll(lblBioIcon, lblBioTitle);

        String currentBio = userId > 0 ? db.UserDAO.getBio(userId) : "";
        javafx.scene.control.TextArea txtBio = new javafx.scene.control.TextArea(currentBio);
        txtBio.setPromptText("Write something about yourself...");
        txtBio.setPrefRowCount(3);
        txtBio.setWrapText(true);
        // Dark-mode bio box — NOT white
        txtBio.setStyle(
            "-fx-control-inner-background: #1e293b;" +
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: #e2e8f0;" +
            "-fx-prompt-text-fill: #475569;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';"
        );
        txtBio.textProperty().addListener((obs, o, nv) -> { if (nv.length() > 300) txtBio.setText(o); });

        HBox bioFooter = new HBox(8);
        bioFooter.setAlignment(Pos.CENTER_RIGHT);
        Label lblCharCount = new Label(currentBio.length() + " / 300");
        lblCharCount.setFont(Font.font("Segoe UI", 10));
        lblCharCount.setTextFill(Color.web("#475569"));
        txtBio.textProperty().addListener((obs, o, nv) -> lblCharCount.setText(nv.length() + " / 300"));
        Region bioSpacer = new Region(); HBox.setHgrow(bioSpacer, Priority.ALWAYS);

        javafx.scene.control.Button btnSaveBio = new javafx.scene.control.Button("💾  Save Bio");
        btnSaveBio.setPrefHeight(34);
        btnSaveBio.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        String saveBioBase  = "-fx-background-color: #38bdf8; -fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 16;";
        String saveBioHover = "-fx-background-color: #7dd3fc; -fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 16;";
        btnSaveBio.setStyle(saveBioBase);
        btnSaveBio.setOnMouseEntered(ev -> btnSaveBio.setStyle(saveBioHover));
        btnSaveBio.setOnMouseExited(ev  -> btnSaveBio.setStyle(saveBioBase));
        btnSaveBio.setOnAction(e -> {
            if (userId > 0) {
                db.UserDAO.updateBio(userId, txtBio.getText().trim());
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setTitle("Saved"); a.setHeaderText(null); a.setContentText("✅ Bio saved successfully!"); a.showAndWait();
            }
        });
        bioFooter.getChildren().addAll(lblCharCount, bioSpacer, btnSaveBio);
        bioSection.getChildren().addAll(bioHeader, txtBio, bioFooter);

        tab.getChildren().addAll(banner, infoBox, bioSection);
        return tab;
    }

    // ── SECURITY TAB — two separate cards ────────────────────────────
    private static VBox buildSecurityTab(int userId) {
        VBox tab = new VBox(20);

        // Card 1: Update Username
        VBox usernameCard = securityCard("👤  Update Username", "Enter your current password to confirm.");
        javafx.scene.control.TextField txtNewUser = popupField("New username");
        javafx.scene.control.PasswordField txtPassForUser = new javafx.scene.control.PasswordField();
        txtPassForUser.setPromptText("Current Password");
        stylePopupField(txtPassForUser);
        Label lblUserStatus = new Label();
        lblUserStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblUserStatus.setWrapText(true);
        javafx.scene.control.Button btnSaveUser = solidBtn("Save Username", "#38bdf8", "#020617");
        btnSaveUser.setOnAction(e -> {
            String newUser = txtNewUser.getText().trim();
            String pass    = txtPassForUser.getText();
            if (newUser.isEmpty()) { setStatus(lblUserStatus, "⚠️ Enter a new username.", "#f59e0b"); return; }
            if (pass.isEmpty())    { setStatus(lblUserStatus, "⚠️ Current password required.", "#f59e0b"); return; }
            int code = db.UserDAO.updateOwnProfile(userId, pass, newUser, null);
            switch (code) {
                case 0 -> { setStatus(lblUserStatus, "✅ Username updated! Re-login to apply.", "#10b981"); txtNewUser.clear(); txtPassForUser.clear(); }
                case 1 -> setStatus(lblUserStatus, "❌ Current password is incorrect.", "#ef4444");
                case 2 -> setStatus(lblUserStatus, "❌ Username already taken.", "#ef4444");
                default -> setStatus(lblUserStatus, "❌ Database error.", "#ef4444");
            }
        });
        usernameCard.getChildren().addAll(fieldLabel("New Username"), txtNewUser, fieldLabel("Current Password"), eyeWrap(txtPassForUser), btnSaveUser, lblUserStatus);

        // Card 2: Update Password
        VBox passwordCard = securityCard("🔑  Update Password", "New password must be at least 6 characters.");
        javafx.scene.control.PasswordField txtCurrentPass = new javafx.scene.control.PasswordField();
        txtCurrentPass.setPromptText("Current Password"); stylePopupField(txtCurrentPass);
        javafx.scene.control.PasswordField txtNewPass = new javafx.scene.control.PasswordField();
        txtNewPass.setPromptText("New Password"); stylePopupField(txtNewPass);
        javafx.scene.control.PasswordField txtConfirmPass = new javafx.scene.control.PasswordField();
        txtConfirmPass.setPromptText("Confirm New Password"); stylePopupField(txtConfirmPass);
        Label lblPassStatus = new Label();
        lblPassStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblPassStatus.setWrapText(true);
        javafx.scene.control.Button btnSavePass = solidBtn("Update Password", "#818cf8", "#020617");
        btnSavePass.setOnAction(e -> {
            String current = txtCurrentPass.getText();
            String newP    = txtNewPass.getText();
            String confirm = txtConfirmPass.getText();
            if (current.isEmpty()) { setStatus(lblPassStatus, "⚠️ Current password required.", "#f59e0b"); return; }
            if (newP.isEmpty())    { setStatus(lblPassStatus, "⚠️ Enter a new password.", "#f59e0b"); return; }
            if (!newP.equals(confirm)) { setStatus(lblPassStatus, "❌ Passwords do not match.", "#ef4444"); return; }
            if (newP.length() < 6)     { setStatus(lblPassStatus, "❌ Minimum 6 characters.", "#ef4444"); return; }
            int code = db.UserDAO.updateOwnProfile(userId, current, null, newP);
            switch (code) {
                case 0 -> { setStatus(lblPassStatus, "✅ Password updated successfully!", "#10b981"); txtCurrentPass.clear(); txtNewPass.clear(); txtConfirmPass.clear(); }
                case 1 -> setStatus(lblPassStatus, "❌ Current password is incorrect.", "#ef4444");
                default -> setStatus(lblPassStatus, "❌ Database error.", "#ef4444");
            }
        });
        passwordCard.getChildren().addAll(fieldLabel("Current Password"), eyeWrap(txtCurrentPass), fieldLabel("New Password"), eyeWrap(txtNewPass), fieldLabel("Confirm Password"), eyeWrap(txtConfirmPass), btnSavePass, lblPassStatus);

        tab.getChildren().addAll(usernameCard, passwordCard);
        return tab;
    }

    // ── THEME TAB — card rows, no duplicate checkboxes ────────────────
    private static VBox buildThemeTab(String[] themes, String[] themeBgColors,
                                      String[] themeAccents, String[] themeIcons,
                                      int[] themeIdx, VBox root) {
        VBox tab = new VBox(16);
        Label lblTitle = new Label("🎨  Choose Your Theme");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.WHITE);
        Label lblSub = new Label("Click a card to apply the theme instantly.");
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web("#64748b"));

        VBox themeList = new VBox(10);
        for (int i = 0; i < themes.length; i++) {
            final int idx = i;

            // Two color dots as preview
            Circle dot1 = new Circle(10); dot1.setFill(Color.web(themeBgColors[i])); dot1.setStroke(Color.web(themeAccents[i], 0.7)); dot1.setStrokeWidth(1.5);
            Circle dot2 = new Circle(10); dot2.setFill(Color.web(themeAccents[i]));  dot2.setStroke(Color.web(themeBgColors[i], 0.5)); dot2.setStrokeWidth(1.5);
            HBox dots = new HBox(4, new StackPane(dot1), new StackPane(dot2));
            dots.setAlignment(Pos.CENTER_LEFT);

            Label lblThemeName = new Label(themeIcons[i] + "  " + themes[i]);
            lblThemeName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblThemeName.setTextFill(Color.web(themeAccents[i]));

            Region rowSpacer = new Region(); HBox.setHgrow(rowSpacer, Priority.ALWAYS);

            Label lblCheck = new Label(i == 0 ? "✓" : "");
            lblCheck.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            lblCheck.setTextFill(Color.web(themeAccents[i]));
            lblCheck.setMinWidth(22);

            HBox row = new HBox(14, dots, lblThemeName, rowSpacer, lblCheck);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 16, 14, 16));

            final String selStyle  = "-fx-background-color: rgba(56,189,248,0.10); -fx-border-color: " + themeAccents[i] + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
            final String normStyle = "-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
            final String hovStyle  = "-fx-background-color: rgba(255,255,255,0.09); -fx-border-color: rgba(255,255,255,0.22); -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
            row.setStyle(i == 0 ? selStyle : normStyle);
            row.setOnMouseEntered(e -> { if (idx != themeIdx[0]) row.setStyle(hovStyle); });
            row.setOnMouseExited(e  -> { row.setStyle(idx == themeIdx[0] ? selStyle : normStyle); });
            row.setOnMouseClicked(e -> {
                themeIdx[0] = idx;
                root.setStyle("-fx-background-color: " + themeBgColors[idx] + ";");
                for (int j = 0; j < themeList.getChildren().size(); j++) {
                    HBox r = (HBox) themeList.getChildren().get(j);
                    boolean sel = (j == idx);
                    String acc = themeAccents[j];
                    r.setStyle(sel
                        ? "-fx-background-color: rgba(56,189,248,0.10); -fx-border-color: " + acc + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;"
                        : "-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                    Label chk = (Label) r.getChildren().get(3);
                    chk.setText(sel ? "✓" : "");
                    chk.setTextFill(Color.web(acc));
                }
            });
            themeList.getChildren().add(row);
        }
        tab.getChildren().addAll(lblTitle, lblSub, themeList);
        return tab;
    }

    // ── Shared helpers ────────────────────────────────────────────────
    private static javafx.scene.control.Button tabBtn(String text, boolean active) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(text);
        b.setPrefHeight(42); b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        applyTabStyle(b, active);
        return b;
    }

    private static void applyTabStyle(javafx.scene.control.Button b, boolean active) {
        b.setStyle(active
            ? "-fx-background-color: rgba(56,189,248,0.15); -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; -fx-border-width: 0 0 2.5 0; -fx-cursor: hand;"
            : "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-border-color: transparent; -fx-cursor: hand;");
    }

    private static void setTabActive(javafx.scene.control.Button active, javafx.scene.control.Button... others) {
        applyTabStyle(active, true);
        for (javafx.scene.control.Button b : others) applyTabStyle(b, false);
    }

    private static VBox securityCard(String title, String subtitle) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 12; -fx-background-radius: 12;");
        Label lblT = new Label(title);
        lblT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblT.setTextFill(Color.WHITE);
        Label lblS = new Label(subtitle);
        lblS.setFont(Font.font("Segoe UI", 11));
        lblS.setTextFill(Color.web("#64748b"));
        lblS.setWrapText(true);
        card.getChildren().addAll(lblT, lblS);
        return card;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#94a3b8"));
        return l;
    }

    private static javafx.scene.control.TextField popupField(String prompt) {
        javafx.scene.control.TextField tf = new javafx.scene.control.TextField();
        tf.setPromptText(prompt); tf.setPrefHeight(40);
        stylePopupField(tf);
        return tf;
    }

    private static void stylePopupField(javafx.scene.control.TextInputControl tf) {
        tf.setPrefHeight(40);
        tf.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #475569;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 0 38 0 12;"
        );
    }

    /**
     * Wraps a PasswordField with a show/hide eye toggle button.
     * The returned StackPane can be used anywhere in place of the raw PasswordField.
     */
    public static StackPane eyeWrap(javafx.scene.control.PasswordField hidden) {
        javafx.scene.control.TextField shown = new javafx.scene.control.TextField();
        shown.setPromptText(hidden.getPromptText());
        shown.setPrefHeight(hidden.getPrefHeight());
        shown.setStyle(hidden.getStyle());
        shown.setVisible(false);
        shown.textProperty().bindBidirectional(hidden.textProperty());

        javafx.scene.control.Button eye = new javafx.scene.control.Button("👁");
        eye.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #64748b;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 0 6 0 0;"
        );
        eye.setOnMouseEntered(e -> eye.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #38bdf8; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 6 0 0;"));
        eye.setOnMouseExited(e -> eye.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 6 0 0;"));
        eye.setOnAction(e -> {
            boolean isHidden = hidden.isVisible();
            hidden.setVisible(!isHidden);
            shown.setVisible(isHidden);
            eye.setText(isHidden ? "🙈" : "👁");
        });

        StackPane pane = new StackPane(shown, hidden, eye);
        StackPane.setAlignment(eye, javafx.geometry.Pos.CENTER_RIGHT);
        javafx.scene.layout.StackPane.setMargin(eye, new Insets(0, 4, 0, 0));
        return pane;
    }

    private static javafx.scene.control.Button solidBtn(String text, String bg, String fg) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(text);
        b.setMaxWidth(Double.MAX_VALUE); b.setPrefHeight(42);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base  = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-background-radius: 8; -fx-cursor: hand;";
        String hover = "-fx-background-color: derive(" + bg + ",20%); -fx-text-fill: " + fg + "; -fx-background-radius: 8; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private static void setStatus(Label lbl, String msg, String color) {
        lbl.setText(msg); lbl.setTextFill(Color.web(color));
    }

    private static HBox buildProfileRow(String icon, String label, String value) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 0 1 0;");
        Label lIcon = new Label(icon); lIcon.setFont(Font.font(16)); lIcon.setMinWidth(24);
        VBox text = new VBox(3);
        Label lLabel = new Label(label);
        lLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lLabel.setTextFill(Color.web("#64748b"));
        Label lValue = new Label(value);
        lValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lValue.setTextFill(Color.WHITE);
        text.getChildren().addAll(lLabel, lValue);
        row.getChildren().addAll(lIcon, text);
        return row;
    }

    private static String getRoleDescription(String role) {
        return switch (role.toLowerCase()) {
            case "admin"     -> "Full system access";
            case "librarian" -> "Circulation & catalog management";
            default          -> "Browse library & view borrows";
        };
    }

    // ── FOOTER ────────────────────────────────────────────────────────
    public static HBox buildFooter() {
        HBox footer = new HBox();
        footer.setMinHeight(40); footer.setMaxHeight(40);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 24, 0, 24));
        footer.setStyle(
            "-fx-background-color: linear-gradient(to right, #0f172a 0%, #1e3a5f 50%, #0f172a 100%);" +
            "-fx-border-color: rgba(56,189,248,0.20); -fx-border-width: 1 0 0 0;"
        );
        footer.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.45)));
        Label lblLeft = new Label("Smart LMS v2026");
        lblLeft.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblLeft.setTextFill(Color.web("#cbd5e1"));
        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);
        Label lblCenter = new Label("✦  Developed by Adisu, Dawit, and Dagnachew  ✦");
        lblCenter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblCenter.setTextFill(Color.web("#e2e8f0"));
        lblCenter.setEffect(new DropShadow(6, Color.web("#38bdf8", 0.35)));
        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);
        Label lblRight = new Label("Bahir Dar University Poly Campus © 2026");
        lblRight.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblRight.setTextFill(Color.web("#cbd5e1"));
        footer.getChildren().addAll(lblLeft, spacerL, lblCenter, spacerR, lblRight);
        return footer;
    }
}
