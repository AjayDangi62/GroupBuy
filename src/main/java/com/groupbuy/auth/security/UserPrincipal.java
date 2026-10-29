package com.groupbuy.auth.security;

import com.groupbuy.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Spring Security {@link UserDetails} implementation wrapping application {@link User}.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String password;
    private final String fullName;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;

    public UserPrincipal(
            Long id,
            String email,
            String username,
            String password,
            String fullName,
            Collection<? extends GrantedAuthority> authorities,
            boolean active
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.authorities = authorities;
        this.active = active;
    }

    public static UserPrincipal fromUser(User user) {
        String roleName = "ROLE_" + user.getRole().name();
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));
        boolean isActive = user.getStatus() == User.Status.ACTIVE;

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                authorities,
                isActive
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
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
        // Return email as the primary principal identifier / login username
        return email;
    }

    public String getUserHandle() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
