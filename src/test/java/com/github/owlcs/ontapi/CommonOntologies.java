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

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.testutils.FileMap;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDataProperty;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Collection of ontology resources for tests.
 * Created by @ssz on 19.04.2020.
 */
public enum CommonOntologies {
    PIZZA("/ontapi/pizza.ttl", "http://www.co-ode.org/ontologies/pizza/pizza.owl"),
    FAMILY("/ontapi/family.ttl", "http://www.co-ode.org/roberts/family-tree.owl"),
    PEOPLE("/ontapi/people.ttl", "http://owl.man.ac.uk/2006/07/sssw/people"),
    CAMERA("/ontapi/camera.ttl", "http://www.xfront.com/owl/ontologies/camera/"),
    KOALA("/ontapi/koala.ttl", "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl"),
    TRAVEL("/ontapi/travel.ttl", "http://www.owl-ontologies.com/travel.owl"),
    WINE("/ontapi/wine.ttl", "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine") {
        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            return load(manager, FOOD, this);
        }
    },
    FOOD("/ontapi/food.ttl", "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food") {
        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            return load(manager, WINE, this);
        }
    },
    NCBITAXON_CUT("/ontapi/ncbitaxon2.ttl", "http://purl.bioontology.org/ontology/NCBITAXON/") {
        @Override
        public String getNS() {
            return getURI();
        }
    },
    HP_CUT("/ontapi/hp-cut.ttl", "http://purl.obolibrary.org/obo/hp.owl"),
    FAMILY_PEOPLE_UNION(null, null) {
        @Override
        public String getNS() {
            return "http://www.ex.org/tribe#";
        }

        @Override
        public OWLOntologyDocumentSource getDocumentSource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OWLOntology fetch(OWLOntologyManager manager) {
            return manager instanceof OntologyManager ?
                    CommonOntologies.createFamilyPeopleModelUsingONTAPI((OntologyManager) manager, getNS()) :
                    CommonOntologies.createFamilyPeopleModelUsingOWLAPI(manager, getNS());
        }
    },
    ;
    private final Path file;
    private final OntFormat format;
    private final String uri;

    CommonOntologies(String resource, String name) {
        this(resource, OntFormat.TURTLE, name);
    }

    CommonOntologies(String resource, OntFormat format, String name) {
        this.file = resource == null ? null : OWLIOUtils.getResourcePath(resource);
        this.format = format;
        this.uri = name;
    }

    static OWLOntology load(OWLOntologyManager manager, CommonOntologies... data) {
        OWLOntology res = null;
        OWLOntologyLoaderConfiguration conf = createConfig(manager);
        long before = manager.ontologies().count();
        if (!(manager instanceof OntologyManager)) { // OWL-API
            manager.setOntologyLoaderConfiguration(conf);
            PriorityCollection<OWLOntologyIRIMapper> maps = manager.getIRIMappers();
            Arrays.stream(data)
                    .map(d -> FileMap.create(IRI.create(d.getURI()), d.getDocumentSource().getDocumentIRI()))
                    .forEach(maps::add);
            try {
                res = manager.loadOntology(IRI.create(data[data.length - 1].getURI()));
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        } else { // ONT-API
            for (CommonOntologies d : data) {
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

    private static Ontology createFamilyPeopleModelUsingONTAPI(OntologyManager manager, String ns) {
        Ontology family = (Ontology) load(manager, FAMILY);
        Ontology people = (Ontology) load(manager, PEOPLE);

        String familyNS = getIRI(family).getIRIString() + "#";
        String peopleNS = getIRI(people).getIRIString() + "#";

        Ontology res = manager.createOntology();
        OntModel ont = res.asGraphModel().setNsPrefix("p", peopleNS).setNsPrefix("f", familyNS).setNsPrefix("t", ns);
        ont.addImport(family.asGraphModel().addImport(people.asGraphModel()));

        OntClass foremother = ont.createOntClass(ns + "foremother");
        OntClass super_bus_company = ont.createOntClass(ns + "super_bus_company");
        super_bus_company.addSuperClass(ont.getOntClass(peopleNS + "bus_company"));
        foremother.addEquivalentClass(ont.createObjectIntersectionOf(ont.getOntClass(familyNS + "Woman"),
                ont.createObjectSomeValuesFrom(ont.getObjectProperty(familyNS + "isForemotherOf"),
                        ont.getOntClass(familyNS + "Person"))));
        OntIndividual.Named i1 = ont.getOntClass(familyNS + "Foremother").createIndividual(ns + "Eva");
        OntIndividual.Anonymous i2 = foremother.createIndividual();
        i2.addComment("This is Eva");

        OntDataProperty dp = ont.getDataProperty(familyNS + "alsoKnownAs");
        i1.addAssertion(dp, ont.createLiteral("Eve"));
        i2.addAssertion(dp, ont.createLiteral("Eve"));
        return res;
    }

    private static OWLOntology createFamilyPeopleModelUsingOWLAPI(OWLOntologyManager manager, String ns) {
        OWLOntology family = load(manager, FAMILY);
        OWLOntology people = load(manager, PEOPLE);
        OWLOntology res;
        try {
            res = manager.createOntology();
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
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

    public String getURI() {
        return uri;
    }

    public String getNS() {
        return uri + "#";
    }
}
