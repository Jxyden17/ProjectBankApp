package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ITransactionRepository extends JpaRepository<Transaction, Long>{
    List<Transaction> findByInitiated_by_user_id(Long id);
    Transaction findTransactionById(Long id);

    void createTransaction(Transaction transaction);
}