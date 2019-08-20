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
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Collection;

/**
 * Internal Object Factory to map {@link OntObject} =&gt; {@link OWLObject}.
 * Used by the {@link InternalModel} while read objects from the graph.
 * It is a functional analogue of {@code uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals}.
 * <p>
 * Created by @szuev on 14.03.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternals.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals</a>
 * @see ONTObject
 */
public interface InternalObjectFactory {

    InternalObjectFactory DEFAULT = new NoCacheObjectFactory(OntManagers.getDataFactory());

    void clear();

    DataFactory getOWLDataFactory();

    ONTObject<OWLClass> get(OntClass ce);

    ONTObject<OWLDatatype> get(OntDT dr);

    ONTObject<OWLObjectProperty> get(OntNOP nop);

    ONTObject<OWLAnnotationProperty> get(OntNAP nap);

    ONTObject<OWLDataProperty> get(OntNDP ndp);

    ONTObject<OWLNamedIndividual> get(OntIndividual.Named i);

    ONTObject<OWLAnonymousIndividual> get(OntIndividual.Anonymous i);

    ONTObject<OWLFacetRestriction> get(OntFR fr);

    ONTObject<SWRLVariable> get(OntSWRL.Variable var);

    ONTObject<OWLLiteral> get(Literal literal);

    ONTObject<OWLAnnotation> get(OntStatement s);

    ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE.Inverse iop);

    ONTObject<? extends OWLClassExpression> get(OntCE ce);

    ONTObject<? extends OWLDataRange> get(OntDR dr);

    ONTObject<? extends SWRLAtom> get(OntSWRL.Atom atom);

    /**
     * Gets an IRI as {@code ONTObject}.
     *
     * @param resource {@link OntObject}, must be URI, not {@code null}
     * @return {@link ONTObject} that wraps {@link IRI}
     */
    ONTObject<IRI> getIRI(OntObject resource);

    /**
     * Fetches an {@link IRI} from String.
     *
     * @param str URI, not {@code null}
     * @return {@link IRI}
     */
    default IRI toIRI(String str) {
        return IRI.create(OntApiException.notNull(str, "Null IRI."));
    }

    /**
     * Gets a {@code Collection} of axiom's {@link OWLAnnotation}s which are wrapped as {@link ONTObject}-containers.
     *
     * @param axiom  {@link OntStatement} - the root statement of an axiom, not {@code null}
     * @param config {@link InternalConfig} the configuration, to
     * @return a {@code Collection} of {@link OWLAnnotation}s as {@link ONTObject}s
     */
    default Collection<ONTObject<OWLAnnotation>> get(OntStatement axiom, InternalConfig config) {
        return ReadHelper.getAnnotations(axiom, config, this);
    }

    /**
     * Gets an {@link OWLAnnotationSubject} from the the {@code OntObject}-resource.
     *
     * @param subject {@link OntObject}, either URI-resource or anonymous individual, not {@code null}
     * @return {@link ONTObject} of {@link OWLAnnotationSubject}
     */
    default ONTObject<? extends OWLAnnotationSubject> getSubject(OntObject subject) {
        if (OntApiException.notNull(subject, "Null resource").isURIResource()) {
            return getIRI(subject);
        }
        if (subject.isAnon()) {
            return get(OntModels.asAnonymousIndividual(subject));
        }
        throw new OntApiException.IllegalArgument("Not an AnnotationSubject " + subject);
    }

    /**
     * Gets an {@link OWLAnnotationValue} for the the {@code RDFNode}
     *
     * @param value {@link OntObject}, either URI-resource, anonymous individual or literal, not {@code null}
     * @return {@link ONTObject} of {@link OWLAnnotationValue}
     */
    default ONTObject<? extends OWLAnnotationValue> getValue(RDFNode value) {
        if (OntApiException.notNull(value, "Null node").isLiteral()) {
            return get(value.asLiteral());
        }
        if (value.isURIResource()) {
            return getIRI(value.as(OntObject.class));
        }
        if (value.isAnon()) {
            return get(OntModels.asAnonymousIndividual(value));
        }
        throw new OntApiException.IllegalArgument("Not an AnnotationValue " + value);
    }

    /**
     * Gets an {@link OWLIndividual} wrapped in {@link ONTObject} for the given {@link OntIndividual}.
     *
     * @param individual {@link OntIndividual}, not {@code null}
     * @return {@link ONTObject} of {@link OWLIndividual}
     */
    default ONTObject<? extends OWLIndividual> get(OntIndividual individual) {
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return get(individual.as(OntIndividual.Named.class));
        }
        return get(individual.as(OntIndividual.Anonymous.class));
    }

    /**
     * Gets an {@link OWLEntity} as {@link ONTObject} from the {@link OntEntity}.
     *
     * @param entity {@link OntEntity}, not {@code null}
     * @return {@link ONTObject} of {@link OWLEntity}
     */
    default ONTObject<? extends OWLEntity> get(OntEntity entity) {
        if (entity instanceof OntClass) {
            return get((OntClass) entity);
        }
        if (entity instanceof OntDT) {
            return get((OntDT) entity);
        }
        if (entity instanceof OntIndividual.Named) {
            return get((OntIndividual.Named) entity);
        }
        if (entity instanceof OntNAP) {
            return get((OntNAP) entity);
        }
        if (entity instanceof OntNDP) {
            return get((OntNDP) entity);
        }
        if (entity instanceof OntNOP) {
            return get((OntNOP) entity);
        }
        throw new OntApiException.IllegalArgument("Unsupported " + entity);
    }

    /**
     * Gets an {@link OWLPropertyExpression} as {@link ONTObject} from the property expression.
     * @param property {@link OntPE}, not {@code null}
     * @return {@link ONTObject} of {@link OWLPropertyExpression}
     */
    default ONTObject<? extends OWLPropertyExpression> get(OntPE property) {
        if (OntApiException.notNull(property, "Null property expression.").canAs(OntNAP.class)) {
            return get(property.as(OntNAP.class));
        }
        return get((OntDOP) property);
    }

    /**
     * Gets an {@link OWLPropertyExpression} as {@link ONTObject} from the data or object property expression.
     *
     * @param property {@link OntDOP}, not {@code null}
     * @return {@link ONTObject} of {@link OWLPropertyExpression}
     */
    default ONTObject<? extends OWLPropertyExpression> get(OntDOP property) {
        // process Object Properties first to match OWL-API-impl behaviour
        if (OntApiException.notNull(property, "Null Data/Object property").canAs(OntOPE.class)) {
            return get(property.as(OntOPE.class));
        }
        if (property.canAs(OntNDP.class)) {
            return get(property.as(OntNDP.class));
        }
        throw new OntApiException("Unsupported property " + property);
    }

    /**
     * Gets an {@link OWLObjectPropertyExpression} as {@link ONTObject} from the {@link OntOPE}.
     *
     * @param objectProperty {@link OntOPE}, not {@code null}
     * @return {@link ONTObject} of {@link OWLObjectPropertyExpression}
     */
    default ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE objectProperty) {
        if (OntApiException.notNull(objectProperty, "Null object property.").isAnon()) {
            return get(objectProperty.as(OntOPE.Inverse.class));
        }
        return get(objectProperty.as(OntNOP.class));
    }

}
