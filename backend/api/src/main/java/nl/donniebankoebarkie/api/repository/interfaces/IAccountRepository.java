package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IAccountRepository extends JpaRepository<Account, Long> {
    boolean existsByIban(String iban);

    List<Account> findByUserId(Long userId);
}
