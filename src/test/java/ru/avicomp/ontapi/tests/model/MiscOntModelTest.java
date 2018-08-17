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

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.StringInputStreamDocumentSource;

/**
 * For testing miscellaneous general model functionality.
 *
 * Created by @szuev on 20.07.2018.
 */
public class MiscOntModelTest extends OntModelTestBase {

    @Test
    public void testLoadDisjointUnion() throws OWLOntologyCreationException {
        OWLOntologyManager m = OntManagers.createONT();
        String s = "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xml:   <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<c1>    a       owl:Class .\n" +
                "<c4>    a       owl:Class .\n" +
                "<c2>    a       owl:Class .\n" +
                "[ a       owl:Ontology ] .\n" +
                "<c0>    a                    owl:Class ;\n" +
                "        owl:disjointUnionOf  ( <c4> <c3> ) ;\n" +
                "        owl:disjointUnionOf  ( <c2> <c1> <c1> ) .\n" +
                "<c3>    a       owl:Class .";

        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringInputStreamDocumentSource(s, OntFormat.TURTLE));
        debug(o);
        Assert.assertEquals(2, o.axioms(AxiomType.DISJOINT_UNION).count());
        Assert.assertEquals(7, o.axioms().count());
    }

    @Test
    public void testLoadHasKey() throws OWLOntologyCreationException {
        OWLOntologyManager m = OntManagers.createONT();
        String s = "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xml:   <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<d2>    a       owl:DatatypeProperty .\n" +
                "<C>     a           owl:Class ;\n" +
                "        owl:hasKey  ( <p2> <p2> <d1> <d3> ) ;\n" +
                "        owl:hasKey  ( <p1> <p2> <d1> <d2> ) .\n" +
                "[ a       owl:Ontology ] .\n" +
                "<d3>    a       owl:ObjectProperty .\n" +
                "<p1>    a       owl:ObjectProperty .\n" +
                "<d1>    a       owl:DatatypeProperty .\n" +
                "<p2>    a       owl:ObjectProperty .";
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringInputStreamDocumentSource(s, OntFormat.TURTLE));
        debug(o);
        Assert.assertEquals(2, o.axioms(AxiomType.HAS_KEY).count());
        Assert.assertEquals(8, o.axioms().count());
    }

    @Test
    public void testInverseOfObjectProperties() throws OWLOntologyCreationException {
        OWLOntologyManager manager = OntManagers.createONT();
        OWLDataFactory df = manager.getOWLDataFactory();
        OWLOntology o = manager.createOntology();
        OWLObjectProperty p1 = df.getOWLObjectProperty(IRI.create("p1"));
        OWLObjectProperty p2 = df.getOWLObjectProperty(IRI.create("p2"));
        OWLObjectInverseOf inv = df.getOWLObjectInverseOf(p1);
        o.add(df.getOWLDeclarationAxiom(p1));
        o.add(df.getOWLObjectPropertyAssertionAxiom(inv, df.getOWLNamedIndividual(IRI.create("I1")), df.getOWLNamedIndividual(IRI.create("I2"))));
        o.add(df.getOWLFunctionalObjectPropertyAxiom(inv));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(inv));
        o.add(df.getOWLObjectPropertyAssertionAxiom(p2, df.getOWLNamedIndividual(IRI.create("I1")), df.getOWLNamedIndividual(IRI.create("I2"))));
        o.add(df.getOWLInverseObjectPropertiesAxiom(p2, p1));
        debug(o);

        Assert.assertEquals(3, o.axioms()
                .filter(a -> OwlObjects.objects(OWLObjectInverseOf.class, a).findAny().isPresent())
                .peek(x -> LOGGER.debug("AxiomWithInverseOf: {}", x)).count());

        Assert.assertEquals(2, ((OntologyModel) o).asGraphModel().statements(null, OWL.inverseOf, null).count());
    }

    @Test
    public void testAnonymousIndividuals() {
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory df = manager.getOWLDataFactory();
        OntologyModel o = manager.createOntology();

        OntGraphModel m = o.asGraphModel();
        OntIndividual ti1 = m.getOWLThing().createIndividual();
        OWLAnonymousIndividual ni1 = df.getOWLAnonymousIndividual();
        o.add(df.getOWLClassAssertionAxiom(df.getOWLNothing(), ni1));
        ReadWriteUtils.print(m);
        LOGGER.debug("Put: {}, {}", ti1, ni1);

        OWLAnonymousIndividual ti2 = o.classAssertionAxioms(df.getOWLThing()).findFirst().orElseThrow(AssertionError::new)
                .getIndividual().asOWLAnonymousIndividual();
        OntIndividual ni2 = m.getOWLNothing().individuals().findFirst().orElseThrow(AssertionError::new);
        LOGGER.debug("Get: {}, {}", ti2, ni2);

        Assert.assertEquals("_:" + ti1.getId().getLabelString(), ti2.toStringID());
        Assert.assertEquals(ni1.toStringID(), "_:" + ni2.getId().getLabelString());
    }
}
