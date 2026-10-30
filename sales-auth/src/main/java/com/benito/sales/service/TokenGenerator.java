package com.benito.sales.service;

import com.benito.sales.dto.TokenResponseDTO;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

@ApplicationScoped
public class TokenGenerator {

    public static TokenResponseDTO buildUserToken(String username, Set<String> roles, String id)
    {
        long expiresIn = 900; // TTL -> 15 MINUTES

        String accessToken=  Jwt.subject(username)
                                .claim("userId", id)
                                .expiresIn(expiresIn)
                                .groups(roles)
                                .sign();

        return new TokenResponseDTO(accessToken, "Bearer", expiresIn);
    }

    public static TokenResponseDTO buildServiceToken(String username, Set<String> roles)
    {
        long expiresIn = 60; // TTL -> 1 MINUTE

        String accessToken =  Jwt.subject(username)
                    .expiresIn(expiresIn)
                    .groups(roles)
                    .sign();

        return new TokenResponseDTO(accessToken, "Bearer", expiresIn);
    }
}
