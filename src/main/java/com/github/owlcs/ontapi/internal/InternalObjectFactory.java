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

import com.github.owlcs.ontapi.BlankNodeId;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationPropertyImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAnonymousClassExpressionImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAnonymousDataRangeImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAnonymousIndividualImpl;
import com.github.owlcs.ontapi.internal.objects.ONTClassImpl;
import com.github.owlcs.ontapi.internal.objects.ONTDataPropertyImpl;
import com.github.owlcs.ontapi.internal.objects.ONTDatatypeImpl;
import com.github.owlcs.ontapi.internal.objects.ONTFacetRestrictionImpl;
import com.github.owlcs.ontapi.internal.objects.ONTIRI;
import com.github.owlcs.ontapi.internal.objects.ONTLiteralImpl;
import com.github.owlcs.ontapi.internal.objects.ONTNamedIndividualImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectInverseOfImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectPropertyImpl;
import com.github.owlcs.ontapi.internal.objects.ONTSWRLAtomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTSWRLIndividualImpl;
import com.github.owlcs.ontapi.internal.objects.ONTSWRLLiteralImpl;
import com.github.owlcs.ontapi.internal.objects.ONTSWRLVariable;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDataRange;
import org.apache.jena.ontapi.model.OntFacetRestriction;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObject;
import org.apache.jena.ontapi.model.OntObjectProperty;
import org.apache.jena.ontapi.model.OntSWRL;
import org.apache.jena.ontapi.model.OntStatement;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An extended Object Factory impl which maps {@link OntObject OntObject}s
 * to {@link OWLObject OWLObject}s directly having no cache.
 * Unlike {@link SimpleObjectFactory}, every object created by this factory is
 * an {@link ONTObject} with a reference to the concrete model obtained from this factory.
 * <p>
 * Created by @ssz on 07.08.2019.
 *
 * @since 2.0.0
 */
public class InternalObjectFactory implements ModelObjectFactory {
    protected final Supplier<OntModel> model;
    protected final DataFactory factory;

    public InternalObjectFactory(DataFactory factory, Supplier<OntModel> model) {
        this.factory = Objects.requireNonNull(factory);
        this.model = Objects.requireNonNull(model);
    }

    /**
     * Using the {@code factory} finds or creates an {@link OWLClass} instance.
     *
     * @param uri     {@code String}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @param model   {@link OntModel}
     * @return an {@link ONTObject} which is {@link OWLClass}
     */
    public static ONTObject<OWLClass> getONTClass(String uri, OntModel model, ONTObjectFactory factory) {
        if (factory instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) factory).getClass(uri);
        }
        return factory.getClass(OntApiException.mustNotBeNull(model.getOntClass(uri)));
    }

    @Override
    public Supplier<OntModel> model() {
        return model;
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return factory;
    }

    @Override
    public ONTObject<IRI> getIRI(String uri) {
        return ONTIRI.create(uri);
    }

    @Override
    public IRI toIRI(String uri) {
        return ONTIRI.create(uri);
    }

    @Override
    public ONTObject<OWLAnnotation> getAnnotation(OntStatement s) {
        return ONTAnnotationImpl.create(s, this, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLClassExpression> getClass(OntClass ce) {
        if (ce.isURIResource())
            return getClass((OntClass.Named) ce);
        return (ONTObject<? extends OWLClassExpression>) ONTAnonymousClassExpressionImpl.create(ce, this, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLDataRange> getDatatype(OntDataRange dr) {
        if (dr.isURIResource())
            return getDatatype((OntDataRange.Named) dr);
        return (ONTObject<? extends OWLDataRange>) ONTAnonymousDataRangeImpl.create(dr, this, model);
    }

    @Override
    public ONTObject<OWLObjectInverseOf> getProperty(OntObjectProperty.Inverse iop) {
        return ONTObjectInverseOfImpl.create(iop, this, model);
    }

    @Override
    public ONTObject<OWLFacetRestriction> getFacetRestriction(OntFacetRestriction fr) {
        return ONTFacetRestrictionImpl.create(fr, this, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends SWRLAtom> getSWRLAtom(OntSWRL.Atom<?> atom) {
        return (ONTObject<? extends SWRLAtom>) ONTSWRLAtomImpl.create(atom, this, model);
    }

    @Override
    public ONTObject<OWLClass> getClass(String uri) {
        return new ONTClassImpl(uri, model);
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> getAnonymousIndividual(BlankNodeId id) {
        return new ONTAnonymousIndividualImpl(id.label(), model);
    }

    @Override
    public ONTObject<OWLNamedIndividual> getNamedIndividual(String uri) {
        return new ONTNamedIndividualImpl(uri, model);
    }

    @Override
    public ONTObject<OWLDatatype> getDatatype(String uri) {
        return new ONTDatatypeImpl(uri, model);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> getAnnotationProperty(String uri) {
        return new ONTAnnotationPropertyImpl(uri, model);
    }

    @Override
    public ONTObject<OWLObjectProperty> getObjectProperty(String uri) {
        return new ONTObjectPropertyImpl(uri, model);
    }

    @Override
    public ONTObject<OWLDataProperty> getDataProperty(String uri) {
        return new ONTDataPropertyImpl(uri, model);
    }

    @Override
    public ONTObject<OWLLiteral> getLiteral(LiteralLabel label) {
        return new ONTLiteralImpl(label, model);
    }

    @Override
    public ONTObject<SWRLVariable> getSWRLVariable(String uri) {
        return new ONTSWRLVariable(uri, model);
    }

    /**
     * Creates a {@link SWRLDArgument} wrapped as {@link ONTObject} for the given {@code label}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLDArgument}
     */
    @Override
    public ONTObject<? extends SWRLDArgument> getSWRLArgument(LiteralLabel label) {
        return new ONTSWRLLiteralImpl(label, model);
    }

    /**
     * Creates a {@link SWRLIArgument} wrapped as {@link ONTObject} with the given {@code uri}.
     *
     * @param uri {@code String}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLIArgument}
     */
    @Override
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(String uri) {
        return new ONTSWRLIndividualImpl(uri, model);
    }

    /**
     * Creates a {@link SWRLIArgument} wrapped as {@link ONTObject} for the given blank node {@code id}.
     *
     * @param id {@link BlankNodeId}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLDArgument}
     */
    @Override
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(BlankNodeId id) {
        return new ONTSWRLIndividualImpl(id, model);
    }
}
