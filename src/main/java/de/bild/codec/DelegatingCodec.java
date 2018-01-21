package de.bild.codec;

import org.bson.codecs.Codec;

public interface DelegatingCodec<T> {
    Codec<T> getDelegate();

    default Codec<T> unWrapRecursively() {
        Codec<T> delegate;
        do {
            delegate =  getDelegate();
        } while (delegate instanceof DelegatingCodec);
        return delegate;
    }

}