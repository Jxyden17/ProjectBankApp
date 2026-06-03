package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ITransactionRepository extends JpaRepository<Transaction, Long>{
    Optional<Transaction> findById(Long transactionId);
}