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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;

import java.util.HashSet;
import java.util.Objects;

/**
 * An Internal Object Factory impl which maps {@link OntObject} to {@link OWLObject} directly having no cache.
 * <p>
 * Created by @szuev on 15.03.2018.
 */
public class NoCacheObjectFactory implements InternalObjectFactory {
    protected final DataFactory factory;

    public NoCacheObjectFactory(DataFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    protected IRI toIRI(Resource r) {
        return toIRI(r.getURI());
    }

    @Override
    public ONTObject<? extends OWLClassExpression> getClass(OntCE ce) {
        return ReadHelper.calcClassExpression(ce, this, new HashSet<>());
    }

    @Override
    public ONTObject<? extends OWLDataRange> getDatatype(OntDR dr) {
        return ReadHelper.calcDataRange(dr, this, new HashSet<>());
    }

    @Override
    public ONTObject<OWLFacetRestriction> getFacetRestriction(OntFR fr) {
        return ReadHelper.getFacetRestriction(fr, this);
    }

    @Override
    public ONTObject<OWLClass> getClass(OntClass ce) {
        IRI iri = toIRI(OntApiException.notNull(ce, "Null class."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLClass(iri), ce);
    }

    @Override
    public ONTObject<OWLDatatype> getDatatype(OntDT dr) {
        IRI iri = toIRI(OntApiException.notNull(dr, "Null datatype."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLDatatype(iri), dr);
    }

    @Override
    public ONTObject<OWLObjectProperty> getProperty(OntNOP nop) {
        IRI iri = toIRI(OntApiException.notNull(nop, "Null object property."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLObjectProperty(iri), nop);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> getProperty(OntNAP nap) {
        IRI iri = toIRI(OntApiException.notNull(nap, "Null annotation property."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLAnnotationProperty(iri), nap);
    }

    @Override
    public ONTObject<OWLDataProperty> getProperty(OntNDP ndp) {
        IRI iri = toIRI(OntApiException.notNull(ndp, "Null data property."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLDataProperty(iri), ndp);
    }

    @Override
    public ONTObject<OWLNamedIndividual> getIndividual(OntIndividual.Named i) {
        IRI iri = toIRI(OntApiException.notNull(i, "Null individual."));
        return ONTObjectImpl.create(getOWLDataFactory().getOWLNamedIndividual(iri), i);
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> getIndividual(OntIndividual.Anonymous i) {
        return ONTObjectImpl.create(getOWLDataFactory().getOWLAnonymousIndividual(i.asNode().getBlankNodeId()), i);
    }

    @Override
    public ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntOPE.Inverse iop) {
        OWLObjectProperty op = getOWLDataFactory().getOWLObjectProperty(toIRI(iop.getDirect()));
        return ONTObjectImpl.create(op.getInverseProperty(), iop);
    }

    @Override
    public ONTObject<OWLLiteral> getLiteral(Literal literal) {
        DataFactory df = getOWLDataFactory();
        OWLLiteral owl = df.getOWLLiteral(literal.asNode().getLiteral());
        ONTObjectImpl<OWLLiteral> res = ONTObjectImpl.create(owl);
        OntGraphModel m = (OntGraphModel) literal.getModel();
        OntDT jdt = m.getDatatype(literal);
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
    public ONTObject<? extends SWRLAtom> getSWRLAtom(OntSWRL.Atom atom) {
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
        return ONTObjectImpl.create(toIRI(uri));
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return factory;
    }

}
