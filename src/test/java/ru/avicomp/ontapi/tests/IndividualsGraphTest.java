package ru.avicomp.ontapi.tests;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * test individuals using jena-graph and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class IndividualsGraphTest extends GraphTestBase {

    @Test
    public void test() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-class-individual");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.asGraphModel();

        OntIRI class1 = iri.addFragment("ClassN1");
        OntIRI class2 = iri.addFragment("ClassN2");
        OntIRI individual1 = iri.addFragment("TestIndividualN1");
        OntIRI individual2 = iri.addFragment("TestIndividualN2");
        OntIRI individual3 = iri.addFragment("TestIndividualN3");
        int classesCount = 2;
        int individualsCount = 3;

        LOGGER.info("Add classes.");
        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(class1))));
        jena.add(class2.toResource(), RDF.type, OWL.Class);

        LOGGER.info("Add individuals.");
        manager.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        jena.add(individual2.toResource(), RDF.type, class1.toResource());
        jena.add(individual3.toResource(), RDF.type, class2.toResource());

        debug(owl);

        Assert.assertEquals("OWL: incorrect classes count", classesCount, owl.axioms(AxiomType.DECLARATION).count());
        Assert.assertEquals("Jena: incorrect classes count.", classesCount, jena.listClasses().toList().size());
        Assert.assertEquals("OWL: incorrect individuals count", individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Jena: incorrect individuals count.", individualsCount, jena.listIndividuals().toList().size());

        LOGGER.info("Remove individuals");
        jena.removeAll(individual3.toResource(), null, null);
        manager.applyChange(new RemoveAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        individualsCount = 1;

        ReadWriteUtils.print(jena, OntFormat.TTL_RDF);
        Assert.assertEquals("OWL: incorrect individuals count after removing", individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Jena: incorrect individuals count after removing.", individualsCount, jena.listIndividuals().toList().size());
        debug(owl);
    }

}
