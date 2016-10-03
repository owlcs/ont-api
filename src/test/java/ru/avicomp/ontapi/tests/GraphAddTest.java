package ru.avicomp.ontapi.tests;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * todo:
 * Created by @szuev on 02.10.2016.
 */
public class GraphAddTest {
    private static final Logger LOGGER = Logger.getLogger(GraphAddTest.class);

    @Test
    public void addImportTest() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-import");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.getOntModel();

        int importsCount = 4;

        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(IRI.create("http://dummy-imports.com/first"))));
        jena.getOntology(iri.getIRIString()).addImport(OntIRI.create("http://dummy-imports.com/second").toResource());
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(IRI.create(ReadWriteUtils.getResourceURI("foaf.rdf")))));
        jena.getOntology(iri.getIRIString()).addImport(ResourceFactory.createResource(ReadWriteUtils.getResourceURI("pizza.ttl").toString()));

        debug(owl);

        Assert.assertEquals("OWL: incorrect imported ontology count.", 0, owl.imports().count());
        Assert.assertEquals("OWL: incorrect imports count.", importsCount, owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count.", importsCount, jena.listStatements(iri.toResource(), OWL.imports, (RDFNode) null).toList().size());
    }

    @Test
    public void addClassIndividualTest() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-class-individual");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.getOntModel();

        OntIRI class1 = iri.addFragment("ClassN1");
        OntIRI class2 = iri.addFragment("ClassN2");
        OntIRI individual1 = iri.addFragment("TestIndividualN1");
        OntIRI individual2 = iri.addFragment("TestIndividualN2");
        OntIRI individual3 = iri.addFragment("TestIndividualN3");
        int classesCount = 2;
        int individualsCount = 3;

        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(class1))));
        jena.add(class2.toResource(), RDF.type, OWL.Class);
        manager.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        jena.add(individual2.toResource(), RDF.type, class1.toResource());
        jena.add(individual3.toResource(), RDF.type, class2.toResource());

        debug(owl);

        Assert.assertEquals("OWL: incorrect classes count", classesCount, owl.axioms(AxiomType.DECLARATION).count());
        Assert.assertEquals("Jena: incorrect classes count.", classesCount, jena.listClasses().toList().size());
        Assert.assertEquals("OWL: incorrect individuals count", individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Jena: incorrect individuals count.", individualsCount, jena.listIndividuals().toList().size());
    }

    private void debug(OntologyModel ontology) {
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        LOGGER.debug("Jena: ");
        ReadWriteUtils.print(ontology.getOntModel(), OntFormat.TTL_RDF);
    }
}
