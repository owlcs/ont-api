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

package org.semanticweb.owlapi.profiles;

import org.junit.Assert;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class ProfileBase extends TestBase {

    protected void test(String in, boolean el, boolean ql, boolean rl, boolean dl) {
        test(setupManager(), in, el, ql, rl, dl);
    }

    protected void test(OWLOntologyManager m, String in, boolean el, boolean ql, boolean rl, boolean dl) {
        try {
            LOGGER.trace("Input: " + in);
            LOGGER.debug(String.format("Expected parameters: EL=%s, QL=%s, RL=%s, DL=%s", el, ql, rl, dl));
            OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(in));
            ru.avicomp.ontapi.utils.ReadWriteUtils.print(o);
            o.axioms().forEach(a -> LOGGER.debug(String.valueOf(a)));
            Assert.assertTrue("Empty axioms list", o.axioms().count() > 0);
            OWLProfileReport OWL2_EL = Profiles.OWL2_EL.checkOntology(o);
            OWLProfileReport OWL2_QL = Profiles.OWL2_QL.checkOntology(o);
            OWLProfileReport OWL2_RL = Profiles.OWL2_RL.checkOntology(o);
            OWLProfileReport OWL2_DL = Profiles.OWL2_DL.checkOntology(o);
            LOGGER.debug("Violations(EL): " + OWL2_EL.getViolations());
            LOGGER.debug("Violations(QL): " + OWL2_QL.getViolations());
            LOGGER.debug("Violations(RL): " + OWL2_RL.getViolations());
            LOGGER.debug("Violations(DL): " + OWL2_DL.getViolations());
            Assert.assertEquals(el, OWL2_EL.isInProfile());
            Assert.assertEquals(ql, OWL2_QL.isInProfile());
            Assert.assertEquals(rl, OWL2_RL.isInProfile());
            Assert.assertEquals(dl, OWL2_DL.isInProfile());
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Exception!", e);
        }
    }
}
