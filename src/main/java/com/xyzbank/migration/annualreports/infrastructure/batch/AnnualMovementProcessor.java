package com.xyzbank.migration.annualreports.infrastructure.batch;

import com.xyzbank.migration.annualreports.domain.AnnualMovement;
import com.xyzbank.migration.shared.domain.DomainError;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnnualMovementProcessor implements ItemProcessor<AnnualMovementLine, AnnualMovement> {

    private final Set<String> seenBusinessKeys = ConcurrentHashMap.newKeySet();

    @Override
    public AnnualMovement process(@NonNull AnnualMovementLine line) {
        if (line.getMonto() == null) {
            throw DomainError.validation("Movement amount cannot be empty");
        }

        AnnualMovement movement = AnnualMovement.create(
                line.getCuentaId(),
                line.getFecha(),
                line.getTransaccion(),
                line.getMonto(),
                line.getDescripcion()
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
