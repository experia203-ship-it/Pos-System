package com.connectors.pos.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<ShiftSession,Long> {

@Query("select s from ShiftSession s where s.user.id=:id and s.status='OPEN'")
    Optional<ShiftSession> findOpenShift(@Param("id") Long userId);


}
