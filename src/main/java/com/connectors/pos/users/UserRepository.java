package com.connectors.pos.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users,Long> {

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByEmailAndMustChangePasswordTrue(String email);

    Optional<Users> findByEmail(String email);

    long count();

    @Modifying
    @Query(value = "UPDATE users SET name = name WHERE id = :id", nativeQuery = true)
    int lockUserForShiftOpening(@org.springframework.data.repository.query.Param("id") Long userId);

    @Query("SELECT COUNT(u) FROM Users u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}
