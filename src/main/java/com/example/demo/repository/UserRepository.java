package com.example.demo.repository;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    @Query("""
            select u from AppUser u
            where u.role = :role
              and (:q is null
                   or lower(u.name) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%')))
            order by u.id desc
            """)
    List<AppUser> searchByRole(@Param("role") Role role, @Param("q") String q);
}
