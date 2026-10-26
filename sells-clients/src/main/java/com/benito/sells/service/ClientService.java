package com.benito.sells.service;

import com.benito.sells.dto.ClientRequestDTO;
import com.benito.sells.dto.ClientResponseDTO;
import com.benito.sells.dto.ClientUpdateRequestDTO;
import com.benito.sells.dto.ClientsPaginatedDTO;
import com.benito.sells.entity.Client;
import com.benito.sells.exception.ClientNotFoundException;
import com.benito.sells.repository.ClientRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
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
        PanacheQuery<Client> query = _clientRepository.findAll(Sort.by("name").ascending().and("lastName").ascending())
                                                      .page(page, pageSize);

        List<ClientResponseDTO> clients = query.stream().filter(client -> client.status)
                                                        .map(ClientResponseDTO::TurnIntoDTO)
                                                        .toList();
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
    //TODO: CHECK USERNAME/EMAIL BEFOREHAND TO AVOID DUPLICATES
    public ClientResponseDTO createClient(ClientRequestDTO requestClient, String username)
    {
        Client newClient = new Client();

        if (username == null)
            username = "self-registration";

        mapRequestToClient(newClient, requestClient, username);

        _clientRepository.persist(newClient);

        return ClientResponseDTO.TurnIntoDTO(newClient);
    }


    //PUT, UPDATE A CLIENT
    public ClientResponseDTO updateClient(ObjectId id, ClientUpdateRequestDTO requestClient, String username)
    {
        Client clientFound = findValidClient(id);
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

    public void mapRequestToClient(Client client, ClientRequestDTO requestClient, String username)
    {
        //MAIN FIELDS
        client.username = requestClient.username;
        client.name = requestClient.name;
        client.lastName = requestClient.lastName;
        client.email = requestClient.email;
        client.passwordHashed = BcryptUtil.bcryptHash(requestClient.password);
        client.phone = requestClient.phone;
        client.address = requestClient.address;
        client.balance = requestClient.balance;

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
        client.balance = requestClient.balance;

        //AUDIT FIELDS
        client.updatedBy = username;
        client.updatedAt = Instant.now();
    }


}
