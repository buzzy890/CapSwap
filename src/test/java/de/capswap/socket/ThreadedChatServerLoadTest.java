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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ThreadedChatServerLoadTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThreadedChatServer chatServer;

    private final List<Company> testCompanies = new ArrayList<>();
    private static final int NUM_CONNECTIONS = 10;
    private static final int TEST_DURATION_SECONDS = 5;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < NUM_CONNECTIONS; i++) {
            Company company = companyRepository.save(Company.builder()
                    .name("LoadTest Inc " + i)
                    .email("loadtest-" + UUID.randomUUID() + "@example.com")
                    .passwordHash("hash")
                    .build());
            testCompanies.add(company);
        }
    }

    @AfterEach
    void tearDown() {
        companyRepository.deleteAll(testCompanies);
        testCompanies.clear();
    }

    @Test
    void testConcurrentConnectionsAndStability() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CONNECTIONS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_CONNECTIONS);
        
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger connectionErrors = new AtomicInteger(0);
        AtomicInteger droppedConnections = new AtomicInteger(0);

        for (int i = 0; i < NUM_CONNECTIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready to start simultaneously
                    startLatch.await();
                    
                    try (Socket socket = new Socket("localhost", chatServer.getPort());
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        
                        successfulConnections.incrementAndGet();

                        // Authenticate
                        SocketMessage authMsg = new SocketMessage();
                        authMsg.setType("AUTH");
                        authMsg.setCompanyId(testCompanies.get(index).getId());
                        out.println(objectMapper.writeValueAsString(authMsg));
                        
                        // Keep connection open and read
                        long endTime = System.currentTimeMillis() + (TEST_DURATION_SECONDS * 1000L);
                        while (System.currentTimeMillis() < endTime) {
                            if (socket.isClosed() || !socket.isConnected()) {
                                droppedConnections.incrementAndGet();
                                break;
                            }
                            // Small sleep to prevent busy waiting
                            Thread.sleep(100); 
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        connectionErrors.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();
        
        // Wait for test to finish
        boolean completedInTime = doneLatch.await(TEST_DURATION_SECONDS + 2, TimeUnit.SECONDS);
        
        executor.shutdown();

        System.out.println("=== Load Test Results ===");
        System.out.println("Requested Connections: " + NUM_CONNECTIONS);
        System.out.println("Successful Initial Connections: " + successfulConnections.get());
        System.out.println("Connection Errors (Failures to connect/communicate): " + connectionErrors.get());
        System.out.println("Dropped Connections during test: " + droppedConnections.get());
        
        assertThat(completedInTime).as("Test should complete within the timeout").isTrue();
        assertThat(successfulConnections.get()).as("All connections should be successful").isEqualTo(NUM_CONNECTIONS);
        assertThat(connectionErrors.get()).as("There should be no connection errors").isEqualTo(0);
        assertThat(droppedConnections.get()).as("No connections should drop during the test").isEqualTo(0);
    }
}
