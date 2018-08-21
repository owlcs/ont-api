/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link OntObjectFactory Ontology Object Factory} implementation that combines several other factories.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class MultiOntObjectFactory extends OntObjectFactory {
    private final List<OntObjectFactory> factories;
    private OntFinder finder;
    private OntFilter fittingFilter;

    /**
     *
     * @param finder        {@link OntFinder}, optional, if null then uses only array of sub-factories to search
     * @param fittingFilter {@link OntFilter}, optional, to trim searching
     * @param factories     the array of factories to combine, not null, not empty.
     */
    public MultiOntObjectFactory(OntFinder finder, OntFilter fittingFilter, OntObjectFactory... factories) {
        this.finder = finder;
        this.fittingFilter = fittingFilter;
        this.factories = unbend(factories);
    }

    private static List<OntObjectFactory> unbend(OntObjectFactory... factories) {
        return Arrays.stream(factories)
                .map(f -> f instanceof MultiOntObjectFactory ? ((MultiOntObjectFactory) f).factories() : Stream.of(f))
                .flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        EnhNode res = doWrap(node, eg);
        if (res != null) return res;
        throw new ConversionException("Can't wrap node " + node + ". Use direct factory.");
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return !(fittingFilter != null && !fittingFilter.test(node, eg)) && factories().anyMatch(f -> f.canWrap(node, eg));
    }

    @Override
    protected EnhNode doWrap(Node node, EnhGraph eg) {
        if (fittingFilter != null && !fittingFilter.test(node, eg)) return null;
        return factories().filter(f -> f.canWrap(node, eg)).map(f -> f.doWrap(node, eg)).findFirst().orElse(null);
    }

    @Override
    public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
        if (finder != null) {
            return finder.iterator(eg).mapWith(n -> doWrap(n, eg)).filterDrop(Objects::isNull);
        }
        // in ONT-API the following code is not used:
        return WrappedIterator.create(factories().flatMap(f -> f.find(eg)).distinct().iterator());

    }

    public OntFinder getFinder() {
        return finder;
    }

    public OntFilter getFilter() {
        return fittingFilter;
    }

    public Stream<? extends OntObjectFactory> factories() {
        return factories.stream();
    }

}
