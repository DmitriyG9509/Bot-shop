package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {
}
