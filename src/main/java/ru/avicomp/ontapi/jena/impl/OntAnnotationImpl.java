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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.CommonOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntMaker;
import ru.avicomp.ontapi.jena.impl.conf.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The implementation of Annotation OntObject.
 * Note: the search is carried out only for the root annotations:
 * the result of snippet {@code model.ontObjects(OntAnnotation.class)} would not contain nested annotations.
 * <p>
 * Created by @szuev on 26.03.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {
    public static final Set<Property> REQUIRED_PROPERTIES = Stream.of(OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
            .collect(Iter.toUnmodifiableSet());
    public static final Set<Property> SPEC = Stream.concat(Stream.of(RDF.type), REQUIRED_PROPERTIES.stream())
            .collect(Iter.toUnmodifiableSet());
    public static final Set<Resource> EXTRA_ROOT_TYPES =
            Stream.of(OWL.AllDisjointClasses, OWL.AllDisjointProperties, OWL.AllDifferent, OWL.NegativePropertyAssertion)
                    .collect(Iter.toUnmodifiableSet());
    public static final Set<Node> EXTRA_ROOT_TYPES_AS_NODES = EXTRA_ROOT_TYPES.stream()
            .map(FrontsNode::asNode)
            .collect(Iter.toUnmodifiableSet());
    public static OntObjectFactory annotationFactory = new CommonOntObjectFactory(new OntMaker.Default(OntAnnotationImpl.class),
            OntAnnotationImpl::findRootAnnotations,
            OntAnnotationImpl::testAnnotation);

    /**
     * The first are annotations with the most numerous assertions and children,
     * the remaining comparison operations are not so important,
     * but the provided order should be preserved after graph reload.
     */
    public static final Comparator<OntAnnotation> DEFAULT_ANNOTATION_COMPARATOR = (left, right) -> {
        Set<OntStatement> leftSet = listRelatedStatements(left).collect(Collectors.toSet());
        Set<OntStatement> rightSet = listRelatedStatements(right).collect(Collectors.toSet());
        int res = Integer.compare(leftSet.size(), rightSet.size());
        while (res == 0) {
            OntStatement s1 = removeMin(leftSet, Models.STATEMENT_COMPARATOR_IGNORE_BLANK);
            OntStatement s2 = removeMin(rightSet, Models.STATEMENT_COMPARATOR_IGNORE_BLANK);
            res = Models.STATEMENT_COMPARATOR_IGNORE_BLANK.compare(s1, s2);
            if (leftSet.isEmpty() || rightSet.isEmpty()) break;
        }
        return -res;
    };

    public OntAnnotationImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<OntStatement> spec() {
        //return SPEC.stream().map(this::getRequiredProperty);
        return statements().filter(s -> SPEC.contains(s.getPredicate()) || s.isAnnotation());
    }

    @Override
    public OntStatement getBase() {
        Resource s = getRequiredObject(OWL.annotatedSource, Resource.class);
        Property p = getRequiredObject(OWL.annotatedProperty, Property.class);
        RDFNode o = getRequiredObject(OWL.annotatedTarget, RDFNode.class);
        return getModel().statements(s, p, o).findAny()
                .orElseThrow(() -> new OntJenaException("Can't find triple [" + s + ", " + p + ", " + o + "]"));
    }

    @Override
    public Stream<OntStatement> assertions() {
        return statements()
                .filter(st -> !OntAnnotationImpl.SPEC.contains(st.getPredicate()))
                .filter(OntStatement::isAnnotation);
    }

    @Override
    public Stream<OntStatement> annotations() {
        return assertions();
    }

    @Override
    public Stream<OntAnnotation> descendants() {
        return getModel().statements(null, OWL.annotatedSource, this)
                .map(OntStatement::getSubject)
                .filter(s -> s.canAs(OntAnnotation.class))
                .map(s -> s.as(OntAnnotation.class));
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        OntGraphModelImpl model = getModel();
        model.add(this, property, value);
        return model.createStatement(this, property, value);
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
                REQUIRED_PROPERTIES.stream()
                        .map(FrontsNode::asNode)
                        .allMatch(p -> graph.asGraph().contains(node, p, Node.ANY))) {
            return true;
        }
        // special cases: owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion
        return EXTRA_ROOT_TYPES_AS_NODES.stream().anyMatch(types::contains);
    }

    private static <S> S removeMin(Set<S> notEmptySet,
                                   @SuppressWarnings("SameParameterValue") Comparator<? super S> comparator)
            throws IllegalStateException {
        S res = notEmptySet.stream().min(comparator).orElseThrow(IllegalStateException::new);
        if (!notEmptySet.remove(res)) throw new IllegalStateException();
        return res;
    }

    /**
     * Lists annotation assertions plus sub-annotation root statements.
     *
     * @param annotation {@link OntAnnotation}
     * @return Stream of {@link OntStatement}s
     * @since 1.3.0
     */
    public static Stream<OntStatement> listRelatedStatements(OntAnnotation annotation) {
        return Stream.concat(annotation.assertions(), annotation.descendants().map(OntObject::getRoot));
    }

}
