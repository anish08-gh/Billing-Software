package com.shreedatta.BillingSoftware.Service;

import com.shreedatta.BillingSoftware.dto.auth.AuthResponse;
import com.shreedatta.BillingSoftware.dto.auth.LoginRequest;
import com.shreedatta.BillingSoftware.dto.auth.RegisterRequest;
import com.shreedatta.BillingSoftware.Entity.Shop;
import com.shreedatta.BillingSoftware.Entity.User;
import com.shreedatta.BillingSoftware.Repository.ShopRepository;
import com.shreedatta.BillingSoftware.Repository.UserRepository;
import com.shreedatta.BillingSoftware.Security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            ShopRepository shopRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        Shop shop = new Shop(
                request.shopName().trim(),
                normalizeOptional(request.phone()),
                email,
                null,
                null
        );

        shop = shopRepository.save(shop);

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                shop,
                request.name().trim(),
                email,
                normalizeOptional(request.phone()),
                passwordHash,
                User.Role.SHOP_OWNER
        );

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Unable to create account",
                    exception
            );
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                shop.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!user.isActive() || !user.getShop().isActive()) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getShop().getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}