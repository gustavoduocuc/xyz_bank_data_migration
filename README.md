# XYZ Bank Data Migration

Migración de datos bancarios con **Spring Boot 3.5** y **Spring Batch 5**. Procesa los CSV de `data/semana_2` mediante tres jobs independientes (Reader → Processor → Writer), con persistencia JDBC en **MySQL**, skip/retry personalizados y process steps multithread.

Documentación ampliada:

- [docs/jobs.md](docs/jobs.md) — diagramas y flujo de cada job
- [docs/mysql.md](docs/mysql.md) — Docker MySQL, conexión y consultas de reportes

## Stack

| Tecnología | Uso |
|---|---|
| Java 17+ | Lenguaje |
| Spring Boot 3.5 | Bootstrap |
| Spring Batch 5 | Jobs, steps, skip/retry |
| MySQL 8.4 | Datos de negocio + JobRepository Batch |
| Docker Compose | MySQL local |
| Maven Wrapper | Build y ejecución |

## Arquitectura

Hexagonal por módulo. Dominio sin Spring. Batch e adapters JDBC en infraestructura.

```
src/main/java/com/xyzbank/migration/
├── shared/
│   ├── application/ports/      MigrationExecutionPort
│   └── infrastructure/
│       ├── adapters/           JdbcMigrationExecutionAdapter
│       └── batch/              Guard, LedgerListener, summary
├── dailytransactions/
│   ├── application/ports/      DailyReportWriter
│   └── infrastructure/
│       ├── adapters/           JdbcDailyReportWriter
│       └── batch/              dailyTransactionsJob
├── monthlyinterests/ ...       AccountBalanceWriter → JdbcAccountBalanceWriter
└── annualreports/ ...          AnnualAuditWriter → JdbcAnnualAuditWriter
```

## Jobs (resumen)

| Job | Guard | Process | Tabla MySQL |
|---|---|---|---|
| `dailyTransactionsJob` | `checkDailyMigrationNotDone` | `processDailyTransactions` | `daily_transaction_reports` |
| `monthlyInterestsJob` | `checkMonthlyMigrationNotDone` | `calculateMonthlyInterests` | `account_balances` |
| `annualGenerationJob` | `checkAnnualMigrationNotDone` | `compileAnnualAudit` | `annual_audit_reports` |

Si el job ya tiene `SUCCESS` en `migration_executions`, se omite el process (`ALREADY_MIGRATED`). Cada lanzamiento usa `RunIdIncrementer` para crear una nueva instancia Batch y consultar el ledger. Detalle en [docs/jobs.md](docs/jobs.md).

## Escalado y resiliencia

| Parámetro | Default | Descripción |
|---|---|---|
| `migration.batch.chunk-size` | `5` | Tamaño de chunk |
| `migration.batch.throttle-limit` | `3` | Hilos del `TaskExecutor` en el process step |
| `migration.batch.skip-limit` | `100` | Tope de skips de dominio/parse |
| `migration.batch.retry-limit` | `3` | Reintentos JDBC transitorios |

Los process steps usan `SynchronizedItemStreamReader`, `DomainSkipPolicy`, `TransientDataAccessRetryPolicy`, y listeners de métricas (`Step metrics ... throughputPerSec`) para ajustar configuración mirando los logs.

## Reglas de negocio

### Transacciones diarias

Se omiten (`skip`) registros con:

- `monto <= 0`
- tipo inválido
- campos obligatorios nulos
- duplicados por `fecha + monto + tipo`

Las anomalías (monto alto, duplicados detectados) se registran en el reporte sin bloquear la escritura cuando el registro es válido.

### Intereses mensuales

Se omiten registros con:

- `saldo <= 0`
- edad fuera del rango 18–100
- tipo inválido
- campos nulos
- `cuenta_id` duplicado

Tasas inferidas:

| Tipo | Condición | Tasa |
|---|---|---|
| ahorro | edad menor a 65 | 1.00% |
| ahorro | edad 65 o más | 1.50% |
| prestamo | — | 1.50% |
| hipoteca | — | 0.80% |

### Auditoría anual

Se omiten registros con:

- depósito con `monto == 0`
- tipo inválido
- campos nulos
- duplicados

Los retiros/compras con montos negativos son válidos. El writer consolida por `cuenta_id`.

## Requisitos previos

Para poder ejecutar el proyecto necesitas tener instalado:
- **JDK 17+** (configurado en el `PATH` o mediante `JAVA_HOME`).
- **Docker** y **Docker Compose** (para levantar la base de datos MySQL local).

*Nota: No es necesario tener Maven instalado de forma global, ya que el proyecto incluye **Maven Wrapper** (`mvnw` / `mvnw.cmd`).*

## Cómo ejecutar

### 1. Levantar MySQL

```bash
docker compose up -d
```

Conexión: `localhost:3306`, DB `xyz_bank_migration`, user/password `migration`/`migration`. Más detalle en [docs/mysql.md](docs/mysql.md).

### 2. Tests

```bash
# En Windows (CMD / PowerShell):
.\mvnw.cmd test

# En Linux / macOS:
./mvnw test
```

### 3. Correr un job

```bash
# En Windows (CMD / PowerShell):
.\mvnw.cmd spring-boot:run -D"spring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=dailyTransactionsJob"
.\mvnw.cmd spring-boot:run -D"spring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=monthlyInterestsJob"
.\mvnw.cmd spring-boot:run -D"spring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=annualGenerationJob"

# En Linux / macOS:
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.enabled=true --spring.batch.job.name=dailyTransactionsJob"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.enabled=true --spring.batch.job.name=monthlyInterestsJob"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.enabled=true --spring.batch.job.name=annualGenerationJob"
```

Por defecto `spring.batch.job.enabled=false`.

### 4. Demo de performance (CSV sintético)

```bash
python3 scripts/generate-performance-data.py

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=performance \
  -Dspring-boot.run.arguments="--spring.batch.job.enabled=true --spring.batch.job.name=dailyTransactionsJob"
```

Los CSV grandes viven en `data/performance/` (ignorados por git). En los logs buscá `Step metrics` y `Starting job=... chunkSize=... throttleLimit=...` para comparar rendimiento.

### 5. Ver reportes migrados

```bash
docker compose exec mysql mysql -umigration -pmigration xyz_bank_migration -e "SELECT * FROM migration_executions; SELECT * FROM daily_transaction_reports LIMIT 10;"
```

Consultas adicionales en [docs/mysql.md](docs/mysql.md).

## Revertir y volver a migrar

```bash
# En Linux / macOS / CMD:
docker compose exec -T mysql mysql -umigration -pmigration xyz_bank_migration < scripts/revert-migration.sql

# En Windows (PowerShell):
Get-Content scripts\revert-migration.sql | docker compose exec -T mysql mysql -umigration -pmigration xyz_bank_migration
```

Luego vuelve a ejecutar el job deseado. El script limpia tablas de negocio, `migration_executions` y metadatos `BATCH_*`.

## Datos de entrada

Default: **semana_2**. También existen `data/semana_1` y CSVs sintéticos en `data/performance/` (generados).

| Archivo | Job |
|---|---|
| [`data/semana_2/transacciones.csv`](data/semana_2/transacciones.csv) | dailyTransactionsJob |
| [`data/semana_2/intereses.csv`](data/semana_2/intereses.csv) | monthlyInterestsJob |
| [`data/semana_2/cuentas_anuales.csv`](data/semana_2/cuentas_anuales.csv) | annualGenerationJob |
