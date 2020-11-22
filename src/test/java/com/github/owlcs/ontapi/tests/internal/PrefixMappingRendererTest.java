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

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.internal.PrefixMappingRenderer;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.tests.ModelData;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * To test {@link com.github.owlcs.ontapi.internal.PrefixMappingRenderer}.
 */
public class PrefixMappingRendererTest {

    @Test
    public void testAxiomsToString() {
        OntologyManager m = OntManagers.createManager();
        DataFactory df = m.getOWLDataFactory();
        ModelData data = ModelData.FAMILY;
        Ontology o = (Ontology) data.fetch(m);

        OWLAxiom a1 = o.annotationAssertionAxioms(IRI.create(data.getNS() + "m138"))
                .findFirst().orElseThrow(AssertionError::new);
        OWLAxiom a2 = o.dataPropertyRangeAxioms(df.getOWLDataProperty(data.getNS() + "knownAs"))
                .findFirst().orElseThrow(AssertionError::new);
        OWLAxiom a3 = o.dataPropertyAssertionAxioms(df.getOWLNamedIndividual(data.getNS() + "m103"))
                .findFirst().orElseThrow(AssertionError::new);
        OWLAxiom a4 = o.declarationAxioms(df.getOWLClass(data.getNS() + "Ancestor"))
                .findFirst().orElseThrow(AssertionError::new);
        OWLAxiom a5 = o.equivalentClassesAxioms(df.getOWLClass(data.getNS() + "Woman"))
                .findFirst().orElseThrow(AssertionError::new);

        Assertions.assertEquals("AnnotationAssertion(rdfs:comment :m138 \"a breeding relationship\")", a1.toString());
        Assertions.assertEquals("DataPropertyRange(:knownAs xsd:string)", a2.toString());
        Assertions.assertEquals("DataPropertyAssertion(:hasMarriageYear :m103 \"1895\"^^xsd:integer)", a3.toString());
        Assertions.assertEquals("Declaration(Class(:Ancestor))", a4.toString());
        Assertions.assertEquals("EquivalentClasses(:Woman " +
                "ObjectIntersectionOf(:Person ObjectSomeValuesFrom(:hasSex :Female)))", a5.toString());

        o.asGraphModel()
                .removeNsPrefix("rdfs").setNsPrefix("r", RDFS.getURI())
                .removeNsPrefix("").setNsPrefix("f", data.getNS())
                .removeNsPrefix("xsd").setNsPrefix("x", XSD.getURI());

        Assertions.assertEquals("AnnotationAssertion(r:comment f:m138 \"a breeding relationship\")", a1.toString());
        Assertions.assertEquals("DataPropertyRange(f:knownAs x:string)", a2.toString());
        Assertions.assertEquals("DataPropertyAssertion(f:hasMarriageYear f:m103 \"1895\"^^x:integer)", a3.toString());
        Assertions.assertEquals("Declaration(Class(f:Ancestor))", a4.toString());
        Assertions.assertEquals("EquivalentClasses(f:Woman " +
                "ObjectIntersectionOf(f:Person ObjectSomeValuesFrom(f:hasSex f:Female)))", a5.toString());
    }

    @Test
    public void testOntologyToString() {
        OntologyManager m = OntManagers.createManager();
        ModelData data = ModelData.PIZZA;
        Ontology o = (Ontology) data.fetch(m);
        PrefixMappingRenderer pmr = new PrefixMappingRenderer(o.asGraphModel());
        Assertions.assertEquals("Ontology(OntologyID(OntologyIRI(<http://www.co-ode.org/ontologies/pizza/pizza.owl>) " +
                "VersionIRI(<null>)) [Axioms: 945] [Logical axioms: 712])", pmr.render(o));
    }
}
