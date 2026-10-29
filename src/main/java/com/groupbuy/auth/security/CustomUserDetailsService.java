package com.groupbuy.auth.security;

import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user details from database for Spring Security authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalized = usernameOrEmail != null ? usernameOrEmail.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));

        return UserPrincipal.fromUser(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return UserPrincipal.fromUser(user);
    }
}
