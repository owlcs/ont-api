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
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLDatatypeImpl;
import ru.avicomp.owlapi.OWLManager;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics
 * Research Group
 * @since 3.2.0
 */
@SuppressWarnings({"javadoc", "SameParameterValue"})
public class HashCodeTestCase {

    @Test
    public void testSetContainsInt() {
        OWLDatatypeImpl datatype = new OWLDatatypeImpl(OWL2Datatype.XSD_INTEGER.getIRI());
        OWLLiteral litNoComp = create("3", datatype);
        OWLLiteral litNoComp2 = create("3", datatype);
        OWLLiteral litIntImpl = create(3);
        Assert.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assert.assertTrue(lncset.contains(litNoComp2));
        Assert.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsDouble() {
        OWLDatatypeImpl datatype = new ru.avicomp.ontapi.owlapi.objects.entity.OWLDatatypeImpl(OWL2Datatype.XSD_DOUBLE.getIRI());
        OWLLiteral litNoComp = create("3.0", datatype);
        OWLLiteral litNoComp2 = create("3.0", datatype);
        OWLLiteral litIntImpl = create(3.0D);
        Assert.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assert.assertTrue(lncset.contains(litNoComp2));
        Assert.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsFloat() {
        OWLDatatypeImpl datatype = new OWLDatatypeImpl(OWL2Datatype.XSD_FLOAT.getIRI());
        OWLLiteral litNoComp = create("3.0", datatype);
        OWLLiteral litNoComp2 = create("3.0", datatype);
        OWLLiteral litIntImpl = create(3.0F);
        Assert.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assert.assertTrue(lncset.contains(litNoComp2));
        Assert.assertTrue(lncset.contains(litIntImpl));
    }

    @Test
    public void testSetContainsBoolean() {
        OWLDatatypeImpl datatype = new OWLDatatypeImpl(OWL2Datatype.XSD_BOOLEAN.getIRI());
        OWLLiteral litNoComp = create("true", datatype);
        OWLLiteral litNoComp2 = create("true", datatype);
        OWLLiteral litIntImpl = create(true);
        Assert.assertEquals(litNoComp.getLiteral(), litIntImpl.getLiteral());
        Set<OWLLiteral> lncset = new HashSet<>();
        lncset.add(litNoComp);
        Assert.assertTrue(lncset.contains(litNoComp2));
        Assert.assertTrue(lncset.contains(litIntImpl));
    }

    private static OWLLiteral create(int i) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplInteger(i) :
                OWLLiteralImpl.createLiteral(i);
    }

    private static OWLLiteral create(double d) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplDouble(d) :
                OWLLiteralImpl.createLiteral(d);
    }

    private static OWLLiteral create(float f) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplFloat(f) :
                OWLLiteralImpl.createLiteral(f);
    }

    private static OWLLiteral create(boolean b) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplBoolean(b) :
                OWLLiteralImpl.createLiteral(b);
    }

    private static OWLLiteral create(String s, OWLDatatype d) {
        return OWLManager.DEBUG_USE_OWL ? new uk.ac.manchester.cs.owl.owlapi.OWLLiteralImpl(s, null, d) :
                OWLLiteralImpl.createLiteral(s, d);
    }
}
