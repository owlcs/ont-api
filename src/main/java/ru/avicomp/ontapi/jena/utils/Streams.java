package ru.avicomp.ontapi.jena.utils;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper to work with java streams ({@link Stream}).
 * Just to simplify the code.
 * <p>
 * Created by @szuev on 24.01.2017.
 */
public class Streams {

    /**
     * converts Iterator to distinct sequential Stream
     *
     * @param iterator iterator
     * @return Stream
     */
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return asStream(iterator, true, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean distinct, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        Stream<T> res = StreamSupport.stream(iterable.spliterator(), parallel);
        return distinct ? res.distinct() : res;
    }
}
