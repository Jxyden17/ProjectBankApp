package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IAuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByBsnNumber(String bsnNumber);

    @Query("""
            select u from User u
            where u.role = :role
              and u.isApproved = false
            """)
    Page<User> findPendingCustomers(@Param("role") UserRole role, Pageable pageable);
}
