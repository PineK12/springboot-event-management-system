package com.example.vadoo.security;

import com.example.vadoo.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = new HashSet<>();

        // Add role as authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getTenRole().toUpperCase()));

        // Add permissions as authorities
        if (user.getRole().getPermissions() != null) {
            user.getRole().getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()))
            );
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }

    public Integer getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getRoleName() {
        return user.getRole().getTenRole();
    }

    public String getDisplayName() {
        if (user.getSinhVien() != null) {
            return user.getSinhVien().getTen();
        } else if (user.getBtc() != null) {
            return user.getBtc().getTen();
        }
        return user.getUsername();
    }
}