package server;

import db.BookDAO;
import db.BorrowDAO;
import model.Book;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  LibraryServer — TCP Server for Distributed Library System          ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ║  Group Project — Adisu, Dawit, and Dagnachew                        ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * DISTRIBUTED CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────────────
 * 1. TCP SOCKET COMMUNICATION (Chapter 2 — Communication)
 *    • Listens on TCP port 9090
 *    • Accepts multiple client connections via Java ServerSocket API
 *    • Text-based protocol: COMMAND:param1:param2
 *
 * 2. MULTI-THREADING (Chapter 3 — Processes)
 *    • Uses ExecutorService thread pool (up to 20 threads)
 *    • Each client connection runs in its own ClientHandler thread
 *    • Demonstrates concurrent client handling
 *
 * 3. SYNCHRONIZATION (Chapter 5 — Synchronization)
 *    • borrowBook() and returnBook() are synchronized methods
 *    • Prevents two clients from borrowing the same book simultaneously
 *    • Demonstrates mutual exclusion in distributed systems
 *
 * 4. SHARED DATA CONSISTENCY (Chapter 6 — Consistency and Replication)
 *    • ConcurrentHashMap tracks in-memory borrow state
 *    • All clients see the same data (single server = single source of truth)
 *    • Changes by one client are immediately visible to all others
 *
 * 5. MESSAGE PASSING PROTOCOL (Chapter 2 — Communication)
 *    • Custom text protocol over TCP
 *    • Commands: LIST, SEARCH, ADD, BORROW, RETURN, QUIT
 *    • Responses: SUCCESS:msg, ERROR:msg, BOOKS:data
 *
 * 6. CLIENT-SERVER MODEL (Chapter 1 — Introduction)
 *    • Clear separation: server manages data, clients provide UI
 *    • Multiple clients can connect to one server simultaneously
 */
public class LibraryServer {

    private static final Logger LOGGER = Logger.getLogger(LibraryServer.class.getName());

    /** TCP port the server listens on. */
    public static final int PORT = 9090;

    /** Maximum number of concurrent client threads in the pool. */
    private static final int THREAD_POOL_SIZE = 20;

    /**
     * In-memory map tracking which books are currently borrowed and by whom.
     *
     * DISTRIBUTED CONCEPT — Shared State / Mutual Exclusion:
     * This ConcurrentHashMap is the shared resource that all client threads
     * compete to access. The synchronized methods below ensure only one
     * thread can modify it at a time, preventing race conditions.
     *
     * Key   = BookID (Integer)
     * Value = "UserID:BorrowerName" (String)
     */
    private static final ConcurrentHashMap<Integer, String> borrowedBooks = new ConcurrentHashMap<>();

