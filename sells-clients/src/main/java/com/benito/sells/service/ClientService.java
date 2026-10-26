package com.benito.sells.service;

import com.benito.sells.dto.ClientRequestDTO;
import com.benito.sells.dto.ClientResponseDTO;
import com.benito.sells.dto.ClientsPaginatedDTO;
import com.benito.sells.entity.Client;
import com.benito.sells.exception.ClientNotFoundException;
import com.benito.sells.repository.ClientRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

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
        PanacheQuery<Client> query = _clientRepository.findAll(Sort.by("name").ascending()).page(page, pageSize);

        List<Client> clients = query.stream().filter(client -> client.status).toList();
        int totalPages = query.pageCount();
        long totalClients = query.count();

        return new ClientsPaginatedDTO(clients, page, pageSize, totalPages, totalClients);
    }



    //GET A CLIENT BY ID
    public ClientResponseDTO getClientById(ObjectId id)
    {
        Client clientFound = findValidClient(id);
        return ClientResponseDTO.TurnIntoDTO(clientFound);
    }



    //POST A NEW CLIENT
    public ClientResponseDTO createClient(ClientRequestDTO requestClient, String username)
    {
        Client newClient = new Client();
        newClient.username = requestClient.username;
        newClient.name = requestClient.name;
        newClient.lastName = requestClient.lastName;
        newClient.email = requestClient.email;
        newClient.phone = requestClient.phone;
        newClient.address = requestClient.address;
        newClient.balance = requestClient.balance;

        //AUDIT FIELDS
        newClient.status = true;
        newClient.createdBy = username;
        newClient.createdAt = Instant.now();

        _clientRepository.persist(newClient);

        return ClientResponseDTO.TurnIntoDTO(newClient);
    }



    //PUT, UPDATE A CLIENT
    public ClientResponseDTO updateClient(ObjectId id, ClientRequestDTO requestClient, String username)
    {
        Client clientFound = findValidClient(id);

        clientFound.username = requestClient.username;
        clientFound.name = requestClient.name;
        clientFound.lastName = requestClient.lastName;
        clientFound.email = requestClient.email;
        clientFound.phone = requestClient.phone;
        clientFound.address = requestClient.address;
        clientFound.balance = requestClient.balance;

        //AUDIT FIELDS
        clientFound.updatedBy = username;
        clientFound.updatedAt = Instant.now();

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

    //HELPER
    public Client findValidClient(ObjectId id)
    {
        Client clientContainer = _clientRepository.findById(id);

        if (clientContainer == null || !clientContainer.status)
            throw new ClientNotFoundException(id);

        return clientContainer;
    }
}
