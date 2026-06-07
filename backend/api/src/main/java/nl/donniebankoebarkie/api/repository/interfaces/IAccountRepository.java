package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IAccountRepository extends JpaRepository<Account, Long> {
    boolean existsByIban(String iban);

    List<Account> findByUserId(Long userId);

    // Fetches active customer accounts for IBAN lookup without a custom JPQL query.
    @EntityGraph(attributePaths = "user")
    Page<Account> findByActiveTrueAndUser_RoleAndUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase(
            UserRole role,
            String firstName,
            String lastName,
            Pageable pageable
    );
}
