package com.xyzbank.migration.annualreports.infrastructure.batch;

import com.xyzbank.migration.shared.infrastructure.batch.CsvFieldNormalizer;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.lang.NonNull;

public class AnnualMovementLineMapper implements FieldSetMapper<AnnualMovementLine> {

    @Override
    public AnnualMovementLine mapFieldSet(@NonNull FieldSet fieldSet) {
        AnnualMovementLine line = new AnnualMovementLine();
        line.setCuentaId(CsvFieldNormalizer.text(fieldSet.readRawString("cuentaId")));
        line.setFecha(CsvFieldNormalizer.text(fieldSet.readRawString("fecha")));
        line.setTransaccion(CsvFieldNormalizer.text(fieldSet.readRawString("transaccion")));
        line.setMonto(CsvFieldNormalizer.amount(fieldSet.readRawString("monto")));
        line.setDescripcion(CsvFieldNormalizer.text(fieldSet.readRawString("descripcion")));
        return line;
    }
}
