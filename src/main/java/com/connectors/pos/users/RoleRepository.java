package com.connectors.pos.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Roles,Long> {


    Optional<Roles> findByName(String name);

    @Modifying
    @Query(value = "UPDATE roles SET name = name WHERE name = 'ROLE_ADMIN'", nativeQuery = true)
    int lockInitialAdministratorRegistration();
}
