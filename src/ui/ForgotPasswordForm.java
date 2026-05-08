package ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import db.DatabaseConnection;

/**
 * Forgot Password — 2-step flow:
 *   Step 1: Enter BDU ID + Phone → verified against DB
 *   Step 2: Enter new password (if verified)
 *
 * 3 failed verification attempts → 30-second lockout, then 3 more.
 */
public class ForgotPasswordForm {

    private StackPane view;
    private int verifyAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    public ForgotPasswordForm() {
        view = new StackPane();

        // Background
        try {
            java.io.InputStream bgStream = getClass().getResourceAsStream("/images/bdu_bg.jpg");
            if (bgStream != null) {
                Image bg = new Image(bgStream);
                view.setBackground(new Background(new BackgroundImage(bg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true))));
            } else { view.setStyle("-fx-background-color: #020617;"); }
        } catch (Exception ignored) {
            view.setStyle("-fx-background-color: #020617;");
        }

        showStep1();
    }

    // ═══════════════════════════════════════════════════════════════
    // Step 1 — Verify identity
    // ═══════════════════════════════════════════════════════════════
    private void showStep1() {
        VBox card = buildCard();

        Label lblIcon = centeredLabel("🔐", 42, null);
        Label lblTitle = centeredLabel("Forgot Password?", 22, Color.WHITE);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label lblSub = centeredLabel(
            "Enter your BDU Student ID and registered phone number\nto verify your identity.", 12, Color.web("#94a3b8"));
        lblSub.setWrapText(true);

        // Info note
        Label lblNote = new Label("🔒  We verify using your BDU ID and phone — no email required.");
        lblNote.setFont(Font.font("Segoe UI", 11));
        lblNote.setTextFill(Color.web("#38bdf8"));
        lblNote.setWrapText(true);
        lblNote.setStyle(
            "-fx-background-color: rgba(56,189,248,0.10);" +
            "-fx-border-color: rgba(56,189,248,0.30);" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10;"
        );

        TextField txtId    = inputField("BDU Student ID  (e.g., BDU1234567)");
        TextField txtPhone = inputField("Registered Phone Number  (e.g., 0911223344)");

        // Lockout label
        Label lblLockout = new Label();
        lblLockout.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblLockout.setTextFill(Color.web("#ef4444"));
        lblLockout.setVisible(false);
        lblLockout.setMaxWidth(Double.MAX_VALUE);
        lblLockout.setAlignment(Pos.CENTER);

        Button btnVerify = actionButton("VERIFY IDENTITY →", "#38bdf8", "#020617");

        Hyperlink linkBack = backLink();
        linkBack.setOnAction(e -> navigateTo(new LoginForm().getView()));

        btnVerify.setOnAction(e -> {
            String id    = txtId.getText().trim().toUpperCase();
            String phone = txtPhone.getText().trim();

            if (id.isEmpty() || phone.isEmpty()) {
                alert(Alert.AlertType.WARNING, "Missing Fields", "Please enter both BDU ID and phone number.");
                return;
            }
            if (!id.matches("^BDU\\d{4,10}$")) {
                alert(Alert.AlertType.ERROR, "Invalid BDU ID", "BDU ID must start with 'BDU' followed by 4–10 digits.");
                return;
            }

            // Verify against DB
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null) { alert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to database."); return; }
                String q = "SELECT UserID, FullName FROM Users WHERE Username=? AND Phone=?";
                try (PreparedStatement pst = conn.prepareStatement(q)) {
                    pst.setString(1, id);
                    pst.setString(2, phone);
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        int userId = rs.getInt("UserID");
                        String name = rs.getString("FullName");
                        verifyAttempts = 0;
                        // Navigate to step 2
                        view.getChildren().clear();
                        showStep2(userId, name, id);
                    } else {
                        verifyAttempts++;
                        if (verifyAttempts >= MAX_ATTEMPTS) {
                            lockButton(btnVerify, lblLockout, txtId, txtPhone);
                        } else {
                            int rem = MAX_ATTEMPTS - verifyAttempts;
                            alert(Alert.AlertType.ERROR, "Verification Failed",
                                "BDU ID and phone number do not match.\n" +
                                "Attempt " + verifyAttempts + " of " + MAX_ATTEMPTS +
                                " — " + rem + " attempt(s) remaining.");
                        }
                    }
                }
            } catch (SQLException ex) {
                alert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
            }
        });

        card.getChildren().addAll(
            lblIcon, lblTitle, lblSub, lblNote,
            sectionLbl("Your Identity"),
            txtId, txtPhone,
            lblLockout,
            new Region(){{ setPrefHeight(4); }},
            btnVerify, linkBack
        );

        placeCard(card);
    }

    // ═══════════════════════════════════════════════════════════════
    // Step 2 — Set new password
    // ═══════════════════════════════════════════════════════════════
    private void showStep2(int userId, String fullName, String bduId) {
        VBox card = buildCard();

        Label lblIcon  = centeredLabel("✅", 42, null);
        Label lblTitle = centeredLabel("Identity Verified!", 22, Color.web("#10b981"));
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label lblSub = centeredLabel(
            "Welcome, " + fullName + ".\nSet your new password below.", 12, Color.web("#94a3b8"));
        lblSub.setWrapText(true);

        Label lblIdBadge = new Label("🎓  " + bduId);
        lblIdBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblIdBadge.setTextFill(Color.web("#38bdf8"));
        lblIdBadge.setStyle(
            "-fx-background-color: rgba(56,189,248,0.12);" +
            "-fx-border-color: rgba(56,189,248,0.35);" +
            "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 14;"
        );
        HBox badgeBox = new HBox(lblIdBadge);
        badgeBox.setAlignment(Pos.CENTER);

        PasswordField txtNew = new PasswordField();
        StackPane newPane = passField("New Password  (min. 6 characters)", txtNew);

        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setPrefWidth(Double.MAX_VALUE); strengthBar.setPrefHeight(4);
        strengthBar.setStyle("-fx-accent:#64748b;");
        Label lblStrength = new Label("Password Strength");
        lblStrength.setFont(Font.font("Segoe UI", 11)); lblStrength.setTextFill(Color.web("#64748b"));
        VBox strengthBox = new VBox(3, strengthBar, lblStrength);
        strengthBox.setPadding(new Insets(-8, 0, 4, 0));
        txtNew.textProperty().addListener((obs, o, nv) -> updateStrength(nv, strengthBar, lblStrength));

        PasswordField txtConfirm = new PasswordField();
        StackPane confirmPane = passField("Confirm New Password", txtConfirm);

        Button btnReset = actionButton("RESET PASSWORD 🔐", "#10b981", "#020617");
        Hyperlink linkBack = backLink();
        linkBack.setOnAction(e -> {
            view.getChildren().clear();
            showStep1();
        });

        btnReset.setOnAction(e -> {
            String np = txtNew.getText();
            String cp = txtConfirm.getText();
            if (np.isEmpty() || cp.isEmpty()) {
                alert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in both password fields.");
                return;
            }
            if (!np.equals(cp)) {
                alert(Alert.AlertType.ERROR, "Mismatch", "Passwords do not match.");
                return;
            }
            if (np.length() < 6) {
                alert(Alert.AlertType.WARNING, "Too Short", "Password must be at least 6 characters.");
                return;
            }
            // Update in DB
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null) { alert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to database."); return; }
                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE Users SET PasswordHash=? WHERE UserID=?")) {
                    pst.setString(1, db.PasswordUtil.hashPassword(np)); // ← Hash before storing
                    pst.setInt(2, userId);
                    pst.executeUpdate();
                }
                alert(Alert.AlertType.INFORMATION, "Password Reset Successful",
                    "Your password has been updated, " + fullName + ".\nYou can now log in with your new password.");
                navigateTo(new LoginForm().getView());
            } catch (SQLException ex) {
                alert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
            }
        });

        card.getChildren().addAll(
            lblIcon, lblTitle, lblSub, badgeBox,
            sectionLbl("New Password"),
            newPane, strengthBox, confirmPane,
            new Region(){{ setPrefHeight(4); }},
            btnReset, linkBack
        );

        placeCard(card);
    }

    // ═══════════════════════════════════════════════════════════════
    // Lockout helper
    // ═══════════════════════════════════════════════════════════════
    private void lockButton(Button btn, Label lblLockout, TextField... fields) {
        final int SECS = 30;
        btn.setDisable(true);
        for (TextField f : fields) f.setDisable(true);
        lblLockout.setVisible(true);

        final int[] left = {SECS};
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            left[0]--;
            if (left[0] > 0) {
                lblLockout.setText("🔒 Too many attempts. Try again in " + left[0] + "s");
            } else {
                verifyAttempts = 0;
                btn.setDisable(false);
                for (TextField f : fields) { f.setDisable(false); f.clear(); }
                lblLockout.setVisible(false);
            }
        }));
        tl.setCycleCount(SECS);
        lblLockout.setText("🔒 Too many attempts. Try again in " + SECS + "s");
        tl.play();
    }

    // ═══════════════════════════════════════════════════════════════
    // UI helpers
    // ═══════════════════════════════════════════════════════════════
    private VBox buildCard() {
        VBox card = new VBox(13);
        card.setMaxWidth(420);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(28, 36, 28, 36));
        card.setStyle(
            "-fx-background-color: rgba(10,25,47,0.90);" +
            "-fx-background-radius: 20; -fx-border-radius: 20;" +
            "-fx-border-color: rgba(56,189,248,0.35); -fx-border-width: 1.5;"
        );
        card.setEffect(new DropShadow(40, Color.rgb(0,0,0,0.65)));
        return card;
    }

    private void placeCard(VBox card) {
        view.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void navigateTo(javafx.scene.Parent target) {
        FadeTransition out = new FadeTransition(Duration.millis(350), view);
        out.setFromValue(1); out.setToValue(0);
        out.setOnFinished(e -> { if (view.getScene() != null) view.getScene().setRoot(target); });
        out.play();
    }

    private Label centeredLabel(String text, int size, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI Emoji", size));
        if (color != null) l.setTextFill(color);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Label sectionLbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#38bdf8"));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(4, 0, 2, 0));
        l.setStyle("-fx-border-color: rgba(56,189,248,0.25); -fx-border-width: 0 0 1 0;");
        return l;
    }

    private TextField inputField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt); tf.setPrefHeight(42);
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 0 12;"
        );
        return tf;
    }

    private StackPane passField(String prompt, PasswordField hidden) {
        String style =
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 0 40 0 12;";
        hidden.setPromptText(prompt); hidden.setPrefHeight(42); hidden.setStyle(style);
        TextField shown = new TextField();
        shown.setPromptText(prompt); shown.setPrefHeight(42); shown.setStyle(style);
        shown.setVisible(false);
        shown.textProperty().bindBidirectional(hidden.textProperty());
        Button eye = new Button("👁️");
        eye.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-cursor: hand;");
        eye.setOnAction(e -> {
            boolean h = hidden.isVisible();
            hidden.setVisible(!h); shown.setVisible(h);
            eye.setText(h ? "🙈" : "👁️");
        });
        StackPane p = new StackPane(shown, hidden, eye);
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        StackPane.setMargin(eye, new Insets(0, 6, 0, 0));
        return p;
    }

    private Button actionButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setPrefWidth(Double.MAX_VALUE); b.setPrefHeight(46);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        String base  = "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-background-radius:8; -fx-cursor:hand;";
        String hover = "-fx-background-color: derive(" + bg + ",20%); -fx-text-fill:" + fg + "; -fx-background-radius:8; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Hyperlink backLink() {
        Hyperlink l = new Hyperlink("⬅  Back to Login");
        l.setTextFill(Color.web("#94a3b8"));
        l.setStyle("-fx-border-color: transparent; -fx-font-size: 12px;");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private void updateStrength(String pw, ProgressBar bar, Label lbl) {
        if (pw.isEmpty()) { bar.setProgress(0); lbl.setText("Password Strength"); lbl.setTextFill(Color.web("#64748b")); bar.setStyle("-fx-accent:#64748b;"); return; }
        int s = 0;
        if (pw.length() >= 6) s++;
        if (pw.length() >= 8) s++;
        if (pw.matches(".*[A-Z].*") && pw.matches(".*[a-z].*")) s++;
        if (pw.matches(".*\\d.*")) s++;
        if (pw.matches(".*[!@#$%^&*()].*")) s++;
        if (s <= 2) { bar.setProgress(0.33); lbl.setText("Weak"); lbl.setTextFill(Color.web("#ef4444")); bar.setStyle("-fx-accent:#ef4444;"); }
        else if (s <= 4) { bar.setProgress(0.66); lbl.setText("Good"); lbl.setTextFill(Color.web("#f59e0b")); bar.setStyle("-fx-accent:#f59e0b;"); }
        else { bar.setProgress(1.0); lbl.setText("Strong ✅"); lbl.setTextFill(Color.web("#10b981")); bar.setStyle("-fx-accent:#10b981;"); }
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public StackPane getView() { return view; }
}
