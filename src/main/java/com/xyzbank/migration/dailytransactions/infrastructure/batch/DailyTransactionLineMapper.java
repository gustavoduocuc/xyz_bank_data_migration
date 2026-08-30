package com.xyzbank.migration.dailytransactions.infrastructure.batch;

import com.xyzbank.migration.shared.infrastructure.batch.CsvFieldNormalizer;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.lang.NonNull;

public class DailyTransactionLineMapper implements FieldSetMapper<DailyTransactionLine> {

    @Override
    public DailyTransactionLine mapFieldSet(@NonNull FieldSet fieldSet) {
        DailyTransactionLine line = new DailyTransactionLine();
        line.setId(CsvFieldNormalizer.text(fieldSet.readRawString("id")));
        line.setFecha(CsvFieldNormalizer.text(fieldSet.readRawString("fecha")));
        line.setMonto(CsvFieldNormalizer.amount(fieldSet.readRawString("monto")));
        line.setTipo(CsvFieldNormalizer.text(fieldSet.readRawString("tipo")));
        return line;
    }
}
