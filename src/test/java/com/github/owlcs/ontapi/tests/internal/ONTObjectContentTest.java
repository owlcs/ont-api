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

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Created by @szz on 12.09.2019.
 */
public class ONTObjectContentTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testAnnotatedSubClassOf() {
        Ontology o = OntManagers.createONT().createOntology();
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
        Ontology o = m.createOntology();
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
}
