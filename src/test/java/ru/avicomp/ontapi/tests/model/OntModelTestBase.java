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

package ru.avicomp.ontapi.tests.model;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * base and utility class for ontology-model-tests
 * <p>
 * Created by @szuev on 02.10.2016.
 */
abstract class OntModelTestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(OntModelTestBase.class);

    public static void debug(OWLOntology ontology) {
        LOGGER.debug("DEBUG:");
        ReadWriteUtils.print(ontology, OntFormat.TURTLE);
        LOGGER.debug("Axioms:");
        ontology.axioms().map(String::valueOf).forEach(LOGGER::debug);
    }

    Stream<OWLAxiom> filterAxioms(OWLOntology ontology, AxiomType... excluded) {
        if (excluded.length == 0) return ontology.axioms();
        List<AxiomType> types = Stream.of(excluded).collect(Collectors.toList());
        return ontology.axioms().filter(axiom -> !types.contains(axiom.getAxiomType()));
    }

    void checkAxioms(OntologyModel original, AxiomType... excluded) {
        LOGGER.debug("Load ontology to another manager from jena graph.");
        OWLOntologyManager manager = OntManagers.createOWL();
        OWLOntology result = ReadWriteUtils.convertJenaToOWL(manager, original.asGraphModel());
        LOGGER.debug("All (actual) axioms from reloaded ontology[OWL]:");
        result.axioms().map(String::valueOf).forEach(LOGGER::debug);
        Map<AxiomType, List<OWLAxiom>> expected = TestUtils.toMap(filterAxioms(original, excluded));
        Map<AxiomType, List<OWLAxiom>> actual = TestUtils.toMap(filterAxioms(result, excluded));
        TestUtils.compareAxioms(expected, actual);
    }
}
