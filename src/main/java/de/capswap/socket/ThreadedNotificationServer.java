package de.capswap.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.capswap.entity.CategorySubscription;
import de.capswap.entity.Listing;
import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.repository.CategorySubscriptionRepository;
import de.capswap.service.CategoryService;
import de.capswap.service.CompanyService;
import de.capswap.service.CategorySubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadedNotificationServer {

    private final CategorySubscriptionService categorySubscriptionService;
    private final CategorySubscriptionRepository categorySubscriptionRepository;
    private final CompanyService companyService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    // Maps companyId to the ClientHandler
    private final Map<Long, ClientHandler> activeClients = new ConcurrentHashMap<>();

    @PostConstruct
    public void startServer() {
        // Start the server socket accept loop in a background thread
        new Thread(this::runServer, "NotificationServer-Accept-Thread").start();
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing notification server socket", e);
        }
    }

    @EventListener
    public void onNewListing(NewListingEvent event) {
        Listing listing = event.getListing();
        Long categoryId = listing.getCategory().getId();

        // Get all subscriptions for this category
        List<CategorySubscription> subscriptions = categorySubscriptionRepository.findByCategoryId(categoryId);

        for (CategorySubscription sub : subscriptions) {
            Long companyId = sub.getCompany().getId();
            // Don't notify the company that created the listing
            if (companyId.equals(listing.getCompany().getId())) {
                continue;
            }

            ClientHandler clientHandler = activeClients.get(companyId);
            if (clientHandler != null) {
                try {
                    SocketMessage outgoingMsg = new SocketMessage();
                    outgoingMsg.setType("CATEGORY_NOTIFICATION");
                    outgoingMsg.setListingId(listing.getId());
                    outgoingMsg.setRecipientId(companyId);
                    outgoingMsg.setMessage("New listing '" + listing.getTitle() + "' in category " + listing.getCategory().getName());

                    clientHandler.sendMessage(objectMapper.writeValueAsString(outgoingMsg));
                } catch (Exception e) {
                    log.error("Failed to broadcast category notification to socket", e);
                }
            }
        }
    }

    @org.springframework.beans.factory.annotation.Value("${capswap.socket.notification.port:8082}")
    private int port;

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(port); // Use configured port for notifications
            log.info("Threaded Socket Notification Server started on port {}", serverSocket.getLocalPort());

            while (running) {
                Socket clientSocket = serverSocket.accept();
                log.info("New client connected to notification server: {}", clientSocket.getRemoteSocketAddress());
                
                // Spawn a new thread for each client
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (running) {
                log.error("Notification Server socket error", e);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Long companyId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue; // Ignore empty lines to prevent Jackson parsing errors
                    }
                    handleMessage(line);
                }
            } catch (IOException e) {
                log.warn("Notification Client connection lost: {}", e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void handleMessage(String line) {
            try {
                SocketMessage msg = objectMapper.readValue(line, SocketMessage.class);
                
                if ("AUTH".equals(msg.getType())) {
                    this.companyId = msg.getCompanyId();
                    if (this.companyId != null) {
                        activeClients.put(this.companyId, this);
                        log.info("Company {} authenticated on notification socket", this.companyId);
                    }
                } else if ("SUBSCRIBE".equals(msg.getType())) {
                    if (this.companyId == null) {
                        log.warn("Unauthenticated client tried to subscribe");
                        return;
                    }
                    if (msg.getCategoryId() != null) {
                        Optional<Company> company = companyService.getCompanyById(this.companyId);
                        Optional<Category> category = categoryService.getCategoryById(msg.getCategoryId());
                        if (company.isPresent() && category.isPresent()) {
                            try {
                                categorySubscriptionService.subscribe(company.get(), category.get());
                                log.info("Company {} subscribed to category {}", this.companyId, msg.getCategoryId());
                            } catch (IllegalArgumentException e) {
                                log.info("Company {} is already subscribed to category {}", this.companyId, msg.getCategoryId());
                            }
                        }
                    }
                } else if ("UNSUBSCRIBE".equals(msg.getType())) {
                    if (this.companyId == null) {
                        log.warn("Unauthenticated client tried to unsubscribe");
                        return;
                    }
                    if (msg.getCategoryId() != null) {
                        categorySubscriptionService.unsubscribe(this.companyId, msg.getCategoryId());
                        log.info("Company {} unsubscribed from category {}", this.companyId, msg.getCategoryId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse or handle notification message: {}", line, e);
            }
        }

        public void sendMessage(String json) {
            if (out != null) {
                out.println(json);
            }
        }

        private void disconnect() {
            if (this.companyId != null) {
                activeClients.remove(this.companyId);
            }
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing notification client socket", e);
            }
            log.info("Notification Client disconnected");
        }
    }
}
