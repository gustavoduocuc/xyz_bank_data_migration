package com.xyzbank.migration.annualreports.infrastructure.batch;

import com.xyzbank.migration.annualreports.domain.AnnualMovement;
import com.xyzbank.migration.shared.domain.DomainError;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.HashSet;
import java.util.Set;

public class AnnualMovementProcessor implements ItemProcessor<AnnualMovementLine, AnnualMovement> {

    private final Set<String> seenBusinessKeys = new HashSet<>();

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
        if (seenBusinessKeys.contains(businessKey)) {
            return true;
        }
        seenBusinessKeys.add(businessKey);
        return false;
    }
}
