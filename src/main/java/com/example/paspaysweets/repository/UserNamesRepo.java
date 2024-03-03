package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.PaspayUserNames;
import com.example.paspaysweets.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNamesRepo extends JpaRepository<PaspayUserNames, String> {

    Optional<PaspayUserNames> findByUserNameOffice(String userName);
}
