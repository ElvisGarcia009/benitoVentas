package com.benito.sales.dto;

import com.benito.sales.entity.User;

import java.util.Set;

public class UserResponseDTO {

    //NOTE: THE HASHED PASSWORD IS NEVER GIVEN BACK
    public String id;
    public String username;
    public String email;
    public Set<String> roles;

    public UserResponseDTO(String id, String username, String email, Set<String> roles)
    {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public static UserResponseDTO TurnIntoDTO(User user)
    {
        return new UserResponseDTO(user.id.toString(),
                user.username,
                user.email,
                user.roles);
    }
}