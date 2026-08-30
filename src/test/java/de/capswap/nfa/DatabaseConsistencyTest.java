package de.capswap.nfa;

import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NFA Test:
 * "Die Datenbanktabellen erfüllen logische Konsistenzregeln, sind vollständig und korrekt.
 * Integrationstest. Integrationstest (via Testcontainers) versucht gezielt fehlerhafte Referenzen 
 * (z. B. Angebot mit nicht-existierender User-ID) zu speichern; DB muss via Exception 
 * (Foreign-Key-Verletzung) ablehnen."
 */
@SpringBootTest
@Testcontainers
public class DatabaseConsistencyTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("capswap-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Ensure Hibernate applies the schema to the Postgres container
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void testForeignKeyViolationOnInvalidCompanyId() {
        // Fetch a valid category (created by DataInitializer) to isolate the failure 
        // strictly to the invalid Company ID.
        Category validCategory = categoryRepository.findAll().stream().findFirst().orElseThrow();

        // Simulate an invalid reference: A Company with an ID that definitely does not exist
        Company nonExistentCompany = new Company();
        nonExistentCompany.setId(99999999L); 

        // Attempt to create a Listing referencing the non-existent Company
        Listing invalidListing = Listing.builder()
                .title("Hacked Listing")
                .description("Trying to bypass FK constraints")
                .location("Dark Web")
                .status(ListingStatus.ACTIVE)
                .category(validCategory)
                .company(nonExistentCompany)
                .build();

        // The Database (PostgreSQL) must reject the insert due to a Foreign Key constraint violation
        // Spring Data JPA wraps the SQL exception in a DataIntegrityViolationException.
        assertThatThrownBy(() -> listingRepository.saveAndFlush(invalidListing))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("constraint");
    }
}
