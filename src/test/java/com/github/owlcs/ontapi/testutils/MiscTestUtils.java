/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.testutils;

import com.github.owlcs.ontapi.BlankNodeId;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.owlapi.objects.AnonymousIndividualImpl;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.common.OntPersonalities;
import org.apache.jena.ontapi.common.OntPersonality;
import org.apache.jena.ontapi.common.PunningsMode;
import org.apache.jena.ontapi.model.OntID;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test Utils.
 * <p>
 * Created by @ssz on 16.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class MiscTestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTestUtils.class);

    private static final OWLAnonymousIndividual ANONYMOUS_INDIVIDUAL = new AnonymousIndividualImpl(BlankNodeId.of().label());

    public static Ontology createModel(OntIRI iri) {
        return createModel(iri.toOwlOntologyID());
    }

    public static Ontology createModel(OWLOntologyID id) {
        return createModel(OntManagers.createManager(), id);
    }

    public static Ontology createModel(OntologyManager manager, OWLOntologyID id) {
        LOGGER.debug("Create ontology {}", id);
        return manager.createOntology(id);
    }

    public static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
        compareAxioms(toMap(expected), toMap(actual));
    }

    public static void compareAxioms(Map<AxiomType<?>, List<OWLAxiom>> expected,
                                     Map<AxiomType<?>, List<OWLAxiom>> actual) {
        LOGGER.debug("[Compare] Expected axioms: ");
        expected.values().forEach(x -> LOGGER.debug("{}", x));
        LOGGER.debug("[Compare] Actual axioms: ");
        actual.values().forEach(x -> LOGGER.debug("{}", x));
        Assertions.assertEquals(expected.keySet(), actual.keySet(), "Incorrect axiom types:");
        List<String> errors = new ArrayList<>();
        for (AxiomType<?> type : expected.keySet()) {
            List<OWLAxiom> exList = expected.get(type);
            List<OWLAxiom> acList = actual.get(type);
            if (exList.size() != acList.size()) {
                errors.add(String.format("[%s]incorrect axioms list: %d != %d", type, exList.size(), acList.size()));
                continue;
            }
            for (int i = 0; i < exList.size(); i++) {
                OWLAxiom a = exList.get(i);
                OWLAxiom b = acList.get(i);
                if (same(a, b)) continue;
                errors.add(String.format("[%s]%s != %s", type, a, b));
            }
        }
        errors.forEach(LOGGER::error);
        Assertions.assertTrue(errors.isEmpty(), "There are " + errors.size() + " errors");
    }

    public static Map<AxiomType<?>, List<OWLAxiom>> toMap(Stream<? extends OWLAxiom> stream) {
        return toMap(stream.collect(Collectors.toList()));
    }

    public static Map<AxiomType<?>, List<OWLAxiom>> toMap(List<? extends OWLAxiom> axioms) {
        Set<AxiomType<?>> types = axioms.stream().map(OWLAxiom::getAxiomType).collect(Collectors.toSet());
        Map<AxiomType<?>, List<OWLAxiom>> res = new HashMap<>();
        types.forEach(type -> {
            List<OWLAxiom> value = res.computeIfAbsent(type, t -> new ArrayList<>());
            List<OWLAxiom> byType = axioms.stream().filter(a -> type.equals(a.getAxiomType())).sorted().collect(Collectors.toList());
            value.addAll(byType);
        });
        return res;
    }

    public static boolean same(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex()
                && OWLAPIStreamUtils.equalStreams(replaceAnonymous(a.components()), replaceAnonymous(b.components()));
    }

    private static Stream<?> replaceAnonymous(Stream<?> stream) {
        return stream.map(o -> o instanceof OWLAnonymousIndividual ? ANONYMOUS_INDIVIDUAL : o);
    }

    public static Stream<OWLAxiom> splitAxioms(OWLOntology o) {
        return o.axioms().flatMap(a -> a instanceof OWLNaryAxiom ?
                ((OWLNaryAxiom<?>) a).splitToAnnotatedPairs().stream() : Stream.of(a)).distinct();
    }

    public static PunningsMode getMode(OntPersonality profile) {
        OntPersonality.Punnings p = profile.getPunnings();
        PunningsMode mode = null;
        if (OntPersonalities.OWL_DL2_PUNNINGS.equals(p)) {
            mode = PunningsMode.DL2;
        } else if (OntPersonalities.OWL_DL_WEAK_PUNNINGS.equals(p)) {
            mode = PunningsMode.DL_WEAK;
        } else if (OntPersonalities.OWL_NO_PUNNINGS.equals(p)) {
            mode = PunningsMode.FULL;
        } else {
            Assertions.fail("Unsupported personality profile " + profile);
        }
        return mode;
    }

    /**
     * gets 'punnings' for rdf:Property types (owl:AnnotationProperty, owl:DatatypeProperty, owl:ObjectProperty)
     *
     * @param model {@link Model}
     * @param mode  {@link PunningsMode}
     * @return Set of resources
     */
    public static Set<Resource> getPropertyPunnings(Model model, PunningsMode mode) {
        if (PunningsMode.FULL.equals(mode)) return Collections.emptySet();
        Set<Resource> objectProperties = model.listStatements(null, RDF.type, OWL.ObjectProperty)
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypeProperties = model.listStatements(null, RDF.type, OWL.DatatypeProperty)
                .mapWith(Statement::getSubject).toSet();
        if (PunningsMode.DL_WEAK.equals(mode))
            return unionOfIntersections(objectProperties, datatypeProperties);
        Set<Resource> annotationProperties = model.listStatements(null, RDF.type, OWL.AnnotationProperty)
                .mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(annotationProperties, objectProperties, datatypeProperties);
    }

    /**
     * gets 'punnings' for rdfs:Class types (owl:Class and rdfs:Datatype)
     *
     * @param model {@link Model}
     * @param mode  {@link PunningsMode}
     * @return Set of resources
     */
    public static Set<Resource> getClassPunnings(Model model, PunningsMode mode) {
        if (PunningsMode.FULL.equals(mode)) return Collections.emptySet();
        Set<Resource> classes = model.listStatements(null, RDF.type, OWL.Class).mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypes = model.listStatements(null, RDF.type, RDFS.Datatype).mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(classes, datatypes);
    }

    /**
     * gets the set of 'illegal punnings' from their explicit declaration accordingly specified mode.
     *
     * @param model {@link Model}
     * @param mode  {@link PunningsMode}
     * @return Set of illegal punnings
     */
    public static Set<Resource> getIllegalPunnings(Model model, PunningsMode mode) {
        Set<Resource> res = new HashSet<>(getPropertyPunnings(model, mode));
        res.addAll(getClassPunnings(model, mode));
        return res;
    }

    @SafeVarargs
    private static <T> Set<T> unionOfIntersections(Collection<T>... collections) {
        Stream<T> res = Stream.empty();
        for (int i = 0; i < collections.length; i++) {
            Set<T> intersection = new HashSet<>(collections[i]);
            intersection.retainAll(collections[i < collections.length - 1 ? i + 1 : 0]);
            res = Stream.concat(res, intersection.stream());
        }
        return res.collect(Collectors.toSet());
    }

    public static void assertAxiom(OWLOntology o, AxiomType<?> t, long expected) {
        long actual = o.axioms(t).count();
        LOGGER.debug("AXIOM:{}::::{}", t, actual);
        if (expected != actual) {
            o.axioms(t).forEach(x -> LOGGER.error("{}", x));
        }
        Assertions.assertEquals(expected, actual, "Wrong axioms for " + t);
    }

    /**
     * Recursively lists all models that are associated with the given model in the form of a flat stream.
     * In normal situation, each of the models must have {@code owl:imports} statement in the overlying graph.
     * In this case the returned stream must correspond the result of the {@link Graphs#dataGraphs(Graph)} method.
     *
     * @param m {@link OntModel}, not {@code null}
     * @return {@code Stream} of models, cannot be empty: must contain at least the input (root) model
     * @throws StackOverflowError in case the given model has a recursion in the hierarchy
     * @see Graphs#dataGraphs(Graph)
     * @see OntID#getImportsIRI()
     */
    public static Stream<OntModel> importsClosure(OntModel m) {
        return Stream.concat(Stream.of(m), m.imports().flatMap(MiscTestUtils::importsClosure));
    }
}
