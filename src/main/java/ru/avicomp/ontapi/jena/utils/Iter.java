/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.utils;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.jena.util.iterator.ClosableIterator;

/**
 * To work with Jena Iterators.
 * Created by szuev on 11.04.2017.
 * @see org.apache.jena.util.iterator.ExtendedIterator
 * @see org.apache.jena.atlas.iterator.Iter
 * @see ClosableIterator
 */
public class Iter {

    /**
     * Wraps CloseableIterator as Stream.
     * Don't forget to call explicit {@link Stream#close()} if the inner iterator are not exhausted
     * ({@link Iterator#hasNext()} is still true).
     * It seems it should be called for such operations as {@link Stream#findFirst()}, {@link Stream#findAny()}, {@link Stream#anyMatch(Predicate)} etc.
     *
     * @param iterator {@link ClosableIterator}
     * @param <T> the class-type of iterator
     * @return Stream
     */
    public static <T> Stream<T> asStream(ClosableIterator<T> iterator) {
        return org.apache.jena.atlas.iterator.Iter.asStream(iterator).onClose(iterator::close);
    }

}
