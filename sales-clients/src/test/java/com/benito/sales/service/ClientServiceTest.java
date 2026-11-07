package com.benito.sales.service;

import com.benito.sales.dto.ClientResponseDTO;
import com.benito.sales.entity.Client;
import com.benito.sales.exception.ConflictUpdateException;
import com.benito.sales.exception.InsufficientBalanceException;
import com.benito.sales.repository.ClientRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientServiceTest {

    @Test
    void debitBalance_throwsException_notEnoughBalance()
    {
        //ARRANGE
        ClientRepository fakeRepository = mock(ClientRepository.class);
        ClientService service = new ClientService(fakeRepository);
        ObjectId id = new ObjectId();

        Client client = new Client();
        client.id = id;
        client.userId = "user-1";
        client.status = true;
        client.balance = new BigDecimal("100.00");

        BigDecimal toDebit = new BigDecimal("150.00");

        when(fakeRepository.findById(id)).thenReturn(client);

        //ACT + ASSERT
        assertThrows(InsufficientBalanceException.class,
                () -> service.debitBalance(id, toDebit, "user-1", false, "ana"));

        //THE BALANCE DID NOT CHANGE AND NOTHING WAS SAVED
        assertEquals(new BigDecimal("100.00"), client.balance);
        verify(fakeRepository, never()).debitAtomicUpdate(any(Client.class), any(BigDecimal.class));
    }

    @Test
    void debitBalance_returnsTheRepositoryBalance_notTheOneCalculatedInJava()
    {
        //ARRANGE
        ClientRepository fakeRepository = mock(ClientRepository.class);
        ClientService service = new ClientService(fakeRepository);
        ObjectId id = new ObjectId();

        Client client = new Client();
        client.id = id;
        client.userId = "user-1";
        client.status = true;
        client.balance = new BigDecimal("100.00");

        BigDecimal amount = new BigDecimal("30.00");

        Client expectedResult = new Client();
        expectedResult.id = id;
        expectedResult.userId = "user-1";
        expectedResult.status = true;
        expectedResult.balance = new BigDecimal("50.00");


        when(fakeRepository.findById(id)).thenReturn(client);
        when(fakeRepository.debitAtomicUpdate(client, amount)).thenReturn(expectedResult);

        //ACT
        ClientResponseDTO clientResult = service.debitBalance(id, amount, "user-1",
                                                            false, "ana");

        //ASSERT
        assertEquals(new BigDecimal("50.00"), clientResult.balance);
        verify(fakeRepository, times(1)).debitAtomicUpdate(client, amount);
    }

    @Test
    void debitBalance_throwsConflict_afterThreeFailedAttempts()
    {
        ClientRepository fakeRepository = mock(ClientRepository.class);
        ClientService service = new ClientService(fakeRepository);

        ObjectId id = new ObjectId();
        Client client = new Client();
        client.id = id;
        client.userId = "user-1";
        client.status = true;
        client.balance = new BigDecimal("100.00");

        BigDecimal amount = new BigDecimal("30.00");

        when(fakeRepository.findById(id)).thenReturn(client);
        when(fakeRepository.debitAtomicUpdate(client, amount)).thenReturn(null);

        //ACT + ASSERT
        assertThrows(ConflictUpdateException.class, () -> service.debitBalance(id, amount, "user-1",
                false, "ana"));

        verify(fakeRepository, times(3)).debitAtomicUpdate(client, amount);
    }
}