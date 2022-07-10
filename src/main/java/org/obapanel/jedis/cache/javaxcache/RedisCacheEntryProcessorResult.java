package org.obapanel.jedis.cache.javaxcache;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

class RedisCacheEntryProcessorResult<T> implements EntryProcessorResult<T> {

    private final T result;
    private final Exception error;

    RedisCacheEntryProcessorResult(T result) {
        this.result = result;
        this.error = null;
    }

    RedisCacheEntryProcessorResult(Exception error) {
        this.result = null;
        this.error = error;
    }


    @Override
    public T get() throws EntryProcessorException {
        if (result != null) {
            return result;
        } else if (error != null) {
            throw new EntryProcessorException(error);
        } else {
            throw new IllegalStateException("RedisCacheEntryProcessorResult has no data nor error");
        }
    }
}