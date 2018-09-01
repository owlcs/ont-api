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
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.Collection;
import java.util.Objects;

/**
 * Internal Data Factory to map {@link OntObject} =&gt; {@link OWLObject}.
 * Used by {@link InternalModel} while read objects from the graph.
 * <p>
 * Created by @szuev on 14.03.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternals.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals</a>
 * @see ONTObject
 */
public interface InternalDataFactory {

    InternalDataFactory DEFAULT = new NoCacheDataFactory(OntManagers.getDataFactory());

    void clear();

    ONTObject<? extends OWLClassExpression> get(OntCE ce);

    ONTObject<? extends OWLDataRange> get(OntDR dr);

    ONTObject<OWLAnnotationProperty> get(OntNAP nap);

    ONTObject<OWLDataProperty> get(OntNDP ndp);

    ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE ope);

    ONTObject<? extends OWLIndividual> get(OntIndividual i);

    ONTObject<OWLLiteral> get(Literal literal);

    ONTObject<? extends SWRLAtom> get(OntSWRL.Atom atom);

    Collection<ONTObject<OWLAnnotation>> get(OntStatement statement, InternalConfig config);

    ONTObject<IRI> asIRI(OntObject s);

    DataFactory getOWLDataFactory();

    default IRI toIRI(String str) {
        return IRI.create(Objects.requireNonNull(str, "Null IRI."));
    }

    default ONTObject<? extends OWLAnnotationValue> get(RDFNode value) {
        if (OntApiException.notNull(value, "Null node").isLiteral()) {
            return get(value.asLiteral());
        }
        if (value.isURIResource()) {
            return asIRI(value.as(OntObject.class));
        }
        if (value.isAnon()) {
            return get(Models.asAnonymousIndividual(value));
        }
        throw new OntApiException("Not an AnnotationValue " + value);
    }

    default ONTObject<? extends OWLAnnotationSubject> get(OntObject subject) {
        if (OntApiException.notNull(subject, "Null resource").isURIResource()) {
            return asIRI(subject);
        }
        if (subject.isAnon()) {
            return get(Models.asAnonymousIndividual(subject));
        }
        throw new OntApiException("Not an AnnotationSubject " + subject);
    }

    @SuppressWarnings("unchecked")
    default ONTObject<OWLClass> get(OntClass cl) {
        return (ONTObject<OWLClass>) get((OntCE) cl);
    }

    @SuppressWarnings("unchecked")
    default ONTObject<OWLDatatype> get(OntDT dt) {
        return (ONTObject<OWLDatatype>) get((OntDR) dt);
    }

    @SuppressWarnings("unchecked")
    default ONTObject<OWLObjectProperty> get(OntNOP nop) {
        return (ONTObject<OWLObjectProperty>) get((OntOPE) nop);
    }

    @SuppressWarnings("unchecked")
    default ONTObject<OWLNamedIndividual> get(OntIndividual.Named individual) {
        return (ONTObject<OWLNamedIndividual>) get((OntIndividual) individual);
    }

    @SuppressWarnings("unchecked")
    default ONTObject<OWLAnonymousIndividual> get(OntIndividual.Anonymous individual) {
        return (ONTObject<OWLAnonymousIndividual>) get((OntIndividual) individual);
    }

    default ONTObject<? extends OWLEntity> get(OntEntity entity) {
        Class<? extends OntObject> type = OntApiException.notNull(((OntObjectImpl) entity).getActualClass(),
                "Can't determine view of entity " + entity);
        if (OntClass.class.equals(type)) {
            return get((OntClass) entity);
        } else if (OntDT.class.equals(type)) {
            return get((OntDT) entity);
        } else if (OntIndividual.Named.class.equals(type)) {
            return get((OntIndividual.Named) entity);
        } else if (OntNAP.class.equals(type)) {
            return get((OntNAP) entity);
        } else if (OntNDP.class.equals(type)) {
            return get((OntNDP) entity);
        } else if (OntNOP.class.equals(type)) {
            return get((OntNOP) entity);
        }
        throw new OntApiException("Unsupported " + entity);
    }

    default ONTObject<? extends OWLPropertyExpression> get(OntPE property) {
        if (OntApiException.notNull(property, "Null property.").canAs(OntNAP.class)) {
            return get(property.as(OntNAP.class));
        }
        // process Object Properties first to match OWL-API-impl behaviour
        if (property.canAs(OntOPE.class)) {
            return get(property.as(OntOPE.class));
        }
        if (property.canAs(OntNDP.class)) {
            return get(property.as(OntNDP.class));
        }
        throw new OntApiException("Unsupported property " + property);
    }

}
