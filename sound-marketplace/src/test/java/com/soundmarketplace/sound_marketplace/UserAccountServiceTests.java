package com.soundmarketplace.sound_marketplace;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.soundmarketplace.sound_marketplace.repository.SoundMarketPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserAccountServiceTests {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SoundMarketPlaceRepository soundMarketPlaceRepository;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();

        // Inserting 3 sample profiles using Lombok Builder
        UserAccount alice = UserAccount.builder()
                .email("alice@example.com")
                .passwordHash("$2a$12$dXJ3ADXeHel5t.s0z2G8zexA4A8Ggq/vK8nEq0U8tq.uGq9G0QZ.W")
                .isActive(true)
                .build();

        UserAccount bob = UserAccount.builder()
                .email("bob@example.com")
                .passwordHash("$2a$12$dXJ3ADXeHel5t.s0z2G8zexA4A8Ggq/vK8nEq0U8tq.uGq9G0QZ.W")
                .isActive(true)
                .build();

        UserAccount charlie = UserAccount.builder()
                .email("charlie@example.com")
                .passwordHash("$2a$12$dXJ3ADXeHel5t.s0z2G8zexA4A8Ggq/vK8nEq0U8tq.uGq9G0QZ.W")
                .isActive(false)
                .build();

        userAccountRepository.save(alice);
        userAccountRepository.save(bob);
        userAccountRepository.save(charlie);
    }

    @Test
    void testGetAllSoundMarketPlaceRepository() {
        UserAccount user = soundMarketPlaceRepository.getAllSoundMarketPlaceRepository();
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isIn("alice@example.com", "bob@example.com", "charlie@example.com");
    }
}
