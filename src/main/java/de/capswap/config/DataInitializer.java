package de.capswap.config;

import de.capswap.entity.Category;
import de.capswap.entity.ChatMessage;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.ChatMessageRepository;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

//diese Klasse erstellt Testdaten beim Start, falls es keine existieren

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            log.info("Database already contains data, skipping initialization.");
            return;
        }

        log.info("Populating database with test data...");

        Category catIT = categoryRepository.save(Category.builder().name("IT Services").build()); //categories
        Category catHardware = categoryRepository.save(Category.builder().name("Hardware").build());
        Category catConsulting = categoryRepository.save(Category.builder().name("Consulting").build());
        Category catLogistics = categoryRepository.save(Category.builder().name("Logistics").build());

        Company techCorp = companyRepository.save(Company.builder() //companies
                .name("TechCorp GmbH")
                .email("info@techcorp.de")
                .passwordHash("hashed_password_mock") 
                .location("Berlin")
                .description("We provide top-notch software solutions.")
                .build());

        Company hardwareInc = companyRepository.save(Company.builder()
                .name("Hardware Inc.")
                .email("contact@hardwareinc.com")
                .passwordHash("hashed_password_mock")
                .location("Munich")
                .description("Supplying the best server components.")
                .build());
                
        Company fastLogistics = companyRepository.save(Company.builder()
                .name("Fast Logistics")
                .email("hello@fastlogistics.com")
                .passwordHash("hashed_password_mock")
                .location("Hamburg")
                .description("Shipping everywhere, fast.")
                .build());

        Listing listing1 = listingRepository.save(Listing.builder() //listings
                .company(techCorp)
                .category(catIT)
                .title("Offer: 100 hours of Java Development")
                .description("We are offering 100 hours of senior Java development in exchange for server hardware.")
                .location("Berlin / Remote")
                .build());

        Listing listing2 = listingRepository.save(Listing.builder()
                .company(hardwareInc)
                .category(catHardware)
                .title("Request: Need Web Application, offering Servers")
                .description("We need a modern web application and can offer 3 high-end rack servers in return.")
                .location("Munich")
                .build());

        chatMessageRepository.save(ChatMessage.builder() //messages
                .senderCompany(hardwareInc)
                .recipientCompany(techCorp)
                .listing(listing1)
                .message("Hello TechCorp, we saw your offer. Would you take 2 rack servers for the 100 hours?")
                .isRead(false)
                .build());

        chatMessageRepository.save(ChatMessage.builder()
                .senderCompany(techCorp)
                .recipientCompany(hardwareInc)
                .listing(listing1)
                .message("Hi Hardware Inc! That sounds interesting, let's schedule a call.")
                .isRead(false)
                .build());

        log.info("Database successfully populated with test data!");
    }
}
