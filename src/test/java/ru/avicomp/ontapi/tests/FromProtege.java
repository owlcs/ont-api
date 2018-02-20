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

package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * To test a bug found by protege tests.
 * See <a href='https://github.com/protegeproject/protege/blob/master/protege-editor-owl/src/test/java/org/protege/editor/owl/model/hierarchy/AssertedClassHierarchyTest.java#L101'>org.protege.editor.owl.model.hierarchy#testAddGCA()</a>
 * <p>
 * Created by @szuev on 20.02.2018.
 */
public class FromProtege {
    private static final Logger LOGGER = LoggerFactory.getLogger(FromProtege.class);

    private Function<OWLOntologyManager, OWLOntology> creator = manager -> {
        try {
            return create(manager);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Can't create ontology", e);
        }
    };

    @Test
    public void testAxiomsByClass() {
        OWLOntology owl = creator.apply(OntManagers.createOWL());
        OWLOntology ont = creator.apply(OntManagers.createONT());
        test(ont, owl);
    }

    public static void test(OWLOntology a, OWLOntology b) {
        Set<OWLClass> classes_a = a.classesInSignature().collect(Collectors.toSet());
        Set<OWLClass> classes_b = b.classesInSignature().collect(Collectors.toSet());
        Assert.assertEquals("Wrong classes list", classes_a, classes_b);

        Map<OWLClass, Set<OWLAxiom>> axioms_a = getAxiomsByClass(classes_a, a);
        Map<OWLClass, Set<OWLAxiom>> axioms_b = getAxiomsByClass(classes_a, b);

        classes_a.forEach(c -> Assert.assertEquals(toString(a) + " - wrong axioms for " + c, axioms_a.get(c), axioms_b.get(c)));
    }

    private static Map<OWLClass, Set<OWLAxiom>> getAxiomsByClass(Set<OWLClass> classes, OWLOntology o) {
        return classes.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        c -> o.axioms(c, Imports.EXCLUDED).collect(Collectors.toSet())));
    }

    private static String toString(OWLOntology o) {
        IRI iri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        return "[" + (o instanceof OntologyModel ? "ONT" : "OWL") + "]:" + iri;
    }

    public static OWLOntology create(OWLOntologyManager manager) throws OWLOntologyCreationException {
        OWLDataFactory factory = OntManagers.getDataFactory();
        String url = "http://test.com/axioms-tester";
        String ns = url + "#";
        OWLOntology ontology = manager.createOntology(IRI.create(url));
        OWLClass x = factory.getOWLClass(IRI.create(ns + "X"));
        OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(ns + "p"));
        OWLClass y = factory.getOWLClass(IRI.create(ns + "Y"));
        OWLClass z = factory.getOWLClass(IRI.create(ns + "Z"));
        OWLAxiom gca = factory.getOWLSubClassOfAxiom(factory.getOWLObjectIntersectionOf(x, factory.getOWLObjectSomeValuesFrom(p, y)), z);
        OWLOntologyChange change = new AddAxiom(ontology, gca);
        manager.applyChange(change);

        ontology.axioms().forEach(a -> LOGGER.debug("{}", a));
        return ontology;
    }
}
