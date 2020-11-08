package io.peasoup.inv.loader;

import groovy.lang.MetaClass;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class InterpolableGroovyObject implements Map<String, String> {

    private final Object source;
    private final MetaClass metaClass;

    public InterpolableGroovyObject(Object source) {
        if (source == null)
            throw new IllegalArgumentException("source");

        this.source = source;
        this.metaClass = InvokerHelper.getMetaClass(source);
    }

    @Override
    public int size() {
        return this.metaClass.getProperties().size();
    }

    @Override
    public boolean isEmpty() {
        return this.metaClass.getProperties().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.metaClass.hasProperty(source, key.toString()) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new NotImplementedException();
    }

    @Override
    public String get(Object key) {
        return this.metaClass.getProperty(source, key.toString()).toString();
    }

    @Override
    public String put(String key, String value) {
        throw new NotImplementedException();
    }

    @Override
    public String remove(Object key) {
        throw new NotImplementedException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> keySet() {
        throw new NotImplementedException();
    }

    @Override
    public Collection<String> values() {
        throw new NotImplementedException();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new NotImplementedException();
    }
}
