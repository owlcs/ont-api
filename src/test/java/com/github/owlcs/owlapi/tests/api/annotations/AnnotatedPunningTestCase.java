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

package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ses on 5/13/14.
 */
public class AnnotatedPunningTestCase extends TestBase {

    private static Arguments of(String testName,
                                Class<? extends PrefixDocumentFormat> formatType,
                                OWLEntity... entities) {
        return Arguments.of(testName, formatType, entities);
    }

    public static List<Arguments> addAllTests() {
        List<Arguments> res = new ArrayList<>();

        DefaultPrefixManager pm = new DefaultPrefixManager("http://localhost#");
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = m.getOWLDataFactory();
        List<? extends OWLEntity> entities = Arrays.asList(df.getOWLClass("a", pm), df.getOWLDatatype("a", pm),
                df.getOWLAnnotationProperty("a", pm), df.getOWLDataProperty("a", pm), df.getOWLObjectProperty("a", pm),
                df.getOWLNamedIndividual("a", pm));
        List<Class<? extends PrefixDocumentFormat>> formats = new ArrayList<>();
        formats.add(RDFXMLDocumentFormat.class);
        formats.add(TurtleDocumentFormat.class);
        formats.add(FunctionalSyntaxDocumentFormat.class);
        formats.add(ManchesterSyntaxDocumentFormat.class);

        for (Class<? extends PrefixDocumentFormat> formatClass : formats) {
            OWLAPIStreamUtils.pairs(entities).forEach(v -> {
                String formatClassName = formatClass.getName();
                int i1 = formatClassName.lastIndexOf('.');
                if (i1 > -1) {
                    formatClassName = formatClassName.substring(i1 + 1);
                }
                String name = String.format("%sVs%sFor%s", v.i.getEntityType(), v.j.getEntityType(), formatClassName);
                res.add(of(name, formatClass, v.i, v.j));
            });
            String name = "multiPun for " + formatClass.getName();
            res.add(of(name, formatClass,
                    df.getOWLClass("a", pm),
                    df.getOWLDatatype("a", pm),
                    df.getOWLAnnotationProperty("a", pm),
                    df.getOWLDataProperty("a", pm),
                    df.getOWLObjectProperty("a", pm),
                    df.getOWLNamedIndividual("a", pm)));
        }
        return res;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("addAllTests")
    public void testForAnnotationsOnPunnedEntitiesForFormat(@SuppressWarnings("unused") String name,
                                                            Class<? extends PrefixDocumentFormat> formatType,
                                                            OWLEntity... entities) throws Exception {
        runTestForAnnotationsOnPunnedEntitiesForFormat(formatType, OWLManager.createOWLOntologyManager(), entities);
    }

    private void runTestForAnnotationsOnPunnedEntitiesForFormat(Class<? extends PrefixDocumentFormat> formatClass,
                                                                OWLOntologyManager m,
                                                                OWLEntity... entities) throws Exception {
        if (!OWLManager.DEBUG_USE_OWL) {
            m.setOntologyLoaderConfiguration(((com.github.owlcs.ontapi.config.OntLoaderConfiguration) m.getOntologyLoaderConfiguration())
                    .setPersonality(com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig.ONT_PERSONALITY_LAX));
        }
        OWLDataFactory df = m.getOWLDataFactory();
        OWLAnnotationProperty annotationProperty = df.getOWLAnnotationProperty(":ap", new DefaultPrefixManager("http://localhost#"));
        OWLOntology o = makeOwlOntologyWithDeclarationsAndAnnotationAssertions(annotationProperty, m, entities);
        for (int i = 0; i < 10; i++) {
            PrefixDocumentFormat format = formatClass.getConstructor().newInstance();
            format.setPrefixManager(new DefaultPrefixManager("http://localhost#"));
            ByteArrayInputStream in = saveForRereading(o, format, m);
            m.removeOntology(o);
            o = m.loadOntologyFromOntologyDocument(in);
        }
        Assertions.assertEquals(entities.length, o.axioms(AxiomType.ANNOTATION_ASSERTION).count(), "annotationCount");
    }

    private static OWLOntology makeOwlOntologyWithDeclarationsAndAnnotationAssertions(
            OWLAnnotationProperty annotationProperty, OWLOntologyManager manager, OWLEntity... entities)
            throws OWLOntologyCreationException {
        Set<OWLAxiom> axioms = new HashSet<>();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        axioms.add(dataFactory.getOWLDeclarationAxiom(annotationProperty));
        for (OWLEntity entity : entities) {
            axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(annotationProperty, entity.getIRI(),
                    dataFactory.getOWLAnonymousIndividual()));
            axioms.add(dataFactory.getOWLDeclarationAxiom(entity));
        }
        return manager.createOntology(axioms);
    }

    private static ByteArrayInputStream saveForRereading(OWLOntology o,
                                                         PrefixDocumentFormat format,
                                                         OWLOntologyManager manager) throws OWLOntologyStorageException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        manager.saveOntology(o, format, out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
