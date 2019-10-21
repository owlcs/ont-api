/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.transforms;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The transformer to restore declarations in the implicit cases
 * (all notations are taken from the <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Short Guide</a>):
 * 1) data or object universal or existential quantifications (restrictions):
 * <ul>
 * <li>{@code _:x rdf:type owl:Restriction; owl:onProperty P; owl:allValuesFrom C}</li>
 * <li>{@code _:x rdf:type owl:Restriction; owl:onProperty R; owl:someValuesFrom D}</li>
 * </ul>
 * 2) property domains:
 * <ul>
 * <li>{@code A rdfs:domain U}</li>
 * <li>{@code P rdfs:domain C}</li>
 * <li>{@code R rdfs:domain C}</li>
 * </ul>
 * 3) property ranges:
 * <ul>
 * <li>{@code A rdfs:range U}</li>
 * <li>{@code R rdfs:range D}</li>
 * <li>{@code P rdfs:range C}</li>
 * </ul>
 * 4) property assertions (during reasoning the {@code C owl:hasKey ( P1 ... Pm R1 ... Rn )}
 * and {@code U rdf:type owl:FunctionalProperty} are used):
 * <ul>
 * <li>{@code s A t}</li>
 * <li>{@code a R v}</li>
 * <li>{@code a1 PN a2}</li>
 * </ul>
 * 5) other expression and property constructions where one part could be determined from some another:
 * <ul>
 * <li>{@code C1 owl:equivalentClass C2}</li>
 * <li>{@code DN owl:equivalentClass D}</li>
 * <li>{@code P1 owl:equivalentProperty P2}</li>
 * <li>{@code R1 owl:propertyDisjointWith R2}</li>
 * <li>{@code A1 rdfs:subPropertyOf A2}</li>
 * <li>{@code P1 rdfs:subPropertyOf P2}</li>
 * <li>{@code R1 rdfs:subPropertyOf R2}</li>
 * <li>{@code _:x owl:unionOf ( D1 ... Dn )}</li>
 * <li>{@code _:x owl:intersectionOf ( C1 ... Cn )}</li>
 * <li>{@code _:x rdf:type owl:AllDisjointProperties; owl:members ( P1 ... Pn )}</li>
 * </ul>
 * <p>
 * Note: ObjectProperty &amp; ClassExpression have more priority then DataProperty &amp; DataRange
 */
@SuppressWarnings("WeakerAccess")
public class ReasonerDeclarator extends BaseDeclarator {
    protected final int maxRerunCount;
    // a strategy decider
    protected Strategy decider;
    // Map with statements and functions to rerun
    protected final Map<Statement, Function<Statement, Res>> rerun;
    // result of processing
    protected Set<Statement> unparsed = new HashSet<>();

    public ReasonerDeclarator(Graph graph) {
        this(graph, DefaultStrategies.FIRST, 10);
    }

    public ReasonerDeclarator(Graph graph, Strategy decider, int count) {
        this(graph, new LinkedHashMap<>(), decider, count);
    }

    /**
     * Base constructor.
     *
     * @param graph   {@link Graph}
     * @param rerun   {@link Map}
     * @param decider {@link Strategy}
     * @param count   int
     */
    protected ReasonerDeclarator(Graph graph,
                                 Map<Statement, Function<Statement, Res>> rerun,
                                 Strategy decider, int count) {
        super(graph);
        if (count <= 0) throw new IllegalArgumentException();
        this.rerun = Objects.requireNonNull(rerun);
        this.decider = Objects.requireNonNull(decider);
        this.maxRerunCount = count;
    }

    protected void parse(Statement s, Function<Statement, Res> function) {
        Res res = function.apply(s);
        if (res != Res.UNKNOWN) {
            return;
        }
        rerun.put(s, function);
    }

    @Override
    public void perform() {
        try {
            parse();
            unparsed.addAll(parseTail());
        } finally { // possibility to rerun
            rerun.clear();
        }
    }

