package com.benito.sales.seed;


import com.benito.sales.entity.User;
import com.benito.sales.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


//THIS CLASS WAS DEVELOPED ONLY TO SERVE A PURPOSE OF THIS TEST
@ApplicationScoped
public class AdminSeeder {

    private final UserRepository _userRepository;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       @ConfigProperty(name = "user.seeder.adminPassword") String adminPassword)
    {
        this._userRepository = userRepository;
        this.adminPassword = adminPassword;
    }

    void seedFirstAdmin(@Observes StartupEvent event)
    {
        long lookForAdmin = _userRepository.find("username", "admin").count();

        if (lookForAdmin > 0)
            return;

        User firstAdmin = new User();
        firstAdmin.username = "admin";
        firstAdmin.email = "admin@toke.com";
        firstAdmin.passwordHash = BcryptUtil.bcryptHash(adminPassword);
        firstAdmin.roles = new HashSet<>(Set.of("admin"));

        firstAdmin.status = true;
        firstAdmin.createdBy = "Quarkus";
        firstAdmin.createdAt = Instant.now();

        _userRepository.persist(firstAdmin);


    }
}
