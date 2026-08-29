package com.xyzbank.migration.monthlyinterests.infrastructure.batch;

import com.xyzbank.migration.shared.infrastructure.batch.CsvFieldNormalizer;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.lang.NonNull;

public class InterestAccountLineMapper implements FieldSetMapper<InterestAccountLine> {

    @Override
    public InterestAccountLine mapFieldSet(@NonNull FieldSet fieldSet) {
        InterestAccountLine line = new InterestAccountLine();
        line.setCuentaId(CsvFieldNormalizer.text(fieldSet.readRawString("cuentaId")));
        line.setNombre(CsvFieldNormalizer.text(fieldSet.readRawString("nombre")));
        line.setSaldo(CsvFieldNormalizer.amount(fieldSet.readRawString("saldo")));
        line.setEdad(CsvFieldNormalizer.integer(fieldSet.readRawString("edad")));
        line.setTipo(CsvFieldNormalizer.text(fieldSet.readRawString("tipo")));
        return line;
    }
}
