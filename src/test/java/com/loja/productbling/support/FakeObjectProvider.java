package com.loja.productbling.support;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FakeObjectProvider<T> implements ObjectProvider<T> {

    private final T value;

    public FakeObjectProvider(T value) {
        this.value = value;
    }

    @Override
    public T getIfAvailable() {
        return value;
    }

    @Override
    public T getIfUnique() {
        return value;
    }

    @Override
    public T getObject() {
        if (value == null) {
            throw new NoSuchBeanDefinitionException("nenhum bean disponível no teste");
        }
        return value;
    }

    @Override
    public T getObject(Object... args) {
        return getObject();
    }

    @Override
    public Iterator<T> iterator() {
        return value == null ? Collections.emptyIterator() : List.of(value).iterator();
    }
}
