package com.votexa.election;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findAllByOrderByIdDesc();
    Optional<Election> findByCode(String code);
}
