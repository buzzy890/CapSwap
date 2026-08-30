package de.capswap.controller;

import de.capswap.entity.Company;
import de.capswap.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //prüft auf der App Ebene ob die Registrierung mit einer bereits vergebenen E-Mail-Adresse abgelehnt wird
class AuthRegistrationUniqueEmailTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void secondRegistrationWithSameEmailIsRejected() {
        String email = "duplicate-" + UUID.randomUUID().toString().substring(0, 8) + "@capswap.de";

        de.capswap.dto.AuthDtos.RegisterRequest first = de.capswap.dto.AuthDtos.RegisterRequest.builder()
                .name("Erste Firma GmbH")
                .email(email)
                .password("plain-pw-1")
                .build();
        ResponseEntity<de.capswap.dto.CompanyResponse> firstResponse =
                restTemplate.postForEntity("/api/auth/register", first, de.capswap.dto.CompanyResponse.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode(),
                "erste Registrierung mit der neuen Email Adresse muss erfolgreich sein.");

        de.capswap.dto.AuthDtos.RegisterRequest second = de.capswap.dto.AuthDtos.RegisterRequest.builder()
                .name("Zweite Firma GmbH")
                .email(email)
                .password("plain-pw-2")
                .build();
        ResponseEntity<String> secondResponse =
                restTemplate.postForEntity("/api/auth/register", second, String.class);

        assertTrue(secondResponse.getStatusCode().is4xxClientError(),
                "zweite Registrierung mit bereits vergebener Email  Adresse muss mit einem "
                        + "Client-Fehler fehlschlagen, war aber: " + secondResponse.getStatusCode());

        long companiesWithEmail = companyRepository.findAll().stream()
                .filter(c -> email.equals(c.getEmail()))
                .count();
        assertEquals(1, companiesWithEmail,
                "Es darf am Ende NUR 1 Unternehmen mit dieser Email existieren.");
    }
}
