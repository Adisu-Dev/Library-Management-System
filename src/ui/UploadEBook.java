package ui;

import db.DatabaseConnection;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UploadEBook {

    private File selectedFile;
    private VBox view; // ዋናውን ቪው የሚይዝ

    // ፎርሙን ሪሴት ለማድረግ እንዲመች ፊልዶቹን ወደዚህ አውጥተናቸዋል
    private TextField txtTitle, txtAuthor, txtISBN, txtQuantity;
    private ComboBox<String> cmbCategory;
    private Label lblFileStatus, lblProgressText;
    private VBox dropZone;
    private ProgressBar progressBar;
    private Button btnUpload, btnCancel;

    public UploadEBook() {
        // Stage አጥፍተን VBox ፈጥረናል
        view = new VBox(20);
        view.setPadding(new Insets(40));
        view.setAlignment(Pos.CENTER);
        view.setStyle("-fx-background-color: white;");

        Label lblHeader = new Label("☁️ Upload E-Book to Digital Library");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblHeader.setTextFill(Color.web("#2c3e50"));

        Label lblNote = new Label("📌 Librarian Tool — Add new e-books to the Digital Library for students to read and download.");
        lblNote.setFont(Font.font("Segoe UI", 13));
        lblNote.setTextFill(Color.web("#64748b"));
        lblNote.setWrapText(true);
        lblNote.setStyle(
            "-fx-background-color: #f0f9ff;" +
            "-fx-border-color: #bae6fd;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 10 14;"
        );

        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-border-color: #e0e0e0; -fx-border-radius: 15;");
        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));

        txtTitle = createTextField("Enter Book Title (e.g. Advanced C#)");
        txtAuthor = createTextField("Author Name");
        txtISBN = createTextField("ISBN (e.g. 978-BDU-05)");
        txtQuantity = createTextField("Number of Copies (e.g. 10)");

        cmbCategory = new ComboBox<>();
        cmbCategory.getItems().addAll("Programming", "Fiction", "Science", "Business", "Technology", "Literature");
        cmbCategory.setPromptText("Select Book Category");
        cmbCategory.setPrefHeight(40);
        cmbCategory.setMaxWidth(Double.MAX_VALUE);

        dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPadding(new Insets(15));
        dropZone.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblDropIcon = new Label("☁️");
        lblDropIcon.setFont(Font.font("Segoe UI Emoji", 26));
        Label lblDropText = new Label("Drag and drop PDF here\nor");
        lblDropText.setTextFill(Color.GRAY);
        lblDropText.setAlignment(Pos.CENTER);

        Button btnFile = new Button("📁 Select PDF Document");
        btnFile.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-cursor: hand;");

        lblFileStatus = new Label("No file chosen");
        lblFileStatus.setTextFill(Color.GRAY);
        lblFileStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        dropZone.getChildren().addAll(lblDropIcon, lblDropText, btnFile, lblFileStatus);

        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    selectedFile = file;
                    updateFileStatus(lblFileStatus, dropZone, file);
                    success = true;
                } else {
                    showAlert(Alert.AlertType.ERROR, "Only PDF files are allowed!");
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        btnFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fc.showOpenDialog(view.getScene() != null ? view.getScene().getWindow() : null);
            if (file != null) {
                selectedFile = file;
                updateFileStatus(lblFileStatus, dropZone, file);
            }
        });

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(15);
        progressBar.setStyle("-fx-accent: #27ae60;");
        progressBar.setVisible(false);

        lblProgressText = new Label("");
        lblProgressText.setTextFill(Color.web("#2980b9"));
        lblProgressText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblProgressText.setVisible(false);

        VBox progressBox = new VBox(5, progressBar, lblProgressText);
        progressBox.setAlignment(Pos.CENTER);

        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        btnUpload = new Button("UPLOAD NOW");
        btnUpload.setPrefHeight(45);
        btnUpload.setPrefWidth(200);
        btnUpload.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnUpload.setOnMouseEntered(e -> btnUpload.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnUpload.setOnMouseExited(e  -> btnUpload.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));

        btnCancel = new Button("CANCEL");
        btnCancel.setPrefHeight(45);
        btnCancel.setPrefWidth(120);
        btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");

        // Cancel ሲነካ ዊንዶው ከመዝጋት ይልቅ ፎርሙን ባዶ ያደርጋል (SPA)
        btnCancel.setOnAction(e -> clearForm());

        actionButtons.getChildren().addAll(btnUpload, btnCancel);

        btnUpload.setOnAction(e -> {
            if (txtTitle.getText().isEmpty() || txtAuthor.getText().isEmpty() || txtISBN.getText().isEmpty() || txtQuantity.getText().isEmpty() || cmbCategory.getValue() == null || selectedFile == null) {
                showAlert(Alert.AlertType.WARNING, "Please fill all fields, select a category, and choose a PDF!");
                return;
            }

            btnUpload.setDisable(true);
            btnCancel.setDisable(true);
            progressBar.setVisible(true);
            lblProgressText.setVisible(true);

            Task<String> uploadTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    for (int i = 0; i <= 100; i++) {
                        Thread.sleep(15);
                        updateProgress(i, 100);
                        updateMessage("Uploading and saving to Database... " + i + "%");
                    }

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        int qty = Integer.parseInt(txtQuantity.getText().trim());

                        String query = "INSERT INTO Books (Title, Author, ISBN, Category, Quantity, AvailableQuantity) VALUES (?, ?, ?, ?, ?, ?)";
                        PreparedStatement pst = conn.prepareStatement(query);
                        pst.setString(1, txtTitle.getText().trim());
                        pst.setString(2, txtAuthor.getText().trim());
                        pst.setString(3, txtISBN.getText().trim());
                        pst.setString(4, cmbCategory.getValue());
                        pst.setInt(5, qty);
                        pst.setInt(6, qty);
                        pst.executeUpdate();

                        return "Success";
                    } catch (Exception ex) {
                        return "Database Error: " + ex.getMessage();
                    }
                }
            };

            progressBar.progressProperty().bind(uploadTask.progressProperty());
            lblProgressText.textProperty().bind(uploadTask.messageProperty());

            uploadTask.setOnSucceeded(event -> {
                String result = uploadTask.getValue();
                if (result.equals("Success")) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Success! '" + txtTitle.getText() + "' has been uploaded to the Digital Library.");
                    success.setHeaderText("Upload Complete");
                    success.showAndWait();
                    clearForm(); // ሲያልቅ ፎርሙን ያጸዳል
                } else {
                    showAlert(Alert.AlertType.ERROR, result);
                    btnUpload.setDisable(false);
                    btnCancel.setDisable(false);
                }
            });

            new Thread(uploadTask).start();
        });

        HBox topRow = new HBox(15,
                new VBox(5, new Label("Book Title"), txtTitle),
                new VBox(5, new Label("Author"), txtAuthor)
        );
        topRow.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox middleRow = new HBox(15,
                new VBox(5, new Label("ISBN"), txtISBN),
                new VBox(5, new Label("Quantity"), txtQuantity)
        );
        middleRow.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        card.getChildren().addAll(
                topRow,
                middleRow,
                new VBox(5, new Label("Category"), cmbCategory),
                dropZone
        );

        view.getChildren().addAll(lblHeader, lblNote, card, progressBox, actionButtons);
    }

    // ለ Dashboard ቪውውን አሳልፎ የሚሰጥ Method (SPA)
    public VBox getView() {
        return view;
    }

    // ፎርሙን ባዶ የሚያደርግ ሜተድ
    private void clearForm() {
        txtTitle.clear();
        txtAuthor.clear();
        txtISBN.clear();
        txtQuantity.clear();
        cmbCategory.setValue(null);
        selectedFile = null;
        lblFileStatus.setText("No file chosen");
        lblFileStatus.setTextFill(Color.GRAY);
        dropZone.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        progressBar.setVisible(false);
        lblProgressText.setVisible(false);
        btnUpload.setDisable(false);
        btnCancel.setDisable(false);
    }

    private TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        return tf;
    }

    private void updateFileStatus(Label lblStatus, VBox dropZone, File file) {
        lblStatus.setText("Selected: " + file.getName());
        lblStatus.setTextFill(Color.web("#27ae60"));
        dropZone.setStyle("-fx-background-color: #e8f8f5; -fx-border-color: #27ae60; -fx-border-style: solid; -fx-border-width: 2; -fx-border-radius: 10;");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}