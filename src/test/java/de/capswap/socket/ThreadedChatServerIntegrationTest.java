package de.capswap.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.capswap.entity.Company;
import de.capswap.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ThreadedChatServerIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private de.capswap.repository.ChatMessageRepository chatMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThreadedChatServer chatServer;

    private Company sender;
    private Company recipient;

    @BeforeEach
    void setUp() {
        sender = companyRepository.save(Company.builder()
                .name("Sender Inc")
                .email("sender-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .build());

        recipient = companyRepository.save(Company.builder()
                .name("Recipient Inc")
                .email("recipient-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .build());
    }

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
        if (sender != null) companyRepository.delete(sender);
        if (recipient != null) companyRepository.delete(recipient);
    }

    @Test
    void testChatLatencyIsUnder200ms() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong startTime = new AtomicLong();
        AtomicLong endTime = new AtomicLong();
        AtomicReference<SocketMessage> receivedMessage = new AtomicReference<>();

        try (
            Socket senderSocket = new Socket("localhost", chatServer.getPort());
            Socket recipientSocket = new Socket("localhost", chatServer.getPort());
            
            PrintWriter senderOut = new PrintWriter(senderSocket.getOutputStream(), true);
            
            PrintWriter recipientOut = new PrintWriter(recipientSocket.getOutputStream(), true);
            BufferedReader recipientIn = new BufferedReader(new InputStreamReader(recipientSocket.getInputStream()))
        ) {
            // Authenticate Sender
            SocketMessage senderAuth = new SocketMessage();
            senderAuth.setType("AUTH");
            senderAuth.setCompanyId(sender.getId());
            senderOut.println(objectMapper.writeValueAsString(senderAuth));

            // Authenticate Recipient
            SocketMessage recipientAuth = new SocketMessage();
            recipientAuth.setType("AUTH");
            recipientAuth.setCompanyId(recipient.getId());
            recipientOut.println(objectMapper.writeValueAsString(recipientAuth));

            // Wait a moment for the server to process AUTH messages
            Thread.sleep(500);

            // Start Recipient listening thread
            Thread recipientThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = recipientIn.readLine()) != null) {
                        endTime.set(System.currentTimeMillis());
                        SocketMessage msg = objectMapper.readValue(line, SocketMessage.class);
                        if ("MESSAGE".equals(msg.getType())) {
                            receivedMessage.set(msg);
                            latch.countDown();
                            break; // Stop listening after the first message
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            recipientThread.start();

            // Prepare and send message from Sender
            String testText = "Hello from integration test!";
            SocketMessage chatMessage = new SocketMessage();
            chatMessage.setType("MESSAGE");
            chatMessage.setSenderId(sender.getId());
            chatMessage.setRecipientId(recipient.getId());
            chatMessage.setMessage(testText);

            String jsonMessage = objectMapper.writeValueAsString(chatMessage);
            
            // Mark start time immediately before writing
            startTime.set(System.currentTimeMillis());
            senderOut.println(jsonMessage);

            // Wait up to 5 seconds for the message to be received
            boolean received = latch.await(5, TimeUnit.SECONDS);

            // Assertions
            assertTrue(received, "Message was not received within timeout");
            
            long duration = endTime.get() - startTime.get();
            System.out.println("Latency: " + duration + " ms");
            
            assertThat(duration)
                    .as("Time between outputStream.write() and inputStream.read() must be < 0.2s (200ms)")
                    .isLessThan(200);

            assertThat(receivedMessage.get()).isNotNull();
            assertThat(receivedMessage.get().getMessage()).isEqualTo(testText);
            assertThat(receivedMessage.get().getSenderId()).isEqualTo(sender.getId());
        }
    }
}
