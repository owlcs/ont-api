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
import com.github.owlcs.ontapi.OntApiException;
import com.github.sszuev.jena.ontapi.model.OntAnnotationProperty;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntDataProperty;
import com.github.sszuev.jena.ontapi.model.OntDataRange;
import com.github.sszuev.jena.ontapi.model.OntFacetRestriction;
import com.github.sszuev.jena.ontapi.model.OntIndividual;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.model.OntObjectProperty;
import com.github.sszuev.jena.ontapi.model.OntSWRL;
import com.github.sszuev.jena.ontapi.model.OntStatement;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
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
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.HashSet;
import java.util.Objects;

/**
 * An Object Factory impl which maps {@link OntObject} to {@link OWLObject} directly having no cache.
 * <p>
 * Created by @ssz on 15.03.2018.
 *
 * @see ONTWrapperImpl
 */
@SuppressWarnings("WeakerAccess")
public class SimpleObjectFactory implements ONTObjectFactory {
    protected final DataFactory factory;

    public SimpleObjectFactory(DataFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    protected IRI toIRI(Resource r) {
        return toIRI(r.getURI());
    }

    @Override
    public ONTObject<? extends OWLClassExpression> getClass(OntClass ce) {
        return ReadHelper.calcClassExpression(ce, this, new HashSet<>());
    }

    @Override
    public ONTObject<? extends OWLDataRange> getDatatype(OntDataRange dr) {
        return ReadHelper.calcDataRange(dr, this, new HashSet<>());
    }

    @Override
    public ONTObject<OWLFacetRestriction> getFacetRestriction(OntFacetRestriction fr) {
        return ReadHelper.getFacetRestriction(fr, this);
    }

    @Override
    public ONTObject<OWLClass> getClass(OntClass.Named ce) {
        IRI iri = toIRI(OntApiException.notNull(ce, "Null class."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLClass(iri), ce);
    }

    @Override
    public ONTObject<OWLDatatype> getDatatype(OntDataRange.Named dr) {
        IRI iri = toIRI(OntApiException.notNull(dr, "Null datatype."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLDatatype(iri), dr);
    }

    @Override
    public ONTObject<OWLObjectProperty> getProperty(OntObjectProperty.Named nop) {
        IRI iri = toIRI(OntApiException.notNull(nop, "Null object property."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLObjectProperty(iri), nop);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> getProperty(OntAnnotationProperty nap) {
        IRI iri = toIRI(OntApiException.notNull(nap, "Null annotation property."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLAnnotationProperty(iri), nap);
    }

    @Override
    public ONTObject<OWLDataProperty> getProperty(OntDataProperty ndp) {
        IRI iri = toIRI(OntApiException.notNull(ndp, "Null data property."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLDataProperty(iri), ndp);
    }

    @Override
    public ONTObject<OWLNamedIndividual> getIndividual(OntIndividual.Named i) {
        IRI iri = toIRI(OntApiException.notNull(i, "Null individual."));
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLNamedIndividual(iri), i);
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> getIndividual(OntIndividual.Anonymous i) {
        return ONTWrapperImpl.create(getOWLDataFactory().getOWLAnonymousIndividual(i.asNode().getBlankNodeId()), i);
    }

    @Override
    public ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntObjectProperty.Inverse iop) {
        OWLObjectProperty op = getOWLDataFactory().getOWLObjectProperty(toIRI(iop.getDirect()));
        return ONTWrapperImpl.create(op.getInverseProperty(), iop);
    }

    @Override
    public ONTObject<OWLLiteral> getLiteral(Literal literal) {
        DataFactory df = getOWLDataFactory();
        OWLLiteral owl = df.getOWLLiteral(literal.asNode().getLiteral());
        ONTWrapperImpl<OWLLiteral> res = ONTWrapperImpl.create(owl);
        OntModel m = (OntModel) literal.getModel();
        OntDataRange.Named jdt = m.getDatatype(literal);
        if (!jdt.isBuiltIn()) {
            return res.append(getDatatype(jdt));
        }
        return res;
    }

    @Override
    public ONTObject<OWLAnnotation> getAnnotation(OntStatement s) {
        return ReadHelper.getAnnotation(s, this);
    }

    @Override
    public ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable var) {
        return ReadHelper.getSWRLVariable(var, this);
    }

    @Override
    public ONTObject<? extends SWRLAtom> getSWRLAtom(OntSWRL.Atom<?> atom) {
        return ReadHelper.calcSWRLAtom(atom, this);
    }

    @Override
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(OntSWRL.IArg arg) {
        return ReadHelper.getSWRLIndividualArg(arg, this);
    }

    @Override
    public ONTObject<? extends SWRLDArgument> getSWRLArgument(OntSWRL.DArg arg) {
        return ReadHelper.getSWRLLiteralArg(arg, this);
    }

    @Override
    public ONTObject<IRI> getIRI(String uri) {
        return ONTWrapperImpl.create(toIRI(uri));
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return factory;
    }

}
