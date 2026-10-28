package com.benito.sales.service;

import com.benito.sales.dto.ClientRequestDTO;
import com.benito.sales.dto.ClientResponseDTO;
import com.benito.sales.dto.ClientUpdateRequestDTO;
import com.benito.sales.dto.ClientsPaginatedDTO;
import com.benito.sales.entity.Client;
import com.benito.sales.exception.ClientAccessDeniedException;
import com.benito.sales.exception.ClientNotFoundException;
import com.benito.sales.exception.InsufficientBalanceException;
import com.benito.sales.repository.ClientRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ClientService {

    //DEPENDENCY INJECTION -> Repository
    private final ClientRepository _clientRepository;
    public ClientService(ClientRepository clientRepository)
    {
        this._clientRepository = clientRepository;
    }

    //GET ALL CLIENTS, PAGINATED
    public ClientsPaginatedDTO getAllClients(int page, int pageSize)
    {
        PanacheQuery<Client> query = _clientRepository.find("status", Sort.by("name").ascending()
                                                                                .and("lastName").ascending(),
                                                            true).page(page, pageSize);

        List<ClientResponseDTO> clients = query.stream().map(ClientResponseDTO::TurnIntoDTO).toList();
        int totalPages = query.pageCount();
        long totalClients = query.count();

        return new ClientsPaginatedDTO(clients, page, pageSize, totalPages, totalClients);
    }


    //GET A CLIENT BY ID
    public ClientResponseDTO getClientById(ObjectId id, String callerUserId, boolean isAdmin)
    {
        Client clientFound = findValidClient(id);
        verifyClientAccess(clientFound, callerUserId, isAdmin);

        return ClientResponseDTO.TurnIntoDTO(clientFound);
    }


    //POST A NEW CLIENT
    public ClientResponseDTO createClient(ClientRequestDTO requestClient, String username)
    {
        Client newClient = new Client();

        if (username == null)
            username = "self-registration";

        mapRequestToClient(newClient, requestClient, username);

        _clientRepository.persist(newClient);

        return ClientResponseDTO.TurnIntoDTO(newClient);
    }

    //SUBTRACT AN AMOUNT FROM THE CLIENT'S BALANCE
    public ClientResponseDTO debitBalance(ObjectId id, BigDecimal amount,
                                          String callerUserId, boolean isAdmin, String username)
    {
        Client clientFound = findValidClient(id);
        verifyClientAccess(clientFound, callerUserId, isAdmin);

        BigDecimal balanceAfterBuying = clientFound.balance.subtract(amount);

        //CAN THE CLIENT PAY THIS?
        if (balanceAfterBuying.compareTo(BigDecimal.ZERO) < 0)
            throw new InsufficientBalanceException();

        clientFound.balance = balanceAfterBuying;

        //AUDIT FIELDS
        clientFound.updatedBy = username;
        clientFound.updatedAt = Instant.now();

        _clientRepository.update(clientFound);

        return ClientResponseDTO.TurnIntoDTO(clientFound);
    }


    //ADD AN AMOUNT TO THE CLIENT'S BALANCE
    public ClientResponseDTO creditBalance(ObjectId id, BigDecimal amount, String username)
    {
        Client clientFound = findValidClient(id);

        clientFound.balance = clientFound.balance.add(amount);

        //AUDIT FIELDS
        clientFound.updatedBy = username;
        clientFound.updatedAt = Instant.now();

        _clientRepository.update(clientFound);

        return ClientResponseDTO.TurnIntoDTO(clientFound);
    }


    //PUT, UPDATE A CLIENT
    public ClientResponseDTO updateClient(ObjectId id, ClientUpdateRequestDTO requestClient,
                                          String callerUserId, boolean isAdmin, String username)
    {
        Client clientFound = findValidClient(id);
        verifyClientAccess(clientFound, callerUserId, isAdmin);

        mapRequestUpdateToClient(clientFound, requestClient, username);

        _clientRepository.update(clientFound);

        return ClientResponseDTO.TurnIntoDTO(clientFound);
    }


    //DELETE A CLIENT (this method just deactivate the client)
    public void deleteClient(ObjectId id, String username)
    {
        Client clientFound = findValidClient(id);

        clientFound.status = false;
        clientFound.updatedBy = username;
        clientFound.updatedAt = Instant.now();

        _clientRepository.update(clientFound);

    }




    //HELPERS
    public Client findValidClient(ObjectId id)
    {
        Client clientContainer = _clientRepository.findById(id);

        if (clientContainer == null || !clientContainer.status)
            throw new ClientNotFoundException(id);

        return clientContainer;
    }

    //NOTE: A CLIENT CAN ONLY ACCESS HIS OWN INFORMATION. ADMINS CAN ACCESS EVERYONE'S
    private void verifyClientAccess(Client client, String callerUserId, boolean isAdmin)
    {
        if (!isAdmin && !client.userId.equals(callerUserId))
            throw new ClientAccessDeniedException();
    }

    public void mapRequestToClient(Client client, ClientRequestDTO requestClient, String username)
    {
        //MAIN FIELDS
        client.userId = requestClient.userId;
        client.name = requestClient.name;
        client.lastName = requestClient.lastName;
        client.phone = requestClient.phone;
        client.address = requestClient.address;

        //NOTE: EVERY CLIENT STARTS WITH NO BALANCE, ONLY ADMINS CAN CREDIT BALANCES
        client.balance = BigDecimal.ZERO;

        //AUDIT FIELDS
        client.status = true;
        client.createdBy = username;
        client.createdAt = Instant.now();
    }

    public void mapRequestUpdateToClient(Client client, ClientUpdateRequestDTO requestClient, String username)
    {
        //NOTE: USERNAME AND EMAIL CANNOT BE UPDATED, THEY OUGHT TO BE STATIC ONCE SETTLED
        client.name = requestClient.name;
        client.lastName = requestClient.lastName;
        client.phone = requestClient.phone;
        client.address = requestClient.address;

        //AUDIT FIELDS
        client.updatedBy = username;
        client.updatedAt = Instant.now();
    }


}
