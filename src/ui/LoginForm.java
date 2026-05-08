package ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import db.DatabaseConnection;

public class LoginForm {

    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final String ACCENT   = "#3b82f6";
    private static final String ACCENT_H = "#2563eb";

    private StackPane view;

    public LoginForm() {
        view = new StackPane();
        // TRANSPARENT — when used as overlay on LandingPage, the landing page
        // background shows through. The card itself has its own dark background.
        view.setStyle("-fx-background-color: transparent;");

        // ── Login card — centred popup over the landing page ─────────
        VBox loginCard = new VBox(24);
        loginCard.setMaxWidth(500);
        loginCard.setMinWidth(400);
        loginCard.setPrefWidth(480);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(60, 56, 56, 56));
        loginCard.setStyle(
            "-fx-background-color: rgba(8,20,42,0.97);" +
            "-fx-background-radius: 22;" +
            "-fx-border-radius: 22;" +
            "-fx-border-color: rgba(56,189,248,0.50);" +
            "-fx-border-width: 1.8;"
        );
        loginCard.setEffect(new DropShadow(60, Color.rgb(0, 0, 0, 0.85)));

        // Logo — large, matches reference
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoView.setImage(logo);
            logoView.setFitWidth(110);
            logoView.setPreserveRatio(true);
            logoView.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.5)));
        } catch (Exception ignored) {}

        // Title block
        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER);
        Label lblTitle = new Label("System Login");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        lblTitle.setTextFill(Color.WHITE);
        Label lblSub = new Label("Secure access for library members");
        lblSub.setFont(Font.font("Segoe UI", 15));
        lblSub.setTextFill(Color.web("#94a3b8"));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // Input style — tall fields matching reference
        String inputStyle =
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-font-size: 16px; -fx-padding: 0 18;";

        TextField txtUser = new TextField();
        txtUser.setPromptText("Username or ID");
        txtUser.setPrefHeight(58);
        txtUser.setStyle(inputStyle);

        String passStyle =
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-font-size: 16px; -fx-padding: 0 48 0 18;";

        PasswordField txtPassHidden = new PasswordField();
        txtPassHidden.setPromptText("Password");
        txtPassHidden.setPrefHeight(58);
        txtPassHidden.setStyle(passStyle);

        TextField txtPassShown = new TextField();
        txtPassShown.setPromptText("Password");
        txtPassShown.setPrefHeight(58);
        txtPassShown.setStyle(passStyle);
        txtPassShown.setVisible(false);
        txtPassShown.textProperty().bindBidirectional(txtPassHidden.textProperty());

        Button btnEye = new Button("👁️");
        btnEye.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-cursor: hand; -fx-font-size: 16px;");
        btnEye.setOnAction(e -> {
            boolean h = txtPassHidden.isVisible();
            txtPassHidden.setVisible(!h);
            txtPassShown.setVisible(h);
            btnEye.setText(h ? "🙈" : "👁️");
        });
        StackPane passPane = new StackPane(txtPassShown, txtPassHidden, btnEye);
        StackPane.setAlignment(btnEye, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnEye, new Insets(0, 10, 0, 0));

        // Forgot password
        HBox forgotBox = new HBox();
        forgotBox.setAlignment(Pos.CENTER_RIGHT);
        Hyperlink linkForgot = new Hyperlink("Forgot Password?");
        linkForgot.setTextFill(Color.web("#38bdf8"));
        linkForgot.setStyle("-fx-border-color: transparent; -fx-font-size: 14px; -fx-padding: 0;");
        forgotBox.getChildren().add(linkForgot);

        // Login button — full width, tall, blue
        Button btnLogin = new Button("LOGIN TO SYSTEM");
        btnLogin.setPrefWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(58);
        btnLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        String btnBase  = "-fx-background-color:" + ACCENT   + "; -fx-text-fill:white; -fx-background-radius:12; -fx-cursor:hand;";
        String btnHover = "-fx-background-color:" + ACCENT_H + "; -fx-text-fill:white; -fx-background-radius:12; -fx-cursor:hand;";
        btnLogin.setStyle(btnBase);
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(btnHover));
        btnLogin.setOnMouseExited(e  -> btnLogin.setStyle(btnBase));
        btnLogin.setDefaultButton(true);
        txtUser.setOnAction(e -> btnLogin.fire());
        txtPassHidden.setOnAction(e -> btnLogin.fire());
        txtPassShown.setOnAction(e -> btnLogin.fire());

        // Sign-up link
        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        Label lblNoAcc = new Label("Don't have an account?");
        lblNoAcc.setTextFill(Color.web("#94a3b8"));
        lblNoAcc.setFont(Font.font("Segoe UI", 14));
        Hyperlink linkReg = new Hyperlink("Sign Up");
        linkReg.setTextFill(Color.web("#38bdf8"));
        linkReg.setStyle("-fx-border-color: transparent; -fx-padding: 0; -fx-underline: true; -fx-font-size: 14px;");
        registerBox.getChildren().addAll(lblNoAcc, linkReg);

        // Lockout label
        Label lblLockout = new Label();
        lblLockout.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblLockout.setTextFill(Color.web("#ef4444"));
        lblLockout.setVisible(false);
        lblLockout.setAlignment(Pos.CENTER);
        lblLockout.setMaxWidth(Double.MAX_VALUE);

        loginCard.getChildren().addAll(
            logoView, titleBox, txtUser, passPane,
            forgotBox, btnLogin, lblLockout, registerBox
        );

        // ── Navigation ────────────────────────────────────────────────
        linkReg.setOnAction(e -> {
            if (view.getScene() != null)
                view.getScene().setRoot(new RegisterForm().getView());
        });
        linkForgot.setOnAction(e -> {
            if (view.getScene() != null)
                view.getScene().setRoot(new ForgotPasswordForm().getView());
        });

        // ── Login action ──────────────────────────────────────────────
        btnLogin.setOnAction(e -> {
            String u = txtUser.getText().trim();
            String p = txtPassHidden.getText();
            if (u.isEmpty() || p.isEmpty()) {
                showAlert("Missing Input", "Please enter both Username and Password.");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null) { showAlert("Database Error", "Could not connect."); return; }
                boolean hasBlocked = columnExists(conn, "Users", "IsBlocked");
                String q = hasBlocked
                    ? "SELECT UserID,FullName,Role,PasswordHash,IsBlocked FROM Users WHERE Username=?"
                    : "SELECT UserID,FullName,Role,PasswordHash FROM Users WHERE Username=?";
                try (PreparedStatement pst = conn.prepareStatement(q)) {
                    pst.setString(1, u);
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        if (db.PasswordUtil.hashPassword(p).equals(rs.getString("PasswordHash"))) {
                            if (hasBlocked && rs.getInt("IsBlocked") == 1) {
                                showAlert("Account Blocked",
                                    "Your account has been blocked.\nContact the library admin.");
                                return;
                            }
                            loginAttempts = 0;
                            String role = rs.getString("Role");
                            String name = rs.getString("FullName");
                            int    id   = rs.getInt("UserID");
                            Stage  st   = (Stage) view.getScene().getWindow();
                            st.close();
                            switch (role.toLowerCase()) {
                                case "admin"     -> new AdminDashboard(name, id).show();
                                case "librarian" -> new LibrarianDashboard(name, id).show();
                                default          -> new StudentDashboard(name, id).show();
                            }
                            db.ActivityLog.log(id, name + " logged in as " + role);
                        } else {
                            loginAttempts++;
                            if (loginAttempts >= MAX_ATTEMPTS)
                                lockLoginButton(btnLogin, lblLockout, txtUser, txtPassHidden, txtPassShown);
                            else
                                showAlert("Wrong Password",
                                    "Incorrect password. " + (MAX_ATTEMPTS - loginAttempts) + " attempt(s) left.");
                        }
                    } else {
                        loginAttempts++;
                        if (loginAttempts >= MAX_ATTEMPTS)
                            lockLoginButton(btnLogin, lblLockout, txtUser, txtPassHidden, txtPassShown);
                        else
                            showAlert("User Not Found",
                                "No account for '" + u + "'. " + (MAX_ATTEMPTS - loginAttempts) + " attempt(s) left.");
                    }
                }
            } catch (SQLException ex) {
                showAlert("System Error", "Database Error: " + ex.getMessage());
            }
        });

        // ── ✕ Close button — inside card, top-right corner ───────────
        Button btnClose = new Button("✕");
        btnClose.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String closeBase =
            "-fx-background-color: rgba(239,68,68,0.85);" +
            "-fx-border-color: #ef4444;" +
            "-fx-border-radius: 50%; -fx-background-radius: 50%;" +
            "-fx-text-fill: white;" +
            "-fx-min-width: 30; -fx-min-height: 30;" +
            "-fx-max-width: 30; -fx-max-height: 30;" +
            "-fx-cursor: hand;";
        String closeHover =
            "-fx-background-color: #ef4444;" +
            "-fx-border-color: #fca5a5;" +
            "-fx-border-radius: 50%; -fx-background-radius: 50%;" +
            "-fx-text-fill: white;" +
            "-fx-min-width: 30; -fx-min-height: 30;" +
            "-fx-max-width: 30; -fx-max-height: 30;" +
            "-fx-cursor: hand;";
        btnClose.setStyle(closeBase);
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(closeHover));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(closeBase));
        btnClose.setOnAction(e -> {
            if (view.getParent() instanceof StackPane overlay
                    && overlay.getParent() instanceof StackPane landingView) {
                FadeTransition fo = new FadeTransition(Duration.millis(200), overlay);
                fo.setFromValue(1); fo.setToValue(0);
                fo.setOnFinished(ev -> landingView.getChildren().remove(overlay));
                fo.play();
            } else if (view.getScene() != null) {
                FadeTransition fo = new FadeTransition(Duration.millis(300), view);
                fo.setFromValue(1); fo.setToValue(0);
                fo.setOnFinished(ev -> view.getScene().setRoot(new LandingPage().getView()));
                fo.play();
            }
        });

        // ✕ inside the card — positive inset keeps it inside the border
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        StackPane.setMargin(btnClose, new Insets(12, 12, 0, 0));

        StackPane cardWrapper = new StackPane(loginCard, btnClose);
        cardWrapper.setMaxWidth(500);

        FadeTransition ft = new FadeTransition(Duration.millis(600), cardWrapper);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Card centred directly in the transparent view — landing page shows through
        view.getChildren().add(cardWrapper);
        StackPane.setAlignment(cardWrapper, Pos.CENTER);
    }

    public StackPane getView() { return view; }

    private void lockLoginButton(Button btnLogin, Label lblLockout,
                                  TextField txtUser, PasswordField txtPassHidden,
                                  TextField txtPassShown) {
        final int SECS = 30;
        btnLogin.setDisable(true);
        txtUser.setDisable(true);
        txtPassHidden.setDisable(true);
        txtPassShown.setDisable(true);
        lblLockout.setVisible(true);
        final int[] left = {SECS};
        javafx.animation.Timeline cd = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.seconds(1), ev -> {
                left[0]--;
                if (left[0] > 0) {
                    lblLockout.setText("🔒 Too many attempts. Try again in " + left[0] + "s");
                } else {
                    loginAttempts = 0;
                    btnLogin.setDisable(false);
                    txtUser.setDisable(false);
                    txtPassHidden.setDisable(false);
                    txtPassShown.setDisable(false);
                    lblLockout.setVisible(false);
                    lblLockout.setText("");
                    txtUser.clear(); txtPassHidden.clear();
                }
            })
        );
        cd.setCycleCount(SECS);
        lblLockout.setText("🔒 Too many attempts. Try again in " + SECS + "s");
        cd.play();
    }

    private boolean columnExists(Connection conn, String table, String col) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, col)) {
            return rs.next();
        } catch (SQLException e) { return false; }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
