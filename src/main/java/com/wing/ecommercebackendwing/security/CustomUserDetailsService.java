package com.wing.ecommercebackendwing.security;

import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetailsImpl(user);
    }

    /**
     * Custom UserDetails implementation that includes user ID and account status
     */
    private static class CustomUserDetailsImpl implements CustomUserDetails {
        private final UUID userId;
        private final String username;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean accountNonLocked;
        private final boolean enabled;

        public CustomUserDetailsImpl(User user) {
            this.userId = user.getId();
            this.username = user.getEmail();
            this.password = user.getPassword();
            this.authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );
            
            // Check if account is locked
            boolean isLocked = Boolean.TRUE.equals(user.getAccountLocked());
            if (isLocked && user.getLockedUntil() != null) {
                // Auto-unlock if lock period has expired
                isLocked = Instant.now().isBefore(user.getLockedUntil());
            }
            this.accountNonLocked = !isLocked;
            this.enabled = Boolean.TRUE.equals(user.getIsActive());
        }

        @Override
        public UUID getUserId() {
            return userId;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}
