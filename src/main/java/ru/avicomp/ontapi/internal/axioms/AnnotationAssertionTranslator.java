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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Collection;

/**
 * Examples:
 * <pre>{@code
 *  foaf:LabelProperty vs:term_status "unstable" .
 *  foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 *  pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class AnnotationAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLAnnotationProperty, OWLAnnotationAssertionAxiom> {

    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom.annotations());
    }

    /**
     * Answers the annotation assertion statements.
     * The rule {@code s A t}, where {@code s} is an IRI or anonymous individual,
     * {@code t} is an IRI, anonymous individual, or literal, and {@code A} is an annotation property.
     * Currently there is following default behaviour:
     * if the annotation value has its own annotations then the specified statement is skipped from consideration
     * but comes as annotation of some other axiom.
     * Also it is skipped if load annotations is disabled in the configuration.
     *
     * @param model  {@link OntGraphModel} the model
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Annotations</a>
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return NullIterator.instance();
        OntID id = model.getID();
        return listStatements(model).filterKeep(s -> !id.equals(s.getSubject()) && filter(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return false;
        if (statement.getSubject().canAs(OntID.class)) return false;
        return filter(statement, config);
    }

    public boolean filter(OntStatement s, InternalConfig c) {
        return ReadHelper.isAnnotationAssertionStatement(s, c)
                && ReadHelper.isEntityOrAnonymousIndividual(s.getSubject());
    }

    @Override
    public ONTObject<OWLAnnotationAssertionAxiom> toAxiom(OntStatement statement,
                                                          InternalObjectFactory reader,
                                                          InternalConfig config) {
        ONTObject<? extends OWLAnnotationSubject> s = reader.getSubject(statement.getSubject(OntObject.class));
        ONTObject<OWLAnnotationProperty> p = reader.getProperty(statement.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = reader.getValue(statement.getObject());
        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        OWLAnnotationAssertionAxiom res = reader.getOWLDataFactory()
                .getOWLAnnotationAssertionAxiom(p.getOWLObject(), s.getOWLObject(), v.getOWLObject(),
                        ONTObject.extract(annotations));
        return ONTObjectImpl.create(res, statement).append(annotations).append(s).append(p).append(v);
    }

}
