package ru.avicomp.ontapi.jena.utils;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.jena.util.iterator.ClosableIterator;

/**
 * To work with Jena Iterators.
 *
 * @see org.apache.jena.util.iterator.ExtendedIterator
 * @see org.apache.jena.atlas.iterator.Iter
 * @see ClosableIterator
 * Created by szuev on 11.04.2017.
 */
public class Iter {

    /**
     * Wraps CloseableIterator as Stream.
     * Don't forget to call explicit {@link Stream#close()} if the inner iterator are not exhausted
     * ({@link Iterator#hasNext()} is still true).
     * It seems it should be called for such operations as {@link Stream#findFirst()}, {@link Stream#anyMatch(Predicate)} etc.
     *
     * @param iterator {@link ClosableIterator}
     * @return Stream
     */
    public static <T> Stream<T> asStream(ClosableIterator<T> iterator) {
        return org.apache.jena.atlas.iterator.Iter.asStream(iterator).onClose(iterator::close);
    }

}
