package org.semanticweb.owlapi.profiles;

import org.junit.Assert;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLRuntimeException;


@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class ProfileBase extends TestBase {

    protected void test(String in, boolean el, boolean ql, boolean rl, boolean dl) {
        try {
            LOGGER.trace("Input: " + in);
            LOGGER.debug(String.format("Expected parameters: EL=%s, QL=%s, RL=%s, DL=%s", el, ql, rl, dl));
            OWLOntology o = loadOntologyFromString(in);
            ru.avicomp.ontapi.utils.ReadWriteUtils.print(o);
            o.axioms().forEach(a -> LOGGER.debug(String.valueOf(a)));
            Assert.assertTrue("Empty ontology", o.axioms().count() > 0);
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
            throw new OWLRuntimeException(e);
        }
    }
}
