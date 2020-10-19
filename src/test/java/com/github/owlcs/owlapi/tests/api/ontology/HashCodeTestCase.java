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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.ontapi.owlapi.objects.OWLLiteralImpl;
import com.github.owlcs.owlapi.OWLManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group
 */
@SuppressWarnings({"SameParameterValue"})
public class HashCodeTestCase {

    @Test
    public void testSetContainsInt() {
        OWLDatatype datatype = createDT(OWL2Datatype.XSD_INTEGER.getIRI());
        OWLLiteral litNoComp = createLiteral("3", datatype);
        OWLLiteral litNoComp2 = createLiteral("3", datatype);
        OWLLiteral litIntImpl = createLiteral(3);
        Assertions.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assertions.assertTrue(lncset.contains(litNoComp2));
        Assertions.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsDouble() {
        OWLDatatype datatype = createDT(OWL2Datatype.XSD_DOUBLE.getIRI());
        OWLLiteral litNoComp = createLiteral("3.0", datatype);
        OWLLiteral litNoComp2 = createLiteral("3.0", datatype);
        OWLLiteral litIntImpl = createLiteral(3.0D);
        Assertions.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assertions.assertTrue(lncset.contains(litNoComp2));
        Assertions.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsFloat() {
        OWLDatatype datatype = createDT(OWL2Datatype.XSD_FLOAT.getIRI());
        OWLLiteral litNoComp = createLiteral("3.0", datatype);
        OWLLiteral litNoComp2 = createLiteral("3.0", datatype);
        OWLLiteral litIntImpl = createLiteral(3.0F);
        Assertions.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assertions.assertTrue(lncset.contains(litNoComp2));
        Assertions.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsBoolean() {
        OWLDatatype datatype = createDT(OWL2Datatype.XSD_BOOLEAN.getIRI());
        OWLLiteral litNoComp = createLiteral("true", datatype);
        OWLLiteral litNoComp2 = createLiteral("true", datatype);
        OWLLiteral litIntImpl = createLiteral(true);
        Assertions.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assertions.assertTrue(lncset.contains(litNoComp2));
        Assertions.assertTrue(lncset.contains(litIntImpl));
    }

    private static OWLDatatype createDT(IRI iri) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLDatatypeImpl(iri) :
                new com.github.owlcs.ontapi.owlapi.objects.entity.OWLDatatypeImpl(iri);
    }

    private static OWLLiteral createLiteral(int i) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplInteger(i) :
                OWLLiteralImpl.createLiteral(i);
    }

    private static OWLLiteral createLiteral(double d) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplDouble(d) :
                OWLLiteralImpl.createLiteral(d);
    }

    private static OWLLiteral createLiteral(float f) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplFloat(f) :
                OWLLiteralImpl.createLiteral(f);
    }

    private static OWLLiteral createLiteral(boolean b) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplBoolean(b) :
                OWLLiteralImpl.createLiteral(b);
    }

    private static OWLLiteral createLiteral(String s, OWLDatatype d) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression(s, null, d) :
                OWLLiteralImpl.createLiteral(s, d);
    }
}
