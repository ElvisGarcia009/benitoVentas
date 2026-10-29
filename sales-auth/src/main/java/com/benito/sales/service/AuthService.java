package com.benito.sales.service;

import com.benito.sales.dto.ServiceTokenRequestDTO;
import com.benito.sales.dto.restClientDTO.ClientRequestDTO;
import com.benito.sales.dto.LoginRequestDTO;
import com.benito.sales.dto.RegisterRequestDTO;
import com.benito.sales.dto.TokenResponseDTO;
import com.benito.sales.entity.User;
import com.benito.sales.exception.ConflictException;
import com.benito.sales.exception.InvalidCredentialsException;
import com.benito.sales.exception.RegistrationFailedException;
import com.benito.sales.repository.UserRepository;
import com.benito.sales.restClient.ClientsApiClient;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private final static Logger LOG = Logger.getLogger(AuthService.class);

    //DEPENDENCY INJECTION
    private final ClientsApiClient _clientsApiClient;
    private final UserRepository _userRepository;
    private final String _ordersServiceSecret;

    public AuthService(UserRepository userRepository,
                       @RestClient ClientsApiClient clientsApiClient,
                       @ConfigProperty(name = "orders.service.secret") String ordersServiceSecret)
    {
        this._clientsApiClient = clientsApiClient;
        this._userRepository = userRepository;
        this._ordersServiceSecret = ordersServiceSecret;
    }


    //REGISTER A NEW CLIENT ACCOUNT
    public void register(RegisterRequestDTO registerRequest)
    {
        long duplicatesCount = _userRepository.count("(username = ?1 or email = ?2) and status = ?3",
                                                registerRequest.username,
                                                registerRequest.email,
                                                true);

        if (duplicatesCount > 0)
            throw new ConflictException();

        User newUser = new User();
        mapRegisterToUser(newUser, registerRequest);

        _userRepository.persist(newUser);

        ClientRequestDTO newClient = new ClientRequestDTO(registerRequest.name,
                                                          newUser.id.toString(),
                                                          registerRequest.lastName,
                                                          registerRequest.email,
                                                          null,
                                                          null,
                                                          null);

        String serviceToken = TokenGenerator.buildServiceToken("AuthService",
                                                                new HashSet<>(Set.of("aService")));

        try
        {
            _clientsApiClient.createClient("Bearer " + serviceToken, newClient);
        }
        catch (WebApplicationException | ProcessingException exception) // CATCH WHEN CLIENT SERVICE ANSWERS WITH AN
                                                                        // EXCEPTION OR IS TIMED OUT
        {
            //FALLBACK: THE USER IS DELETED BECAUSE HIS CLIENT PROFILE COULD NOT BE CREATED
            LOG.error("Client profile could not be created, rolling back user: " + newUser.id, exception);
            _userRepository.delete(newUser);

            throw new RegistrationFailedException();
        }
    }


    //LOGIN, RETURNS AN ACCESS TOKEN
    public TokenResponseDTO login(LoginRequestDTO loginRequest)
    {
        User userFound = _userRepository.find("username = ?1 and status = ?2",
                                                loginRequest.username,
                                                true).firstResult();


        if (userFound == null || !BcryptUtil.matches(loginRequest.password, userFound.passwordHash))
            throw new InvalidCredentialsException();

        return TokenGenerator.buildUserToken(userFound.username, userFound.roles, userFound.id.toString());
    }


    //ISSUES A SHORT-LIVED TOKEN FOR AN INTERNAL SERVICE
    public TokenResponseDTO issueServiceToken(ServiceTokenRequestDTO request)
    {
        boolean validSecret = MessageDigest.isEqual(request.secret.getBytes(StandardCharsets.UTF_8),
                                             _ordersServiceSecret.getBytes(StandardCharsets.UTF_8));

        if (!"orders-service".equals(request.serviceName) || !validSecret)
            throw new InvalidCredentialsException();

        String token = TokenGenerator.buildServiceToken(request.serviceName, Set.of("orderService"));
        return new TokenResponseDTO(token, "Bearer", 60);
    }




    //HELPERS
    private void mapRegisterToUser(User user, RegisterRequestDTO registerRequest)
    {
        //MAIN FIELDS
        user.username = registerRequest.username;
        user.email = registerRequest.email;
        user.passwordHash = BcryptUtil.bcryptHash(registerRequest.password);

        //EVERY ACCOUNT CREATED THROUGH REGISTER ENDPOINT START AS A CLIENT
        user.roles = new HashSet<>();
        user.roles.add("client");

        //AUDIT FIELDS
        user.status = true;
        user.createdBy = "self-registration";
        user.createdAt = Instant.now();
    }
}