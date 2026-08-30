package de.capswap.nfa;

import de.capswap.dto.ChatMessageRequest;
import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerErrorRateTests { //prüft die Fehlerrate bei Vielzahl von gültigen Anfragen 

    private static final double MAX_INTERNAL_ERROR_RATIO = 0.005; //99,5 % muss ohne internen Fehler laufen

    @Autowired
    private TestRestTemplate restTemplate;

    private final List<Integer> statusCodes = new ArrayList<>();

    @BeforeEach
    void useJdkHttpClientToSupportPatchRequests() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void validRequestSpectrumStaysBelowInternalErrorThreshold() {
        String s = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> companyA = post("/api/auth/register", registerRequest("Firma A " + s, "a-" + s + "@capswap.de"));
        Map<String, Object> companyB = post("/api/auth/register", registerRequest("Firma B " + s, "b-" + s + "@capswap.de"));
        Long companyAId = idOf(companyA);
        Long companyBId = idOf(companyB);

        post("/api/auth/login", Map.of("email", "a-" + s + "@capswap.de", "password", "irrelevant"));
        Map<String, Object> resetToken = post("/api/auth/password-reset/request", Map.of("email", "a-" + s + "@capswap.de"));
        post("/api/auth/password-reset/confirm", Map.of("token", resetToken.get("token"), "newPassword", "neues-passwort-hash"));

        get("/api/companies");
        get("/api/companies/" + companyAId);
        put("/api/companies/" + companyAId, company("Firma A " + s + " (aktualisiert)", "a-" + s + "@capswap.de"));

        Map<String, Object> categoryX = post("/api/categories", category("Kategorie X " + s));
        Map<String, Object> categoryY = post("/api/categories", category("Kategorie Y " + s));
        Long categoryXId = idOf(categoryX);
        Long categoryYId = idOf(categoryY);
        get("/api/categories");
        get("/api/categories/" + categoryXId);

        List<Long> listingIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Long catId = (i % 2 == 0) ? categoryXId : categoryYId;
            Map<String, Object> created = post("/api/listings", listing(companyAId, catId, "Angebot " + s + "-" + i));
            listingIds.add(idOf(created));
        }
        get("/api/listings");
        get("/api/listings?categoryId=" + categoryXId);
        get("/api/listings?companyId=" + companyAId);
        get("/api/listings?status=ACTIVE");
        get("/api/listings/" + listingIds.get(0));
        put("/api/listings/" + listingIds.get(0), listing(companyAId, categoryYId, "Angebot " + s + "-aktualisiert"));

        post("/api/subscriptions?companyId=" + companyBId + "&categoryId=" + categoryXId, null);
        get("/api/subscriptions?companyId=" + companyBId);
        delete("/api/subscriptions?companyId=" + companyBId + "&categoryId=" + categoryXId);

        Map<String, Object> chatMessage = post("/api/chats",
                chatMessage(companyAId, companyBId, listingIds.get(0), "Hallo, ist das noch verfügbar?"));
        Long chatMessageId = idOf(chatMessage);
        get("/api/chats?senderId=" + companyAId + "&recipientId=" + companyBId);
        get("/api/chats?listingId=" + listingIds.get(0));
        patch("/api/chats/" + chatMessageId + "/read");

        post("/api/favorites?companyId=" + companyBId + "&listingId=" + listingIds.get(1), null);
        get("/api/favorites?companyId=" + companyBId);
        delete("/api/favorites?companyId=" + companyBId + "&listingId=" + listingIds.get(1));

        get("/api/notifications?companyId=" + companyBId);

        delete("/api/listings/" + listingIds.get(2));

        long total = statusCodes.size(); //messt die Anzahl der Anfragen
        long internalErrors = statusCodes.stream().filter(code -> code >= 500).count(); //filtert die Severerrors (5xx)
        double ratio = total == 0 ? 0 : (double) internalErrors / total; //berechnet die Quote der Servererrors

        System.out.printf(
                "Fehlerraten-Test: %d Anfragen gesamt, %d interne Fehler (5xx), Quote=%.4f%n",
                total, internalErrors, ratio);

        assertTrue(ratio <= MAX_INTERNAL_ERROR_RATIO,
                "Interne-Fehler-Quote " + ratio + " überschreitet die zuläßigen 0,5 % ("
                        + internalErrors + "/" + total + " Anfragen, Statuscodes: " + statusCodes + ")");
    }


    // hilfsmethoden für die Testanfragen
    private Map<String, Object> post(String url, Object body) {
        return exchangeForObject(url, HttpMethod.POST, body);
    }

    private Map<String, Object> put(String url, Object body) {
        return exchangeForObject(url, HttpMethod.PUT, body);
    }

    private void patch(String url) {
        record(restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, String.class));
    }

    private void get(String url) {
        record(restTemplate.getForEntity(url, String.class));
    }

    private void delete(String url) {
        record(restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, String.class));
    }

    private Map<String, Object> exchangeForObject(String url, HttpMethod method, Object body) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, method, body == null ? HttpEntity.EMPTY : new HttpEntity<>(body),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        statusCodes.add(response.getStatusCode().value());
        return response.getBody();
    }

    private void record(ResponseEntity<?> response) { //fuegt den statuscode einer anfrage in die statusCodes liste
        statusCodes.add(response.getStatusCode().value());
    }

    private Long idOf(Map<String, Object> body) {
        return ((Number) body.get("id")).longValue();
    }

    private de.capswap.dto.AuthDtos.RegisterRequest registerRequest(String name, String email) {
        return de.capswap.dto.AuthDtos.RegisterRequest.builder()
                .name(name)
                .email(email)
                .password("plain-password")
                .location("München")
                .description("Unternehmen für die Fehlerraten-Test.")
                .build();
    }

    private Company company(String name, String email) {
        return Company.builder()
                .name(name)
                .email(email)
                .passwordHash("hashed-password")
                .location("München")
                .description("Unternehmen für die Fehlerraten-Test.")
                .build();
    }

    private Category category(String name) {
        return Category.builder().name(name).build();
    }

    private Listing listing(Long companyId, Long categoryId, String title) {
        return Listing.builder()
                .company(Company.builder().id(companyId).build())
                .category(Category.builder().id(categoryId).build())
                .title(title)
                .description("Beschreibung für " + title)
                .location("Berlin")
                .status(ListingStatus.ACTIVE)
                .build();
    }

    private ChatMessageRequest chatMessage(Long senderId, Long recipientId, Long listingId, String text) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setSenderCompanyId(senderId);
        request.setRecipientCompanyId(recipientId);
        request.setListingId(listingId);
        request.setMessage(text);
        return request;
    }
}
