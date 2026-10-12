package com.shreedatta.BillingSoftware.Security;

import com.shreedatta.BillingSoftware.Entity.User;
import com.shreedatta.BillingSoftware.Repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password")
                );

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        if (!user.getShop().isActive()) {
            throw new UsernameNotFoundException("Shop account is inactive");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
                .disabled(!user.isActive())
                .build();
    }
}