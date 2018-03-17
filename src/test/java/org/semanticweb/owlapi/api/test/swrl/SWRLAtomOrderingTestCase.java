/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.semanticweb.owlapi.api.test.swrl;

import javax.annotation.Nonnull;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.SWRLAtom;

import uk.ac.manchester.cs.owl.owlapi.SWRLRuleImpl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asList;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics
 *         Research Group, Date: 04/04/2014
 */
@SuppressWarnings("javadoc")
@RunWith(MockitoJUnitRunner.class)
public class SWRLAtomOrderingTestCase extends TestBase {

    protected SWRLAtom atomA;
    protected SWRLAtom atomB;
    protected SWRLAtom atomC;
    protected SWRLAtom atomD;
    private SWRLRuleImpl rule;
    private final
    @Nonnull
    Set<SWRLAtom> body = new LinkedHashSet<>();

    @Before
    public void setUp() {
        OWLClass predicate = df.getOWLClass(iri("a"));
        atomA = df.getSWRLClassAtom(predicate, df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri("i"))));
        atomB = df.getSWRLClassAtom(predicate, df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri("j"))));
        atomC = df.getSWRLClassAtom(predicate, df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri("k"))));
        atomD = df.getSWRLClassAtom(predicate, df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri("l"))));
        body.add(atomC);
        body.add(atomB);
        body.add(atomA);
        Set<SWRLAtom> head = new LinkedHashSet<>();
        head.add(atomD);
        rule = new SWRLRuleImpl(body, head, Collections.emptySet());
    }

    @Test
    public void shouldPreserveBodyOrdering() {
        List<SWRLAtom> ruleImplBody = asList(rule.body());
        List<SWRLAtom> specifiedBody = new ArrayList<>(body);
        assertThat(ruleImplBody, is(equalTo(specifiedBody)));
    }
}
