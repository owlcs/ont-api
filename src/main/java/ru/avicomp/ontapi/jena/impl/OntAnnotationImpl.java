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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The implementation of {@link OntAnnotation Annotation} {@link OntObject Ontology Object}.
 * Note: the search is carried out only for the root annotations:
 * the result of snippet {@code model.ontObjects(OntAnnotation.class)} would not contain the nested annotations.
 * <p>
 * Created by @szuev on 26.03.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {
    public static final Set<Property> REQUIRED_PROPERTIES = Stream.of(OWL.annotatedSource
            , OWL.annotatedProperty
            , OWL.annotatedTarget).collect(Iter.toUnmodifiableSet());
    private static final List<Node> REQUIRED_PROPERTY_NODES = REQUIRED_PROPERTIES.stream()
            .map(FrontsNode::asNode).collect(Iter.toUnmodifiableList());
    private static final Node AXIOM = OWL.Axiom.asNode();
    private static final Node ANNOTATION = OWL.Annotation.asNode();
    public static final Set<Property> SPEC = Stream.concat(Stream.of(RDF.type), REQUIRED_PROPERTIES.stream())
            .collect(Iter.toUnmodifiableSet());
    public static final Set<Resource> EXTRA_ROOT_TYPES =
            Stream.of(OWL.AllDisjointClasses, OWL.AllDisjointProperties, OWL.AllDifferent, OWL.NegativePropertyAssertion)
                    .collect(Iter.toUnmodifiableSet());
    public static final List<Resource> ROOT_TYPES = Stream.concat(Stream.of(OWL.Axiom, OWL.Annotation)
            , EXTRA_ROOT_TYPES.stream()).collect(Iter.toUnmodifiableList());
    public static final Set<Node> EXTRA_ROOT_TYPES_AS_NODES = Iter.asUnmodifiableNodeSet(EXTRA_ROOT_TYPES);
    public static ObjectFactory annotationFactory = Factories.createCommon(OntAnnotationImpl.class,
            OntAnnotationImpl::listRootAnnotations,
            OntAnnotationImpl::testAnnotation);

    /**
     * The first are annotations with the most numerous assertions and children,
     * the remaining comparison operations are not so important,
     * but the provided order should be preserved after graph reload.
     */
    public static final Comparator<OntAnnotation> DEFAULT_ANNOTATION_COMPARATOR = (left, right) -> {
        Set<OntStatement> leftSet = listRelatedStatements(left).toSet();
        Set<OntStatement> rightSet = listRelatedStatements(right).toSet();
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

    /**
     * Creates a new annotation b-node resource with the given type and base statement.
     *
     * @param model {@link Model}
     * @param base  base ont-statement
     * @param type  owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    public static OntAnnotation createAnnotation(Model model, Statement base, Resource type) {
        Resource res = Objects.requireNonNull(model).createResource();
        if (!model.contains(Objects.requireNonNull(base))) {
            throw new OntJenaException.IllegalArgument("Can't find " + Models.toString(base));
        }
        res.addProperty(RDF.type, type);
        res.addProperty(OWL.annotatedSource, base.getSubject());
        res.addProperty(OWL.annotatedProperty, base.getPredicate());
        res.addProperty(OWL.annotatedTarget, base.getObject());
        return res.as(OntAnnotation.class);
    }

    @Override
    public Stream<OntStatement> spec() {
        //return SPEC.stream().map(this::getRequiredProperty);
        return Iter.asStream(listStatements().filterKeep(s -> SPEC.contains(s.getPredicate()) || s.isAnnotation()));
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
    public ExtendedIterator<OntStatement> listAssertions() {
        return listStatements().filterKeep(s -> !SPEC.contains(s.getPredicate()) && s.isAnnotation());
    }

    @Override
    public ExtendedIterator<OntStatement> listAnnotations() {
        return listAssertions();
    }

    @Override
    public Stream<OntAnnotation> descendants() {
        return Iter.asStream(listDescendants());
    }

    /**
     * Returns an iterator over all descendants of this ont-annotation resource.
     *
     * @return {@link ExtendedIterator} of {@link OntAnnotation}s
     */
    public ExtendedIterator<OntAnnotation> listDescendants() {
        OntGraphModelImpl m = getModel();
        return listAnnotatedSources()
                .mapWith(s -> m.findNodeAs(((OntStatementImpl) s).getSubjectNode(), OntAnnotation.class))
                .filterDrop(Objects::isNull);
        /*return getModel().listStatements(null, OWL.annotatedSource, this)
                .filterKeep(s -> s.getSubject().canAs(OntAnnotation.class))
                .mapWith(s -> s.getSubject().as(OntAnnotation.class));*/
    }

    protected ExtendedIterator<OntStatement> listAnnotatedSources() {
        return getModel().listOntStatements(null, OWL.annotatedSource, this);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return getModel().add(this, property, value).createStatement(this, property, value);
    }

    @Override
    public Class<? extends OntObject> getActualClass() {
        return OntAnnotation.class;
    }

    /**
     * Lists all root {@link Node}s of top-level {@link OntAnnotation}s in the given model.
     * In OWL2 a top-level annotation must have one of the following {@code rdf:type}s:
     * {@link OWL#Axiom owl:Axiom}, {@link OWL#AllDisjointClasses owl:AllDisjointClasses},
     * {@link OWL#AllDisjointProperties owl:AllDisjointProperties}, {@link OWL#AllDifferent owl:AllDifferent} or
     * {@link OWL#NegativePropertyAssertion owl:NegativePropertyAssertion}
     *
     * @param eg {@link EnhGraph} model to search in
     * @return {@link ExtendedIterator} of {@link Node}s
     */
    public static ExtendedIterator<Node> listRootAnnotations(EnhGraph eg) {
        return Iter.flatMap(Iter.of(OWL.Axiom.asNode()).andThen(EXTRA_ROOT_TYPES_AS_NODES.iterator()),
                t -> eg.asGraph().find(Node.ANY, RDF.Nodes.type, t))
                .mapWith(Triple::getSubject);
    }

    public static boolean testAnnotation(Node node, EnhGraph graph) {
        return testAnnotation(node, graph.asGraph());
    }

    public static boolean testAnnotation(Node node, Graph graph) {
        if (!node.isBlank()) return false;
        ExtendedIterator<Node> types = graph.find(node, RDF.Nodes.type, Node.ANY).mapWith(Triple::getObject);
        try {
            while (types.hasNext()) {
                Node t = types.next();
                if (AXIOM.equals(t) || ANNOTATION.equals(t)) {
                    // test spec
                    for (Node r : REQUIRED_PROPERTY_NODES) {
                        if (!graph.contains(node, r, Node.ANY)) return false;
                    }
                    return true;
                }
                // special cases: owl:AllDisjointClasses, owl:AllDisjointProperties,
                // owl:AllDifferent or owl:NegativePropertyAssertion
                if (OntAnnotationImpl.EXTRA_ROOT_TYPES_AS_NODES.contains(t)) {
                    return true;
                }
            }
        } finally {
            types.close();
        }
        return false;
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
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @since 1.3.0
     */
    public static ExtendedIterator<OntStatement> listRelatedStatements(OntAnnotation annotation) {
        OntAnnotationImpl a = (OntAnnotationImpl) annotation;
        return a.listAssertions().andThen(a.listDescendants().mapWith(OntObject::getRoot));
    }

}
