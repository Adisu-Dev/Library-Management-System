package ui;

import java.io.File;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

// 🚀 Database Connection Imports
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import db.DatabaseConnection;

// 1. extends Application የሚለውን አጥፍተነዋል
public class LoginForm {

    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    // 2. ዋናውን ቪው የሚይዝ Variable
    private StackPane view;

    public LoginForm() {
        // 3. Stage ላይ ይሰሩ የነበሩትን አጥፍተን StackPane እንፈጥራለን
        view = new StackPane();

        // ==========================================
        // Responsive Background
        // ==========================================
        try {
            File bgFile = new File("src/images/bdu_bg.jpg");
            Image bgImage = new Image(bgFile.toURI().toString());

            BackgroundSize bgSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true);
            BackgroundImage backgroundImage = new BackgroundImage(bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, bgSize);
            view.setBackground(new Background(backgroundImage));
        } catch (Exception e) {
            System.out.println("Background image Error: " + e.getMessage());
            view.setStyle("-fx-background-color: linear-gradient(to bottom right, #020617, #0f172a, #1e293b);");
        }

        // ==========================================
        // 2. Marquee bar — pinned at the very top
        // ==========================================
        // Use a fixed-height HBox so it sits ABOVE the card, not behind it
        HBox marqueeBar = new HBox();
        marqueeBar.setMinHeight(44);
        marqueeBar.setMaxHeight(44);
        marqueeBar.setAlignment(Pos.CENTER_LEFT);
        marqueeBar.setStyle(
            "-fx-background-color: rgba(2,6,23,0.70);" +
            "-fx-border-color: rgba(56,189,248,0.25);" +
            "-fx-border-width: 0 0 1 0;"
        );
        marqueeBar.setClip(new javafx.scene.shape.Rectangle(20000, 44));

