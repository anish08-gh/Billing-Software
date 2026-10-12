package com.shreedatta.BillingSoftware.Security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String role = jwt.getClaimAsString("role");

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT does not contain a valid role"
            );
        }

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + role);

        return new JwtAuthenticationToken(
                jwt,
                List.of(authority),
                jwt.getSubject()
        );
    }
}