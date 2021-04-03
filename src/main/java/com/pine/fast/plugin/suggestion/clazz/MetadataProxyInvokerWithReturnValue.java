package com.pine.fast.plugin.suggestion.clazz;

public interface MetadataProxyInvokerWithReturnValue<T> {

    T invoke(MetadataProxy delegate);
}
