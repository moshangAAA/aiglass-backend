package com.almousleck.config;

import com.almousleck.enums.UserRole;
import com.almousleck.model.User;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ApplicationUserDetails implements UserDetails {

    private final User user;
    private final Collection<GrantedAuthority> authorities;

    public static ApplicationUserDetails buildApplicationDetails(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        return new ApplicationUserDetails(user, authorities);
    }

    public Long getId() {
        return user.getId();
    }
    public UserRole getRole() {
        return user.getRole();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.getLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getPhoneVerified();
    }
}


