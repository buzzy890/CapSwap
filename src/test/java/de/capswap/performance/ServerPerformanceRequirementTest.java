package de.capswap.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance requirement test:
 * "Der Server muss bei weniger als 50 Anfragen pro Sekunde jede Anfrage in unter 0,2 Sekunden beantworten.
 * Client sendet Anfragen in angegebener Rate mittels Taktung und misst für jede Anfrage die Antwortzeit.
 * Ergebnis ist die Zahl der Anfragen mit (un-)zufriedenstellender Antwortzeit."
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerPerformanceRequirementTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testServerResponseTimeUnderLoad() throws InterruptedException {
        // Taktung / Load configuration
        int requestsPerSecond = 45; // "weniger als 50 Anfragen pro Sekunde"
        int testDurationSeconds = 3;
        int totalRequests = requestsPerSecond * testDurationSeconds;
        long intervalMs = 1000 / requestsPerSecond;

        // Warm-up request to initialize Jackson, DispatcherServlet, and Hibernate caches.
        // This prevents the very first request from failing the 200ms limit due to JVM classloading.
        restTemplate.getForEntity("/api/listings", String.class);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(requestsPerSecond);
        ExecutorService workerPool = Executors.newFixedThreadPool(requestsPerSecond * 2);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);

        AtomicInteger satisfactoryCount = new AtomicInteger(0);
        AtomicInteger unsatisfactoryCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long maxAllowedLatencyMs = 200; // "unter 0,2 Sekunden"

        System.out.println("Starting Performance Test: " + requestsPerSecond + " requests/sec for " + testDurationSeconds + " seconds.");

        for (int i = 0; i < totalRequests; i++) {
            long initialDelay = (long) i * intervalMs;
            
            scheduler.schedule(() -> {
                workerPool.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        // Sendet Anfragen
                        ResponseEntity<String> response = restTemplate.getForEntity("/api/listings", String.class);
                        
                        // misst für jede Anfrage die Antwortzeit
                        long latency = System.currentTimeMillis() - startTime;
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            if (latency < maxAllowedLatencyMs) {
                                satisfactoryCount.incrementAndGet();
                            } else {
                                unsatisfactoryCount.incrementAndGet();
                                System.err.println("Unsatisfactory latency: " + latency + "ms");
                            }
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }, initialDelay, TimeUnit.MILLISECONDS);
        }

        // Wait for all requests to finish, with a generous timeout buffer
        boolean completed = completionLatch.await(testDurationSeconds + 10, TimeUnit.SECONDS);

        scheduler.shutdownNow();
        workerPool.shutdownNow();

        // Ergebnis ist die Zahl der Anfragen mit (un-)zufriedenstellender Antwortzeit.
        System.out.println("=== Performance Test Results ===");
        System.out.println("Total requests sent: " + totalRequests);
        System.out.println("Satisfactory response times (< " + maxAllowedLatencyMs + "ms): " + satisfactoryCount.get());
        System.out.println("Unsatisfactory response times (>= " + maxAllowedLatencyMs + "ms): " + unsatisfactoryCount.get());
        System.out.println("Errors: " + errorCount.get());

        // Assertions based on requirement
        assertThat(completed).as("All requests should complete within the timeout").isTrue();
        assertThat(errorCount.get()).as("There should be no HTTP errors during the load test").isEqualTo(0);
        assertThat(unsatisfactoryCount.get())
                .as("All requests should be answered in under 0.2 seconds")
                .isEqualTo(0);
    }
}
