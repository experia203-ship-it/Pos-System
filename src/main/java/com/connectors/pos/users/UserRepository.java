package com.connectors.pos.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users,Long> {

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);
}
