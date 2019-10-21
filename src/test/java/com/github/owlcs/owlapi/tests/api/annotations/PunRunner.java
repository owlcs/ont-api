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

package com.github.owlcs.owlapi.tests.api.annotations;

import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLAPIPreconditions;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Created by ses on 3/2/15.
 */

@SuppressWarnings("javadoc")
public class PunRunner extends org.junit.runner.Runner {

    private final Class<?> testClass;

    public PunRunner(Class<?> testClass) {
        this.testClass = testClass;
    }

    class TestSetting {

        OWLEntity[] entities;
        Class<? extends PrefixDocumentFormat> formatClass;
        OWLOntologyManager manager;

        TestSetting(Class<? extends PrefixDocumentFormat> formatClass,
                    OWLOntologyManager m,
                    OWLEntity... entities) {
            this.formatClass = formatClass;
            this.entities = entities;
            manager = m;
        }
    }

    private Description suiteDescription;
    private final Map<Description, TestSetting> testSettings = new HashMap<>();

    @Override
    public Description getDescription() {
        suiteDescription = Description.createSuiteDescription(testClass);
        addAllTests();
        return suiteDescription;
    }

    private void addAllTests() {
        DefaultPrefixManager pm = new DefaultPrefixManager("http://localhost#");
        OWLOntologyManager m = TestBase.createOWLManager();
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
                Description testDescription = Description.createTestDescription(testClass, name);
                testSettings.put(testDescription, new TestSetting(formatClass, m, v.i, v.j));
                suiteDescription.addChild(testDescription);
            });
            String name = "multiPun for " + formatClass.getName();
            Description testDescription = Description.createTestDescription(testClass, name);
            suiteDescription.addChild(testDescription);
            TestSetting setting = new TestSetting(formatClass, m,
                    df.getOWLClass("a", pm),
                    df.getOWLDatatype("a", pm),
                    df.getOWLAnnotationProperty("a", pm),
                    df.getOWLDataProperty("a", pm),
                    df.getOWLObjectProperty("a", pm),
                    df.getOWLNamedIndividual("a", pm));
            testSettings.put(testDescription, setting);
        }
    }

    /**
     * Run the tests for this runner.
     *
     * @param notifier will be notified of events while tests are being run--tests being
     *                 started, finishing, and failing
     */
    @Override
    public void run(@Nullable RunNotifier notifier) {
        OWLAPIPreconditions.checkNotNull(notifier);
        assert notifier != null;
        for (Map.Entry<Description, TestSetting> entry : testSettings.entrySet()) {
            Description description = entry.getKey();
            notifier.fireTestStarted(description);
            try {
                TestSetting setting = entry.getValue();
                runTestForAnnotationsOnPunnedEntitiesForFormat(setting.formatClass, setting.manager, setting.entities);
            } catch (Throwable t) {
                notifier.fireTestFailure(new Failure(description, t));
            } finally {
                notifier.fireTestFinished(description);
            }
        }
    }

    private void runTestForAnnotationsOnPunnedEntitiesForFormat(Class<? extends PrefixDocumentFormat> formatClass,
                                                                OWLOntologyManager m,
                                                                OWLEntity... entities) throws Exception {
        if (!OWLManager.DEBUG_USE_OWL) {
            m.setOntologyLoaderConfiguration(((com.github.owlcs.ontapi.config.OntLoaderConfiguration) m.getOntologyLoaderConfiguration())
                    .setPersonality(com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig.ONT_PERSONALITY_LAX));
        }
        OWLOntologyManager ontologyManager;
        OWLDataFactory df;
        synchronized (OWLManager.class) {
            ontologyManager = m;
            ontologyManager.clearOntologies();
            df = ontologyManager.getOWLDataFactory();
        }
        OWLAnnotationProperty annotationProperty = df.getOWLAnnotationProperty(":ap", new DefaultPrefixManager("http://localhost#"));
        OWLOntology o = makeOwlOntologyWithDeclarationsAndAnnotationAssertions(annotationProperty, ontologyManager,
                entities);
        for (int i = 0; i < 10; i++) {
            PrefixDocumentFormat format = formatClass.newInstance();
            format.setPrefixManager(new DefaultPrefixManager("http://localhost#"));
            ByteArrayInputStream in = saveForRereading(o, format, ontologyManager);
            ontologyManager.removeOntology(o);
            o = ontologyManager.loadOntologyFromOntologyDocument(in);
        }
        Assert.assertEquals("annotationCount", entities.length, o.axioms(AxiomType.ANNOTATION_ASSERTION).count());
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
