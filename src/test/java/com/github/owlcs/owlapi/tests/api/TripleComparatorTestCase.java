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

package com.github.owlcs.owlapi.tests.api;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.RDFNode;
import org.semanticweb.owlapi.io.RDFResourceBlankNode;
import org.semanticweb.owlapi.io.RDFResourceIRI;
import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IRI;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectProperty;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_CLASS;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DISJOINT_WITH;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_SUBCLASS_OF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDF_TYPE;

/**
 * Copy-paste from <a href="https://github.com/owlcs/owlapi">OWL-API, ver. 5.1.1</a>
 */
@SuppressWarnings("SameParameterValue")
public class TripleComparatorTestCase {

    private final String ns = "http://www.co-ode.org/roberts/pto.owl#";
    private final RDFResourceIRI g = r(Class(IRI(ns, "MoleOfGoldAtom")));
    private final RDFResourceIRI d = r(ObjectProperty(OWL_DISJOINT_WITH.getIRI()));
    private final RDFResourceIRI subtype = r(RDFS_SUBCLASS_OF.getIRI());

    private static RDFResourceIRI r(OWLEntity e) {
        return new RDFResourceIRI(e.getIRI());
    }

    private static RDFResourceIRI r(IRI e) {
        return new RDFResourceIRI(e);
    }

    private static RDFNode r(int s) {
        return new RDFResourceBlankNode(s, false, false, false);
    }

    @Test
    public void shouldSort() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<RDFTriple> list = new ArrayList<>(Arrays.asList(
                //@formatter:off
                triple("MoleOfNiobiumAtom"),
                triple("MoleOfMercuryAtom"),
                triple("MoleOfHydrogenAtom"),
                triple("MoleOfSodiumAtom"),
                triple("MoleOfIodineAtom"),
                triple(608551021),
                triple(1419046060),
                triple(908505087),
                triple("MoleOfManganeseAtom"),
                triple("MoleOfIronAtom"),
                triple("MoleOfYttriumAtom"),
                triple("MoleOfRadiumAtom"),
                triple("MoleOfPoloniumAtom"),
                triple("MoleOfPalladiumAtom"),
                triple("MoleOfLeadAtom"),
                triple("MoleOfTinAtom"),
                triple("MoleOfIndiumAtom"),
                triple(589710844),
                triple("MoleOfPhosphorusAtom"),
                triple(767224527),
                triple("MoleOfXenonAtom"),
                triple("MoleOfZirconiumAtom"),
                triple("MoleOfNickelAtom"),
                triple("MoleOfRhodiumAtom"),
                triple("MoleOfThalliumAtom"),
                triple("MoleOfHafniumAtom"),
                triple(12186480),
                triple(1975184526),
                triple("MoleOfVanadiumAtom"),
                triple(484873262),
                triple("MoleOfScandiumAtom"),
                triple("MoleOfRubidiumAtom"),
                triple("MoleOfMolybdenumAtom"),
                triple("MoleOfTelluriumAtom"),
                triple(21622515),
                triple("MoleOfMagnesiumAtom"),
                triple("MoleOfTungstenAtom"),
                triple("MoleOfPotassiumAtom"),
                triple("MoleOfSulfurAtom"),
                triple("MoleOfOxygenAtom"),
                triple("MoleOfHeliumAtom"),
                triple("MoleOfRutheniumAtom"),
                triple(315300697),
                triple(1711957716),
                triple("MoleOfLithiumAtom"),
                triple("MoleOfTitaniumAtom"),
                triple("MoleOfOsmiumAtom"),
                triple("MoleOfSiliconAtom"),
                triple("MoleOfTantalumAtom"),
                triple(624417224),
                triple("MoleOfRadonAtom"),
                triple(1556170233),
                new RDFTriple(g, subtype, r(IRI(ns, "MoleOfAtom"))),
                triple("MoleOfSeleniumAtom"),
                triple("MoleOfNeonAtom"),
                triple("MoleOfKryptonAtom"),
                triple(RDF_TYPE, OWL_CLASS),
                triple("MoleOfPlatinumAtom"),
                triple("MoleOfSilverAtom"),
                triple("MoleOfStrontiumAtom"),
                triple(1340998166)
                , triple("MoleOfIridiumAtom")
                , triple("MoleOfNitrogenAtom")
                , triple("MoleOfRheniumAtom")
                , triple("MoleOfZincAtom")
                //@formatter:on
        ));
        Collections.sort(list);
    }

    private RDFTriple triple(String n) {
        return new RDFTriple(g, d, r(IRI(ns, n)));
    }

    private RDFTriple triple(OWLRDFVocabulary p, OWLRDFVocabulary n) {
        return new RDFTriple(g, r(p.getIRI()), r(n.getIRI()));
    }

    private RDFTriple triple(int n) {
        return new RDFTriple(g, subtype, r(n));
    }
}