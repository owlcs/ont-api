package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * to test behaviour with owl:imports
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ImportsGraphTest extends GraphTestBase {

    @Test
    public void testAdd() {
        OntIRI iri = OntIRI.create("http://test.test/add-import/1");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
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

    @Test
    public void testGraph() {
        OntIRI baseIRI = OntIRI.create("http://test.test/add-import/base");
        OntologyModel base = TestUtils.createModel(baseIRI);
        OWLOntologyManager manager = base.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntIRI classIRI1 = baseIRI.addFragment("Class-1");
        OntIRI classIRI2 = baseIRI.addFragment("Class-2");
        OntIRI objPropIRI = baseIRI.addFragment("obj-prop-1");
        OntIRI dataPropIRI = baseIRI.addFragment("data-prop-1");
        OntIRI annPropIRI = baseIRI.addFragment("ann-prop-1");
        OntIRI dataTypeIRI = baseIRI.addFragment("data-type-1");

        OWLClass class1 = factory.getOWLClass(classIRI1);
        OWLClass class2 = factory.getOWLClass(classIRI2);
        OWLObjectProperty objProperty = factory.getOWLObjectProperty(objPropIRI);
        OWLDataProperty dataProperty = factory.getOWLDataProperty(dataPropIRI);
        OWLAnnotationProperty annProperty = factory.getOWLAnnotationProperty(annPropIRI);
        OWLDatatype dataType = factory.getOWLDatatype(dataTypeIRI);

        List<OWLAxiom> baseAxioms = new ArrayList<>();
        baseAxioms.add(factory.getOWLDeclarationAxiom(objProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(dataProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(annProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(class1));
        baseAxioms.add(factory.getOWLDeclarationAxiom(class2));
        baseAxioms.add(factory.getOWLDeclarationAxiom(dataType));
        baseAxioms.forEach(axiom -> base.applyChanges(new AddAxiom(base, axiom)));

        debug(base);

        LOGGER.info("Apply axioms to the base ontology " + baseIRI);
        OntIRI childIRI = OntIRI.create("http://test.test/add-import/child");
        OntologyModel child = TestUtils.createModel(manager, childIRI.toOwlOntologyID());
        child.applyChanges(new AddImport(child, factory.getOWLImportsDeclaration(baseIRI)));

        Assert.assertEquals("Incorrect imports count", 1, child.imports().count());

        OWLDatatypeRestriction dataRange1 = factory.getOWLDatatypeMinMaxInclusiveRestriction(1, 2.3);

        OWLNamedIndividual individual1 = factory.getOWLNamedIndividual(childIRI.addFragment("Individual-1"));
        OWLNamedIndividual individual2 = factory.getOWLNamedIndividual(childIRI.addFragment("Individual-2"));
        OWLClassExpression ce1 = factory.getOWLObjectUnionOf(class1, class2);
        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getOWLDeclarationAxiom(individual1));
        axioms.add(factory.getOWLDeclarationAxiom(individual2));
        axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(objProperty));
        axioms.add(factory.getOWLDataPropertyRangeAxiom(dataProperty, dataRange1));
        axioms.add(factory.getOWLClassAssertionAxiom(class1, individual1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce1, individual2));
        axioms.add(factory.getOWLAnnotationPropertyDomainAxiom(annProperty, class1.getIRI()));

        LOGGER.info("Apply axioms to the subsidiary ontology " + child);
        axioms.forEach(axiom -> child.applyChanges(new AddAxiom(child, axiom)));

        debug(child);

        LOGGER.info("Check triplets presence.");
        checkTriple(base.asGraphModel(), child.asGraphModel(), classIRI1.toResource(), RDF.type, OWL.Class);
        checkTriple(base.asGraphModel(), child.asGraphModel(), classIRI2.toResource(), RDF.type, OWL.Class);
        checkTriple(base.asGraphModel(), child.asGraphModel(), objPropIRI.toResource(), RDF.type, OWL.ObjectProperty);
        checkTriple(base.asGraphModel(), child.asGraphModel(), dataPropIRI.toResource(), RDF.type, OWL.DatatypeProperty);
        checkTriple(base.asGraphModel(), child.asGraphModel(), annPropIRI.toResource(), RDF.type, OWL.AnnotationProperty);
        checkTriple(base.asGraphModel(), child.asGraphModel(), dataTypeIRI.toResource(), RDF.type, RDFS.Datatype);

        LOGGER.info("Reload models.");
        OWLOntologyManager newManager = OntManagerFactory.createOWLOntologyManager();
        OntologyModel newBase = TestUtils.loadOntologyFromIOStream(newManager, base.asGraphModel(), null);
        OntologyModel newChild = TestUtils.loadOntologyFromIOStream(newManager, child.asGraphModel(), null);
        Assert.assertEquals("Incorrect imports count", 1, newChild.imports().count());
        Assert.assertEquals("Should be the same number of statements",
                child.asGraphModel().listStatements().toList().size(),
                newChild.asGraphModel().listStatements().toList().size());
        TestUtils.compareAxioms(base.axioms(), newBase.axioms());

        LOGGER.debug("Check axioms after reload:");
        child.axioms().forEach(LOGGER::debug);
        newChild.axioms().forEach(LOGGER::debug);
        TestUtils.compareAxioms(child.axioms(), newChild.axioms());

        LOGGER.info("Remove import test");
        child.applyChanges(new RemoveImport(child, factory.getOWLImportsDeclaration(baseIRI)));
        debug(child);
        checkTriplePresence(child.asGraphModel(), classIRI1.toResource(), RDF.type, OWL.Class);
        checkTriplePresence(child.asGraphModel(), classIRI2.toResource(), RDF.type, OWL.Class);
        checkTriplePresence(child.asGraphModel(), objPropIRI.toResource(), RDF.type, OWL.ObjectProperty);
        checkTriplePresence(child.asGraphModel(), dataPropIRI.toResource(), RDF.type, OWL.DatatypeProperty);
        checkTriplePresence(child.asGraphModel(), annPropIRI.toResource(), RDF.type, OWL.AnnotationProperty);
        checkTripleAbsence(child.asGraphModel(), dataTypeIRI.toResource(), RDF.type, RDFS.Datatype);
    }

    private static void checkTriple(OntModel base, OntModel child, Resource subject, Property predicate, RDFNode object) {
        checkTriplePresence(base, subject, predicate, object);
        checkTripleAbsence(child, subject, predicate, object);
    }

    private static void checkTriplePresence(OntModel model, Resource subject, Property predicate, RDFNode object) {
        Assert.assertTrue("Can't find the triple " + TestUtils.createTriple(subject, predicate, object), model.contains(subject, predicate, object));
    }

    private static void checkTripleAbsence(OntModel model, Resource subject, Property predicate, RDFNode object) {
        Assert.assertFalse("There is the triple " + TestUtils.createTriple(subject, predicate, object), model.contains(subject, predicate, object));
    }
}
