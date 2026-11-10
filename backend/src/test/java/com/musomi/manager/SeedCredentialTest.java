package com.musomi.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SeedCredentialTest {

    private static final String DEVELOPMENT_SEED_HASH =
            "$2a$10$lVqHihSi7//szMpQ9K/sIeLbjbEy2KlzjjMTwO24n8gf4BQoWcBMu";

    @Test
    void documentedDevelopmentSeedPasswordMatchesItsStoredBcryptHash() {
        assertThat(new BCryptPasswordEncoder().matches("password123", DEVELOPMENT_SEED_HASH))
                .as("The repeatable dev seed migration sets password123 for local admin and teacher seed accounts")
                .isTrue();
    }
}
