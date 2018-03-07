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

package ru.avicomp.ontapi.jena.impl;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import ru.avicomp.ontapi.jena.impl.configuration.CommonOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntMaker;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * The implementation of Annotation OntObject.
 * Note: the search is carried out only for the root annotations,
 * i.e the result of snippet {@code model.ontObjects(OntAnnotation.class)} would not contain nested annotations.
 * <p>
 * Created by @szuev on 26.03.2017.
 */
@SuppressWarnings("ALL")
public class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {
    public static final Set<Property> SPEC =
            Stream.of(RDF.type, OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                    .collect(Collectors.toSet());
    public static final Set<Resource> EXTRA_ROOT_TYPES =
            Stream.of(OWL.AllDisjointClasses, OWL.AllDisjointProperties, OWL.AllDifferent, OWL.NegativePropertyAssertion)
                    .collect(Collectors.toSet());
    public static final Set<Node> EXTRA_ROOT_TYPES_AS_NODES = EXTRA_ROOT_TYPES.stream()
            .map(FrontsNode::asNode)
            .collect(Collectors.toSet());
    public static Configurable<OntObjectFactory> annotationFactory = m -> new CommonOntObjectFactory(
            new OntMaker.Default(OntAnnotationImpl.class),
            OntAnnotationImpl::findRootAnnotations,
            OntAnnotationImpl::testAnnotation);

    public OntAnnotationImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<OntStatement> content() {
        return SPEC.stream().map(this::getRequiredProperty);
    }

    @Override
    public Stream<OntStatement> assertions() {
        return Iter.asStream(listProperties())
                .filter(st -> !SPEC.contains(st.getPredicate()))
                // original 'right' way:
                //.filter(st -> st.getPredicate().canAs(OntNAP.class))
                //.map(st -> getModel().createOntStatement(false, this, st.getPredicate(), st.getObject()))
                // expected to be faster a little:
                .map(new Function<Statement, OntStatement>() {
                    @Override
                    public OntStatement apply(Statement st) {
                        OntGraphModelImpl model = getModel();
                        OntNAP nap = model.getOntEntity(OntNAP.class, st.getPredicate());
                        return nap == null ? null : model.createOntStatement(false, OntAnnotationImpl.this, nap, st.getObject());
                    }
                })
                .filter(Objects::nonNull)
                ;
    }

    public static Stream<Node> findRootAnnotations(EnhGraph eg) {
        return Stream.concat(Stream.of(OWL.Axiom.asNode()), EXTRA_ROOT_TYPES_AS_NODES.stream())
                .map(t -> eg.asGraph().find(Node.ANY, RDF.type.asNode(), t))
                .map(Iter::asStream)
                .flatMap(Function.identity())
                .map(Triple::getSubject);
    }

    public static boolean testAnnotation(Node node, EnhGraph graph) {
        if (!node.isBlank()) return false;
        Set<Node> types = graph.asGraph().find(node, RDF.type.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
        if ((types.contains(OWL.Axiom.asNode()) || types.contains(OWL.Annotation.asNode())) &&
                Stream.of(OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                        .map(FrontsNode::asNode)
                        .allMatch(p -> graph.asGraph().contains(node, p, Node.ANY))) {
            return true;
        }
        // special cases: owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion
        return EXTRA_ROOT_TYPES_AS_NODES.stream().anyMatch(types::contains);
    }
}
