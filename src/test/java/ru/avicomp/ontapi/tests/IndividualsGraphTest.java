package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.OntIRI;

/**
 * test individuals using jena-graph and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class IndividualsGraphTest extends GraphTestBase {

    @Test
    public void test() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-class-individual");
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel owl = manager.createOntology(iri.toOwlOntologyID());
        OntGraphModel jena = owl.asGraphModel();

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
        LOGGER.debug("Add individuals using OWL");
        manager.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        LOGGER.debug("Add individuals using ONT");
        jena.add(individual2.toResource(), RDF.type, class1.toResource());
        jena.add(individual2.toResource(), RDF.type, OWL.NamedIndividual);
        jena.getOntEntity(OntClass.class, class2.getIRIString()).createIndividual(individual3.getIRIString());

        debug(owl);

        Assert.assertEquals("OWL: incorrect classes count", classesCount + individualsCount, owl.axioms(AxiomType.DECLARATION).count());
        Assert.assertEquals("Jena: incorrect classes count.", classesCount, jena.ontEntities(OntClass.class).count());
        Assert.assertEquals("OWL: incorrect individuals count", individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Jena: incorrect individuals count.", individualsCount, jena.ontObjects(OntIndividual.class).count());

        LOGGER.info("Remove individuals");
        // remove class assertion and declaration:
        jena.removeAll(individual3.toResource(), null, null);
        // remove class-assertion:
        manager.applyChange(new RemoveAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        // remove declaration:
        owl.remove(factory.getOWLDeclarationAxiom(factory.getOWLNamedIndividual(individual1)));
        individualsCount = 1;

        debug(owl);

        Assert.assertEquals("OWL: incorrect individuals count after removing", individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Jena: incorrect individuals count after removing.", individualsCount, jena.ontObjects(OntIndividual.class).count());
    }

}
