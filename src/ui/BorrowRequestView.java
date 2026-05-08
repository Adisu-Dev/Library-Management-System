package ui;

import db.BorrowDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Feature 2 — Librarian Approval Workflow
 *
 * Displays all pending borrow requests fetched from the BorrowRequests table.
 * The librarian can:
 *   • Approve  → issues the book (inserts BorrowRecord, decrements AvailableQuantity,
 *                marks request as 'Approved') — all in one atomic DB transaction.
 *   • Reject   → marks request as 'Rejected' without touching book availability.
 *
 * The ObservableList<RequestRecord> is bound directly to the TableView so the
 * table refreshes automatically whenever the list changes.
 *
 * Threading: DB calls run on a background executor; all list mutations happen
 * inside Platform.runLater() to keep the FX thread safe.
 */
public class BorrowRequestView {

    // ── Model ─────────────────────────────────────────────────────────
    public static class RequestRecord {
        private final int    requestId, bookId, userId;
        private final String bookTitle, studentName, requestedAt, status;

        public RequestRecord(int requestId, String bookTitle, String studentName,
                             int userId, int bookId, String requestedAt, String status) {
            this.requestId   = requestId;
            this.bookTitle   = bookTitle;
            this.studentName = studentName;
            this.userId      = userId;
            this.bookId      = bookId;
            this.requestedAt = requestedAt;
            this.status      = status;
        }

        public int    getRequestId()     { return requestId; }
        public int    getBookId()        { return bookId; }
        public int    getUserId()        { return userId; }
        public String getBookTitle()     { return bookTitle; }
        public String getStudentName()   { return studentName; }
        public String getRequestedAt()   { return requestedAt; }
        public String getStatus()        { return status; }
    }

    // ── State ─────────────────────────────────────────────────────────
    /** Observable list bound to the TableView — updates propagate automatically. */
    private final ObservableList<RequestRecord> masterData =
            FXCollections.observableArrayList();

    private TableView<RequestRecord> table;
    private Label lblCount;

    /** Optional callback fired after an approval so the parent dashboard can refresh KPIs. */
    private Runnable onApproved;
    public void setOnApproved(Runnable r) { this.onApproved = r; }

    private static final java.util.concurrent.ExecutorService BG =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "BorrowRequests-Loader");
                t.setDaemon(true);
                return t;
            });

    // ─────────────────────────────────────────────────────────────────
    // Build the view
    // ─────────────────────────────────────────────────────────────────
    public VBox getView() {
        VBox root = new VBox(16);
        root.setStyle("-fx-background-color: #f4f7f6;");
        root.setPadding(new Insets(24, 36, 24, 36));

        // ── Header ────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("📋 Pending Borrow Requests");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lbl.setTextFill(Color.web("#1e293b"));

        lblCount = new Label();
        lblCount.setFont(Font.font("Segoe UI", 13));
        lblCount.setTextFill(Color.web("#64748b"));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
        btnRefresh.setOnAction(e -> loadRequests());

        header.getChildren().addAll(lbl, lblCount, sp, btnRefresh);

        // ── Table ─────────────────────────────────────────────────────
        table = new TableView<>(masterData);   // bound directly to the ObservableList
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        table.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.06)));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<RequestRecord, Integer> colId = new TableColumn<>("Req #");
        colId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colId.setPrefWidth(60);

        TableColumn<RequestRecord, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));

        TableColumn<RequestRecord, String> colStudent = new TableColumn<>("Student");
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));

        TableColumn<RequestRecord, String> colTime = new TableColumn<>("Requested At");
        colTime.setCellValueFactory(new PropertyValueFactory<>("requestedAt"));
        colTime.setPrefWidth(150);

        // Action column — Approve / Reject buttons
        TableColumn<RequestRecord, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(210);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject  = new Button("❌ Reject");
            private final HBox   pane       = new HBox(8, btnApprove, btnReject);
            {
                btnApprove.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                btnReject.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                pane.setAlignment(Pos.CENTER_LEFT);

                // ── Approve ───────────────────────────────────────────
                btnApprove.setOnAction(e -> {
                    RequestRecord rec = getTableView().getItems().get(getIndex());
                    btnApprove.setDisable(true);
                    btnReject.setDisable(true);

                    BG.submit(() -> {
                        // approveRequest: inserts BorrowRecord + decrements AvailableQuantity
                        // + marks BorrowRequests.Status = 'Approved' — all in one transaction
                        String result = BorrowDAO.approveRequest(
                            rec.getRequestId(), rec.getBookId(), rec.getUserId(), 14);

                        Platform.runLater(() -> {
                            if ("Success".equals(result)) {
                                showAlert(Alert.AlertType.INFORMATION, "Approved",
                                    "Book issued to " + rec.getStudentName() + " for 14 days.\n" +
                                    "Book availability has been updated.");
                                loadRequests();
                                if (onApproved != null) onApproved.run();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Approval Failed", result);
                                btnApprove.setDisable(false);
                                btnReject.setDisable(false);
                            }
                        });
                    });
                });

                // ── Reject ────────────────────────────────────────────
                btnReject.setOnAction(e -> {
                    RequestRecord rec = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Reject request for \"" + rec.getBookTitle() + "\" by " +
                        rec.getStudentName() + "?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Confirm Rejection");
                    confirm.setHeaderText(null);

                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        btnApprove.setDisable(true);
                        btnReject.setDisable(true);

                        BG.submit(() -> {
                            // rejectRequest: marks Status = 'Rejected', book availability unchanged
                            BorrowDAO.rejectRequest(
                                rec.getRequestId(), rec.getUserId(), rec.getBookId());

                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Rejected",
                                    "Request #" + rec.getRequestId() + " has been rejected.");
                                loadRequests();
                            });
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colId, colBook, colStudent, colTime, colAction);

        Label lblEmpty = new Label("✅ No pending requests — all caught up!");
        lblEmpty.setFont(Font.font("Segoe UI", 14));
        lblEmpty.setTextFill(Color.web("#64748b"));
        table.setPlaceholder(lblEmpty);

        root.getChildren().addAll(header, table);

        // Initial load
        loadRequests();
        return root;
    }

    // ─────────────────────────────────────────────────────────────────
    // Feature 2 core: background fetch → Platform.runLater list update
    // ─────────────────────────────────────────────────────────────────
    private void loadRequests() {
        BG.submit(() -> {
            List<String[]> rows = BorrowDAO.getPendingRequests();

            Platform.runLater(() -> {
                masterData.clear();
                for (String[] r : rows) {
                    masterData.add(new RequestRecord(
                        Integer.parseInt(r[0]),   // requestId
                        r[1],                     // bookTitle
                        r[2],                     // studentName
                        Integer.parseInt(r[3]),   // userId
                        Integer.parseInt(r[4]),   // bookId
                        r[5],                     // requestedAt
                        r[6]                      // status
                    ));
                }
                lblCount.setText(masterData.isEmpty()
                    ? "" : "(" + masterData.size() + " pending)");
            });
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