        Label marqueeText = new Label("Welcome to Smart Library Management System  •  Bahir Dar University Poly Campus  •  Smart LMS 2026  •  ");
        marqueeText.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 18));
        marqueeText.setTextFill(Color.WHITE);

        DropShadow neonGlow = new DropShadow(15, Color.web("#38bdf8"));
        neonGlow.setSpread(0.4);
        marqueeText.setEffect(neonGlow);
        marqueeBar.getChildren().add(marqueeText);

        TranslateTransition marqueeAnim = new TranslateTransition(Duration.seconds(22), marqueeText);
        marqueeAnim.setFromX(1200);
        marqueeAnim.setToX(-1800);
        marqueeAnim.setCycleCount(Animation.INDEFINITE);
        marqueeAnim.setInterpolator(Interpolator.LINEAR);
        marqueeAnim.play();

        FadeTransition pulseAnim = new FadeTransition(Duration.seconds(1.5), marqueeText);
        pulseAnim.setFromValue(0.80);
        pulseAnim.setToValue(1.0);
        pulseAnim.setCycleCount(Animation.INDEFINITE);
        pulseAnim.setAutoReverse(true);
        pulseAnim.play();

        // ==========================================
        // Modern Navy Blue Glass Card
        // ==========================================
        VBox loginCard = new VBox(18);
        loginCard.setMaxWidth(380);
        loginCard.setMaxHeight(580);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(30, 40, 30, 40));

        loginCard.setStyle("-fx-background-color: rgba(10, 25, 47, 0.85); " +
                "-fx-background-radius: 16; " +
                "-fx-border-radius: 16; " +
                "-fx-border-color: rgba(56, 189, 248, 0.4); " +
                "-fx-border-width: 1.5;");

        loginCard.setEffect(new DropShadow(40, Color.rgb(0, 0, 0, 0.7)));

        // Logo
        ImageView bduLogoView = new ImageView();
        try {
            String logoPath = getClass().getResource("/images/logo.png").toExternalForm();
            Image bduLogo = new Image(logoPath);
            bduLogoView.setImage(bduLogo);
            bduLogoView.setFitWidth(80);
            bduLogoView.setPreserveRatio(true);
            bduLogoView.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.5)));
        } catch (Exception e) {
            System.out.println("Logo image Error: " + e.getMessage());
        }

        // Titles
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        Label lblLoginTitle = new Label("System Login");
        lblLoginTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblLoginTitle.setTextFill(Color.WHITE);

        Label lblSubtitle = new Label("Secure access for library members");
        lblSubtitle.setFont(Font.font("Segoe UI", 13));
        lblSubtitle.setTextFill(Color.web("#94a3b8"));
        titleBox.getChildren().addAll(lblLoginTitle, lblSubtitle);
        VBox.setMargin(titleBox, new Insets(0, 0, 10, 0));

        // Shared Input Styles
        String inputStyle = "-fx-background-color: rgba(255, 255, 255, 0.08); " +
                "-fx-text-fill: white; " +
                "-fx-prompt-text-fill: #64748b; " +
                "-fx-border-color: rgba(255, 255, 255, 0.15); " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 0 15;";

        // Username
        TextField txtUser = new TextField();
        txtUser.setPromptText("Username or ID");
        txtUser.setPrefHeight(45);
        txtUser.setStyle(inputStyle);

        // Password
        String passStyle = "-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-padding: 0 40 0 15;";

        PasswordField txtPassHidden = new PasswordField();
        txtPassHidden.setPromptText("Password");
        txtPassHidden.setPrefHeight(45);
        txtPassHidden.setStyle(passStyle);

        TextField txtPassShown = new TextField();
        txtPassShown.setPromptText("Password");
        txtPassShown.setPrefHeight(45);
        txtPassShown.setStyle(passStyle);
        txtPassShown.setVisible(false);

        txtPassShown.textProperty().bindBidirectional(txtPassHidden.textProperty());

        Button btnToggleEye = new Button("👁️");
        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-font-size: 14px;");

        btnToggleEye.setOnAction(e -> {
            boolean isHidden = txtPassHidden.isVisible();
            txtPassHidden.setVisible(!isHidden);
            txtPassShown.setVisible(isHidden);
            btnToggleEye.setText(isHidden ? "🙈" : "👁️");
        });

        StackPane passwordPane = new StackPane(txtPassShown, txtPassHidden, btnToggleEye);
        StackPane.setAlignment(btnToggleEye, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnToggleEye, new Insets(0, 5, 0, 0));

        // Forgot Password
        HBox forgotBox = new HBox();
        forgotBox.setAlignment(Pos.CENTER_RIGHT);
        Hyperlink linkForgot = new Hyperlink("Forgot Password?");
        linkForgot.setTextFill(Color.web("#38bdf8"));
        linkForgot.setStyle("-fx-border-color: transparent; -fx-font-size: 12px; -fx-padding: 0;");
        forgotBox.getChildren().add(linkForgot);

        // Login Button
        Button btnLogin = new Button("LOGIN TO SYSTEM");
        btnLogin.setPrefWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(45);
        btnLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnLogin.setStyle("-fx-background-color: #fbbf24; -fx-text-fill: #0f172a; -fx-background-radius: 6; -fx-cursor: hand;");

        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #0f172a; -fx-background-radius: 6; -fx-cursor: hand;"));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle("-fx-background-color: #fbbf24; -fx-text-fill: #0f172a; -fx-background-radius: 6; -fx-cursor: hand;"));

        // Register Link
        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        Label lblNoAccount = new Label("Don't have an account?");
        lblNoAccount.setTextFill(Color.web("#94a3b8"));
        Hyperlink linkRegister = new Hyperlink("Sign Up");
        linkRegister.setTextFill(Color.web("#38bdf8"));
        linkRegister.setStyle("-fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;");
        registerBox.getChildren().addAll(lblNoAccount, linkRegister);

        // Demo Section
        Label lblOr = new Label("── QUICK LOGIN DEMO ──");
        lblOr.setTextFill(Color.web("#475569"));
        lblOr.setFont(Font.font("Segoe UI", 11));
        lblOr.setPadding(new Insets(10, 0, 5, 0));

        HBox demoPanel = new HBox(10);
        demoPanel.setAlignment(Pos.CENTER);
        Button btnAdminDemo = createDemoButton("👑 Admin", "#f87171");
        Button btnLibrarianDemo = createDemoButton("📚 Librarian", "#38bdf8");
        Button btnStudentDemo = createDemoButton("🎓 Student", "#34d399");
        demoPanel.getChildren().addAll(btnAdminDemo, btnLibrarianDemo, btnStudentDemo);

        // ==========================================
        // 🚀 NAVIGATION & DATABASE LOGIC (SPA Integrated)
        // ==========================================

        linkRegister.setOnAction(e -> {
            // 🚀 SPA Navigation: እዛው ዊንዶው ላይ ወደ RegisterForm ይቀይራል
            if (view.getScene() != null) {
                view.getScene().setRoot(new RegisterForm().getView());
            }
        });

        linkForgot.setOnAction(e -> {
            // 🚀 SPA Navigation: እዛው ዊንዶው ላይ ወደ ForgotPasswordForm ይቀይራል
            if (view.getScene() != null) {
                view.getScene().setRoot(new ForgotPasswordForm().getView());
            }
        });

        // ── Lockout label (shown during cooldown) ────────────────────
        Label lblLockout = new Label();
        lblLockout.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblLockout.setTextFill(Color.web("#ef4444"));
        lblLockout.setVisible(false);
        lblLockout.setAlignment(Pos.CENTER);
        lblLockout.setMaxWidth(Double.MAX_VALUE);

        btnLogin.setOnAction(e -> {
            String u = txtUser.getText().trim();
            String p = txtPassHidden.getText();

            if (u.isEmpty() || p.isEmpty()) {
                showAlert("Missing Input", "Please enter both Username and Password.");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn != null) {

                    // Try with IsBlocked column first; fall back if column doesn't exist yet
                    String query;
                    boolean hasIsBlockedColumn = columnExists(conn, "Users", "IsBlocked");
                    if (hasIsBlockedColumn) {
                        query = "SELECT UserID, FullName, Role, PasswordHash, IsBlocked FROM Users WHERE Username = ?";
                    } else {
                        query = "SELECT UserID, FullName, Role, PasswordHash FROM Users WHERE Username = ?";
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setString(1, u);
                        ResultSet rs = pstmt.executeQuery();

                        if (rs.next()) {
                            String dbPass = rs.getString("PasswordHash");

                            // Hash the entered password before comparing
                            String enteredHash = db.PasswordUtil.hashPassword(p);

                            if (enteredHash.equals(dbPass)) {
                                String dbRole     = rs.getString("Role");
                                String dbFullName = rs.getString("FullName");
                                int    dbId       = rs.getInt("UserID");
                                boolean isBlocked = hasIsBlockedColumn && rs.getInt("IsBlocked") == 1;

                                if (isBlocked) {
                                    showAlert("Account Blocked",
                                        "Your account has been blocked by the administrator.\n" +
                                        "Please contact the library admin for assistance.");
                                    return;
                                }

                                // Reset attempts on success
                                loginAttempts = 0;
                                Stage currentStage = (Stage) view.getScene().getWindow();
                                currentStage.close();

                                switch (dbRole.toLowerCase()) {
                                    case "admin":
                                        new AdminDashboard(dbFullName).show();
                                        break;
                                    case "librarian":
                                        new LibrarianDashboard(dbFullName, dbId).show();
                                        break;
                                    default:
                                        // Student and Guest both use StudentDashboard
                                        new StudentDashboard(dbFullName, dbId).show();
                                        break;
                                }
                                System.out.println("✅ Login Successful for: " + dbFullName + " [" + dbRole + "]");

                            } else {
                                // Wrong password
                                loginAttempts++;
                                if (loginAttempts >= MAX_ATTEMPTS) {
                                    lockLoginButton(btnLogin, lblLockout, txtUser, txtPassHidden, txtPassShown);
                                } else {
                                    int remaining = MAX_ATTEMPTS - loginAttempts;
                                    showAlert("Wrong Password",
                                        "The password is incorrect.\n" +
                                        "You have used " + loginAttempts + " of " + MAX_ATTEMPTS + " attempts.\n" +
                                        remaining + " attempt(s) left before temporary lockout.");
                                }
                            }
                        } else {
                            // User not found
                            loginAttempts++;
                            if (loginAttempts >= MAX_ATTEMPTS) {
                                lockLoginButton(btnLogin, lblLockout, txtUser, txtPassHidden, txtPassShown);
                            } else {
                                int remaining = MAX_ATTEMPTS - loginAttempts;
                                showAlert("User Not Found",
                                    "No account found for username '" + u + "'.\n" +
                                    "You have used " + loginAttempts + " of " + MAX_ATTEMPTS + " attempts.\n" +
                                    remaining + " attempt(s) left before temporary lockout.");
                            }
                        }
                    }
                } else {
                    showAlert("Database Error", "Could not connect to the SQL Server database.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("System Error", "Database Error: " + ex.getMessage());
            }
        });

        btnAdminDemo.setOnAction(e -> {
            Stage currentStage = (Stage) view.getScene().getWindow();
            if (currentStage != null) currentStage.close();
            new AdminDashboard("System Admin Demo").show();
        });

        btnLibrarianDemo.setOnAction(e -> {
            Stage currentStage = (Stage) view.getScene().getWindow();
            if (currentStage != null) currentStage.close();
            new LibrarianDashboard("Librarian Demo", 0).show();
        });

        btnStudentDemo.setOnAction(e -> {
            Stage currentStage = (Stage) view.getScene().getWindow();
            if (currentStage != null) currentStage.close();
            new StudentDashboard("Student Demo", 1001).show();
        });

        // Add all elements to Card
        loginCard.getChildren().addAll(bduLogoView, titleBox, txtUser, passwordPane, forgotBox, btnLogin, lblLockout, registerBox, lblOr, demoPanel);

        // ==========================================
        // Back to Landing Page Button
        // ==========================================
        Button btnBack = new Button("← Back");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btnBack.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #94a3b8; -fx-padding: 7 18; -fx-cursor: hand;"
        );
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
            "-fx-background-color: rgba(56,189,248,0.15);" +
            "-fx-border-color: #38bdf8;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #38bdf8; -fx-padding: 7 18; -fx-cursor: hand;"
        ));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #94a3b8; -fx-padding: 7 18; -fx-cursor: hand;"
        ));
        btnBack.setOnAction(e -> {
            if (view.getScene() != null) {
                javafx.animation.FadeTransition fadeOut =
                    new javafx.animation.FadeTransition(Duration.millis(400), view);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> view.getScene().setRoot(new LandingPage().getView()));
                fadeOut.play();
            }
        });

        StackPane.setAlignment(btnBack, Pos.TOP_LEFT);
        StackPane.setMargin(btnBack, new Insets(18, 0, 0, 22));

        // Fade Animation
        FadeTransition ft = new FadeTransition(Duration.millis(1200), loginCard);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        // ── Layout: BorderPane keeps marquee at top, card centered ────
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: transparent;");
        layout.setTop(marqueeBar);
        layout.setCenter(loginCard);
        BorderPane.setAlignment(loginCard, Pos.CENTER);

        view.getChildren().addAll(layout, btnBack);
        StackPane.setAlignment(layout, Pos.TOP_LEFT);
        StackPane.setAlignment(btnBack, Pos.TOP_LEFT);
    }

    // 4. ለ Main Window ቪውውን አሳልፎ የሚሰጥ Method
    public StackPane getView() {
        return view;
    }

    private void lockLoginButton(Button btnLogin, Label lblLockout,
                                  TextField txtUser, PasswordField txtPassHidden,
                                  TextField txtPassShown) {
        final int LOCKOUT_SECONDS = 30;
        btnLogin.setDisable(true);
        txtUser.setDisable(true);
        txtPassHidden.setDisable(true);
        txtPassShown.setDisable(true);
        lblLockout.setVisible(true);

        // Countdown using JavaFX Timeline
        javafx.animation.Timeline countdown = new javafx.animation.Timeline();
        final int[] secondsLeft = {LOCKOUT_SECONDS};

        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(
            Duration.seconds(1),
            event -> {
                secondsLeft[0]--;
                if (secondsLeft[0] > 0) {
                    lblLockout.setText("🔒 Too many failed attempts. Try again in " + secondsLeft[0] + "s");
                } else {
                    // Reset everything
                    loginAttempts = 0;
                    btnLogin.setDisable(false);
                    txtUser.setDisable(false);
                    txtPassHidden.setDisable(false);
                    txtPassShown.setDisable(false);
                    lblLockout.setVisible(false);
                    lblLockout.setText("");
                    txtUser.clear();
                    txtPassHidden.clear();
                    countdown.stop();
                }
            }
        ));
        countdown.setCycleCount(LOCKOUT_SECONDS);
        lblLockout.setText("🔒 Too many failed attempts. Try again in " + LOCKOUT_SECONDS + "s");
        countdown.play();
    }

    private boolean columnExists(Connection conn, String table, String column) {
        try {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, table, column)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Button createDemoButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-text-fill: " + color + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        btn.setPrefHeight(32);
        btn.setPrefWidth(100);

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #0f172a; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-text-fill: " + color + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;"));
        return btn;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}