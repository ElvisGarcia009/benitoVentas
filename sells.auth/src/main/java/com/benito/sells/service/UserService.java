package com.benito.sells.service;

import com.benito.sells.dto.UserRequestDTO;
import com.benito.sells.dto.UserResponseDTO;
import com.benito.sells.dto.UserRolesRequestDTO;
import com.benito.sells.dto.UsersPaginatedDTO;
import com.benito.sells.entity.User;
import com.benito.sells.exception.ConflictException;
import com.benito.sells.exception.UserNotFoundException;
import com.benito.sells.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class UserService {

    //DEPENDENCY INJECTION -> Repository
    private final UserRepository _userRepository;
    public UserService(UserRepository userRepository)
    {
        this._userRepository = userRepository;
    }

    //GET ALL USERS, PAGINATED
    public UsersPaginatedDTO getAllUsers(int page, int pageSize)
    {
        PanacheQuery<User> query = _userRepository.find("status", Sort.by("username").ascending(), true)
                                                  .page(page, pageSize);

        List<UserResponseDTO> users = query.stream().map(UserResponseDTO::TurnIntoDTO).toList();
        int totalPages = query.pageCount();
        long totalUsers = query.count();

        return new UsersPaginatedDTO(users, page, pageSize, totalPages, totalUsers);
    }


    //GET A USER BY ID
    public UserResponseDTO getUserById(ObjectId id)
    {
        User userFound = findValidUser(id);
        return UserResponseDTO.TurnIntoDTO(userFound);
    }


    //POST A NEW USER
    public UserResponseDTO createUser(UserRequestDTO requestUser, String username)
    {
        long duplicatesCount = _userRepository.count("(username = ?1 or email = ?2) and status = ?3",
                requestUser.username,
                requestUser.email,
                true);

        if (duplicatesCount > 0)
            throw new ConflictException();

        User newUser = new User();
        mapRequestToUser(newUser, requestUser, username);

        _userRepository.persist(newUser);

        return UserResponseDTO.TurnIntoDTO(newUser);
    }


    //PUT, UPDATE THE ROLES OF A USER
    public UserResponseDTO updateUserRoles(ObjectId id, UserRolesRequestDTO requestRoles, String username)
    {
        User userFound = findValidUser(id);

        userFound.roles = new HashSet<>(requestRoles.roles);

        //AUDIT FIELDS
        userFound.updatedBy = username;
        userFound.updatedAt = Instant.now();

        _userRepository.update(userFound);

        return UserResponseDTO.TurnIntoDTO(userFound);
    }


    //DELETE A USER (this method just deactivate the user)
    public void deleteUser(ObjectId id, String username)
    {
        User userFound = findValidUser(id);

        userFound.status = false;
        userFound.updatedBy = username;
        userFound.updatedAt = Instant.now();

        _userRepository.update(userFound);
    }




    //HELPERS
    private User findValidUser(ObjectId id)
    {
        User userContainer = _userRepository.findById(id);

        if (userContainer == null || !userContainer.status)
            throw new UserNotFoundException(id);

        return userContainer;
    }

    private void mapRequestToUser(User user, UserRequestDTO requestUser, String username)
    {
        //MAIN FIELDS
        user.username = requestUser.username;
        user.email = requestUser.email;
        user.passwordHash = BcryptUtil.bcryptHash(requestUser.password);
        user.roles = new HashSet<>(requestUser.roles);

        //AUDIT FIELDS
        user.status = true;
        user.createdBy = username;
        user.createdAt = Instant.now();
    }
}