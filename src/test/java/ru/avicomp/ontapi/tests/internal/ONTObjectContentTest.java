/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.internal;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.List;

/**
 * Created by @szz on 12.09.2019.
 */
public class ONTObjectContentTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testAnnotatedSubClassOf() {
        OntologyModel o = OntManagers.createONT().createOntology();
        OntGraphModel g = o.asGraphModel();
        g.createOntClass("X").addSubClassOfStatement(g.createOntClass("Y")).annotate(g.getRDFSComment(), "XY");
        ReadWriteUtils.print(g);

        Assert.assertEquals(3, o.getAxiomCount());
        OWLSubClassOfAxiom owl = o.axioms(AxiomType.SUBCLASS_OF).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(owl.isAnnotated());

        ONTObject<OWLSubClassOfAxiom> ont = (ONTObject<OWLSubClassOfAxiom>) owl;
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertEquals(8, res.size());

        o.remove(ont.getOWLObject());
        Assert.assertEquals(3, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAnnotatedDeclaration() {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();
        g.createOntClass("X").addComment("X");
        ReadWriteUtils.print(g);

        Assert.assertEquals(1, o.getAxiomCount());
        OWLDeclarationAxiom owl = o.axioms(AxiomType.DECLARATION).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(owl.isAnnotated());

        ONTObject<OWLDeclarationAxiom> ont = (ONTObject<OWLDeclarationAxiom>) owl;
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertEquals(2, res.size());

        o.remove(ont.getOWLObject());
        Assert.assertEquals(1, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMergedHeaderAnnotations() {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();
        // two identical annotations, but one is assertion, and the second one is bulk
        g.getID().addComment("x");
        g.asStatement(g.getID().getRoot().asTriple()).annotate(g.getRDFSComment(), "x");
        ReadWriteUtils.print(g);

        // in OWL-view must be one (merged) annotation:
        List<OWLAnnotation> owl = o.annotationsAsList();
        Assert.assertEquals(1, owl.size());

        ONTObject<OWLAnnotation> ont = (ONTObject<OWLAnnotation>) owl.get(0);
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        ReadWriteUtils.print(res);

        m.applyChange(new RemoveOntologyAnnotation(o, ont.getOWLObject()));
        Assert.assertEquals(1, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEquivalentClassesMerge() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntClass x = g.createOntClass("X");
        OntClass y = g.createOntClass("Y");
        OntClass z = g.createOntClass("Z");
        x.addEquivalentClass(y.addEquivalentClass(x)).addEquivalentClass(z);
        ReadWriteUtils.print(g);

        Assert.assertEquals(5, o.axioms().count());

        DataFactory df = m.getOWLDataFactory();
        OWLEquivalentClassesAxiom xz = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                .filter(a -> a.contains(df.getOWLClass(z.getURI()))).findFirst().orElseThrow(AssertionError::new);
        OWLEquivalentClassesAxiom xy = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                .filter(a -> a.contains(df.getOWLClass(y.getURI()))).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(xy.containsEntityInSignature(df.getOWLClass(x.getURI())));

        ONTObject<OWLEquivalentClassesAxiom> xzOnt = (ONTObject<OWLEquivalentClassesAxiom>) xz;
        ONTObject<OWLEquivalentClassesAxiom> xyOnt = (ONTObject<OWLEquivalentClassesAxiom>) xy;

        Assert.assertEquals(3, xzOnt.triples().count());

        // can't test carefully, since no method to get value (merged axiom), only keys are available:
        Assert.assertEquals(3, xyOnt.triples().count());
        // but can delete axiom with all its triples
        o.remove(xyOnt.getOWLObject());

        ReadWriteUtils.print(g);
        Assert.assertEquals(4, o.axioms().count());
        Assert.assertEquals(1, o.axioms(AxiomType.EQUIVALENT_CLASSES).count());
        // header + "<X> owl:equivalentClass <Z>" + 3 declarations
        Assert.assertEquals(5, g.size());
        Assert.assertEquals(1, g.statements(null, OWL.equivalentClass, null).count());
    }

    @Test
    public void testInverseObjectPropertiesAxiomMerge() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntOPE x = g.createObjectProperty("X");
        OntOPE y = g.createObjectProperty("Y");
        x.addInverseProperty(y.addInverseProperty(x));

        // header + 2 declarations + 2 owl:inverseOf
        Assert.assertEquals(5, g.size());

        Assert.assertEquals(3, o.axioms().count());

        OWLInverseObjectPropertiesAxiom a = o.axioms(AxiomType.INVERSE_OBJECT_PROPERTIES)
                .findFirst().orElseThrow(AssertionError::new);
        o.remove(a);

        Assert.assertEquals(2, o.axioms().count());
        // header + 2 declarations
        Assert.assertEquals(3, g.size());
    }
}
