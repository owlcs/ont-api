/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.utils.FileMap;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Collection of ontology resources for tests.
 * Created by @ssz on 19.04.2020.
 */
public enum ModelData {
    PIZZA("/ontapi/pizza.ttl"),
    FAMILY("/ontapi/family.ttl"),
    PEOPLE("/ontapi/people.ttl"),
    CAMERA("/ontapi/camera.ttl"),
    KOALA("/ontapi/koala.ttl"),
    TRAVEL("/ontapi/travel.ttl"),
    WINE("/ontapi/wine.ttl") {
        @Override
        String getName() {
            return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine";
        }

        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            return load(manager, FOOD, this);
        }
    },
    FOOD("/ontapi/food.ttl") {
        @Override
        String getName() {
            return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food";
        }

        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            return load(manager, WINE, this);
        }
    },
    NCBITAXON_CUT("/ontapi/ncbitaxon2.ttl"),
    HP_CUT("/ontapi/hp-cut.ttl"),
    FAMILY_PEOPLE_UNION(null) {
        @Override
        public OWLOntologyDocumentSource getDocumentSource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            String ns = "http://www.ex.org/tribe#";
            OWLOntology family = load(manager, FAMILY);
            OWLOntology people = load(manager, PEOPLE);
            OWLOntology res;
            try {
                res = manager.createOntology();
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
            IRI peopleIRI = getIRI(people);
            IRI familyIRI = getIRI(family);
            String familyNS = familyIRI.getIRIString() + "#";
            String peopleNS = peopleIRI.getIRIString() + "#";

            OWLDataFactory df = manager.getOWLDataFactory();
            OWLClass foremother = df.getOWLClass(ns + "foremother");
            OWLClass super_bus_company = df.getOWLClass(ns + "super_bus_company");
            OWLNamedIndividual i1 = df.getOWLNamedIndividual(ns + "Eva");
            OWLAnonymousIndividual i2 = df.getOWLAnonymousIndividual();
            OWLDataProperty dp = df.getOWLDataProperty(familyNS + "alsoKnownAs");

            manager.applyChange(new AddImport(family, df.getOWLImportsDeclaration(peopleIRI)));
            manager.applyChange(new AddImport(res, df.getOWLImportsDeclaration(familyIRI)));

            res.add(df.getOWLDeclarationAxiom(foremother));
            res.add(df.getOWLDeclarationAxiom(super_bus_company));
            res.add(df.getOWLSubClassOfAxiom(super_bus_company, df.getOWLClass(peopleNS + "bus_company")));
            res.add(df.getOWLEquivalentClassesAxiom(foremother,
                    df.getOWLObjectIntersectionOf(df.getOWLClass(familyNS + "Woman"),
                            df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(familyNS + "isForemotherOf"),
                                    df.getOWLClass(familyNS + "Person")))));
            res.add(df.getOWLDeclarationAxiom(i1));
            res.add(df.getOWLClassAssertionAxiom(df.getOWLClass(familyNS + "Foremother"), i1));
            res.add(df.getOWLClassAssertionAxiom(foremother, i2));
            res.add(df.getOWLAnnotationAssertionAxiom(i2, df.getRDFSComment("This is Eva")));
            res.add(df.getOWLDataPropertyAssertionAxiom(dp, i1, df.getOWLLiteral("Eve")));
            res.add(df.getOWLDataPropertyAssertionAxiom(dp, i2, df.getOWLLiteral("Eve")));

            if (res instanceof Ontology) { // for debug
                ((Ontology) res).asGraphModel().setNsPrefix("p", peopleNS).setNsPrefix("f", familyNS).setNsPrefix("t", ns);
            }
            return res;
        }
    },
    ;
    private final Path file;
    private final OntFormat format;

    ModelData(String file) {
        this(file, OntFormat.TURTLE);
    }

    ModelData(String file, OntFormat format) {
        this.file = file == null ? null : toPath(file);
        this.format = format;
    }

    private static Path toPath(String file) {
        try {
            return Paths.get(ModelData.class.getResource(file).toURI()).toRealPath();
        } catch (IOException | URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static OWLOntology load(OWLOntologyManager manager, ModelData... data) {
        OWLOntology res = null;
        OWLOntologyLoaderConfiguration conf = createConfig(manager);
        long before = manager.ontologies().count();
        if (!(manager instanceof OntologyManager)) { // OWL-API
            manager.setOntologyLoaderConfiguration(conf);
            PriorityCollection<OWLOntologyIRIMapper> maps = manager.getIRIMappers();
            Arrays.stream(data)
                    .map(d -> FileMap.create(IRI.create(d.getName()), d.getDocumentSource().getDocumentIRI()))
                    .forEach(maps::add);
            try {
                res = manager.loadOntology(IRI.create(data[data.length - 1].getName()));
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        } else { // ONT-API
            for (ModelData d : data) {
                try {
                    res = manager.loadOntologyFromOntologyDocument(d.getDocumentSource(), conf);
                } catch (OWLOntologyCreationException e) {
                    throw new AssertionError(e);
                }
            }
        }
        Assertions.assertEquals(data.length + before, manager.ontologies().count());
        return res;
    }

    static OWLOntologyLoaderConfiguration createConfig(OWLOntologyManager manager) {
        OWLOntologyLoaderConfiguration conf = manager.getOntologyLoaderConfiguration();
        if (manager instanceof OntologyManager) {
            conf = OWLAdapter.get().asONT(conf).setProcessImports(false).setPerformTransformation(false);
        } else {
            conf = conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        }
        return conf;
    }

    static IRI getIRI(OWLOntology ont) {
        return ont.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
    }

    public OWLOntology fetch(OWLOntologyManager manager) {
        return fetch(manager, createConfig(manager));
    }

    OWLOntology fetch(OWLOntologyManager manager, OWLOntologyLoaderConfiguration conf) {
        try {
            return manager.loadOntologyFromOntologyDocument(getDocumentSource(), conf);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public OWLOntologyDocumentSource getDocumentSource() {
        return new FileDocumentSource(file.toFile(), getDocumentFormat());
    }

    public OWLDocumentFormat getDocumentFormat() {
        return format.createOwlFormat();
    }

    String getName() {
        return name();
    }
}