    protected void parse() {
        parseDataAndObjectRestrictions();
        parsePropertyDomains();
        parseRanges();

        parseEquivalentClasses();
        parseUnionAndIntersectionClassExpressions();
        parseEquivalentAndDisjointProperties();
        parseAllDisjointProperties();
        parseSubProperties();

        // last, since it traverses over whole graph
        parsePropertyAssertions();
    }

    protected Set<Statement> parseTail() {
        Map<Statement, Function<Statement, Res>> prev = new LinkedHashMap<>(rerun);
        Map<Statement, Function<Statement, Res>> next = new LinkedHashMap<>();
        int count = 0;
        while (count++ < maxRerunCount) {
            for (Statement s : prev.keySet()) {
                Function<Statement, Res> func = prev.get(s);
                if (Res.UNKNOWN == func.apply(s)) {
                    next.put(s, prev.get(s));
                }
            }
            if (next.isEmpty()) {
                return Collections.emptySet();
            }
            if (next.size() == prev.size()) {
                decider = decider.next();
            }
            if (decider == null) {
                break;
            }
            prev = next;
            next = new LinkedHashMap<>();
        }
        LOGGER.warn("Ambiguous statements {}", next.keySet());
        return next.keySet();
    }

    @Override
    public Stream<Triple> uncertainTriples() {
        return unparsed.stream().map(FrontsTriple::asTriple);
    }

    protected void parseDataAndObjectRestrictions() {
        // "_:x rdf:type owl:Restriction; owl:onProperty P; owl:allValuesFrom C" and
        // "_:x rdf:type owl:Restriction; owl:onProperty R; owl:someValuesFrom D"
        Iter.flatMap(Iter.of(OWL.allValuesFrom, OWL.someValuesFrom),
                p -> listStatements(null, p, null)) // add sorting to process punnings in restrictions
                .forEachRemaining(s -> parse(s, this::testRestrictions));
    }

