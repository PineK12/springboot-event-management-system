package com.example.vadoo.service;

import com.example.vadoo.entity.User;
import com.example.vadoo.repository.UserRepository;
import com.example.vadoo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsernameWithRoleAndPermissions(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("Không tìm thấy người dùng: " + username);
                });

        if (!user.getIsActive()) {
            log.warn("User is inactive: {}", username);
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa: " + username);
        }

        log.debug("User loaded successfully: {} with role: {}", username, user.getRole().getTenRole());
        return new CustomUserDetails(user);
    }
}