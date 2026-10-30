package com.benito.sales.service;

import com.benito.sales.dto.ServiceTokenRequestDTO;
import com.benito.sales.exception.InvalidCredentialsException;
import com.benito.sales.exception.InvalidServiceCredentialsException;
import com.benito.sales.repository.UserRepository;
import com.benito.sales.restClient.ClientsApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuthServiceTest {

    @Test
    void issueServiceToken_throwsException_noOrdersCertificate()
    {
        //ARRANGE
        UserRepository fakeRepository = mock(UserRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        AuthService service = new AuthService(fakeRepository, fakeClientsApi, "the-secret");

        //THE SECRET IS CORRECT
        ServiceTokenRequestDTO request = new ServiceTokenRequestDTO();
        request.serviceName = "orders-service";
        request.secret = "the-secret";

        //BUT THE CALLER DID NOT PRESENT THE ORDERS CERTIFICATE
        String callerCertificateName = null;

        //ACT + ASSERT
        assertThrows(InvalidServiceCredentialsException.class,
                     () -> service.issueServiceToken(request, callerCertificateName));
    }

    @Test
    void issueServiceToken_throwsException_wrongSecret()
    {
        //ARRANGE
        UserRepository fakeRepository = mock(UserRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        AuthService service = new AuthService(fakeRepository, fakeClientsApi, "the-secret");

        //THE SECRET IS WRONG
        ServiceTokenRequestDTO request = new ServiceTokenRequestDTO();
        request.serviceName = "orders-service";
        request.secret = "wrong-secret";

        //BUT THE CALLER DID PRESENT THE ORDERS CERTIFICATE
        String callerCertificateName = "CN=sales-orders";

        //ACT + ASSERT
        assertThrows(InvalidServiceCredentialsException.class,
                     () -> service.issueServiceToken(request, callerCertificateName));
    }
}