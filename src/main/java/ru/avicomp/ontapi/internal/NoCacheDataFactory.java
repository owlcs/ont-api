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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * An Internal Data Factory impl without cache.
 * <p>
 * Created by @szuev on 15.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class NoCacheDataFactory implements InternalDataFactory {
    protected final DataFactory factory;

    public NoCacheDataFactory(DataFactory factory) {
        this.factory = Objects.requireNonNull(factory);
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
    public ONTObject<OWLClass> get(OntClass ce) {
        IRI iri = toIRI(OntApiException.notNull(ce, "Null class."));
        return ONTObject.create(getOWLDataFactory().getOWLClass(iri), ce);
    }

    @Override
    public ONTObject<OWLDatatype> get(OntDT dr) {
        IRI iri = toIRI(OntApiException.notNull(dr, "Null datatype."));
        return ONTObject.create(getOWLDataFactory().getOWLDatatype(iri), dr);
    }

    @Override
    public ONTObject<OWLObjectProperty> get(OntNOP nop) {
        IRI iri = toIRI(OntApiException.notNull(nop, "Null object property."));
        return ONTObject.create(getOWLDataFactory().getOWLObjectProperty(iri), nop);
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
    public ONTObject<OWLNamedIndividual> get(OntIndividual.Named i) {
        IRI iri = toIRI(OntApiException.notNull(i, "Null individual."));
        return ONTObject.create(getOWLDataFactory().getOWLNamedIndividual(iri), i);
    }

    @Override
    public ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE ope) {
        OntApiException.notNull(ope, "Null object property.");
        if (ope.isAnon()) {
            OWLObjectProperty op = getOWLDataFactory().getOWLObjectProperty(toIRI(ope.as(OntOPE.Inverse.class).getDirect()));
            return ONTObject.create(op.getInverseProperty(), ope);
        }
        return get(ope.as(OntNOP.class));
    }

    @Override
    public ONTObject<? extends OWLPropertyExpression> get(OntDOP property) {
        // process Object Properties first to match OWL-API-impl behaviour
        if (property.canAs(OntOPE.class)) {
            return get(property.as(OntOPE.class));
        }
        if (property.canAs(OntNDP.class)) {
            return get(property.as(OntNDP.class));
        }
        throw new OntApiException("Unsupported property " + property);
    }

    @Override
    public ONTObject<? extends OWLIndividual> get(OntIndividual individual) {
        DataFactory df = getOWLDataFactory();
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return get(individual.as(OntIndividual.Named.class));
        }
        return ONTObject.create(df.getOWLAnonymousIndividual(individual.asNode().getBlankNodeId()), individual);
    }

    @Override
    public ONTObject<OWLLiteral> get(Literal literal) {
        DataFactory df = getOWLDataFactory();
        OWLLiteral owl = df.getOWLLiteral(literal.asNode().getLiteral());
        ONTObject<OWLLiteral> res = ONTObject.create(owl);
        OntDT dt = literal.getModel().getResource(literal.getDatatypeURI()).as(OntDT.class);
        if (!dt.isBuiltIn()) {
            if (owl instanceof OWLLiteralImpl) {
                ((OWLLiteralImpl) owl).putOWLDatatype(get(dt).getObject());
            }
            return res.append(get(dt));
        }
        return res;
    }

    @Override
    public ONTObject<? extends OWLAnnotationValue> get(RDFNode value) {
        if (OntApiException.notNull(value, "Null node").isLiteral()) {
            return get(value.asLiteral());
        }
        if (value.isURIResource()) {
            return asIRI(value.as(OntObject.class));
        }
        if (value.isAnon()) {
            return getAnonymous(Models.asAnonymousIndividual(value));
        }
        throw new OntApiException("Not an AnnotationValue " + value);
    }

    @Override
    public ONTObject<? extends OWLAnnotationSubject> get(OntObject subject) {
        if (OntApiException.notNull(subject, "Null resource").isURIResource()) {
            return asIRI(subject);
        }
        if (subject.isAnon()) {
            return getAnonymous(Models.asAnonymousIndividual(subject));
        }
        throw new OntApiException("Not an AnnotationSubject " + subject);
    }

    @SuppressWarnings("unchecked")
    public ONTObject<OWLAnonymousIndividual> getAnonymous(OntIndividual.Anonymous individual) {
        return (ONTObject<OWLAnonymousIndividual>) get(individual);
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
    public Collection<ONTObject<OWLAnnotation>> get(OntStatement statement, InternalConfig config) {
        return ReadHelper.getAnnotations(statement, config, this);
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return factory;
    }

}
