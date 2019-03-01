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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.transforms.vocabulary.AVC;
import ru.avicomp.ontapi.transforms.vocabulary.DEPRECATED;

import java.util.*;
import java.util.stream.Stream;

/**
 * The transformer to convert OWL 1 DL =&gt; OWL 2 DL
 * <p>
 * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_RDF_Graphs_to_the_Structural_Specification'>Chapter 3</a>
 * also <a href='https://www.w3.org/TR/owl2-quick-reference/'>4.2 Additional Vocabulary in OWL 2 RDF Syntax</a>
 */
@SuppressWarnings("WeakerAccess")
public class OWLCommonTransform extends Transform {
    private static final RDFDatatype NON_NEGATIVE_INTEGER = XSDDatatype.XSDnonNegativeInteger;
    private static final Map<Property, Property> QUALIFIED_CARDINALITY_REPLACEMENT =
            Collections.unmodifiableMap(new HashMap<Property, Property>() {
                {
                    put(OWL.cardinality, OWL.qualifiedCardinality);
                    put(OWL.maxCardinality, OWL.maxQualifiedCardinality);
                    put(OWL.minCardinality, OWL.minQualifiedCardinality);
                }
            });
    private static final Set<Property> DEPRECATED_OWL_FACET = Stream.of(DEPRECATED.OWL.maxExclusive,
            DEPRECATED.OWL.maxInclusive,
            DEPRECATED.OWL.minExclusive, DEPRECATED.OWL.minInclusive).collect(Iter.toUnmodifiableSet());
    private static Set<Property> CARDINALITY_PREDICATES = Stream.of(OWL.cardinality, OWL.qualifiedCardinality,
            OWL.maxCardinality, OWL.maxQualifiedCardinality,
            OWL.minCardinality, OWL.minQualifiedCardinality).collect(Iter.toUnmodifiableSet());
    private boolean processIndividuals;

    public OWLCommonTransform(Graph graph) {
        this(graph, false);
    }

    protected OWLCommonTransform(Graph graph, boolean processIndividuals) {
        super(graph);
        this.processIndividuals = processIndividuals;
    }

    protected static boolean isQualified(Resource r) {
        return r.hasProperty(OWL.onClass) || r.hasProperty(OWL.onDataRange);
    }

    private static Literal asNonNegativeIntegerLiteral(Literal n) {
        return n.getModel().createTypedLiteral(n.asLiteral().getLexicalForm(), NON_NEGATIVE_INTEGER);
    }

    @Override
    public void perform() {
        fixProperties();
        fixPropertyChains();
        fixExpressions();
        fixClassExpressions();
        fixDataRanges();
        if (processIndividuals) {
            fixNamedIndividuals();
        }
    }

    protected void fixProperties() {
        // replace owl:AntisymmetricProperty with owl:AsymmetricProperty
        changeType(DEPRECATED.OWL.AntisymmetricProperty, OWL.AsymmetricProperty);

        // replace owl:OntologyProperty with owl:AnnotationProperty (table 6)
        changeType(OWL.OntologyProperty, OWL.AnnotationProperty);

        // definitely ObjectProperty (table 6, supplemented by owl:AsymmetricProperty, owl:ReflexiveProperty, owl:IrreflexiveProperty):
        Iter.flatMap(Iter.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty,
                OWL.AsymmetricProperty, OWL.ReflexiveProperty, OWL.IrreflexiveProperty),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> declare(s.getSubject(), OWL.ObjectProperty));

