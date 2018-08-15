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

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * An Internal Data Factory without cache.
 * <p>
 * Created by @szuev on 15.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class NoCacheDataFactory implements InternalDataFactory {
    protected final ConfigProvider.Config config;

    public NoCacheDataFactory(ConfigProvider.Config config) {
        this.config = config;
    }

    @Override
    public void clear() {
        // nothing
    }

    protected IRI toIRI(Resource r) {
        return toIRI(r.getURI());
    }

    @Override
    public ONTObject<? extends OWLClassExpression> get(OntCE ce) {
        return ReadHelper.calcClassExpression(ce, this, new HashSet<>());
    }

    @Override
    public ONTObject<? extends OWLDataRange> get(OntDR dr) {
        return ReadHelper.calcDataRange(dr, this, new HashSet<>());
    }

    @Override
    public ONTObject<OWLAnnotationProperty> get(OntNAP nap) {
        IRI iri = toIRI(OntApiException.notNull(nap, "Null annotation property."));
        return ONTObject.create(getOWLDataFactory().getOWLAnnotationProperty(iri), nap);
    }

    @Override
    public ONTObject<OWLDataProperty> get(OntNDP ndp) {
        IRI iri = toIRI(OntApiException.notNull(ndp, "Null data property."));
        return ONTObject.create(getOWLDataFactory().getOWLDataProperty(iri), ndp);
    }

    @Override
    public ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE ope) {
        OntApiException.notNull(ope, "Null object property.");
        if (ope.isAnon()) { //todo: handle inverse of inverseOf (?)
            OWLObjectProperty op = getOWLDataFactory().getOWLObjectProperty(toIRI(ope.as(OntOPE.Inverse.class).getDirect()));
            return ONTObject.create(op.getInverseProperty(), ope);
        }
        return ONTObject.create(getOWLDataFactory().getOWLObjectProperty(toIRI(ope)), ope);
    }

    @Override
    public ONTObject<? extends OWLIndividual> get(OntIndividual individual) {
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return ONTObject.create(getOWLDataFactory().getOWLNamedIndividual(toIRI(individual)), individual);
        }
        String label = //NodeFmtLib.encodeBNodeLabel(individual.asNode().getBlankNodeLabel());
                individual.asNode().getBlankNodeLabel();
        return ONTObject.create(getOWLDataFactory().getOWLAnonymousIndividual(label), individual);
    }

    @Override
    public ONTObject<OWLLiteral> get(Literal literal) {
        String txt = OntApiException.notNull(literal, "Null literal").getLexicalForm();
        String lang = literal.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            txt = txt + "@" + lang;
        }
        OntDT dt = literal.getModel().getResource(literal.getDatatypeURI()).as(OntDT.class);
        ONTObject<OWLDatatype> owl;
        if (dt.isBuiltIn()) {
            owl = ONTObject.create(getOWLDataFactory().getOWLDatatype(toIRI(dt)));
        } else {
            owl = get(dt);
        }
        OWLLiteral res = getOWLDataFactory().getOWLLiteral(txt, owl.getObject());
        return ONTObject.create(res).append(owl);
    }

    @Override
    public ONTObject<? extends SWRLAtom> get(OntSWRL.Atom atom) {
        return ReadHelper.calcSWRLAtom(atom, this);
    }

    @Override
    public ONTObject<IRI> asIRI(OntObject object) {
        return ONTObject.create(toIRI(object), object.canAs(OntEntity.class) ? object.as(OntEntity.class) : object);
    }

    @Override
    public Collection<ONTObject<OWLAnnotation>> get(OntStatement statement) {
        return ReadHelper.getAnnotations(statement, this);
    }

    public SimpleMap<OntCE, ONTObject<? extends OWLClassExpression>> classExpressionStore() {
        return new NoOpMap<>();
    }

    public SimpleMap<OntDR, ONTObject<? extends OWLDataRange>> dataRangeStore() {
        return new NoOpMap<>();
    }

    @Override
    public OWLDataFactory getOWLDataFactory() {
        return config.dataFactory();
    }

    /**
     * A truncated "Map" with only three operations.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    public interface SimpleMap<K, V> {
        /**
         * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
         *
         * @param key the key whose associated value is to be returned, not null.
         * @return the value or null.
         */
        V get(K key);

        /**
         * Associates the specified value with the specified key in this map.
         *
         * @param key   key with which the specified value is to be associated, not null.
         * @param value value to be associated with the specified key, not null.
         */
        void put(K key, V value);

        /**
         * If the specified key is not already associated with a value, attempts to compute its value using the given mapping
         * function and enters it into this map unless {@code null}.
         *
         * @param key key with which the specified value is to be associated
         * @param map the function to compute a value
         * @return the current (existing or computed) value associated with the specified key.
         */
        default V get(K key, Function<? super K, ? extends V> map) {
            V v = get(key);
            if (v != null) return v;
            v = Objects.requireNonNull(Objects.requireNonNull(map, "Null mapping function.").apply(key),
                    "Null map result, key: " + key);
            put(key, v);
            return v;
        }

        static <K, V> SimpleMap<K, V> fromMap(Map<K, V> map) {
            return new SimpleMap<K, V>() {
                @Override
                public V get(K key) {
                    return map.get(key);
                }

                @Override
                public void put(K key, V value) {
                    map.put(key, value);
                }
            };
        }
    }

    /**
     * A fake implementation of {@link SimpleMap}.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    public static class NoOpMap<K, V> implements SimpleMap<K, V> {
        @Override
        public V get(K key) {
            return null;
        }

        @Override
        public void put(K key, V value) {
            // nothing
        }
    }
}
