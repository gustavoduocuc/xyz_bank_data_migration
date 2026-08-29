package com.xyzbank.migration.monthlyinterests.infrastructure.batch;

import com.xyzbank.migration.monthlyinterests.domain.Account;
import com.xyzbank.migration.monthlyinterests.domain.InterestApplied;
import com.xyzbank.migration.monthlyinterests.domain.InterestRatePolicy;
import com.xyzbank.migration.shared.domain.DomainError;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MonthlyInterestProcessor implements ItemProcessor<InterestAccountLine, InterestApplied> {

    private final Set<String> seenAccountIds = ConcurrentHashMap.newKeySet();

    @Override
    public InterestApplied process(@NonNull InterestAccountLine line) {
        if (line.getSaldo() == null) {
            throw DomainError.validation("Account balance cannot be empty");
        }
        if (line.getEdad() == null) {
            throw DomainError.validation("Account age cannot be empty");
        }

        Account account = Account.create(
                line.getCuentaId(),
                line.getNombre(),
                line.getSaldo(),
                line.getEdad(),
                line.getTipo()
        );

        if (isDuplicateAccount(account.idValue())) {
            throw DomainError.validation("Duplicate account skipped: " + account.idValue());
        }

        return InterestRatePolicy.apply(account);
    }

    private boolean isDuplicateAccount(String accountId) {
        boolean alreadySeen = !seenAccountIds.add(accountId);
        return alreadySeen;
    }
}
