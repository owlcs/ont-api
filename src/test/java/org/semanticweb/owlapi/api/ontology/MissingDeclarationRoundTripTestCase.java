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
package org.semanticweb.owlapi.api.ontology;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;
import ru.avicomp.owlapi.OWLManager;


@SuppressWarnings("javadoc")
public class MissingDeclarationRoundTripTestCase extends TestBase {

    /**
     * ONT-API comment:
     * There is no possibility to ignore some axioms if they are present in the graph.
     * And I can't imagine why such functionality is invited by the OWL-API developers.
     * But in ONT-API we can use pure OWL-API mechanisms for loading and saving ontology to reproduce such strange behaviour.
     *
     * @throws Exception
     */
    @Test
    public void shouldFindOneAxiom() throws Exception {
        OWLAnnotationProperty p = OWLFunctionalSyntaxFactory.AnnotationProperty(IRI.create("http://test.org/MissingDeclaration.owl#", "p"));
        OWLOntology o1 = createOntology(p);
        Assert.assertTrue(o1.containsAnnotationPropertyInSignature(p.getIRI()));
        Assert.assertEquals(1, o1.getAxiomCount());
        RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();
        format.setAddMissingTypes(false);
        StringDocumentTarget target = new StringDocumentTarget();
        new RDFXMLStorerFactory().createStorer().storeOntology(o1, target, format);
        LOGGER.debug("Target:\n{}", target);
        OWLOntologyLoaderConfiguration conf = new OWLOntologyLoaderConfiguration().setStrict(true);
        if (!OWLManager.DEBUG_USE_OWL) {
            conf = ru.avicomp.ontapi.OntologyFactoryImpl.asONT(conf).setUseOWLParsersToLoad(true);
        }
        OWLOntology o2 = setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(target), conf);
        o2.axioms().forEach(a -> LOGGER.debug("{}", a));
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(o2);
        Assert.assertFalse(o2.containsAnnotationPropertyInSignature(p.getIRI()));
        Assert.assertEquals(0, o2.getAxiomCount());
    }

    private OWLOntology createOntology(OWLAnnotationProperty p) {
        OWLClass a = OWLFunctionalSyntaxFactory.Class(IRI.create("http://test.org/MissingDeclaration.owl#", "A"));
        OWLOntology ontology = getOWLOntology();
        OWLAxiom axiom = OWLFunctionalSyntaxFactory.AnnotationAssertion(p, a.getIRI(), OWLFunctionalSyntaxFactory.Literal("Hello"));
        ontology.add(axiom);
        return ontology;
    }
}
