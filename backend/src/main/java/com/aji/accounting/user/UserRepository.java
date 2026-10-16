package com.aji.accounting.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "company"})
    @Query("select u from User u where lower(u.username) = lower(:identifier) or lower(u.email) = lower(:identifier)")
    Optional<User> findByIdentifierWithAuthorities(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "company"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdWithAuthorities(@Param("id") Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (:q is null or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(u.fullName) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("q") String q, @Param("status") UserStatus status, Pageable pageable);

    @Query("select count(u) from User u join u.roles r where r.code = :roleCode and u.status = :status")
    long countByRoleAndStatus(@Param("roleCode") String roleCode, @Param("status") UserStatus status);
}
