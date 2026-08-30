package de.capswap.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.capswap.entity.ChatMessage;
import de.capswap.entity.Company;
import de.capswap.repository.CompanyRepository;
import de.capswap.service.ChatMessageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadedChatServer {

    private final ChatMessageService chatMessageService;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    // Maps companyId to the ClientHandler
    private final Map<Long, ClientHandler> activeClients = new ConcurrentHashMap<>();

    @PostConstruct
    public void startServer() {
        // Start the server socket accept loop in a background thread
        new Thread(this::runServer, "ChatServer-Accept-Thread").start();
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }
    }

    @org.springframework.context.event.EventListener
    public void onNewChatMessage(NewChatMessageEvent event) {
        ChatMessage message = event.getChatMessage();
        Long recipientId = message.getRecipientCompany().getId();
        
        ClientHandler recipientHandler = activeClients.get(recipientId);
        if (recipientHandler != null) {
            try {
                SocketMessage outgoingMsg = new SocketMessage();
                outgoingMsg.setType("MESSAGE");
                outgoingMsg.setSenderId(message.getSenderCompany().getId());
                outgoingMsg.setRecipientId(recipientId);
                outgoingMsg.setMessage(message.getMessage());

                recipientHandler.sendMessage(objectMapper.writeValueAsString(outgoingMsg));
            } catch (Exception e) {
                log.error("Failed to broadcast message from REST to socket", e);
            }
        }
    }

    @org.springframework.beans.factory.annotation.Value("${capswap.socket.chat.port:8081}")
    private int port;

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Threaded Socket Chat Server started on port {}", serverSocket.getLocalPort());

            while (running) {
                Socket clientSocket = serverSocket.accept();
                log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                
                // Spawn a new thread for each client
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (running) {
                log.error("Server socket error", e);
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
                log.warn("Client connection lost: {}", e.getMessage());
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
                        log.info("Company {} authenticated on socket", this.companyId);
                    }
                } else if ("MESSAGE".equals(msg.getType())) {
                    if (this.companyId == null) {
                        log.warn("Unauthenticated client tried to send a message");
                        return;
                    }

                    Long recipientId = msg.getRecipientId();
                    String text = msg.getMessage();

                    // Save to DB
                    Company sender = companyRepository.findById(this.companyId).orElse(null);
                    Company recipient = companyRepository.findById(recipientId).orElse(null);
                    
                    if (sender != null && recipient != null) {
                        ChatMessage chatMessage = ChatMessage.builder()
                                .senderCompany(sender)
                                .recipientCompany(recipient)
                                .message(text)
                                .isRead(false)
                                .build();
                        
                        // Saving the message will trigger the NewChatMessageEvent,
                        // which is then caught by our EventListener above to forward it.
                        chatMessageService.sendMessage(chatMessage);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse or handle message: {}", line, e);
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
                log.error("Error closing client socket", e);
            }
            log.info("Client disconnected");
        }
    }
}