        // table 5 (remove rdf:Property):
        Iter.flatMap(Iter.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty,
                OWL.TransitiveProperty, OWL.DatatypeProperty, OWL.AnnotationProperty, OWL.OntologyProperty),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> undeclare(s.getSubject(), RDF.Property));

        Model m = getWorkModel();
        // properties
        listStatements(null, RDF.type, DEPRECATED.OWL.DataProperty).toList()
                .forEach(s -> m.remove(s).add(s.getSubject(), RDF.type, OWL.DatatypeProperty));


        replacePredicates(OWL.propertyDisjointWith, DEPRECATED.OWL.disjointObjectProperties, DEPRECATED.OWL.disjointDataProperties);
        replacePredicates(OWL.equivalentProperty, DEPRECATED.OWL.equivalentObjectProperty, DEPRECATED.OWL.equivalentDataProperty);
    }

    protected void fixDataRanges() {
        // replace owl:DataRange with rdfs:Datatype
        changeType(OWL.DataRange, RDFS.Datatype);

        Model m = getWorkModel();
        listStatements(null, RDF.type, RDFS.Datatype)
                .mapWith(Statement::getSubject)
                .forEachRemaining(r -> r.listProperties(DEPRECATED.OWL.dataComplementOf)
                        .toList()
                        .forEach(s -> m.remove(s).add(s.getSubject(), OWL.datatypeComplementOf, s.getObject())));
        listStatements(null, OWL.datatypeComplementOf, null)
                .mapWith(Statement::getSubject)
                .toList()
                .forEach(s -> {
                    declare(s, RDFS.Datatype);
                    moveToEquivalentClass(s, OWL.datatypeComplementOf, RDFS.Datatype);
                });
        // fix facet data ranges
        listStatements(null, RDF.type, RDFS.Datatype)
                .mapWith(Statement::getSubject)
                .toList()
                .forEach(this::fixDatatype);
    }

    /**
     * Fixes datatypes.
     * Example of wrong named datatype:
     * <pre>{@code
     * family:Between10and20
     *         a                 owl:DataRange ;
     *         owl:maxExclusive  "20" ;
     *         owl:onDataRange   [ a                 owl:DataRange ;
     *                             owl:minInclusive  "10" ;
     *                             owl:onDataRange   <http://www.w3.org/2001/XMLSchema#nonNegativeInteger>
     *                           ] .
     * }</pre>
     *
     * @param r {@link Resource}
     */
    protected void fixDatatype(Resource r) {
        Model m = getWorkModel();
        // owl:onDataRange -> owl:onDatatype
        r.listProperties(OWL.onDataRange)
                .toList()
                .forEach(s -> {
                    RDFNode o = s.getObject();
                    if (o.isURIResource()) {
                        m.remove(s).add(s.getSubject(), OWL.onDatatype, s.getObject());
                    } else if (o.isAnon()) {
                        Resource auto = AVC.randomIRI().inModel(m);
                        m.remove(s).add(s.getSubject(), OWL.onDatatype, auto);
                        auto.addProperty(OWL.equivalentClass, o);
                    }
                });
        Resource anon;
        if (r.isURIResource()) {
            List<Statement> list = r.listProperties()
                    .filterKeep(s -> s.getPredicate().equals(OWL.onDatatype) ||
                            DEPRECATED_OWL_FACET.contains(s.getPredicate()))
                    .toList();
            if (list.isEmpty()) return;
            anon = m.createResource(RDFS.Datatype);
            list.forEach(s -> m.remove(s).add(anon, s.getPredicate(), s.getObject()));
            r.addProperty(OWL.equivalentClass, anon);
        } else if (r.isAnon()) {
            anon = r;
        } else {
            return;
        }
        Iter.flatMap(Iter.create(DEPRECATED_OWL_FACET), anon::listProperties)
                .toList().forEach(s -> {
            Property p = m.getProperty(XSD.NS + s.getPredicate().getLocalName());
            Resource f = m.createResource().addProperty(p, s.getObject());
            m.add(s.getSubject(), OWL.withRestrictions, m.createList(f)).remove(s);
        });
    }

    private void replacePredicates(Property newPredicate, Property... oldPredicates) {
        Model m = getWorkModel();
        Iter.flatMap(Iter.of(oldPredicates),
                p -> listStatements(null, p, null)).toList()
                .forEach(s -> m.remove(s).add(s.getSubject(), newPredicate, s.getObject()));
    }

    /**
     * To fix {@code SubPropertyChainOfAxiom}.
     * Examples:
     * <pre>{@code
     * [    rdfs:subPropertyOf  :r ;
     *      owl:propertyChain   ( :p :q )
     * ] .
     * }</pre>
     * <pre>{@code
     * [ a                   rdf:List ;
     *   rdf:first           :p ;
     *   rdf:rest            ( :q ) ;
     *   rdfs:subPropertyOf  :r
     * ] .
     * }</pre>
     * Correct:
     * <pre>{@code
     * :r owl:propertyChainAxiom ( :p :q )
     * }</pre>
     */
    public void fixPropertyChains() {
        Model m = getWorkModel();
        listStatements(null, DEPRECATED.OWL.propertyChain, null).toList()
                .forEach(s -> m.remove(s).add(s.getSubject(), OWL.propertyChainAxiom, s.getObject()));

        listStatements(null, OWL.propertyChainAxiom, null)
                .mapWith(Statement::getSubject)
                .filterKeep(s -> s.isAnon()
                        && s.hasProperty(RDFS.subPropertyOf)
                        && s.getPropertyResourceValue(RDFS.subPropertyOf).isURIResource()
                        && s.getPropertyResourceValue(OWL.propertyChainAxiom).canAs(RDFList.class))
                .toSet()
                .forEach(s -> {
                    Resource p = s.getRequiredProperty(RDFS.subPropertyOf).getResource();
                    Resource list = s.getRequiredProperty(OWL.propertyChainAxiom).getResource();
                    m.add(p, OWL.propertyChainAxiom, list)
                            .removeAll(s, RDFS.subPropertyOf, null)
                            .removeAll(s, OWL.propertyChainAxiom, null);
                });
        listStatements(null, RDF.type, RDF.List)
                .mapWith(Statement::getSubject)
                .filterKeep(x -> x.isAnon() && x.hasProperty(RDFS.subPropertyOf))
                .toList()
                .forEach(s -> {
                    Statement p = s.getRequiredProperty(RDFS.subPropertyOf);
                    RDFList list = s.as(RDFList.class);
                    p.getResource().addProperty(OWL.propertyChainAxiom, m.createList(list.iterator()));
                    m.remove(p);
                    Models.getAssociatedStatements(list).forEach(m::remove);
                });
    }

    /**
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_Axioms'>Chapter 3.2.5, Table 18</a>
     * Warning: there is also an interpretation of empty rdf:List as owl:Nothing or owl:Thing in table.
     * But I don't see that OWL-API works such way (it treats rdf:nil as just an empty collection).
     * So we skip also fixing for this case.
     * Plus - it works currently only for named owl:Class resources.
     */
    protected void fixClassExpressions() {
        listStatements(null, OWL.complementOf, null)
                .mapWith(Statement::getSubject)
                .toList()
                .forEach(s -> {
                    declare(s, OWL.Class);
                    OWLCommonTransform.this.moveToEquivalentClass(s, OWL.complementOf, OWL.Class);
                });
        fixRestrictions();
    }

    protected void fixExpressions() {
        // table 5 (remove rdfs:Class):
        Iter.flatMap(Iter.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> undeclare(s.getSubject(), RDFS.Class));

        Iter.of(OWL.unionOf, OWL.intersectionOf, OWL.oneOf)
                .forEachRemaining(p -> listStatements(null, p, null)
                        .mapWith(Statement::getSubject)
                        .filterKeep(RDFNode::isURIResource)
                        .toSet()
                        .forEach(s -> moveToEquivalentClass(s, p, null)));
    }

    protected void moveToEquivalentClass(Resource subject, Property predicate, Resource type) {
        if (subject.isAnon()) return;
        List<Statement> statements = listStatements(subject, predicate, null).toList();
        if (statements.isEmpty()) return;
        Model m = getWorkModel();
        Resource newRoot = type != null ? m.createResource(type) : m.createResource();
        m.add(subject, OWL.equivalentClass, newRoot);
        statements.forEach(s -> newRoot.addProperty(s.getPredicate(), s.getObject()));
        m.remove(statements);
    }

    protected void fixRestrictions() {
        Model m = getWorkModel();
        listStatements(null, RDF.type, OWL.Restriction)
                .mapWith(Statement::getSubject)
                .forEachRemaining(this::fixRestriction);
        listStatements(null, RDF.type, DEPRECATED.OWL.SelfRestriction)
                .toList()
                .forEach(s -> m.remove(s)
                        .add(s.getSubject(), RDF.type, OWL.Restriction)
                        .add(s.getSubject(), OWL.hasSelf, Models.TRUE));
        Iter.of(DEPRECATED.OWL.DataRestriction, DEPRECATED.OWL.ObjectRestriction)
                .forEachRemaining(type -> listStatements(null, RDF.type, type)
                        .toList()
                        .forEach(s -> m.remove(s)
                                .add(s.getSubject(), RDF.type, OWL.Restriction)));
    }

    protected void fixRestriction(Resource r) {
        fixCardinalityRestriction(r);
    }

    /**
     * Fixes cardinality restriction.
     * There are two possible issues:
     * <ul><li>OWL1 does not support Qualified Cardinality restrictions.
     * Note that {@link org.apache.jena.vocabulary.OWL Jena OWL vocabulary}
     * does not contain {@code owl:onClass} and {@code owl:onDataRange}.</li>
     * <li>A literal from the right side of the cardinality statement
     * must have {@code xsd:nonNegativeInteger}, but sometimes there is {@code xsd:int}</li></ul>
     *
     * @param r {@link Resource} in model
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Minimum_Cardinality'>8.3.1 Minimum Cardinality</a>
     */
    protected void fixCardinalityRestriction(Resource r) {
        Model m = getWorkModel();
        // xsd^^int -> xsd:nonNegativeInteger
        CARDINALITY_PREDICATES.forEach(p -> r.listProperties(p)
                .filterKeep(s -> s.getObject().isLiteral())
                .filterDrop(s -> NON_NEGATIVE_INTEGER.equals(s.getLiteral().getDatatype()))
                .toList()
                .forEach(s -> m.remove(s)
                        .add(s.getSubject(), s.getPredicate(), asNonNegativeIntegerLiteral(s.getLiteral()))));
        if (isQualified(r)) {
            QUALIFIED_CARDINALITY_REPLACEMENT
                    .forEach((a, b) -> r.listProperties(a).toList().forEach(s -> m.remove(s).add(r, b, s.getObject())));
        }
    }

    protected void fixNamedIndividuals() {
        Set<Resource> forbidden = builtins.reservedResources();
        listStatements(null, RDF.type, null)
                .filterKeep(s -> s.getSubject().isURIResource() && s.getObject().isResource())
                .filterDrop(s -> forbidden.contains(s.getObject().asResource()))
                .toList()
                .forEach(s -> declare(s.getSubject(), OWL.NamedIndividual));
    }

}
