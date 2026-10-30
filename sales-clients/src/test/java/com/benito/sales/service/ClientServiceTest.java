package com.benito.sales.service;

import com.benito.sales.entity.Client;
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

        when(fakeRepository.findById(id)).thenReturn(client);

        //ACT + ASSERT
        assertThrows(InsufficientBalanceException.class,
                () -> service.debitBalance(id, new BigDecimal("150.00"), "user-1", false, "ana"));

        //THE BALANCE DID NOT CHANGE AND NOTHING WAS SAVED
        assertEquals(new BigDecimal("100.00"), client.balance);
        verify(fakeRepository, never()).update(any(Client.class));
    }

    @Test
    void debitBalance_subtractsTheAmount_enoughBalance()
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

        when(fakeRepository.findById(id)).thenReturn(client);

        //ACT
        service.debitBalance(id, new BigDecimal("30.00"), "user-1", false, "ana");

        //ASSERT
        assertAll(() -> assertEquals(new BigDecimal("70.00"), client.balance),
                  () -> assertEquals("ana", client.updatedBy));

        verify(fakeRepository).update(client);
    }
}