package de.capswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean // beans are managed by Spring and can be injected into other components. Also, einmal iniziiert und danach überall genutzt (aus einem  Objekt-Container gezogen)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
