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

package ru.avicomp.ontapi.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.conf.Configurable;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.owlapi.objects.OWLAnonymousIndividualImpl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test Utils.
 * <p>
 * Created by @szuev on 16.10.2016.
 */
public class TestUtils {
    private static final Logger LOGGER = Logger.getLogger(TestUtils.class);

    private static final OWLAnonymousIndividual ANONYMOUS_INDIVIDUAL = new OWLAnonymousIndividualImpl(NodeID.getNodeID());

    public static OntologyModel createModel(OntIRI iri) {
        return createModel(iri.toOwlOntologyID());
    }

    public static OntologyModel createModel(OWLOntologyID id) {
        return createModel(OntManagers.createONT(), id);
    }

    public static OntologyModel createModel(OntologyManager manager, OWLOntologyID id) {
        LOGGER.info("Create ontology " + id);
        return manager.createOntology(id);
    }

    public static OntGraphModel copyOntModel(OntGraphModel original, String newURI) {
        String oldURI = getURI(original);
        if (newURI == null) newURI = oldURI != null ? oldURI + ".copy" : null;
        UnionGraph copy = new UnionGraph(original.getBaseGraph());
        original.imports().forEach(model -> copy.addGraph(model.getGraph()));
        OntGraphModel res = OntModelFactory.createModel(copy, OntModelFactory.getPersonality(original));
        if (newURI != null) {
            res.setNsPrefix("", newURI + "#");
        }
        res.add(original.getBaseModel().listStatements());
        res.setID(newURI);
        return res;
    }

    public static void setDefaultPrefixes(Model m) {
        m.setNsPrefix("owl", OWL.getURI());
        m.setNsPrefix("rdfs", RDFS.getURI());
        m.setNsPrefix("rdf", RDF.getURI());
        m.setNsPrefix("xsd", XSD.getURI());
    }

    public static String getURI(Model model) {
        if (model == null) return null;
        return Graphs.getURI(Graphs.getBase(model.getGraph()));
    }

    public static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
        compareAxioms(toMap(expected), toMap(actual));
    }

    public static void compareAxioms(Map<AxiomType, List<OWLAxiom>> expected, Map<AxiomType, List<OWLAxiom>> actual) {
        LOGGER.debug("[Compare] Expected axioms: ");
        expected.values().forEach(LOGGER::debug);
        LOGGER.debug("[Compare] Actual axioms: ");
        actual.values().forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect axiom types:", expected.keySet(), actual.keySet());
        List<String> errors = new ArrayList<>();
        for (AxiomType type : expected.keySet()) {
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
        Assert.assertTrue("There are " + errors.size() + " errors", errors.isEmpty());
    }

    public static Map<AxiomType, List<OWLAxiom>> toMap(Stream<? extends OWLAxiom> stream) {
        return toMap(stream.collect(Collectors.toList()));
    }

    public static Map<AxiomType, List<OWLAxiom>> toMap(List<? extends OWLAxiom> axioms) {
        Set<AxiomType> types = axioms.stream().map(OWLAxiom::getAxiomType).collect(Collectors.toSet());
        Map<AxiomType, List<OWLAxiom>> res = new HashMap<>();
        types.forEach(type -> {
            List<OWLAxiom> value = res.computeIfAbsent(type, t -> new ArrayList<>());
            List<OWLAxiom> byType = axioms.stream().filter(a -> type.equals(a.getAxiomType())).sorted().collect(Collectors.toList());
            value.addAll(byType);
        });
        return res;
    }

    public static boolean same(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex() && OWLAPIStreamUtils.equalStreams(replaceAnonymous(a.components()), replaceAnonymous(b.components()));
    }

    private static Stream<?> replaceAnonymous(Stream<?> stream) {
        return stream.map(o -> o instanceof OWLAnonymousIndividual ? ANONYMOUS_INDIVIDUAL : o);
    }

    @SuppressWarnings("unchecked")
    public static Stream<OWLAxiom> splitAxioms(OWLOntology o) {
        return o.axioms()
                .map(a -> a instanceof OWLNaryAxiom ? (Stream<OWLAxiom>) ((OWLNaryAxiom) a).splitToAnnotatedPairs().stream() : Stream.of(a))
                .flatMap(Function.identity()).distinct();
    }

    public static Configurable.Mode getMode(OntPersonality profile) {
        Configurable.Mode mode = null;
        if (OntModelConfig.ONT_PERSONALITY_STRICT.equals(profile)) {
            mode = Configurable.Mode.STRICT;
        } else if (OntModelConfig.ONT_PERSONALITY_MEDIUM.equals(profile)) {
            mode = Configurable.Mode.MEDIUM;
        } else if (OntModelConfig.ONT_PERSONALITY_LAX.equals(profile)) {
            mode = Configurable.Mode.LAX;
        } else {
            Assert.fail("Unsupported personality profile " + profile);
        }
        return mode;
    }

    /**
     * gets 'punnings' for rdf:Property types (owl:AnnotationProperty, owl:DatatypeProperty, owl:ObjectProperty)
     *
     * @param model {@link Model}
     * @param mode  {@link Configurable.Mode}
     * @return Set of resources
     */
    public static Set<Resource> getPropertyPunnings(Model model, Configurable.Mode mode) {
        if (Configurable.Mode.LAX.equals(mode)) return Collections.emptySet();
        Set<Resource> objectProperties = model.listStatements(null, RDF.type, OWL.ObjectProperty).mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypeProperties = model.listStatements(null, RDF.type, OWL.DatatypeProperty).mapWith(Statement::getSubject).toSet();
        if (Configurable.Mode.MEDIUM.equals(mode)) return unionOfIntersections(objectProperties, datatypeProperties);
        Set<Resource> annotationProperties = model.listStatements(null, RDF.type, OWL.AnnotationProperty).mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(annotationProperties, objectProperties, datatypeProperties);
    }

    /**
     * gets 'punnings' for rdfs:Class types (owl:Class and rdfs:Datatype)
     *
     * @param model {@link Model}
     * @param mode  {@link Configurable.Mode}
     * @return Set of resources
     */
    public static Set<Resource> getClassPunnings(Model model, Configurable.Mode mode) {
        if (Configurable.Mode.LAX.equals(mode)) return Collections.emptySet();
        Set<Resource> classes = model.listStatements(null, RDF.type, OWL.Class).mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypes = model.listStatements(null, RDF.type, RDFS.Datatype).mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(classes, datatypes);
    }

    /**
     * gets the set of 'illegal punnings' from their explicit declaration accordingly specified mode.
     *
     * @param model {@link Model}
     * @param mode  {@link Configurable.Mode}
     * @return Set of illegal punnings
     */
    public static Set<Resource> getIllegalPunnings(Model model, Configurable.Mode mode) {
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
}
