package com.limitedgoods.limitedgoods.user.repository;

import com.limitedgoods.limitedgoods.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
          from User u
         where u.id = :userId
    """)
    Optional<User> findByIdForUpdate(
            @Param("userId") Long userId
    );
}
