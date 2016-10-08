package ru.avicomp.ontapi.tests;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test behaviour with owl:imports
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ImportsGraphTest extends GraphTestBase {

    @Test
    public void test() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-import");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.asGraphModel();
        int importsCount = 4;
        Ontology jenaOnt = jena.getOntology(iri.getIRIString());
        LOGGER.info("Add imports.");
        OntIRI import1 = OntIRI.create("http://dummy-imports.com/first");
        OntIRI import2 = OntIRI.create("http://dummy-imports.com/second");
        OntIRI import3 = OntIRI.create(ReadWriteUtils.getResourceURI("foaf.rdf"));
        OntIRI import4 = OntIRI.create(ReadWriteUtils.getResourceURI("pizza.ttl"));
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import1)));
        jenaOnt.addImport(import2.toResource());
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import3)));
        jenaOnt.addImport(import4.toResource());

        debug(owl);

        Assert.assertEquals("OWL: incorrect imported ontology count.", 0, owl.imports().count());
        Assert.assertEquals("OWL: incorrect imports count.", importsCount, owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count.", importsCount, jena.listStatements(iri.toResource(), OWL.imports, (RDFNode) null).toList().size());

        LOGGER.info("Remove imports.");
        jenaOnt.removeImport(import4.toResource());
        manager.applyChange(new RemoveImport(owl, factory.getOWLImportsDeclaration(import1)));
        debug(owl);
        importsCount = 2;
        Assert.assertEquals("OWL: incorrect imports count after removing.", importsCount, owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count after removing.", importsCount, jenaOnt.listImports().toList().size());

        debug(owl);
    }
}
