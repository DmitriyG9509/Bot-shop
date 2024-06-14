package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.PaspayUserNames;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserNamesRepo extends JpaRepository<PaspayUserNames, String> {

    Optional<PaspayUserNames> findByUserNameOffice(String userName);
    @Transactional
    @Modifying
    @Query(name = "PaspayUserNames.deleteByUserNameOffice")
    void deleteByUserNameOffice(@Param("userName") String userName);
}
