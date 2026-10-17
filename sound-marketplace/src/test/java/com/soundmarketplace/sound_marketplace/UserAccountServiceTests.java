package com.soundmarketplace.sound_marketplace;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserAccountServiceTests {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        if (userAccountRepository.findByEmail("alice_test@example.com").isEmpty()) {
            UserAccount alice = UserAccount.builder()
                    .email("alice_test@example.com")
                    .passwordHash("$2a$12$dXJ3ADXeHel5t.s0z2G8zexA4A8Ggq/vK8nEq0U8tq.uGq9G0QZ.W")
                    .isActive(true)
                    .build();
            userAccountRepository.save(alice);
        }
    }

    @Test
    void testFindUserAccount() {
        Optional<UserAccount> user = userAccountRepository.findByEmail("alice_test@example.com");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("alice_test@example.com");
    }
}
