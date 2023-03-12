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
package com.github.owlcs.owlapi.tests.api.fileroundtrip;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class NonSymmetricAxiomsRoundTrippingTestCase extends TestBase {

    private static final IRI iriA = iri("A");
    private static final OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iriA);
    private static final OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
    private static final OWLClass clsC = OWLFunctionalSyntaxFactory.Class(iri("C"));
    private static final OWLDatatype dataD = OWLFunctionalSyntaxFactory.Datatype(iri("D"));
    private static final OWLDatatype dataE = OWLFunctionalSyntaxFactory.Datatype(iri("E"));
    private static final OWLObjectProperty propA = OWLFunctionalSyntaxFactory.ObjectProperty(iri("propA"));
    private static final OWLDataProperty propB = OWLFunctionalSyntaxFactory.DataProperty(iri("propB"));
    private static final OWLObjectSomeValuesFrom d = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propA,
            OWLFunctionalSyntaxFactory.ObjectIntersectionOf(clsB, clsC));
    private static final OWLDataSomeValuesFrom e = OWLFunctionalSyntaxFactory.DataSomeValuesFrom(propB,
            OWLFunctionalSyntaxFactory.DataIntersectionOf(dataD, dataE));
    private static final OWLClassExpression du = OWLFunctionalSyntaxFactory.ObjectUnionOf(clsB, clsC);
    private static final OWLDataUnionOf eu = OWLFunctionalSyntaxFactory.DataUnionOf(dataD, dataE);

    public static List<OWLAxiom[]> getData() {
        List<OWLAxiom[]> list = new ArrayList<>();
        list.add(new OWLAxiom[]{OWLFunctionalSyntaxFactory.SubClassOf(clsA, OWLFunctionalSyntaxFactory.ObjectIntersectionOf(d, d)),
                OWLFunctionalSyntaxFactory.SubClassOf(clsA, d)});
        list.add(new OWLAxiom[]{OWLFunctionalSyntaxFactory.SubClassOf(clsA, OWLFunctionalSyntaxFactory.ObjectUnionOf(e, e)),
                OWLFunctionalSyntaxFactory.SubClassOf(clsA, e)});
        list.add(new OWLAxiom[]{OWLFunctionalSyntaxFactory.SubClassOf(clsA, OWLFunctionalSyntaxFactory.ObjectIntersectionOf(du, du)),
                OWLFunctionalSyntaxFactory.SubClassOf(clsA, du)});
        list.add(new OWLAxiom[]{OWLFunctionalSyntaxFactory.DatatypeDefinition(dataD, OWLFunctionalSyntaxFactory.DataUnionOf(eu, eu)),
                OWLFunctionalSyntaxFactory.DatatypeDefinition(dataD, eu)});
        return list;
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @ParameterizedTest
    @MethodSource("getData")
    public void shouldRoundTripAReadableVersion(OWLAxiom in, OWLAxiom out) throws Exception {
        OWLOntology output = getOWLOntology();
        output.add(in);
        OWLOntology o = roundTrip(output, new FunctionalSyntaxDocumentFormat());
        Assertions.assertEquals(1, o.logicalAxioms().count());
        Assertions.assertEquals(out, o.logicalAxioms().iterator().next());
    }
}
