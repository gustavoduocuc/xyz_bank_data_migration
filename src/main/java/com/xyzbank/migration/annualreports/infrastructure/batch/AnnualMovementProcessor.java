package com.xyzbank.migration.annualreports.infrastructure.batch;

import com.xyzbank.migration.annualreports.domain.AnnualMovement;
import com.xyzbank.migration.shared.domain.DomainError;
import com.xyzbank.migration.shared.infrastructure.batch.CsvFieldNormalizer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnnualMovementProcessor implements ItemProcessor<AnnualMovementLine, AnnualMovement> {

    private final Set<String> seenBusinessKeys = ConcurrentHashMap.newKeySet();

    @Override
    public AnnualMovement process(@NonNull AnnualMovementLine line) {
        Double amount = CsvFieldNormalizer.scaleAmount(line.getMonto());
        if (amount == null) {
            throw DomainError.validation("Movement amount cannot be empty");
        }

        AnnualMovement movement = AnnualMovement.create(
                CsvFieldNormalizer.text(line.getCuentaId()),
                CsvFieldNormalizer.text(line.getFecha()),
                CsvFieldNormalizer.text(line.getTransaccion()),
                amount,
                CsvFieldNormalizer.text(line.getDescripcion())
        );

        if (isDuplicateMovement(movement.businessKey())) {
            throw DomainError.validation("Duplicate annual movement skipped: " + movement.businessKey());
        }

        return movement;
    }

    private boolean isDuplicateMovement(String businessKey) {
        boolean alreadySeen = !seenBusinessKeys.add(businessKey);
        return alreadySeen;
    }
}
