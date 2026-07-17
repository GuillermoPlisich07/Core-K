package com.konverza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @EnableScheduling backs ScenarioExpirationJob (scenario-privacy-and-lifecycle) — first scheduled job in this app. */
@SpringBootApplication
@EnableScheduling
public class KonverzaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KonverzaApplication.class, args);
    }
}
