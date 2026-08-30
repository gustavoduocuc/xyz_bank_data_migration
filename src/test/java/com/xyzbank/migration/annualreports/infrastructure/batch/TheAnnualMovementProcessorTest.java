package com.xyzbank.migration.annualreports.infrastructure.batch;

import com.xyzbank.migration.annualreports.domain.AnnualMovement;
import com.xyzbank.migration.annualreports.domain.MovementType;
import com.xyzbank.migration.shared.domain.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TheAnnualMovementProcessorTest {

    /*
     * Cases:
     * 1. Accepts valid movement
     * 2. Does not allow zero deposit
     * 3. Does not allow duplicate movement
     */

    @Nested
    class TheAnnualMovementProcessor {

        private AnnualMovementProcessor processor;

        @BeforeEach
        void setUp() {
            processor = new AnnualMovementProcessor();
        }

        @Test
        void acceptsValidMovement() {
            AnnualMovement movement = processor.process(line("101", "2024-01-01", "deposito", 1000.0, "Ingreso mensual"));

            assertEquals(MovementType.DEPOSIT, movement.type());
        }

        @Test
        void doesNotAllowZeroDeposit() {
            assertThrows(DomainError.class,
                    () -> processor.process(line("107", "2024-12-25", "deposito", 0.0, "Ingreso navideño")));
        }

        @Test
        void doesNotAllowDuplicateMovement() {
            processor.process(line("101", "2024-01-01", "deposito", 1000.0, "Ingreso mensual"));

            assertThrows(DomainError.class,
                    () -> processor.process(line("101", "2024-01-01", "deposito", 1000.0, "Ingreso mensual")));
        }

        @Test
        void acceptsPaddedTextFields() {
            AnnualMovement movement = processor.process(line(" 101 ", " 2024-01-01 ", " deposito ", 1000.0, " Ingreso mensual "));

            assertEquals(MovementType.DEPOSIT, movement.type());
            assertEquals("101", movement.accountIdValue());
        }

        private AnnualMovementLine line(String accountId, String date, String type, Double amount, String description) {
            AnnualMovementLine line = new AnnualMovementLine();
            line.setCuentaId(accountId);
            line.setFecha(date);
            line.setTransaccion(type);
            line.setMonto(amount);
            line.setDescripcion(description);
            return line;
        }
    }
}
