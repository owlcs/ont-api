/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentClassesAxiomImpl;

/**
 * Base class {@link AbstractNaryTranslator}
 * Example of ttl:
 * <pre>{@code
 *  pizza:SpicyTopping owl:equivalentClass [ a owl:Class; owl:intersectionOf ( pizza:PizzaTopping [a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot] )] ;
 * }
 * </pre>
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class EquivalentClassesTranslator extends AbstractNaryTranslator<OWLEquivalentClassesAxiom, OWLClassExpression, OntCE> {
    @Override
    public Property getPredicate() {
        return OWL.equivalentClass;
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
    }

    @Override
    OWLEquivalentClassesAxiom create(Stream<OWLClassExpression> components, Set<OWLAnnotation> annotations) {
        return new OWLEquivalentClassesAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    public InternalObject<OWLEquivalentClassesAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLClassExpression> a = ReadHelper.fetchClassExpression(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<? extends OWLClassExpression> b = ReadHelper.fetchClassExpression(statement.getObject().as(getView()), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLEquivalentClassesAxiom res = conf.dataFactory().getOWLEquivalentClassesAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
