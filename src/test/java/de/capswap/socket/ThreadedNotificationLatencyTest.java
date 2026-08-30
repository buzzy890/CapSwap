package de.capswap.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.CompanyRepository;
import de.capswap.service.ListingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ThreadedNotificationLatencyTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingService listingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThreadedNotificationServer notificationServer;

    @Autowired
    private de.capswap.repository.CategorySubscriptionRepository categorySubscriptionRepository;

    private Company subscriber;
    private Company creator;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        subscriber = companyRepository.save(Company.builder()
                .name("Subscriber Inc")
                .email("subscriber-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .build());

        creator = companyRepository.save(Company.builder()
                .name("Creator Inc")
                .email("creator-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .build());

        testCategory = categoryRepository.save(new Category(null, "Test Category " + UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        // We'll let the application clean up or we can delete manually,
        // but typically @Transactional helps. For integration tests with threads,
        // sometimes manual cleanup is safer.
    }

    @Test
    void testNotificationLatencyIsUnderOneMinute() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong startTime = new AtomicLong();
        AtomicLong endTime = new AtomicLong();
        AtomicReference<SocketMessage> receivedMessage = new AtomicReference<>();

        try (
            Socket clientSocket = new Socket("localhost", notificationServer.getPort());
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            // 1. Authenticate Subscriber
            SocketMessage authMsg = new SocketMessage();
            authMsg.setType("AUTH");
            authMsg.setCompanyId(subscriber.getId());
            out.println(objectMapper.writeValueAsString(authMsg));

            // Wait a brief moment to ensure server processes AUTH
            // (Using sleep here since AUTH doesn't change DB state we can easily poll, 
            // but usually we could wait for the subscribe to succeed instead)

            // 2. Subscribe to Category
            SocketMessage subscribeMsg = new SocketMessage();
            subscribeMsg.setType("SUBSCRIBE");
            subscribeMsg.setCompanyId(subscriber.getId());
            subscribeMsg.setCategoryId(testCategory.getId());
            out.println(objectMapper.writeValueAsString(subscribeMsg));

            // Wait for the subscription to be written to the database
            org.awaitility.Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> categorySubscriptionRepository.existsByCompanyIdAndCategoryId(subscriber.getId(), testCategory.getId()));

            // Start a thread to listen for the notification
            Thread listenerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        SocketMessage msg = objectMapper.readValue(line, SocketMessage.class);
                        if ("CATEGORY_NOTIFICATION".equals(msg.getType())) {
                            endTime.set(System.currentTimeMillis());
                            receivedMessage.set(msg);
                            latch.countDown();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listenerThread.start();

            // 3. Create a new Listing (Offer)
            Listing newListing = Listing.builder()
                    .title("Awesome Test Offer")
                    .description("Details here")
                    .location("Berlin")
                    .status(ListingStatus.ACTIVE)
                    .company(creator)
                    .category(testCategory)
                    .expiresAt(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS))
                    .build();

            // Mark start time right before creating the listing
            startTime.set(System.currentTimeMillis());
            
            // This method triggers the NewListingEvent which the server broadcasts
            listingService.createListing(newListing);

            // 4. Wait up to 60 seconds (1 minute) for the notification
            boolean received = latch.await(60, TimeUnit.SECONDS);

            // Assertions
            assertThat(received).as("Notification should be received within 1 minute").isTrue();

            long latencyMs = endTime.get() - startTime.get();
            System.out.println("Notification Latency: " + latencyMs + " ms");
            
            // Requirement is 1 minute (60,000 ms)
            assertThat(latencyMs)
                    .as("Time between offer creation and notification must be < 60s")
                    .isLessThan(60000);

            assertThat(receivedMessage.get()).isNotNull();
            assertThat(receivedMessage.get().getType()).isEqualTo("CATEGORY_NOTIFICATION");
            assertThat(receivedMessage.get().getRecipientId()).isEqualTo(subscriber.getId());
        }
    }
}
