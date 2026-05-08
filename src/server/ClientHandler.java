package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  ClientHandler — One Thread Per Connected Client                     ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * DISTRIBUTED CONCEPT — Processes & Threads (Chapter 3):
 * Each ClientHandler instance runs in its own thread inside the server's
 * ExecutorService thread pool. This allows the server to handle many
 * clients concurrently without blocking.
 *
 * DISTRIBUTED CONCEPT — Message Passing Protocol (Chapter 2):
 * Communication uses a simple text-based protocol over TCP:
 *
 *   CLIENT → SERVER (Commands):
 *   ─────────────────────────────────────────────────────────────────
 *   LIST                          → list all books
 *   SEARCH:keyword                → search books by keyword
 *   ADD:title:author:category     → add a new book
 *   BORROW:bookId:userId:name:days → borrow a book
 *   RETURN:recordId:bookId        → return a book
 *   QUIT                          → disconnect
 *
 *   SERVER → CLIENT (Responses):
 *   ─────────────────────────────────────────────────────────────────
 *   BOOKS:id|title|author|isbn|category|qty|available\n...
 *   BOOKS:EMPTY
 *   SUCCESS:message
 *   ERROR:reason
 *   WELCOME:message
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ConcurrentHashMap<Integer, String> borrowedBooks;

    /**
     * @param socket       the accepted client socket
     * @param borrowedBooks shared in-memory borrow state (from LibraryServer)
     */
    public ClientHandler(Socket socket, ConcurrentHashMap<Integer, String> borrowedBooks) {
        this.socket = socket;
        this.borrowedBooks = borrowedBooks;
    }

    // ─────────────────────────────────────────────────────────────────
    // Thread Entry Point
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();

        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            // Send welcome message to the newly connected client
            out.println("WELCOME:Connected to Distributed Library Server (BDU) — port " + LibraryServer.PORT);

            String line;
            // Read commands line-by-line until client disconnects or sends QUIT
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                System.out.println("[" + clientAddr + "] → " + line);

                String response = handleCommand(line);

                // Send response back to this client
                out.println(response);
                System.out.println("[" + clientAddr + "] ← " + summarize(response));

                // If client said QUIT, close the connection
                if ("QUIT".equalsIgnoreCase(line)) break;
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Client " + clientAddr + " disconnected unexpectedly: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("[-] Client disconnected: " + clientAddr);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Command Dispatcher
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parses a raw command string and dispatches to the appropriate handler.
     *
     * DISTRIBUTED CONCEPT — Message Passing:
     * This is the protocol parser. Each command is a colon-delimited string.
     * The server interprets the command and returns a structured response.
     *
     * @param command raw command line from the client
     * @return response string to send back
     */
    private String handleCommand(String command) {
        // Split on ":" — first token is the command verb
        String[] parts = command.split(":", -1);
        String verb = parts[0].toUpperCase();

        switch (verb) {

            // ── LIST — return all books ──────────────────────────────
            case "LIST":
                return LibraryServer.listBooks("");

            // ── SEARCH:keyword — search books ───────────────────────
            case "SEARCH":
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    return "ERROR:SEARCH requires a keyword. Usage: SEARCH:keyword";
                }
                return LibraryServer.listBooks(parts[1]);

            // ── ADD:title:author:category — add a book ───────────────
            case "ADD":
                if (parts.length < 4) {
                    return "ERROR:ADD requires 3 params. Usage: ADD:title:author:category";
                }
                return LibraryServer.addBook(parts[1], parts[2], parts[3]);

            // ── BORROW:bookId:userId:name:days — borrow a book ───────
            // DISTRIBUTED CONCEPT — Mutual Exclusion:
            // LibraryServer.borrowBook() is synchronized, so only one
            // ClientHandler thread can execute it at a time.
            case "BORROW":
                if (parts.length < 5) {
                    return "ERROR:BORROW requires 4 params. Usage: BORROW:bookId:userId:name:days";
                }
                try {
                    int bookId   = Integer.parseInt(parts[1].trim());
                    int userId   = Integer.parseInt(parts[2].trim());
                    String name  = parts[3].trim();
                    int days     = Integer.parseInt(parts[4].trim());
                    return LibraryServer.borrowBook(bookId, userId, name, days);
                } catch (NumberFormatException e) {
                    return "ERROR:bookId, userId, and days must be integers";
                }

            // ── RETURN:recordId:bookId[:penalty] — return a book ────────
            // penalty is optional, defaults to 0.0
            case "RETURN":
                if (parts.length < 3) {
                    return "ERROR:RETURN requires at least 2 params. Usage: RETURN:recordId:bookId[:penalty]";
                }
                try {
                    int    recordId = Integer.parseInt(parts[1].trim());
                    int    bookId   = Integer.parseInt(parts[2].trim());
                    double penalty  = parts.length >= 4 ? Double.parseDouble(parts[3].trim()) : 0.0;
                    return LibraryServer.returnBook(recordId, bookId, penalty);
                } catch (NumberFormatException e) {
                    return "ERROR:recordId and bookId must be integers; penalty must be a number";
                }

            // ── QUIT — graceful disconnect ───────────────────────────
            case "QUIT":
                return "SUCCESS:Goodbye! Disconnected from server.";

            // ── Unknown command ──────────────────────────────────────
            default:
                return "ERROR:Unknown command '" + verb
                        + "'. Valid commands: LIST, SEARCH, ADD, BORROW, RETURN, QUIT";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────

    /** Truncates long BOOKS responses in the server console log. */
    private String summarize(String response) {
        if (response != null && response.startsWith("BOOKS:") && response.length() > 80) {
            return response.substring(0, 80) + "... [" + response.split("\n").length + " books]";
        }
        return response;
    }
}