    protected Res testRestrictions(Statement statement) {
        Resource p = getObjectResource(statement.getSubject(), OWL.onProperty);
        Resource c = getObjectResource(statement.getSubject(), statement.getPredicate());
        if (p == null || c == null) {
            return Res.FALSE;
        }
        declare(statement.getSubject(), OWL.Restriction);
        if (isClassExpression(c) || isObjectPropertyExpression(p)) {
            declareObjectProperty(p);
            if (declareClass(c))
                return Res.TRUE;
        }
        if (isDataRange(c) || isDataProperty(p)) {
            declareDataProperty(p).declareDatatype(c);
            return Res.TRUE;
        }
        if (decider.chooseClass()) {
            if (canBeClass(c)) {
                declareObjectProperty(p).declareClass(c);
            } else if (canBeDatatype(c)) {
                declareDataProperty(p).declareDatatype(c);
            } else {
                return Res.FALSE;
            }
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parsePropertyDomains() {
        // "P rdfs:domain C" or "R rdfs:domain C" or "A rdfs:domain U"
        listStatements(null, RDFS.domain, null)
                .filterKeep(s -> s.getObject().isResource())
                .forEachRemaining(s -> parse(s, this::testDomains));
    }

    public Res testDomains(Statement statement) {
        Resource left = statement.getSubject();
        Resource right = statement.getResource();
        if (isAnnotationProperty(left) && right.isURIResource()) {
            return Res.TRUE;
        }
        if (isDataProperty(left) || isObjectPropertyExpression(left)) {
            if (declareClass(right))
                return Res.TRUE;
        }
        if (right.isAnon()) {
            declareClass(right);
        }
        if (decider.chooseAnnotationProperty()) {
            declareAnnotationProperty(left);
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parseRanges() {
        // "P rdfs:range C" or "R rdfs:range D" or "A rdfs:range U"
        listStatements(null, RDFS.range, null)
                .filterKeep(s -> s.getObject().isResource())
                .forEachRemaining(s -> parse(s, this::testPropertyRanges));
    }

    protected Res testPropertyRanges(Statement statement) {
        Resource left = statement.getSubject();
        Resource right = statement.getObject().asResource();
        if (isAnnotationProperty(left) && right.isURIResource()) {
            // "A rdfs:range U"
            return Res.TRUE;
        }
        if (isClassExpression(right)) {
            // "P rdfs:range C"
            declareObjectProperty(left);
            return Res.TRUE;
        }
        if (isDataRange(right)) {
            // "R rdfs:range D"
            declareDataProperty(left);
            return Res.TRUE;
        }
        if (decider.chooseAnnotationProperty()) {
            declareAnnotationProperty(left);
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parsePropertyAssertions() {
        // "a1 PN a2", "a R v", "s A t"
        // to avoid ConcurrentModificationException:
        Map<Resource, Resource> add = new HashMap<>();
        Map<Resource, Resource> del = new HashMap<>();
        ReasonerDeclarator self = this;
        new ReasonerDeclarator(getGraph(), rerun, decider, maxRerunCount) {
            @Override
            protected ReasonerDeclarator declare(Resource subject, Resource type) {
                add.put(subject, type);
                return this;
            }

            @Override
            protected ReasonerDeclarator undeclare(Resource subject, Resource type) {
                del.put(subject, type);
                return this;
            }

            private void parse(Statement s) {
                Res res = super.testPropertyAssertions(s);
                if (res != Res.UNKNOWN) {
                    return;
                }
                rerun.put(s, self::testPropertyAssertions);
            }

            @Override
            protected void parsePropertyAssertions() {
                listStatements(null, null, null) // everything!
                        .filterDrop(s -> builtins.reservedProperties().contains(s.getPredicate()))
                        .forEachRemaining(this::parse);
            }
        }.parsePropertyAssertions();
        add.forEach(this::declare);
        del.forEach(this::undeclare);
    }

    protected Res testPropertyAssertions(Statement statement) {
        Resource subject = statement.getSubject();
        RDFNode right = statement.getObject();
        Property property = statement.getPredicate();
        if (isAnnotationProperty(property)) { // annotation assertion ("s A t")
            return Res.TRUE;
        }
        if (right.isLiteral()) { // data property assertion ("a R v")
            if (isDataProperty(property)) {
                declareIndividual(subject);
                return Res.TRUE;
            }
            if (isIndividual(subject) && canBeDataPropertyInAssertion(property)) {
                declareDataProperty(property);
                return Res.TRUE;
            }
            if (mustBeDataOrObjectProperty(property)) {
                declareDataProperty(property).declareIndividual(subject);
                return Res.TRUE;
            }
            if (isClass(subject)) {
                // empirically workaround for NCBITAXON (issue #67)
                declareIndividual(subject).declareAnnotationProperty(property);
                return Res.TRUE;
            }
        } else {
            Resource object = right.asResource();
            if (isIndividual(object) || canBeIndividual(object)) {  // object property assertion ("a1 PN a2")
                if (isObjectPropertyExpression(property)) {
                    declareIndividual(subject).declareIndividual(object);
                    return Res.TRUE;
                }
                if (isIndividual(subject)) {
                    declareObjectProperty(property).declareIndividual(object);
                    return Res.TRUE;
                }
                if (mustBeDataOrObjectProperty(property)) {
                    declareObjectProperty(property).declareIndividual(subject).declareIndividual(object);
                    return Res.TRUE;
                }
            }
        }
        if (decider.chooseAnnotationProperty()) {
            declareAnnotationProperty(property);
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected boolean mustBeDataOrObjectProperty(Resource candidate) {
        // "P rdf:type owl:FunctionalProperty", "R rdf:type owl:FunctionalProperty"
        if (candidate.hasProperty(RDF.type, OWL.FunctionalProperty)) return true;
        // "C owl:hasKey (P1 ... Pm R1 ... Rn)"
        ExtendedIterator<RDFList> lists = listStatements(null, OWL.hasKey, null)
                .mapWith(Statement::getObject)
                .filterKeep(o -> o.canAs(RDFList.class))
                .mapWith(o -> o.as(RDFList.class));
        return Iter.anyMatch(Iter.flatMap(lists, RDFList::iterator), candidate::equals);
    }

    protected boolean canBeIndividual(RDFNode candidate) {
        return candidate.isResource() && (candidate.isAnon() ? !candidate.canAs(RDFList.class) :
                !builtins.reserved().contains(candidate.asResource()));
    }

    protected boolean canBeDataPropertyInAssertion(Property candidate) {
        // if the property participates in assertion where the right part is non-plain literal:
        return Iter.findFirst(listStatements(null, candidate, null)
                .filterKeep(x -> x.getObject().isLiteral())
                .mapWith(Statement::getLiteral)
                .filterDrop(s -> XSDDatatype.XSDstring.equals(s.getDatatype())))
                .isPresent();
    }

    protected boolean canBeClass(Resource resource) {
        return builtins.classes().contains(resource) || !builtins.datatypes().contains(resource);
    }

    protected boolean canBeDatatype(Resource resource) {
        return builtins.datatypes().contains(resource) || !builtins.classes().contains(resource);
    }

    protected void parseEquivalentClasses() {
        // "C1 owl:equivalentClass C2" and "DN owl:equivalentClass D"
        listStatements(null, OWL.equivalentClass, null)
                .filterKeep(s -> s.getObject().isResource())
                .forEachRemaining(s -> parse(s, this::testEquivalentClasses));
    }

    protected Res testEquivalentClasses(Statement statement) {
        Resource a = statement.getSubject();
        Resource b = statement.getObject().asResource();
        if (Iter.anyMatch(Iter.of(a, b), this::isClassExpression)) {
            declareClass(a);
            declareClass(b);
            return Res.TRUE;
        }
        if (a.isURIResource() && Iter.anyMatch(Iter.of(a, b), this::isDataRange)) {
            declareDatatype(a);
            declareDatatype(b);
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parseUnionAndIntersectionClassExpressions() {
        // "_:x owl:unionOf ( D1 ... Dn )", "_:x owl:intersectionOf ( C1 ... Cn )"
        Iter.flatMap(Iter.of(OWL.unionOf, OWL.intersectionOf), p -> listStatements(null, p, null))
                .filterKeep(s -> s.getSubject().isAnon() && s.getObject().canAs(RDFList.class))
                .forEachRemaining(s -> parse(s, this::testUnionAndIntersectionClassExpressions));
    }

    protected Res testUnionAndIntersectionClassExpressions(Statement statement) {
        Set<Resource> set = subjectAndObjects(statement).toSet();
        if (Iter.anyMatch(Iter.create(set), this::isClassExpression)) {
            set.forEach(this::declareClass);
            return Res.TRUE;
        }
        if (Iter.anyMatch(Iter.create(set), this::isDataRange)) {
            set.forEach(this::declareDatatype);
            return Res.TRUE;
        }
        if (decider.chooseClass()) {
            if (Iter.allMatch(Iter.create(set), this::canBeClass)) {
                set.forEach(this::declareClass);
            } else if (Iter.allMatch(Iter.create(set), this::canBeDatatype)) {
                set.forEach(this::declareDatatype);
            } else {
                return Res.FALSE;
            }
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parseEquivalentAndDisjointProperties() {
        Iter.flatMap(Iter.of(OWL.equivalentProperty, OWL.propertyDisjointWith), p -> listStatements(null, p, null))
                .filterKeep(s -> s.getObject().isResource())
                .forEachRemaining(s -> parse(s, this::testEquivalentAndDisjointProperties));
    }

    protected Res testEquivalentAndDisjointProperties(Statement statement) {
        Resource a = statement.getSubject();
        Resource b = statement.getResource();
        if (Stream.of(a, b).anyMatch(this::isObjectPropertyExpression)) {
            declareObjectProperty(a, builtins.properties());
            declareObjectProperty(b, builtins.properties());
            return Res.TRUE;
        }
        if (Stream.of(a, b).anyMatch(this::isDataProperty)) {
            declareDataProperty(a, builtins.properties());
            declareDataProperty(b, builtins.properties());
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parseAllDisjointProperties() {
        // "_:x rdf:type owl:AllDisjointProperties; owl:members ( P1 ... Pn )"
        listStatements(null, RDF.type, OWL.AllDisjointProperties)
                .filterKeep(s -> s.getSubject().isAnon() && s.getSubject().hasProperty(OWL.members))
                .forEachRemaining(s -> parse(s, this::testAllDisjointProperties));
    }

    protected Res testAllDisjointProperties(Statement statement) {
        Set<Resource> set = members(statement.getSubject(), OWL.members).toSet();
        if (set.isEmpty()) {
            return Res.FALSE;
        }
        if (set.stream().anyMatch(this::isObjectPropertyExpression)) {
            set.forEach(this::declareObjectProperty);
            return Res.TRUE;
        }
        if (set.stream().anyMatch(this::isDataProperty)) {
            set.forEach(this::declareDataProperty);
            return Res.TRUE;
        }
        return Res.UNKNOWN;
    }

    protected void parseSubProperties() {
        listStatements(null, RDFS.subPropertyOf, null)
                .filterKeep(s -> s.getObject().isResource())
                .forEachRemaining(s -> parse(s, this::testSubProperties));
    }

    protected Res testSubProperties(Statement statement) {
        Resource a = statement.getSubject();
        Resource b = statement.getResource();
        Res res = Res.UNKNOWN;
        if (Stream.of(a, b).anyMatch(this::isObjectPropertyExpression)) {
            declareObjectProperty(a, builtins.properties());
            declareObjectProperty(b, builtins.properties());
            res = Res.TRUE;
        }
        if (Stream.of(a, b).anyMatch(this::isDataProperty)) {
            declareDataProperty(a, builtins.properties());
            declareDataProperty(b, builtins.properties());
            res = Res.TRUE;
        }
        if (Stream.of(a, b).anyMatch(this::isAnnotationProperty) ||
                (Res.UNKNOWN.equals(res) && decider.chooseAnnotationProperty())) {
            declareAnnotationProperty(a, builtins.properties());
            declareAnnotationProperty(b, builtins.properties());
            res = Res.TRUE;
        }
        return res;
    }

    public enum Res {
        TRUE, // found ad fixed
        FALSE, // found, but can not be fixed
        UNKNOWN, // ambiguous situation
    }

    /**
     * A control interface, which determines the behaviour in ambiguous situations.
     */
    public interface Strategy {
        /**
         * Answers {@code true}
         * if a {@link OWL#AnnotationProperty owl:AnnotationProperty} must be chosen in an ambiguous situation.
         *
         * @return boolean
         */
        boolean chooseAnnotationProperty();

        /**
         * Answers {@code true} if a class or datatype (in that order) must be chosen in an ambiguous situation.
         *
         * @return boolean
         */
        boolean chooseClass();

        /**
         * Returns a different {@link Strategy} to process in next iteration.
         *
         * @return {@link Strategy}.
         */
        Strategy next();
    }

    public enum DefaultStrategies implements Strategy {
        FIRST(false, false) {
            @Override
            public Strategy next() {
                return SECOND;
            }
        },
        SECOND(false, true) {
            @Override
            public Strategy next() {
                return THIRD;
            }
        },
        THIRD(true, true) {
            @Override
            public Strategy next() {
                return null;
            }
        };

        private final boolean chooseAnnotationProperty;
        private final boolean chooseClass;

        DefaultStrategies(boolean chooseAnnotationProperty, boolean chooseClass) {
            this.chooseAnnotationProperty = chooseAnnotationProperty;
            this.chooseClass = chooseClass;
        }

        @Override
        public boolean chooseAnnotationProperty() {
            return chooseAnnotationProperty;
        }

        @Override
        public boolean chooseClass() {
            return chooseClass;
        }

        @Override
        public abstract Strategy next();
    }
}
