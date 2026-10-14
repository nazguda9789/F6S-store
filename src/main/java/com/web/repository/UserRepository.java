package com.web.repository;

import com.web.entity.User;
import com.web.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("select u from User u where u.email = ?1")
    Optional<User> findByEmail(String email);

    @Query("select u from User u where u.role = ?1")
    List<User> findByRole(Role role);

    @Query("select cast(count(u.id) as int ) from User u where u.role = ?1")
    public Integer countByRole(Role role);

    @Query("select u.id from User u where u.email = ?1")
    Long findIdByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

}