    /** Thread pool — one thread per connected client. */
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    // ─────────────────────────────────────────────────────────────────
    // Server Entry Point
    // ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        new LibraryServer().start();
    }

    /**
     * Starts the TCP server and accepts incoming client connections.
     *
     * DISTRIBUTED CONCEPT — TCP Communication:
     * ServerSocket.accept() blocks until a client connects, then hands
     * the socket to a ClientHandler running in the thread pool.
     */
    public void start() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       DISTRIBUTED LIBRARY SYSTEM — SERVER                ║");
        System.out.println("║       Bahir Dar University — Distributed Systems         ║");
        System.out.println("║       Adisu, Dawit, and Dagnachew                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Listening on port: " + PORT);
            System.out.println("   Thread pool size : " + THREAD_POOL_SIZE);
            System.out.println("   Protocol         : TEXT (COMMAND:param1:param2)");
            System.out.println("   Synchronization  : synchronized borrow/return methods");
            System.out.println();
            System.out.println("Waiting for clients...");

            // Accept loop — runs forever until the process is killed
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddr = clientSocket.getInetAddress().getHostAddress();
                System.out.println("[+] Client connected: " + clientAddr
                        + "  (active threads: " + Thread.activeCount() + ")");

                // Hand off to a dedicated thread — demonstrates multi-threading
                threadPool.execute(new ClientHandler(clientSocket, borrowedBooks));
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error: ", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Synchronized Operations — Mutual Exclusion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Attempts to borrow a book for a given user.
     *
     * DISTRIBUTED CONCEPT — Synchronization / Mutual Exclusion:
     * The 'synchronized' keyword ensures that only ONE thread can execute
     * this method at a time. If two clients try to borrow the same book
     * simultaneously, the second one will block until the first finishes,
     * then receive an "already borrowed" error.
     *
     * This prevents the classic race condition:
     *   Client A reads: book is available
     *   Client B reads: book is available
     *   Client A borrows: success
     *   Client B borrows: should fail, but without sync it might succeed!
     *
     * @param bookId   the ID of the book to borrow
     * @param userId   the ID of the borrowing user
     * @param userName the display name of the borrowing user
     * @param days     number of days to borrow
     * @return "SUCCESS:message" or "ERROR:reason"
     */
    public static synchronized String borrowBook(int bookId, int userId, String userName, int days) {
        // Check in-memory state first (fast path — avoids DB round-trip)
        if (borrowedBooks.containsKey(bookId)) {
            String borrowerInfo = borrowedBooks.get(bookId);
            return "ERROR:Book is already borrowed by " + borrowerInfo;
        }

        // Delegate to BorrowDAO which handles DB transaction
        String result = BorrowDAO.issueBook(bookId, userId, days);

        if (result != null && result.startsWith("Success")) {
            // Record in shared in-memory map — visible to ALL client threads
            borrowedBooks.put(bookId, userId + ":" + userName);
            return "SUCCESS:" + result;
        } else {
            return "ERROR:" + (result != null ? result : "Could not borrow book");
        }
    }

    /**
     * Returns a previously borrowed book.
     *
     * DISTRIBUTED CONCEPT — Synchronization:
     * Also synchronized to prevent concurrent return attempts on the same book.
     *
     * @param recordId      the borrow record ID to close
     * @param bookId        the book being returned (to update in-memory map)
     * @param penaltyAmount any late-return penalty (0.0 if none)
     * @return "SUCCESS:message" or "ERROR:reason"
     */
    public static synchronized String returnBook(int recordId, int bookId, double penaltyAmount) {
        String result = BorrowDAO.returnBook(recordId, penaltyAmount);

        if (result != null && result.startsWith("Success")) {
            // Remove from shared map — book is now available to all clients
            borrowedBooks.remove(bookId);
            return "SUCCESS:Book returned successfully";
        } else {
            return "ERROR:" + (result != null ? result : "Could not return book");
        }
    }

    /**
     * Lists all books from the database.
     * Serializes each book as a pipe-delimited string for the text protocol.
     *
     * @param keyword optional search keyword (empty = all books)
     * @return "BOOKS:id|title|author|isbn|category|qty|available\n..." or "ERROR:..."
     */
    public static String listBooks(String keyword) {
        try {
            List<Book> books;
            if (keyword == null || keyword.trim().isEmpty()) {
                books = BookDAO.getAllBooks();
            } else {
                books = BookDAO.searchBooks(keyword.trim());
            }

            if (books.isEmpty()) {
                return "BOOKS:EMPTY";
            }

            StringBuilder sb = new StringBuilder("BOOKS:");
            for (Book b : books) {
                sb.append(b.getBookID()).append("|")
                  .append(b.getTitle()).append("|")
                  .append(b.getAuthor()).append("|")
                  .append(b.getIsbn()).append("|")
                  .append(b.getCategory()).append("|")
                  .append(b.getQuantity()).append("|")
                  .append(b.getAvailableQuantity())
                  .append("\n");
            }
            return sb.toString().trim();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "listBooks error: ", e);
            return "ERROR:Failed to retrieve books: " + e.getMessage();
        }
    }

    /**
     * Adds a new book to the database.
     *
     * @param title    book title
     * @param author   book author
     * @param category book category
     * @return "SUCCESS:Book added" or "ERROR:reason"
     */
    public static String addBook(String title, String author, String category) {
        try {
            Book book = new Book(title, author, "", category, 1, 1);
            boolean ok = BookDAO.addBook(book);
            return ok ? "SUCCESS:Book added successfully" : "ERROR:Failed to add book to database";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "addBook error: ", e);
            return "ERROR:" + e.getMessage();
        }
    }
}
