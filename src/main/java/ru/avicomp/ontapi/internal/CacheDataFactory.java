/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The internal cache holder which is using while reading owl-objects.
 * Currently it is based on caffeine cache since it is used widely by OWL-API.
 *
 * Created by @ssz on 09.09.2018.
 */
@SuppressWarnings("WeakerAccess")
public class CacheDataFactory extends NoCacheDataFactory {
    /**
     * This magic '2048' is taken from OWL-API DataFactory impl:
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternalsImpl.java#L63'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl#builder(CacheLoader)</a>
     */
    public static final int CACHE_SIZE = 2048;
    protected final InternalCache<OntClass, ONTObject<OWLClass>> classes;
    protected final InternalCache<OntDT, ONTObject<OWLDatatype>> datatypes;
    protected final InternalCache<OntNAP, ONTObject<OWLAnnotationProperty>> annotationProperties;
    protected final InternalCache<OntNDP, ONTObject<OWLDataProperty>> datatypeProperties;
    protected final InternalCache<OntNOP, ONTObject<OWLObjectProperty>> objectProperties;
    protected final InternalCache<OntIndividual.Named, ONTObject<OWLNamedIndividual>> individuals;
    protected final InternalCache.Loading<String, IRI> iris;

    public CacheDataFactory(DataFactory factory) {
        this(factory, InternalCache.createBounded(true, CACHE_SIZE).asLoading(IRI::create), CACHE_SIZE);
    }

    /**
     * Makes an instance based on 7 {@link InternalCache.Loading Loading Cache}s, for all OWL entities and IRIs.
     *
     * @param factory {@link DataFactory}
     * @param iris    {@link InternalCache.Loading} for {@link IRI}s
     * @param size    int, caches size, negative for unlimited
     */
    public CacheDataFactory(DataFactory factory, InternalCache.Loading<String, IRI> iris, int size) {
        this(factory, () -> InternalCache.createBounded(true, size), iris);
    }

    /**
     * The primary constructor.
     *
     * @param dataFactory  {@link DataFactory}
     * @param cacheFactory {@link Supplier} that produces {@link InternalCache}
     * @param iris         {@link InternalCache.Loading} for {@link IRI}s
     */
    @SuppressWarnings("unchecked")
    protected CacheDataFactory(DataFactory dataFactory,
                               Supplier<InternalCache<?, ?>> cacheFactory,
                               InternalCache.Loading<String, IRI> iris) {
        super(dataFactory);
        this.iris = Objects.requireNonNull(iris);
        this.classes = (InternalCache<OntClass, ONTObject<OWLClass>>) cacheFactory.get();
        this.datatypes = (InternalCache<OntDT, ONTObject<OWLDatatype>>) cacheFactory.get();
        this.annotationProperties = (InternalCache<OntNAP, ONTObject<OWLAnnotationProperty>>) cacheFactory.get();
        this.datatypeProperties = (InternalCache<OntNDP, ONTObject<OWLDataProperty>>) cacheFactory.get();
        this.objectProperties = (InternalCache<OntNOP, ONTObject<OWLObjectProperty>>) cacheFactory.get();
        this.individuals = (InternalCache<OntIndividual.Named, ONTObject<OWLNamedIndividual>>) cacheFactory.get();
    }

    @Override
    public void clear() {
        classes.clear();
        datatypes.clear();
        annotationProperties.clear();
        datatypeProperties.clear();
        objectProperties.clear();
        individuals.clear();
    }

    @Override
    public ONTObject<OWLClass> get(OntClass ce) {
        return classes.get(ce, super::get);
    }

    @Override
    public ONTObject<OWLDatatype> get(OntDT dr) {
        return datatypes.get(dr, super::get);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> get(OntNAP nap) {
        return annotationProperties.get(nap, super::get);
    }

    @Override
    public ONTObject<OWLDataProperty> get(OntNDP ndp) {
        return datatypeProperties.get(ndp, super::get);
    }

    @Override
    public ONTObject<OWLObjectProperty> get(OntNOP nop) {
        return objectProperties.get(nop, super::get);
    }

    @Override
    public ONTObject<OWLNamedIndividual> get(OntIndividual.Named i) {
        return individuals.get(i, super::get);
    }

    @Override
    public IRI toIRI(String str) {
        return iris.get(str);
    }

}
