package com.benito.sells.service;

import com.benito.sells.dto.*;
import com.benito.sells.entity.User;
import com.benito.sells.repository.UserRepository;
import com.benito.sells.restClient.ClientsApiClient;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class AuthService {

    private final ClientsApiClient _clientsApiClient;
    private final UserRepository _userRepository;

    public AuthService(UserRepository userRepository, @RestClient ClientsApiClient clientsApiClient)
    {
        this._clientsApiClient = clientsApiClient;
        this._userRepository = userRepository;
    }

    public void register(RegisterRequestDTO registerRequest)
    {
        long duplicatesCount = _userRepository.count("(username = ?1 or email = ?2) and status = ?3",
                                registerRequest.username,
                                registerRequest.email,
                                true);

//        if (duplicatesCount > 0)
//            throw new ConflictException(); TODO: BUILD EXCEPTIONS

        User newUser = new User();
        newUser.username = registerRequest.username;
        newUser.email = registerRequest.email;
        newUser.passwordHash = BcryptUtil.bcryptHash(registerRequest.password);
        newUser.roles.add("client");

        _userRepository.persist(newUser);

        ClientRequestDTO newClient = new ClientRequestDTO(registerRequest.name,
                                                          newUser.id.toString(),
                                                          registerRequest.lastName,
                                                          registerRequest.email,
                                                          null,
                                                          null,
                                                          null);


        RestResponse<ClientResponseDTO> clientsResponse = _clientsApiClient.createClient(newClient);

//        if (clientsResponse.getStatus() != 201)
//            TODO: COMPENSATION METHOD

    }

//    public TokenResponseDTO login(LoginRequestDTO loginRequest)
//    {
//        return TokenGenerator.build(newUser.username, newUser.roles, newUser.id.toString());
//    }
}
