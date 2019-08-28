package com.connectycube.messenger.api.utils;

import com.connectycube.core.EntityCallback;
import com.connectycube.core.PerformProcessor;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.server.Performer;

public class PerformerImpl<T> implements Performer<T> {
    @Override
    public void performAsync(EntityCallback<T> entityCallback) {

    }

    @Override
    public T perform() throws ResponseException {
        return null;
    }

    @Override
    public <R> R convertTo(PerformProcessor<?> performProcessor) {
        return null;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void cancel() {

    }
}
