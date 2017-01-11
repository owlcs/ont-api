package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * Test loading from different formats.
 * At the moment it is only for four unbroken owl-formats which are not supported by jena: fss, obo, omn, owl-rdf.
 * The pure OWL-API mechanism is used for loading a document in these formats.
 * <p>
 * Created by szuev on 20.12.2016.
 */
@RunWith(Parameterized.class)
public class FormatsTest {
    private static final Logger LOGGER = Logger.getLogger(FormatsTest.class);
    private OntFormat format;
    private static final String fileName = "test2";
    private static List<OWLAxiom> expected;

    public FormatsTest(OntFormat format) {
        this.format = format;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<OntFormat> getData() {
        return OntFormat.owlOnly().collect(Collectors.toList());
    }

    @BeforeClass
    public static void before() {
        OWLDataFactory factory = OntManagerFactory.getDataFactory();

        OntIRI iri = OntIRI.create("http://test/formats");
        OWLClass clazz = factory.getOWLClass(iri.addFragment("ClassN1"));
        OWLDataProperty ndp = factory.getOWLDataProperty(iri.addFragment("DataPropertyN1"));
        OWLObjectProperty nop = factory.getOWLObjectProperty(iri.addFragment("ObjectPropertyN1"));
        OWLDatatype dt = OWL2Datatype.XSD_ANY_URI.getDatatype(factory);

        expected = Stream.of(
                factory.getOWLDeclarationAxiom(clazz),
                factory.getOWLDeclarationAxiom(ndp),
                factory.getOWLDeclarationAxiom(nop),
                factory.getOWLObjectPropertyDomainAxiom(nop, clazz),
                factory.getOWLObjectPropertyRangeAxiom(nop, clazz),
                factory.getOWLDataPropertyDomainAxiom(ndp, clazz),
                factory.getOWLDataPropertyRangeAxiom(ndp, dt)).sorted().collect(Collectors.toList());
    }

    @Test
    public void test() {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName + "." + format.getExt()));
        LOGGER.info("Load ontology " + fileIRI + ". Format: " + format);
        OntologyModel o;
        try {
            o = OntManagerFactory.createONTManager().loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Can't load " + fileIRI + "[" + format + "] :: ", e);
        }
        ReadWriteUtils.print(o);
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
                })
                .sorted().collect(Collectors.toList());
        Assert.assertThat("[" + format + "] Incorrect list of axioms (expected=" + expected.size() + ",actual=" + actual.size() + ")", actual, IsEqual.equalTo(expected));
    }
}
