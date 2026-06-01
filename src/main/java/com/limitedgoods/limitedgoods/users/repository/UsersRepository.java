package com.limitedgoods.limitedgoods.users.repository;

import com.limitedgoods.limitedgoods.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

}
