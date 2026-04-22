package ui;

import javafx.animation.FadeTransition;
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
 * Registration form with two paths:
 *   🎓 Student  — requires BDU ID, department, year
 *   👤 Guest    — open to anyone, uses email as username
 *
 * Librarian and Admin accounts are created by the Admin only.
 */
public class RegisterForm {

    private StackPane view;

    public RegisterForm() {
        view = new StackPane();

        // Background
        try {
            Image bg = new Image("file:src/images/bdu_bg.jpg");
            view.setBackground(new Background(new BackgroundImage(bg,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true))));
        } catch (Exception ignored) {
            view.setStyle("-fx-background-color: #020617;");
        }

        // ── Outer card ────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setMaxWidth(480);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(28, 36, 28, 36));
        card.setStyle(
            "-fx-background-color: rgba(10,25,47,0.92);" +
            "-fx-background-radius: 20; -fx-border-radius: 20;" +
            "-fx-border-color: rgba(56,189,248,0.35); -fx-border-width: 1.5;"
        );
        card.setEffect(new DropShadow(40, Color.rgb(0, 0, 0, 0.65)));

        // ── Header ────────────────────────────────────────────────────
        Label lblIcon = centeredLabel("📚", 40, null);
        Label lblTitle = centeredLabel("Create Your Account", 23, Color.WHITE);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 23));
        Label lblSub = centeredLabel("Bahir Dar University Smart Library", 12, Color.web("#94a3b8"));

        // ── Role type selector (tab-style toggle) ─────────────────────
        HBox typeSelector = new HBox(0);
        typeSelector.setAlignment(Pos.CENTER);
        typeSelector.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 12; -fx-border-radius: 12;"
        );

        Button btnStudent = typeBtn("🎓  BDU Student", true);
        Button btnGuest   = typeBtn("👤  Guest / Visitor", false);
        typeSelector.getChildren().addAll(btnStudent, btnGuest);

        // ── Content area — swaps between Student and Guest forms ──────
        VBox contentArea = new VBox(12);
        contentArea.setAlignment(Pos.TOP_CENTER);

        // Build both forms
        VBox studentForm = buildStudentForm();
        VBox guestForm   = buildGuestForm();

        // Start with student form visible
        contentArea.getChildren().add(studentForm);

        btnStudent.setOnAction(e -> {
            setTypeActive(btnStudent, btnGuest);
            contentArea.getChildren().setAll(studentForm);
        });
        btnGuest.setOnAction(e -> {
            setTypeActive(btnGuest, btnStudent);
            contentArea.getChildren().setAll(guestForm);
        });

        // ── Admin/Librarian notice ────────────────────────────────────
        HBox adminNotice = new HBox(10);
        adminNotice.setAlignment(Pos.CENTER_LEFT);
        adminNotice.setPadding(new Insets(8, 12, 8, 12));
        adminNotice.setStyle(
            "-fx-background-color: rgba(251,191,36,0.08);" +
            "-fx-border-color: rgba(251,191,36,0.25);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        Label lAdminIcon = new Label("⚠️");
        lAdminIcon.setFont(Font.font(13));
        Label lAdminText = new Label(
            "Librarian & Admin accounts are created by the System Administrator only.");
        lAdminText.setFont(Font.font("Segoe UI", 11));
        lAdminText.setTextFill(Color.web("#fbbf24"));
        lAdminText.setWrapText(true);
        adminNotice.getChildren().addAll(lAdminIcon, lAdminText);

        // ── Login link ────────────────────────────────────────────────
        HBox loginBox = new HBox(5);
        loginBox.setAlignment(Pos.CENTER);
        Label lblHave = new Label("Already have an account?");
        lblHave.setTextFill(Color.web("#94a3b8"));
        lblHave.setFont(Font.font("Segoe UI", 12));
        Hyperlink linkLogin = new Hyperlink("Login Here");
        linkLogin.setTextFill(Color.web("#38bdf8"));
        linkLogin.setStyle("-fx-border-color: transparent; -fx-underline: true; -fx-font-size: 12px;");
        linkLogin.setOnAction(e -> {
            if (view.getScene() != null)
                view.getScene().setRoot(new LoginForm().getView());
        });
        loginBox.getChildren().addAll(lblHave, linkLogin);

        card.getChildren().addAll(
            lblIcon, lblTitle, lblSub,
            typeSelector,
            contentArea,
            adminNotice,
            loginBox
        );

        // Scrollable wrapper
        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setMaxWidth(480);
        scroll.setMaxHeight(700);

        view.getChildren().add(scroll);
        StackPane.setAlignment(scroll, Pos.CENTER);

        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(700), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ═══════════════════════════════════════════════════════════════
    // Student registration form
    // ═══════════════════════════════════════════════════════════════
    private VBox buildStudentForm() {
        VBox form = new VBox(11);

        Label lblInfo = new Label(
            "ℹ️  For BDU students only. Your Student ID is your username.");
        lblInfo.setFont(Font.font("Segoe UI", 11));
        lblInfo.setTextFill(Color.web("#38bdf8"));
        lblInfo.setWrapText(true);
        lblInfo.setStyle(
            "-fx-background-color: rgba(56,189,248,0.10);" +
            "-fx-border-color: rgba(56,189,248,0.30);" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10;"
        );

        TextField txtName  = field("Full Name  (e.g., Adisu Dereje)");
        TextField txtId    = field("BDU Student ID  (e.g., BDU1234567)");
        Label lblIdStatus  = new Label("");
        lblIdStatus.setFont(Font.font("Segoe UI", 11));
        txtId.textProperty().addListener((obs, o, nv) -> {
            if (nv.isEmpty()) { lblIdStatus.setText(""); return; }
            if (nv.toUpperCase().matches("^BDU\\d{4,10}$")) {
                lblIdStatus.setText("✅ Valid BDU ID"); lblIdStatus.setTextFill(Color.web("#10b981"));
            } else {
                lblIdStatus.setText("❌ Must be BDU + 4–10 digits"); lblIdStatus.setTextFill(Color.web("#ef4444"));
            }
        });

        TextField txtEmail = field("Email Address  (e.g., student@bdu.edu.et)");
        TextField txtPhone = field("Phone Number  (e.g., 0911223344)");

        ComboBox<String> cmbDept = styledCombo("Select Department / Faculty",
            "Information Technology (IT)", "Computer Science", "Software Engineering",
            "Information Systems", "Electrical Engineering", "Civil Engineering",
            "Business Administration", "Accounting", "Law", "Medicine", "Natural Science", "Other");

        ComboBox<String> cmbYear = styledCombo("Select Year / Level",
            "Year 1", "Year 2", "Year 3", "Year 4", "Year 5", "Postgraduate");

        PasswordField txtPass = new PasswordField();
        StackPane passPane = passField("Create Password  (min. 6 characters)", txtPass);
        VBox strengthBox = strengthMeter(txtPass);

        PasswordField txtConfirm = new PasswordField();
        StackPane confirmPane = passField("Confirm Password", txtConfirm);

        Button btnReg = registerButton("CREATE STUDENT ACCOUNT", "#38bdf8");
        btnReg.setOnAction(e -> {
            String name    = txtName.getText().trim();
            String id      = txtId.getText().trim().toUpperCase();
            String email   = txtEmail.getText().trim();
            String phone   = txtPhone.getText().trim();
            String dept    = cmbDept.getValue();
            String year    = cmbYear.getValue();
            String pass    = txtPass.getText();
            String confirm = txtConfirm.getText();

            if (name.isEmpty() || id.isEmpty() || email.isEmpty() || phone.isEmpty()
                    || dept == null || year == null || pass.isEmpty()) {
                alert(Alert.AlertType.ERROR, "Missing Fields", "Please fill in all required fields.");
                return;
            }
            if (!id.matches("^BDU\\d{4,10}$")) {
                alert(Alert.AlertType.ERROR, "Invalid BDU ID",
                    "Student ID must start with 'BDU' followed by 4–10 digits.\nExample: BDU1234567");
                return;
            }
            if (!email.contains("@") || !email.contains(".")) {
                alert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }
            if (!phone.matches("^(09|07)\\d{8}$")) {
                alert(Alert.AlertType.ERROR, "Invalid Phone",
                    "Enter a valid Ethiopian phone number (e.g., 0911223344).");
                return;
            }
            if (!pass.equals(confirm)) {
                alert(Alert.AlertType.ERROR, "Password Mismatch", "Passwords do not match."); return;
            }
            if (pass.length() < 6) {
                alert(Alert.AlertType.WARNING, "Weak Password", "Password must be at least 6 characters."); return;
            }
            insertUser(name, id, pass, "Student", phone, email,
                "Registration Successful",
                "Welcome, " + name + "!\n\nBDU ID: " + id +
                "\nDepartment: " + dept + "  |  " + year +
                "\n\nYou can now log in.");
        });

        form.getChildren().addAll(
            lblInfo,
            sectionLbl("Personal Information"),
            txtName, txtId, lblIdStatus, txtEmail, txtPhone,
            sectionLbl("Academic Details"),
            cmbDept, cmbYear,
            sectionLbl("Security"),
            passPane, strengthBox, confirmPane,
            new Region(){{ setPrefHeight(4); }},
            btnReg
        );
        return form;
    }

    // ═══════════════════════════════════════════════════════════════
    // Guest registration form
    // ═══════════════════════════════════════════════════════════════
    private VBox buildGuestForm() {
        VBox form = new VBox(11);

        Label lblInfo = new Label(
            "👤  Open to anyone — community members, researchers, or visitors.\n" +
            "Guest accounts can browse the catalog and read e-books.");
        lblInfo.setFont(Font.font("Segoe UI", 11));
        lblInfo.setTextFill(Color.web("#a78bfa"));
        lblInfo.setWrapText(true);
        lblInfo.setStyle(
            "-fx-background-color: rgba(129,140,248,0.10);" +
            "-fx-border-color: rgba(129,140,248,0.30);" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10;"
        );

        TextField txtName  = field("Full Name  (e.g., Kebede Alemu)");
        TextField txtEmail = field("Email Address  (used as your username)");
        TextField txtPhone = field("Phone Number  (optional, e.g., 0911223344)");

        ComboBox<String> cmbPurpose = styledCombo("Purpose of Visit",
            "Personal Reading", "Research", "Community Member",
            "Teacher / Educator", "Journalist / Writer", "Other");

        PasswordField txtPass = new PasswordField();
        StackPane passPane = passField("Create Password  (min. 6 characters)", txtPass);
        VBox strengthBox = strengthMeter(txtPass);

        PasswordField txtConfirm = new PasswordField();
        StackPane confirmPane = passField("Confirm Password", txtConfirm);

        // Guest access info
        VBox accessInfo = new VBox(5);
        accessInfo.setStyle(
            "-fx-background-color: rgba(15,23,42,0.70);" +
            "-fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12;"
        );
        Label lblAccessTitle = new Label("📖  Guest Access Includes:");
        lblAccessTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblAccessTitle.setTextFill(Color.web("#94a3b8"));
        accessInfo.getChildren().addAll(
            lblAccessTitle,
            accessRow("✅", "Browse the full book catalog"),
            accessRow("✅", "Read & download e-books for free"),
            accessRow("✅", "Search books by title or author"),
            accessRow("❌", "Borrow physical books  (students only)"),
            accessRow("❌", "Reserve books  (students only)")
        );

        Button btnReg = registerButton("CREATE GUEST ACCOUNT", "#818cf8");
        btnReg.setOnAction(e -> {
            String name    = txtName.getText().trim();
            String email   = txtEmail.getText().trim();
            String phone   = txtPhone.getText().trim();
            String purpose = cmbPurpose.getValue();
            String pass    = txtPass.getText();
            String confirm = txtConfirm.getText();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || purpose == null) {
                alert(Alert.AlertType.ERROR, "Missing Fields",
                    "Full Name, Email, Purpose, and Password are required.");
                return;
            }
            if (!email.contains("@") || !email.contains(".")) {
                alert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }
            if (!pass.equals(confirm)) {
                alert(Alert.AlertType.ERROR, "Password Mismatch", "Passwords do not match."); return;
            }
            if (pass.length() < 6) {
                alert(Alert.AlertType.WARNING, "Weak Password", "Password must be at least 6 characters."); return;
            }
            // Use email as username for guests
            String username = email.toLowerCase().replace(" ", "");
            insertUser(name, username, pass, "Guest",
                phone.isEmpty() ? null : phone, email,
                "Guest Account Created",
                "Welcome, " + name + "!\n\n" +
                "Your guest account has been created.\n" +
                "Username: " + username + "\n" +
                "Purpose: " + purpose + "\n\n" +
                "You can now browse the catalog and read e-books.");
        });

        form.getChildren().addAll(
            lblInfo,
            sectionLbl("Your Information"),
            txtName, txtEmail, txtPhone,
            sectionLbl("Visit Purpose"),
            cmbPurpose,
            accessInfo,
            sectionLbl("Security"),
            passPane, strengthBox, confirmPane,
            new Region(){{ setPrefHeight(4); }},
            btnReg
        );
        return form;
    }

    // ═══════════════════════════════════════════════════════════════
    // Shared DB insert
    // ═══════════════════════════════════════════════════════════════
    private void insertUser(String name, String username, String pass,
                            String role, String phone, String email,
                            String successTitle, String successMsg) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                alert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to database."); return;
            }
            // Duplicate check
            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT Username FROM Users WHERE Username=?")) {
                chk.setString(1, username);
                if (chk.executeQuery().next()) {
                    alert(Alert.AlertType.ERROR, "Already Registered",
                        "An account with '" + username + "' already exists.");
                    return;
                }
            }
            // Insert
            boolean hasEmail = columnExists(conn, "Users", "Email");
            String sql = hasEmail
                ? "INSERT INTO Users (FullName,Username,PasswordHash,Role,Phone,Email) VALUES(?,?,?,?,?,?)"
                : "INSERT INTO Users (FullName,Username,PasswordHash,Role,Phone) VALUES(?,?,?,?,?)";
            try (PreparedStatement ins = conn.prepareStatement(sql)) {
                ins.setString(1, name);
                ins.setString(2, username);
                ins.setString(3, db.PasswordUtil.hashPassword(pass)); // ← Hash before storing
                ins.setString(4, role);
                ins.setString(5, phone);
                if (hasEmail) ins.setString(6, email);
                if (ins.executeUpdate() > 0) {
                    alert(Alert.AlertType.INFORMATION, successTitle, successMsg);
                    if (view.getScene() != null)
                        view.getScene().setRoot(new LoginForm().getView());
                }
            }
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI helpers
    // ═══════════════════════════════════════════════════════════════
    public StackPane getView() { return view; }

    private Label centeredLabel(String text, int size, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI Emoji", size));
        if (color != null) l.setTextFill(color);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Button typeBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setPrefHeight(38); b.setPrefWidth(200);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        applyTypeStyle(b, active);
        return b;
    }

    private void applyTypeStyle(Button b, boolean active) {
        b.setStyle(active
            ? "-fx-background-color: #38bdf8; -fx-text-fill: #020617;" +
              "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;"
            : "-fx-background-color: transparent; -fx-text-fill: #64748b;" +
              "-fx-background-radius: 10; -fx-cursor: hand;");
    }

    private void setTypeActive(Button active, Button inactive) {
        applyTypeStyle(active, true);
        applyTypeStyle(inactive, false);
    }

    private Label sectionLbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#38bdf8"));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-border-color: rgba(56,189,248,0.22); -fx-border-width: 0 0 1 0; -fx-padding: 5 0 3 0;");
        return l;
    }

    private TextField field(String prompt) {
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

    private ComboBox<String> styledCombo(String prompt, String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.getItems().addAll(items);
        c.setPromptText(prompt);
        c.setPrefHeight(42); c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px;"
        );
        c.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? c.getPromptText() : item);
                setStyle(empty || item == null
                    ? "-fx-text-fill: #64748b; -fx-background-color: transparent;"
                    : "-fx-text-fill: white; -fx-background-color: transparent;");
            }
        });
        return c;
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

    private VBox strengthMeter(PasswordField pw) {
        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(Double.MAX_VALUE); bar.setPrefHeight(4);
        bar.setStyle("-fx-accent:#64748b;");
        Label lbl = new Label("Password Strength");
        lbl.setFont(Font.font("Segoe UI", 11)); lbl.setTextFill(Color.web("#64748b"));
        VBox box = new VBox(3, bar, lbl);
        box.setPadding(new Insets(-8, 0, 4, 0));
        pw.textProperty().addListener((obs, o, nv) -> {
            if (nv.isEmpty()) { bar.setProgress(0); lbl.setText("Password Strength"); lbl.setTextFill(Color.web("#64748b")); bar.setStyle("-fx-accent:#64748b;"); return; }
            int s = 0;
            if (nv.length() >= 6) s++;
            if (nv.length() >= 8) s++;
            if (nv.matches(".*[A-Z].*") && nv.matches(".*[a-z].*")) s++;
            if (nv.matches(".*\\d.*")) s++;
            if (nv.matches(".*[!@#$%^&*()].*")) s++;
            if (s <= 2) { bar.setProgress(0.33); lbl.setText("Weak"); lbl.setTextFill(Color.web("#ef4444")); bar.setStyle("-fx-accent:#ef4444;"); }
            else if (s <= 4) { bar.setProgress(0.66); lbl.setText("Good"); lbl.setTextFill(Color.web("#f59e0b")); bar.setStyle("-fx-accent:#f59e0b;"); }
            else { bar.setProgress(1.0); lbl.setText("Strong ✅"); lbl.setTextFill(Color.web("#10b981")); bar.setStyle("-fx-accent:#10b981;"); }
        });
        return box;
    }

    private Button registerButton(String text, String color) {
        Button b = new Button(text);
        b.setPrefWidth(Double.MAX_VALUE); b.setPrefHeight(46);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        String base  = "-fx-background-color:" + color + "; -fx-text-fill:#020617; -fx-background-radius:8; -fx-cursor:hand;";
        String hover = "-fx-background-color: derive(" + color + ",20%); -fx-text-fill:#020617; -fx-background-radius:8; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private HBox accessRow(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label li = new Label(icon); li.setFont(Font.font(12));
        Label lt = new Label(text); lt.setFont(Font.font("Segoe UI", 11));
        lt.setTextFill(icon.equals("✅") ? Color.web("#94a3b8") : Color.web("#475569"));
        row.getChildren().addAll(li, lt);
        return row;
    }

    private boolean columnExists(Connection conn, String table, String col) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, col)) {
            return rs.next();
        } catch (SQLException e) { return false; }
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
