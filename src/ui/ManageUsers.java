package ui;

import db.UserDAO;
import model.UserRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;

// 🚀 ከ "extends Stage" ወደ Component ተቀይሯል
public class ManageUsers {
    private TextField txtName, txtUser, txtPhone, txtSearch;
    private PasswordField txtPass;
    private ComboBox<String> comboRole;
    private TableView<UserRecord> userTable;

    private Pagination pagination;
    private ObservableList<UserRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<UserRecord> filteredData;
    private javafx.collections.transformation.SortedList<UserRecord> sortedData;
    private ComboBox<Integer> cmbPageSize;

    private int selectedUserId = -1;

    // 🚀 አሁን ሜተዱ VBox ነው የሚመልሰው (ለ SPA አሰራር)
    public VBox getView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f4f7f6;");

        // --- 1. Header Panel ---
        HBox headerPanel = new HBox(20);
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerPanel.setMinHeight(70);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(0, 30, 0, 30));
        headerPanel.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.05)));

        Label lblHeader = new Label("👥 Manage Users");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblHeader.setTextFill(Color.web("#1e293b"));
        Label lblHeaderSub = new Label("Register librarians · View & remove all members");
        lblHeaderSub.setFont(Font.font("Segoe UI", 13));
        lblHeaderSub.setTextFill(Color.web("#64748b"));
        VBox headerContent = new VBox(2, lblHeader, lblHeaderSub);
        headerPanel.getChildren().add(headerContent);

        // --- Main Content Splitter ---
        HBox mainSplit = new HBox(30);
        mainSplit.setPadding(new Insets(30));

        // --- 2. Left Input Panel (Form Card) ---
        VBox westPanel = new VBox(15);
        westPanel.setPrefWidth(350);

        VBox formCard = new VBox(12);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        formCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));

        txtName = createStyledField("Full Name");
        txtUser = createStyledField("Username");

        txtPass = new PasswordField();
        txtPass.setPromptText("Password (Leave blank to keep old)");
        txtPass.setPrefHeight(40);
        txtPass.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        txtPhone = createStyledField("Phone Number");

        comboRole = new ComboBox<>(FXCollections.observableArrayList("Librarian"));
        comboRole.setValue("Librarian");
        comboRole.setPrefHeight(40); comboRole.setMaxWidth(Double.MAX_VALUE);
        comboRole.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Info note about registration scope
        Label lblNote = new Label("ℹ️ Admin can register Librarian accounts only.\nStudent accounts are self-registered via Sign Up.");
        lblNote.setFont(Font.font("Segoe UI", 11));
        lblNote.setTextFill(Color.web("#64748b"));
        lblNote.setWrapText(true);
        lblNote.setStyle("-fx-background-color: #f0f9ff; -fx-border-color: #bae6fd; -fx-border-radius: 6; -fx-padding: 8 10;");

        Button btnAdd = createActionButton("REGISTER", "#64748b");
        Button btnUpdate = createActionButton("UPDATE", "#64748b");
        Button btnDelete = createActionButton("REMOVE", "#64748b");
        Button btnResetPwd = createActionButton("🔄 RESET PASSWORD", "#64748b");
        Button btnClear = createActionButton("CLEAR", "#64748b");

        btnAdd.setOnAction(e -> registerUser());
        btnUpdate.setOnAction(e -> updateUser());
        btnDelete.setOnAction(e -> deleteUser());
        btnResetPwd.setOnAction(e -> resetPassword());
        btnClear.setOnAction(e -> clearFields());

        formCard.getChildren().addAll(
                new Label("Register Librarian"){{setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); setTextFill(Color.web("#1e293b"));}},
                lblNote,
                new Label("Full Name"), txtName,
                new Label("Username"), txtUser,
                new Label("Password"), txtPass,
                new Label("Phone Number"), txtPhone,
                new Label("Role"), comboRole,
                btnAdd, btnUpdate, btnDelete, btnResetPwd, btnClear
        );

        // Wrap in ScrollPane so all buttons are always reachable
        ScrollPane leftScroll = new ScrollPane(formCard);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        leftScroll.setPrefWidth(350);
        leftScroll.setMinWidth(350);
        westPanel.getChildren().add(leftScroll);

        // --- 3. Center/Table Panel ---
        VBox centerArea = new VBox(20);
        HBox.setHgrow(centerArea, Priority.ALWAYS);

        VBox tableCard = new VBox(15);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;");
        tableCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        HBox tableControls = new HBox(15);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Live Search Members...");
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 0 20; -fx-border-color: #e2e8f0; -fx-border-radius: 25;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label lblRows = new Label("Rows per page:");
        cmbPageSize = new ComboBox<>(FXCollections.observableArrayList(5, 10, 20, 50));
        cmbPageSize.setValue(10);
        cmbPageSize.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0;");
        cmbPageSize.setOnAction(e -> updatePagination());

        Button btnExportExcel = new Button("📊 CSV Export");
        btnExportExcel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btnExportExcel.setPrefHeight(40);
        btnExportExcel.setOnAction(e -> exportUsersData("Excel"));

        Button btnExportPDF = new Button("📄 Text Export");
        btnExportPDF.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btnExportPDF.setPrefHeight(40);
        btnExportPDF.setOnAction(e -> exportUsersData("PDF"));

        tableControls.getChildren().addAll(txtSearch, lblRows, cmbPageSize, btnExportExcel, btnExportPDF);

        userTable = new TableView<>();
        setupTableColumns();
        VBox.setVgrow(userTable, Priority.ALWAYS);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        pagination.setMaxHeight(40);
        pagination.setMinHeight(40);
        HBox paginationBox = new HBox(pagination);
        paginationBox.setAlignment(Pos.CENTER);

        tableCard.getChildren().addAll(
                new Label("📋 Member Directory"){{setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); setTextFill(Color.web("#1e293b"));}},
                tableControls,
                userTable,
                paginationBox
        );
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        centerArea.getChildren().add(tableCard);

        mainSplit.getChildren().addAll(westPanel, centerArea);

        // Header እና Main Content ወደ root ይገባሉ
        root.getChildren().addAll(headerPanel, mainSplit);

        loadUsers();
        setupSearchLogic();

        return root;
    }

    // =========================================================
    // Logic & Operations
    // =========================================================

    private boolean validateInput(String name, String user, String pass, String phone, boolean isNewUser) {
        if (name.isEmpty() || user.isEmpty() || phone.isEmpty()) {
            showAlert("Error", "All fields (Name, Username, Phone) are required!");
            return false;
        }
        if (isNewUser && pass.isEmpty()) {
            showAlert("Error", "Password is required for new users.");
            return false;
        }
        if (!phone.matches("^(09|07)\\d{8}$")) {
            showAlert("Invalid Phone", "Please enter a valid Ethiopian phone number (e.g., 0911223344).");
            return false;
        }
        return true;
    }

    private void registerUser() {
        String name = txtName.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();
        String phone = txtPhone.getText().trim();
        String role = comboRole.getValue();

        if (!validateInput(name, user, pass, phone, true)) return;

        // Librarians are created as BLOCKED by default — Admin must unblock to activate
        if (UserDAO.registerUser(name, user, pass, role, phone, true)) {
            loadUsers(); clearFields();
            showAlert("Success",
                "Librarian account created successfully.\n\n" +
                "⚠️ Account is BLOCKED by default.\n" +
                "Click the '✅ Unblock' button to activate it.");
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) {
            showAlert("Notice", "Please select a user from the table to update.");
            return;
        }
        // Only allow updating Librarian accounts
        UserRecord selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null && !"Librarian".equalsIgnoreCase(selected.getRole())) {
            showAlert("Restricted", "Only Librarian accounts can be edited here.\nStudent accounts are managed by the students themselves.");
            return;
        }
        String name = txtName.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();
        String phone = txtPhone.getText().trim();
        String role = comboRole.getValue();

        if (!validateInput(name, user, pass, phone, false)) return;

        if (UserDAO.updateUser(selectedUserId, name, user, pass, role, phone)) {
            loadUsers(); clearFields();
            showAlert("Success", "User updated successfully!");
        }
    }

    private VBox createPage(int pageIndex) {
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int fromIndex = pageIndex * pageSize;
        ObservableList<UserRecord> source =
            sortedData != null ? FXCollections.observableArrayList(sortedData) :
                                 FXCollections.observableArrayList(masterData);
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        userTable.setItems(fromIndex < source.size()
            ? FXCollections.observableArrayList(source.subList(fromIndex, toIndex))
            : FXCollections.emptyObservableList());
        return new VBox();
    }

    private void updatePagination() {
        if (filteredData == null) filteredData = new FilteredList<>(masterData, p -> true);
        if (sortedData   == null) {
            sortedData = new javafx.collections.transformation.SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        }
        int pageSize  = cmbPageSize != null ? cmbPageSize.getValue() : 10;
        int pageCount = (int) Math.ceil((double) sortedData.size() / pageSize);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        int toIndex = Math.min(pageSize, sortedData.size());
        userTable.setItems(FXCollections.observableArrayList(sortedData.subList(0, toIndex)));
    }

    private void setupTableColumns() {
        TableColumn<UserRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colId.setPrefWidth(50);

        TableColumn<UserRecord, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<UserRecord, String> colUser = new TableColumn<>("Username / BDU ID");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<UserRecord, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<UserRecord, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        // Status column
        TableColumn<UserRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusLabel"));
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.contains("Blocked")
                    ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                    : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        });

        // Block / Unblock action column (Student and Librarian — not Admin)
        TableColumn<UserRecord, Void> colAction = new TableColumn<>("Access Control");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                btn.setPrefWidth(120);
                btn.setOnAction(e -> {
                    UserRecord rec = getTableView().getItems().get(getIndex());
                    boolean nowBlocked = !rec.isBlocked();
                    String action = nowBlocked ? "BLOCK" : "UNBLOCK";
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        action + " account for " + rec.getFullName() + " (" + rec.getUsername() + ")?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Confirm " + action);
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        boolean success = UserDAO.setUserBlocked(rec.getUserId(), nowBlocked);
                        if (success) {
                            loadUsers();
                            int currentPage = pagination.getCurrentPageIndex();
                            pagination.setCurrentPageIndex(0);
                            pagination.setCurrentPageIndex(currentPage);
                            showAlert("Success", "Account " + action + "ED successfully.");
                        } else {
                            showAlert("Error",
                                "Could not update account status.\n\n" +
                                "Make sure the IsBlocked column exists in your database.\n" +
                                "Run this SQL in SSMS:\n" +
                                "ALTER TABLE Users ADD IsBlocked BIT NOT NULL DEFAULT 0;");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                UserRecord rec = getTableView().getItems().get(getIndex());
                // Show button for Student and Librarian — hide for Admin
                String role = rec.getRole();
                if ("Admin".equalsIgnoreCase(role)) {
                    setGraphic(null); return;
                }
                if (rec.isBlocked()) {
                    btn.setText("✅ Unblock");
                    btn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                } else {
                    btn.setText("🚫 Block");
                    btn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                }
                setGraphic(btn);
            }
        });

        userTable.getColumns().addAll(colId, colName, colUser, colRole, colPhone, colStatus, colAction);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedUserId = newVal.getUserId();
                String role = newVal.getRole();
                boolean isLibrarian = "Librarian".equalsIgnoreCase(role);

                if (isLibrarian) {
                    txtName.setText(newVal.getFullName());
                    txtUser.setText(newVal.getUsername());
                    comboRole.setValue(role);
                    txtPhone.setText(newVal.getPhone() != null ? newVal.getPhone() : "");
                    txtName.setDisable(false);
                    txtUser.setDisable(false);
                    txtPass.setDisable(false);
                    txtPhone.setDisable(false);
                } else {
                    txtName.setText(newVal.getFullName() + "  [" + role + " — read only]");
                    txtUser.setText(newVal.getUsername());
                    txtPhone.setText(newVal.getPhone() != null ? newVal.getPhone() : "");
                    comboRole.setValue("Librarian");
                    txtName.setDisable(true);
                    txtUser.setDisable(true);
                    txtPass.setDisable(true);
                    txtPhone.setDisable(true);
                }
            }
        });
    }

    private void setupSearchLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData   = new javafx.collections.transformation.SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        txtSearch.textProperty().addListener((obs, old, nv) -> {
            filteredData.setPredicate(user -> {
                if (nv == null || nv.isEmpty()) return true;
                String filter = nv.toLowerCase();
                return user.getFullName().toLowerCase().contains(filter) ||
                        user.getUsername().toLowerCase().contains(filter) ||
                        user.getRole().toLowerCase().contains(filter);
            });
            updatePagination();
        });
    }

    private void loadUsers() {
        masterData.setAll(UserDAO.getAllUsersWithStatus());
        updatePagination();
        // Force table to re-render all cells (updates Status and Access Control columns)
        userTable.refresh();
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            showAlert("Notice", "Select a member to remove.");
            return;
        }
        if (UserDAO.deleteUser(selectedUserId)) {
            loadUsers(); clearFields();
            showAlert("Success", "Member removed.");
        }
    }

    private void resetPassword() {
        if (selectedUserId == -1) {
            showAlert("Notice", "Please select a user from the table first.");
            return;
        }
        UserRecord selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Prompt for new password
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset password for: " + selected.getFullName());
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Enter new password");
        newPassField.setPrefHeight(40);
        newPassField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox content = new VBox(8, new Label("New Password:"), newPassField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? newPassField.getText() : null);
        dialog.showAndWait().ifPresent(newPass -> {
            if (newPass == null || newPass.trim().isEmpty()) {
                showAlert("Error", "Password cannot be empty.");
                return;
            }
            if (UserDAO.updateUser(selectedUserId, selected.getFullName(), selected.getUsername(),
                    newPass, selected.getRole(), selected.getPhone() != null ? selected.getPhone() : "")) {
                showAlert("Success", "Password reset successfully for " + selected.getFullName() + ".");
            } else {
                showAlert("Error", "Failed to reset password. Please try again.");
            }
        });
    }

    private void clearFields() {
        txtName.clear(); txtUser.clear(); txtPass.clear(); txtPhone.clear();
        txtName.setDisable(false); txtUser.setDisable(false);
        txtPass.setDisable(false); txtPhone.setDisable(false);
        comboRole.setValue("Librarian");
        selectedUserId = -1;
        userTable.getSelectionModel().clearSelection();
    }

    private void exportUsersData(String format) {
        File dir = new File("C:\\LMS_Reports");
        if (!dir.exists()) dir.mkdirs();
        String ext = format.equals("Excel") ? ".csv" : ".txt";
        File file = new File(dir, "System_Members_" + LocalDate.now() + ext);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            if (format.equals("Excel")) {
                writer.println("User ID,Full Name,Username,Role,Phone");
                for (UserRecord user : masterData) {
                    writer.printf("%d,%s,%s,%s,%s\n", user.getUserId(), user.getFullName(), user.getUsername(), user.getRole(), user.getPhone());
                }
            } else {
                writer.println("==========================================================");
                writer.println("               OFFICIAL MEMBERS DIRECTORY                 ");
                writer.println("==========================================================");
                for (UserRecord user : masterData) {
                    writer.printf("%-5d | %-20s | %-10s | %-12s\n", user.getUserId(), user.getFullName(), user.getRole(), user.getPhone());
                }
            }
            showAlert("Export Success", "Saved at: " + file.getAbsolutePath());
        } catch (Exception ex) { showAlert("Error", "Export failed."); }
    }

    private TextField createStyledField(String p) {
        TextField t = new TextField(); t.setPromptText(p); t.setPrefHeight(40);
        t.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 0 10;");
        return t;
    }

    private Button createActionButton(String t, String c) {
        Button b = new Button(t); b.setMaxWidth(Double.MAX_VALUE); b.setPrefHeight(45);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base  = "-fx-background-color: " + c + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;";
        String hover = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(title.equals("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
}