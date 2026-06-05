package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface ITransactionRepository extends JpaRepository<Transaction, Long>{
    List<Transaction> findByInitiatedByUserId(Long id);
}