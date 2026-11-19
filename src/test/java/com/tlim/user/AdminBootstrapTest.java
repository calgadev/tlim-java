package com.tlim.user;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void doesNothingWhenNotConfigured() {
        new AdminBootstrap(repo, encoder, "", "").run(null);
        verifyNoInteractions(repo);
    }

    @Test
    void createsAdminWhenMissing() {
        when(repo.findByUsername("admin")).thenReturn(Optional.empty());
        when(encoder.encode("supersecret")).thenReturn("hash");

        new AdminBootstrap(repo, encoder, "admin", "supersecret").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getRole()).isEqualTo("ADMIN");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void promotesExistingUserWithoutTouchingPassword() {
        User existing = new User();
        existing.setUsername("admin");
        existing.setRole("USER");
        existing.setPasswordHash("original");
        when(repo.findByUsername("admin")).thenReturn(Optional.of(existing));

        new AdminBootstrap(repo, encoder, "admin", "supersecret").run(null);

        assertThat(existing.getRole()).isEqualTo("ADMIN");
        assertThat(existing.getPasswordHash()).isEqualTo("original");
        verifyNoInteractions(encoder);
    }

    @Test
    void skipsCreationWhenPasswordTooShort() {
        when(repo.findByUsername("admin")).thenReturn(Optional.empty());

        new AdminBootstrap(repo, encoder, "admin", "short").run(null);

        verify(repo, never()).save(any());
    }
}
