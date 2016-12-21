package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * test loading from different formats
 * <p>
 * Created by szuev on 20.12.2016.
 */
public class FormatsTest {
    private static final Logger LOGGER = Logger.getLogger(FormatsTest.class);

    /**
     * Tests OWL unbroken formats which is not supported by jena and (fss, obo, omn, owl)
     */
    @Test
    public void test() {
        OWLDataFactory factory = OntManagerFactory.createDataFactory();
        String file = "test2";

        OntIRI iri = OntIRI.create("http://test/formats");
        OWLClass clazz = factory.getOWLClass(iri.addFragment("ClassN1"));
        OWLDataProperty ndp = factory.getOWLDataProperty(iri.addFragment("DataPropertyN1"));
        OWLObjectProperty nop = factory.getOWLObjectProperty(iri.addFragment("ObjectPropertyN1"));
        OWLDatatype dt = OWL2Datatype.XSD_ANY_URI.getDatatype(factory);

        List<OWLAxiom> expected = Stream.of(
                factory.getOWLDeclarationAxiom(clazz),
                factory.getOWLDeclarationAxiom(ndp),
                factory.getOWLDeclarationAxiom(nop),
                factory.getOWLObjectPropertyDomainAxiom(nop, clazz),
                factory.getOWLObjectPropertyRangeAxiom(nop, clazz),
                factory.getOWLDataPropertyDomainAxiom(ndp, clazz),
                factory.getOWLDataPropertyRangeAxiom(ndp, dt)).sorted().collect(Collectors.toList());

        OntFormat.owlOnly().forEach(f -> {
            IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(file + "." + f.getExt()));
            LOGGER.info("Load ontology " + fileIRI);
            OntologyModel o;
            try {
                o = (OntologyModel) OntManagerFactory.createONTManager().loadOntology(fileIRI);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError("Can't load " + fileIRI + "[" + f + "] :: ", e);
            }
            o.axioms().forEach(LOGGER::info);
            List<OWLAxiom> actual = o.axioms()
                    .filter(axiom -> !AxiomType.ANNOTATION_ASSERTION.equals(axiom.getAxiomType()))
                    .filter(axiom -> {
                        if (AxiomType.DECLARATION.equals(axiom.getAxiomType())) {
                            OWLDeclarationAxiom declarationAxiom = (OWLDeclarationAxiom) axiom;
                            if (declarationAxiom.getEntity().isBuiltIn()) return false;
                            if (declarationAxiom.getEntity().isOWLAnnotationProperty()) return false;
                        }
                        return true;
                    }).sorted().collect(Collectors.toList());
            if (OntFormat.OBO.equals(f)) { // strange uri prefixes
                Assert.assertEquals("[" + f + "] Incorrect list of axioms", expected.size(), actual.size());
            } else {
                Assert.assertThat("[" + f + "] Incorrect list of axioms (expected=" + expected.size() + ",actual=" + actual.size() + ")", actual, IsEqual.equalTo(expected));
            }
        });
    }
}
