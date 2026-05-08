package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  LibraryClient — TCP Client for Distributed Library System          ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * DISTRIBUTED CONCEPT — TCP Socket Communication (Chapter 2):
 * This class wraps Java's Socket API to provide a clean interface for
 * sending commands to the LibraryServer and receiving responses.
 *
 * The client uses a persistent TCP connection (not a new connection per
 * request) to reduce latency and demonstrate stateful communication.
 *
 * DISTRIBUTED CONCEPT — Client-Server Model (Chapter 1):
 * The client has NO direct database access. All data operations go
 * through the server via the text protocol. This is the fundamental
 * separation of concerns in a distributed system.
 *
 * Usage:
 *   LibraryClient client = new LibraryClient("localhost", 9090);
 *   client.connect();
 *   String books = client.sendCommand("LIST");
 *   String result = client.sendCommand("BORROW:5:101:Alice:14");
 *   client.disconnect();
 */
public class LibraryClient {

    private static final Logger LOGGER = Logger.getLogger(LibraryClient.class.getName());

    /** Read timeout in milliseconds — prevents blocking forever on a dead server. */
    private static final int READ_TIMEOUT_MS = 10_000;

    private final String serverHost;
    private final int    serverPort;

    private Socket       socket;
    private PrintWriter  out;
    private BufferedReader in;

    private boolean connected = false;

    /**
     * @param serverHost hostname or IP of the LibraryServer
     * @param serverPort TCP port (default 9090)
     */
    public LibraryClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    // ─────────────────────────────────────────────────────────────────
    // Connection Management
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens a TCP connection to the server.
     *
     * DISTRIBUTED CONCEPT — TCP Handshake:
     * new Socket(host, port) performs the TCP three-way handshake
     * (SYN → SYN-ACK → ACK) before returning. After this call,
     * a reliable byte stream is established between client and server.
     *
     * @return the WELCOME message from the server, or an error string
     * @throws IOException if the connection cannot be established
     */
    public String connect() throws IOException {
        socket = new Socket(serverHost, serverPort);
        socket.setSoTimeout(READ_TIMEOUT_MS);

        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        connected = true;

        // Read the WELCOME message the server sends on connect
        String welcome = in.readLine();
        LOGGER.info("Connected to server: " + welcome);
        return welcome != null ? welcome : "WELCOME:Connected";
    }

    /**
     * Closes the TCP connection gracefully by sending QUIT first.
     */
    public void disconnect() {
        if (!connected) return;
        try {
            if (out != null) out.println("QUIT");
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during disconnect: ", e);
        } finally {
            connected = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Command / Response
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sends a command to the server and reads the full response.
     *
     * DISTRIBUTED CONCEPT — Message Passing Protocol (Chapter 2):
     * Commands are plain text lines terminated by '\n'.
     * Responses may span multiple lines (e.g., BOOKS: with one book per line).
     * The client reads until it sees a line that starts with a known
     * response prefix (SUCCESS:, ERROR:, BOOKS:, WELCOME:).
     *
     * @param command the command string, e.g. "LIST" or "BORROW:5:101:Alice:14"
     * @return the full server response as a single string
     */
    public String sendCommand(String command) {
        if (!connected) {
            return "ERROR:Not connected to server";
        }

        try {
            // Send the command
            out.println(command);

            // Read the response — may be multi-line for BOOKS:
            StringBuilder response = new StringBuilder();
            String line;

            // Read the first line to determine response type
            line = in.readLine();
            if (line == null) {
                connected = false;
                return "ERROR:Server closed the connection";
            }
            response.append(line);

            // If it's a BOOKS response, keep reading until we get all books
            if (line.startsWith("BOOKS:") && !line.equals("BOOKS:EMPTY")) {
                // Read additional book lines until we hit a blank line or timeout
                try {
                    socket.setSoTimeout(500); // short timeout for subsequent lines
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        response.append("\n").append(line);
                    }
                } catch (SocketTimeoutException e) {
                    // No more lines — that's fine, we have all the data
                } finally {
                    socket.setSoTimeout(READ_TIMEOUT_MS); // restore normal timeout
                }
            }

            return response.toString();

        } catch (SocketTimeoutException e) {
            return "ERROR:Server did not respond within " + (READ_TIMEOUT_MS / 1000) + " seconds";
        } catch (IOException e) {
            connected = false;
            return "ERROR:Connection lost — " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Convenience Methods (wrap the text protocol)
    // ─────────────────────────────────────────────────────────────────

    /** @return "BOOKS:..." response string */
    public String listBooks()                                          { return sendCommand("LIST"); }

    /** @return "BOOKS:..." response string */
    public String searchBooks(String keyword)                          { return sendCommand("SEARCH:" + keyword); }

    /** @return "SUCCESS:..." or "ERROR:..." */
    public String addBook(String title, String author, String category){ return sendCommand("ADD:" + title + ":" + author + ":" + category); }

    /** @return "SUCCESS:..." or "ERROR:..." */
    public String borrowBook(int bookId, int userId, String name, int days) {
        return sendCommand("BORROW:" + bookId + ":" + userId + ":" + name + ":" + days);
    }

    /** @return "SUCCESS:..." or "ERROR:..." */
    public String returnBook(int recordId, int bookId)                 { return sendCommand("RETURN:" + recordId + ":" + bookId + ":0.0"); }

    // ─────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────

    public boolean isConnected() { return connected; }
    public String  getServerHost() { return serverHost; }
    public int     getServerPort() { return serverPort; }
}
