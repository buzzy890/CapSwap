package de.capswap.nfa;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.capswap.dto.AuthDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ErrorMessageLatencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testInvalidLoginErrorMessageAndLatency() throws Exception {
        AuthDtos.LoginRequest invalidLogin = new AuthDtos.LoginRequest();
        invalidLogin.setEmail("doesnotexist@example.com");
        invalidLogin.setPassword("wrongpassword");

        String jsonRequest = objectMapper.writeValueAsString(invalidLogin);

        // Warm-up
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));

        // Start timer
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andReturn();
        
        long latency = System.currentTimeMillis() - startTime;

        System.out.println("Login Failed Response Time: " + latency + "ms");

        // Assert latency < 1000ms
        assertThat(latency)
                .as("Die Antwortzeit bei fehlerhaftem Login muss unter 1 Sekunde (1000ms) liegen")
                .isLessThan(1000);

        // Assert comprehensible error message in response body
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody)
                .as("Der Response-Body muss eine verständliche Fehlermeldung für den Nutzer enthalten")
                .contains("falsch")
                .contains("E-Mail-Adresse");
    }
}
