package ru.avicomp.ontapi.tests;

import org.apache.jena.system.JenaSystem;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * Created by @szuev on 27.09.2016.
 */
public class ExampleTest {
    private static final Logger LOGGER = Logger.getLogger(ExampleTest.class);

    @BeforeClass
    public static void before() {
        LOGGER.debug("Before -- START");
        JenaSystem.init();
        LOGGER.debug("Before -- END");
    }

    @After
    public void after() {
        LOGGER.debug("After");
    }

    @Test
    public void test() throws OWLOntologyCreationException {
        OntIRI owlURI = OntIRI.create("http://test.test/example");
        int statementsNumber = 15;
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLOntology owl = manager.createOntology(owlURI.toOwlOntologyID());
        // use NonConcurrentOWLOntologyBuilder:
        Assert.assertEquals("incorrect class", OntologyModel.class, owl.getClass());
        OntologyModel ontology = (OntologyModel) owl;
        manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create(ReadWriteUtils.getResourceURI("sp.ttl")))));
        //manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create(SPINMAP_SPIN.BASE_URI))));
        manager.applyChange(new AddOntologyAnnotation(ontology, factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("test-comment"))));
        manager.applyChange(new AddOntologyAnnotation(ontology, factory.getOWLAnnotation(factory.getOWLVersionInfo(), factory.getOWLLiteral("test-version-info"))));

        OWLClass owlClass = factory.getOWLClass(owlURI.addFragment("SomeClass"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDeclarationAxiom(owlClass)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLSubClassOfAxiom(owlClass, factory.getOWLThing())));

        OWLAnnotation classAnnotationLabel = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("some-class-label"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), classAnnotationLabel)));

        OWLDataProperty owlProperty = factory.getOWLDataProperty(owlURI.addFragment("someDataProperty"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDeclarationAxiom(owlProperty)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyDomainAxiom(owlProperty, owlClass)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyRangeAxiom(owlProperty, factory.getStringOWLDatatype())));
        OWLAnnotation propertyAnnotationLabel = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("some-property-label"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlProperty.getIRI(), propertyAnnotationLabel)));
        OWLAnnotation propertyAnnotationComment = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("some property comment"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlProperty.getIRI(), propertyAnnotationComment)));

        OWLIndividual individual = factory.getOWLNamedIndividual(owlURI.addFragment("the-individual"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLClassAssertionAxiom(owlClass, individual)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(owlProperty, individual, factory.getOWLLiteral("TheName"))));

        //System.out.println(ontology.directImports().collect(Collectors.toList()));
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);

        ontology.axioms().forEach(LOGGER::debug);

        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TTL_RDF);
        LOGGER.debug("All statements: " + ontology.asGraphModel().listStatements().toList().size());
        Assert.assertEquals("incorrect statements size", statementsNumber, ontology.asGraphModel().getBaseModel().listStatements().toList().size());
    }
}
