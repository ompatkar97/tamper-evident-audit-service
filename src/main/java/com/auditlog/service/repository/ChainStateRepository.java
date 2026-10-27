package com.auditlog.service.repository;import com.auditlog.service.entity.ChainState;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface ChainStateRepository extends JpaRepository<ChainState,Short>{@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select s from ChainState s where s.id=1")ChainState lock();}
