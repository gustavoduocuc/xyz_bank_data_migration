package com.xyzbank.migration.dailytransactions.infrastructure.batch;

import com.xyzbank.migration.dailytransactions.domain.AnomalyDetector;
import com.xyzbank.migration.dailytransactions.domain.ProcessedTransaction;
import com.xyzbank.migration.dailytransactions.domain.Transaction;
import com.xyzbank.migration.shared.domain.DomainError;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

public class DailyTransactionProcessor implements ItemProcessor<DailyTransactionLine, ProcessedTransaction> {

    private final AnomalyDetector anomalyDetector;

    public DailyTransactionProcessor(AnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    @Override
    public ProcessedTransaction process(@NonNull DailyTransactionLine line) {
        if (line.getMonto() == null) {
            throw DomainError.validation("Transaction amount cannot be empty");
        }

        Transaction transaction = Transaction.create(
                line.getId(),
                line.getFecha(),
                line.getMonto(),
                line.getTipo()
        );
        ProcessedTransaction processed = anomalyDetector.evaluate(transaction);

        if (processed.isDuplicate()) {
            throw DomainError.validation("Duplicate transaction skipped: " + transaction.businessKey());
        }

        return processed;
    }
}
