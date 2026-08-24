package com.xyzbank.migration.shared.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.lang.NonNull;

public class LoggingSkipListener<T, S> implements SkipListener<T, S> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingSkipListener.class);

    @Override
    public void onSkipInRead(@NonNull Throwable throwable) {
        logger.warn(
                "Skipped during read exceptionType={} reason={}",
                throwable.getClass().getSimpleName(),
                throwable.getMessage()
        );
    }

    @Override
    public void onSkipInProcess(@NonNull T item, @NonNull Throwable throwable) {
        logger.warn(
                "Skipped during process exceptionType={} item={} reason={}",
                throwable.getClass().getSimpleName(),
                item,
                throwable.getMessage()
        );
    }

    @Override
    public void onSkipInWrite(@NonNull S item, @NonNull Throwable throwable) {
        logger.warn(
                "Skipped during write exceptionType={} item={} reason={}",
                throwable.getClass().getSimpleName(),
                item,
                throwable.getMessage()
        );
    }
}
