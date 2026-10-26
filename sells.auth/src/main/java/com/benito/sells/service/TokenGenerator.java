package com.benito.sells.service;

import com.benito.sells.dto.TokenResponseDTO;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

@ApplicationScoped
public class TokenGenerator {

    public static TokenResponseDTO build(String username, Set<String> roles, String id)
    {
        long expiresIn = 5000;

        String accessToken=  Jwt.upn(username)
                                .claim("userId", id)
                                .expiresIn(expiresIn)
                                .groups(roles)
                                .sign();

        return new TokenResponseDTO(accessToken, "Bearer", expiresIn);
    }
}
