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
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.vocabulary.WRONG_OWL;

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
    protected static final boolean PROCESS_INDIVIDUALS_DEFAULT = false;

    private static final RDFDatatype NON_NEGATIVE_INTEGER = XSDDatatype.XSDnonNegativeInteger;
    private static Set<Property> CARDINALITY_PREDICATES = Stream.of(OWL.cardinality, OWL.qualifiedCardinality,
            OWL.maxCardinality, OWL.maxQualifiedCardinality,
            OWL.minCardinality, OWL.minQualifiedCardinality).collect(Iter.toUnmodifiableSet());
    private static final Map<Property, Property> QUALIFIED_CARDINALITY_REPLACEMENT = Collections.unmodifiableMap(new HashMap<Property, Property>() {
        {
            put(OWL.cardinality, OWL.qualifiedCardinality);
            put(OWL.maxCardinality, OWL.maxQualifiedCardinality);
            put(OWL.minCardinality, OWL.minQualifiedCardinality);
        }
    });
    private boolean processIndividuals;

    public OWLCommonTransform(Graph graph) {
        this(graph, PROCESS_INDIVIDUALS_DEFAULT);
    }

    protected OWLCommonTransform(Graph graph, boolean processIndividuals) {
        super(graph);
        this.processIndividuals = processIndividuals;
    }

    @Override
    public void perform() {
        // table 5:
        Iter.flatMap(Iter.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> undeclare(s.getSubject(), RDFS.Class));

        // table 5:
        Iter.flatMap(Iter.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty,
                OWL.TransitiveProperty, OWL.DatatypeProperty, OWL.AnnotationProperty, OWL.OntologyProperty),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> undeclare(s.getSubject(), RDF.Property));

        // definitely ObjectProperty (table 6, supplemented by owl:AsymmetricProperty, owl:ReflexiveProperty, owl:IrreflexiveProperty):
        Iter.flatMap(Iter.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty,
                OWL.AsymmetricProperty, OWL.ReflexiveProperty, OWL.IrreflexiveProperty),
                p -> listStatements(null, RDF.type, p))
                .forEachRemaining(s -> declare(s.getSubject(), OWL.ObjectProperty));

        // replace owl:OntologyProperty with owl:AnnotationProperty (table 6)
        changeType(OWL.OntologyProperty, OWL.AnnotationProperty);
        // replace owl:DataRange(as deprecated) with rdfs:Datatype (see quick-reference guide)
        changeType(OWL.DataRange, RDFS.Datatype);

        fixInvalidURIs();
        fixAxioms();
        if (processIndividuals) {
            fixNamedIndividuals();
        }
    }

    protected void fixAxioms() {
        fixClassExpressions();
        fixPropertyChains();
    }

    /**
     * to replace
     * {@link WRONG_OWL#propertyChain} -&gt; {@link OWL#propertyChainAxiom}
     * {@link WRONG_OWL#DataProperty} -&gt; {@link OWL#DatatypeProperty}
     *
     * @see WRONG_OWL
     */
    public void fixInvalidURIs() {
        Model m = getBaseModel();
        listStatements(null, WRONG_OWL.propertyChain, null).toList()
                .forEach(s -> m.remove(s).add(s.getSubject(), OWL.propertyChainAxiom, s.getObject()));
        listStatements(null, RDF.type, WRONG_OWL.DataProperty).toList()
                .forEach(s -> m.remove(s).add(s.getSubject(), RDF.type, OWL.DatatypeProperty));
    }

    /**
     * As shown by tests from OWL-API-contract, SubPropertyChainOfAxiom could also be expressed in the following form:
     * <pre>{@code
     * [    rdfs:subPropertyOf  :r ;
     *      owl:propertyChain   ( :p :q )
     * ] .
     * }</pre>
     * Unfortunately I could not find specification for this case,
     * but I believe that this is an example of some rudimental OWL dialect, so it must be fixed here.
     */
    public void fixPropertyChains() {
        Model m = getBaseModel();
        listStatements(null, OWL.propertyChainAxiom, null)
                .mapWith(Statement::getSubject)
                .filterKeep(RDFNode::isAnon)
                .filterKeep(s -> s.hasProperty(RDFS.subPropertyOf)
                        && s.getPropertyResourceValue(RDFS.subPropertyOf).isURIResource())
                .filterKeep(s -> s.getPropertyResourceValue(OWL.propertyChainAxiom).canAs(RDFList.class))
                .toSet()
                .forEach(a -> {
                    Resource s = a.getRequiredProperty(RDFS.subPropertyOf).getResource();
                    Resource o = a.getRequiredProperty(OWL.propertyChainAxiom).getResource();
                    m.add(s, OWL.propertyChainAxiom, o)
                            .removeAll(a, RDFS.subPropertyOf, null)
                            .removeAll(a, OWL.propertyChainAxiom, null);
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
        Stream.of(OWL.complementOf, OWL.unionOf, OWL.intersectionOf, OWL.oneOf)
                .forEach(property -> listStatements(null, property, null)
                        .mapWith(Statement::getSubject)
                        .filterKeep(RDFNode::isURIResource)
                        .toSet()
                        .forEach(c -> moveToEquivalentClass(c, property)));
        fixRestrictions();
    }

    protected void moveToEquivalentClass(Resource subject, Property predicate) {
        Model m = getBaseModel();
        List<Statement> statements = listStatements(subject, predicate, null).toList();
        Resource newRoot = m.createResource().addProperty(RDF.type, OWL.Class);
        m.add(subject, OWL.equivalentClass, newRoot);
        statements.forEach(s -> newRoot.addProperty(s.getPredicate(), s.getObject()));
        m.remove(statements);
    }

    protected void fixRestrictions() {
        listStatements(null, RDF.type, OWL.Restriction)
                .mapWith(Statement::getSubject)
                .forEachRemaining(this::fixRestriction);
    }

    protected void fixRestriction(Resource r) {
        fixCardinalityRestriction(r);
    }

    /**
     * Fixes cardinality restriction.
     * There are two possible issues:
     * <ul><li>OWL1 does not support Qualified Cardinality restrictions.
     * Note that {@link org.apache.jena.vocabulary.OWL Jena OWL vocabulary}
     * does not contain {@code owl:onClass} and {@code owl:onDataRange}.</li></ul>
     * <li>A literal from the right side of the cardinality statement
     * must have {@code xsd:nonNegativeInteger}, but sometimes there is {@code xsd:int}</li>
     *
     * @param r {@link Resource} in model
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Minimum_Cardinality'>8.3.1 Minimum Cardinality</a>
     */
    protected void fixCardinalityRestriction(Resource r) {
        Model m = getBaseModel();
        // xsd^^int -> xsd:nonNegativeInteger
        CARDINALITY_PREDICATES.forEach(property -> r.listProperties(property)
                .filterKeep(s -> s.getObject().isLiteral())
                .filterDrop(s -> NON_NEGATIVE_INTEGER.equals(s.getObject().asLiteral().getDatatype()))
                .toList()
                .forEach(s -> m.remove(s)
                        .add(s.getSubject(), s.getPredicate(), asNonNegativeIntegerLiteral(s.getObject()))));
        if (isQualified(r)) {
            QUALIFIED_CARDINALITY_REPLACEMENT
                    .forEach((a, b) -> r.listProperties(a).toList().forEach(s -> m.remove(s).add(r, b, s.getObject())));
        }
    }

    protected static boolean isQualified(Resource r) {
        return r.hasProperty(OWL.onClass) || r.hasProperty(OWL.onDataRange);
    }

    private static Literal asNonNegativeIntegerLiteral(RDFNode n) {
        return n.getModel().createTypedLiteral(n.asLiteral().getLexicalForm(), NON_NEGATIVE_INTEGER);
    }

    protected void fixNamedIndividuals() {
        Set<Resource> forbidden = builtIn.reservedResources();
        listStatements(null, RDF.type, null)
                .filterKeep(s -> s.getSubject().isURIResource())
                .filterKeep(s -> s.getObject().isResource())
                .filterDrop(s -> forbidden.contains(s.getObject().asResource()))
                .toList()
                .forEach(s -> declare(s.getSubject(), OWL.NamedIndividual));
    }

}
