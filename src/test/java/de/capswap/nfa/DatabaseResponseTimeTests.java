package de.capswap.nfa;

import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //erstellt eine Instanz der App auf einem zufälligen Port
class DatabaseResponseTimeTests { // prüft ob die DB Anfragen innerhalb von 2 Sekunden beantwortet werden

    private static final long MAX_RESPONSE_TIME_MS = 2000;
    private static final int LISTINGS_PER_CATEGORY = 150;
    private static final int CONCURRENT_SEARCH_REQUESTS = 40;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> companyIds = new ArrayList<>();

    @BeforeEach
    void seedRealisticDataVolume() { //erstellt eine realistische Datenmenge von Unternehmen, Kategorien und Listings(5,3,150 pro KAtegorie)
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        for (int c = 0; c < 5; c++) {
            Company company = companyRepository.save(Company.builder()
                    .name("test-Firma " + suffix + "-" + c)
                    .email("loadtest-" + suffix + "-" + c + "@capswap.de")
                    .passwordHash("hashed-password")
                    .location("Hamburg")
                    .build());
            companyIds.add(company.getId());
        }

        for (int cat = 0; cat < 3; cat++) {
            Category category = categoryRepository.save(Category.builder()
                    .name("test-Kategorie " + suffix + "-" + cat)
                    .build());
            categoryIds.add(category.getId());

            List<Listing> batch = new ArrayList<>();
            for (int i = 0; i < LISTINGS_PER_CATEGORY; i++) {
                Company owner = companyRepository.getReferenceById(companyIds.get(i % companyIds.size()));
                batch.add(Listing.builder()
                        .company(owner)
                        .category(category)
                        .title("Angebot " + suffix + "-" + cat + "-" + i)
                        .description(("Beschreibung für test-listing Nr. " + i
                                + " in Kategorie " + cat + ". ").repeat(10))
                        .location("Hamburg")
                        .status(ListingStatus.ACTIVE)
                        .build());
            }
            listingRepository.saveAll(batch);
        }
    }

    @Test
    void complexSearchQueriesRespondWithinTwoSeconds() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool( //erzeugt einen Thread-Pool, um mehrere Suchanfragen gleichzeitig auszuführen
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        try { 
            List<Future<Long>> futures = executor.invokeAll(buildSearchTasks()); //baut und ausfuehrt die Aufgaben

            List<Long> latenciesMs = new ArrayList<>();
            for (Future<Long> future : futures) { //sammelt ergebnisse
                try {
                    latenciesMs.add(future.get());
                } catch (ExecutionException e) {
                    throw new AssertionError("Search-Anfrage ist während des Lasttests durchgefallen", e.getCause());
                }
            }

            long maxLatency = latenciesMs.stream().mapToLong(Long::longValue).max().orElseThrow();
            double avgLatency = latenciesMs.stream().mapToLong(Long::longValue).average().orElseThrow();

            System.out.printf("Datenbank-Lasttest: %d Suchanfragen, max=%dms, avg=%.1fms%n",
                    latenciesMs.size(), maxLatency, avgLatency);

            assertTrue(maxLatency < MAX_RESPONSE_TIME_MS,
                    "Wenigstens 1 Suchanfrage hat mehr als 2 Sekunden gedauert: " + maxLatency + "ms");
        } finally {
            executor.shutdown();
        }
    }

    private List<Callable<Long>> buildSearchTasks() { //erstellt die Suchanfragen mit verschiedenen Parametern für den Test
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_SEARCH_REQUESTS; i++) {
            String url = switch (i % 4) { //auswahl von 4 verschiedenen Suchanfragen. Z.B. 42%4=2 -> case 2
                case 0 -> "/api/listings?categoryId=" + categoryIds.get(i % categoryIds.size());
                case 1 -> "/api/listings?companyId=" + companyIds.get(i % companyIds.size());
                case 2 -> "/api/listings?status=ACTIVE";
                default -> "/api/listings";
            };
            tasks.add(() -> timedGet(url));
        }
        return tasks;
    }

    private long timedGet(String url) { //messt die Antwortzeit
        Instant start = Instant.now(); //startet den Timer
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Suchanfrage " + url + " lieferte keinen success-status: " + response.getStatusCode());
        return elapsedMs;
    }
}
