/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.internal.objects.ONTIRI;
import org.apache.jena.ontapi.model.OntModel;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The internal cache holder which is using while reading owl-objects.
 * Currently, it is based on caffeine cache since it is used widely by OWL-API.
 * <p>
 * Created by @ssz on 09.09.2018.
 */
@SuppressWarnings("WeakerAccess")
public class CacheObjectFactory extends InternalObjectFactory {
    /**
     * This magic '2048' is taken from OWL-API DataFactory impl:
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternalsImpl.java#L63'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl#builder(CacheLoader)</a>
     * @see com.github.owlcs.ontapi.config.OntConfig#getManagerIRIsCacheSize()
     */
    public static final int CACHE_SIZE = 2048;

    protected final InternalCache.Loading<String, ONTObject<OWLClass>> classes;
    protected final InternalCache.Loading<String, ONTObject<OWLDatatype>> datatypes;
    protected final InternalCache.Loading<String, ONTObject<OWLAnnotationProperty>> annotationProperties;
    protected final InternalCache.Loading<String, ONTObject<OWLDataProperty>> datatypeProperties;
    protected final InternalCache.Loading<String, ONTObject<OWLObjectProperty>> objectProperties;
    protected final InternalCache.Loading<String, ONTObject<OWLNamedIndividual>> individuals;
    protected final InternalCache.Loading<String, IRI> iris;
    protected final Set<InternalCache<?, ?>> caches;

    /**
     * Creates a default instance.
     * For testing and debugging.
     *
     * @param factory {@link DataFactory}, not {@code null}
     * @param model   {@link OntModel}, not {@code null}
     */
    @SuppressWarnings("unused")
    public CacheObjectFactory(DataFactory factory, OntModel model) {
        this(factory, model, CACHE_SIZE);
    }

    /**
     * Provides an instance with {@code 7} inner {@link InternalCache Loading Cache}s, for all OWL entities and IRIs.
     * Each of them will be bounded with {@code size} limit
     *
     * @param factory {@link DataFactory}, not {@code null}
     * @param model   {@link OntModel}, not {@code null}
     * @param size    int, caches size, a negative for unlimited
     */
    public CacheObjectFactory(DataFactory factory, OntModel model, int size) {
        this(factory, () -> model, Collections.emptyMap(), () -> InternalCache.createBounded(true, size));
    }

    /**
     * The primary constructor.
     * Provides an instance, that contain both shared (outer) and fresh (inner) caches.
     *
     * @param dataFactory  {@link DataFactory}, not {@code null}
     * @param model        a facility (as {@code Supplier}) to provide nonnull {@link OntModel} instance, not {@code null}
     * @param external     a {@code Map} containing existing outer caches, not {@code null}
     * @param cacheFactory a facility ({@code Supplier}) to produce new cache instances, not {@code null}
     */
    protected CacheObjectFactory(DataFactory dataFactory,
                                 Supplier<OntModel> model,
                                 Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> external,
                                 Supplier<InternalCache<?, ?>> cacheFactory) {
        super(dataFactory, model);
        this.caches = new HashSet<>();
        this.iris = fetchCache(external, caches, cacheFactory, IRI.class).asLoading(super::toIRI);
        this.classes = fetchCache(external, caches, cacheFactory, OWLClass.class).asLoading(super::getClass);
        this.datatypes = fetchCache(external, caches, cacheFactory, OWLDatatype.class).asLoading(super::getDatatype);
        this.annotationProperties = fetchCache(external, caches, cacheFactory, OWLAnnotationProperty.class)
                .asLoading(super::getAnnotationProperty);
        this.datatypeProperties = fetchCache(external, caches, cacheFactory, OWLDataProperty.class)
                .asLoading(super::getDataProperty);
        this.objectProperties = fetchCache(external, caches, cacheFactory, OWLObjectProperty.class)
                .asLoading(super::getObjectProperty);
        this.individuals = fetchCache(external, caches, cacheFactory, OWLNamedIndividual.class)
                .asLoading(super::getNamedIndividual);
    }

    @SuppressWarnings("unchecked")
    private static <R> InternalCache<String, R> fetchCache(Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> system,
                                                           Set<InternalCache<?, ?>> caches,
                                                           Supplier<InternalCache<?, ?>> factory,
                                                           Class<? extends OWLPrimitive> key) {
        InternalCache<?, ?> res = system.get(key);
        if (res == null) {
            res = factory.get();
            caches.add(res);
        }
        return (InternalCache<String, R>) res;
    }

    /**
     * Clears all caches.
     */
    public void clear() {
        caches.forEach(InternalCache::clear);
    }

    @Override
    public ONTObject<OWLClass> getClass(String uri) {
        return classes.get(uri);
    }

    @Override
    public ONTObject<OWLDatatype> getDatatype(String uri) {
        return datatypes.get(uri);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> getAnnotationProperty(String uri) {
        return annotationProperties.get(uri);
    }

    @Override
    public ONTObject<OWLDataProperty> getDataProperty(String uri) {
        return datatypeProperties.get(uri);
    }

    @Override
    public ONTObject<OWLObjectProperty> getObjectProperty(String uri) {
        return objectProperties.get(uri);
    }

    @Override
    public ONTObject<OWLNamedIndividual> getNamedIndividual(String uri) {
        return individuals.get(uri);
    }

    @Override
    public ONTObject<IRI> getIRI(String uri) {
        return ONTIRI.asONT(toIRI(uri));
    }

    @Override
    public IRI toIRI(String str) {
        return iris.get(str);
    }

}
