package ru.avicomp.ontapi.tests;

import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;

/**
 * test for annotations.
 * <p>
 * Created by szuev on 11.10.2016.
 */
public class AnnotationsGraphTest extends GraphTestBase {

    @Test
    public void test1() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.org/annotations/1");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();

        // test data:
        OntIRI clazzIRI = iri.addFragment("SomeClass1");
        OntIRI annotationProperty = iri.addFragment("some-annotation-property");
        String comment = "comment here";
        String commentLang = "s";
        String label = "some-label";

        LOGGER.info("Create fresh ontology (" + iri + ").");
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.asGraphModel();
        OntClass ontClass = jena.createClass(clazzIRI.getIRIString());

        LOGGER.info("Assemble annotations using jena.");
        Resource commentURI;
        Literal label4, label2;
        Resource root = jena.createResource();
        jena.add(root, RDF.type, OWL2.Annotation);
        jena.add(root, OWL2.annotatedProperty, RDFS.label);
        jena.add(root, OWL2.annotatedTarget, label4 = ResourceFactory.createLangLiteral(comment, commentLang));
        jena.add(root, RDFS.comment, commentURI = ResourceFactory.createResource(annotationProperty.getIRIString()));
        jena.add(root, RDFS.label, label2 = ResourceFactory.createPlainLiteral(label));
        Resource anon = jena.createResource();
        jena.add(root, OWL2.annotatedSource, anon);
        jena.add(anon, RDF.type, OWL2.Axiom);
        jena.add(anon, OWL2.annotatedSource, ontClass);
        jena.add(anon, OWL2.annotatedProperty, RDF.type);
        jena.add(anon, OWL2.annotatedTarget, OWL.Class);
        jena.add(anon, RDFS.comment, commentURI);
        jena.add(anon, RDFS.label, label2);
        jena.add(anon, RDFS.label, label4);

        jena.rebind();
        debug(owl);

        LOGGER.info("Check");
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLAxiom expected = factory.getOWLDeclarationAxiom(factory.getOWLClass(clazzIRI), Stream.of(
                factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(comment, commentLang), Stream.of(
                        factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                        factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))
                )),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))
        ).collect(Collectors.toSet()));
        compareAxioms(Stream.of(expected), owl.axioms());
    }

    @Test
    public void test2() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.org/annotations/2");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        long count = manager.ontologies().count();

        OWLOntologyID id1 = iri.toOwlOntologyID(iri.addPath("1.0"));
        LOGGER.info("Create ontology " + id1);
        OntologyModel owl1 = (OntologyModel) manager.createOntology(id1);

        OWLAnnotation simple1 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("PLAIN-1"));
        OWLAnnotation simple2 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("PLAIN-2"));

        OWLAnnotation root1child2child1child1 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("ROOT1->CHILD2->CHILD1->CHILD1 (NIL)"));
        OWLAnnotation root1child2child1child2 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("ROOT1->CHILD2->CHILD1->CHILD2 (NIL)"));

        OWLAnnotation root1child2child1 = factory.getOWLAnnotation(factory.getRDFSIsDefinedBy(), factory.getOWLLiteral("ROOT1->CHILD2->CHILD1"), Stream.of(root1child2child1child1, root1child2child1child2));
        OWLAnnotation root1child2child2 = factory.getOWLAnnotation(factory.getRDFSIsDefinedBy(), factory.getOWLLiteral("ROOT1->CHILD2->CHILD2 (NIL)"));

        OWLAnnotation root1child1child1 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("ROOT1->CHILD1->CHILD1 (NIL)"));
        OWLAnnotation root1child1 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("ROOT1->CHILD1"), root1child1child1);
        OWLAnnotation root1child2 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("ROOT1->CHILD2"), Stream.of(root1child2child1, root1child2child2));
        OWLAnnotation root1 = factory.getOWLAnnotation(factory.getRDFSIsDefinedBy(), factory.getOWLLiteral("ROOT1"), Stream.of(root1child2, root1child1));

        OWLAnnotation root2child1 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("ROOT2->CHILD1 (NIL)"));
        OWLAnnotation root2 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("ROOT2"), root2child1);

        OWLClass owlClass = factory.getOWLClass(iri.addFragment("SomeClass1"));
        OWLAxiom axiom = factory.getOWLDeclarationAxiom(owlClass, Stream.of(root1, root2, simple2, simple1).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, axiom));

        OWLAnnotation indiAnn = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("INDI-ANN"));
        OWLAxiom indiAxiom = factory.getOWLClassAssertionAxiom(owlClass, factory.getOWLNamedIndividual(iri.addFragment("Indi")), Stream.of(indiAnn).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, indiAxiom));

        debug(owl1);

        OWLOntologyID id2 = iri.toOwlOntologyID(iri.addPath("2.0"));
        LOGGER.info("Create ontology " + id2 + " (empty)");
        OntologyModel owl2 = (OntologyModel) manager.createOntology(id2);
        Assert.assertEquals("Incorrect number of ontologies.", count + 2, manager.ontologies().count());

        LOGGER.info("Pass all content from " + id1 + " to " + id2 + " using jena");
        OntModel source = owl1.asGraphModel();
        OntModel target = owl2.asGraphModel();
        Iterator<Statement> toCopy = source.getBaseModel().listStatements().filterDrop(statement -> iri.toResource().equals(statement.getSubject()));
        toCopy.forEachRemaining(target::add);
        target.setNsPrefixes(source.getNsPrefixMap()); // just in case
        target.rebind();
        debug(owl2);

        LOGGER.info("Compare axioms"); // note! there is one more axiom in new ontology: Declaration(NamedIndividual(<http://test.org/annotations/2#Indi>))
        compareAxioms(owl1.axioms(), owl2.axioms().filter(OWLAxiom::isAnnotated));
    }

    @Test
    public void test3() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.org/annotations/3");
        OWLOntologyManager manager = //OWLManager.createOWLOntologyManager();
                OntManagerFactory.createOWLOntologyManager();
        OWLOntology owl = manager.createOntology(iri.toOwlOntologyID());
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass clazz1 = factory.getOWLClass(iri.addFragment("MyClass1"));
        OWLClass clazz2 = factory.getOWLClass(iri.addFragment("MyClass2"));
        OWLClass clazz3 = factory.getOWLClass(iri.addFragment("MyClass3"));
        OWLIndividual ind1 = factory.getOWLAnonymousIndividual();
        OWLIndividual ind2 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi1"));
        OWLIndividual ind3 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi2"));
        OWLObjectPropertyExpression objectProperty1 = factory.getOWLObjectProperty(iri.addFragment("objectProperty1"));
        OWLObjectPropertyExpression objectProperty2 = factory.getOWLObjectProperty(iri.addFragment("objectProperty2"));
        OWLObjectPropertyExpression objectProperty3 = factory.getOWLObjectProperty(iri.addFragment("objectProperty3"));
        OWLDataPropertyExpression dataProperty1 = factory.getOWLDataProperty(iri.addFragment("dataProperty1"));
        OWLDataPropertyExpression dataProperty2 = factory.getOWLDataProperty(iri.addFragment("dataProperty2"));
        OWLDataPropertyExpression dataProperty3 = factory.getOWLDataProperty(iri.addFragment("dataProperty3"));

        owl.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(clazz1)));

        OWLAnnotation ann1 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("annotation №1"));
        OWLAnnotation ann2 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("annotation №2"), Stream.of(ann1));
        owl.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz1, ind1, Stream.of(ann1).collect(Collectors.toSet()))));
        owl.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz1, ind2)));
        owl.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz1, ind3)));
        // different individuals:
        owl.applyChange(new AddAxiom(owl, factory.getOWLDifferentIndividualsAxiom(
                Stream.of(ind1, ind2, ind3).collect(Collectors.toSet()),
                Stream.of(ann2).collect(Collectors.toSet()))));

        // disjoint classes
        OWLAnnotation ann3 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("annotation №3"));
        OWLAnnotation ann4 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("annotation №4"), ann3);
        OWLAnnotation ann5 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("annotation №5"));
        owl.applyChange(new AddAxiom(owl, factory.getOWLDisjointClassesAxiom(
                Stream.of(clazz1, clazz2, clazz3),
                Stream.of(ann4, ann5).collect(Collectors.toSet()))));

        // negative object property:
        OWLAnnotation ann6 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("annotation №6"));
        OWLAnnotation ann7 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("annotation №7"), ann6);
        owl.applyChange(new AddAxiom(owl, factory.getOWLNegativeObjectPropertyAssertionAxiom(objectProperty1, ind1, ind2, Stream.of(ann6).collect(Collectors.toSet()))));
        owl.applyChange(new AddAxiom(owl, factory.getOWLNegativeDataPropertyAssertionAxiom(dataProperty1, ind3, factory.getOWLLiteral("TEST"), Stream.of(ann7).collect(Collectors.toSet()))));

        // disjoint properties
        OWLAnnotation ann8 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("annotation №8"));
        OWLAnnotation ann9 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), factory.getOWLLiteral("annotation №9"), ann8);
        owl.applyChange(new AddAxiom(owl, factory.getOWLDisjointObjectPropertiesAxiom(
                Stream.of(objectProperty1, objectProperty2, objectProperty3).collect(Collectors.toSet()),
                Stream.of(ann8).collect(Collectors.toSet()))));
        owl.applyChange(new AddAxiom(owl, factory.getOWLDisjointDataPropertiesAxiom(
                Stream.of(dataProperty3, dataProperty1, dataProperty2).collect(Collectors.toSet()),
                Stream.of(ann9).collect(Collectors.toSet()))));

        debug((OntologyModel) owl);
        //TODO:
    }

    @Test
    public void test4() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.org/annotations/4");
        OWLOntologyManager manager = //OWLManager.createOWLOntologyManager();
                OntManagerFactory.createOWLOntologyManager();
        OWLOntology owl = manager.createOntology(iri.toOwlOntologyID());
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass clazz1 = factory.getOWLClass(iri.addFragment("MyClass1"));
        OWLClass clazz2 = factory.getOWLClass(iri.addFragment("MyClass2"));
        OWLClass clazz3 = factory.getOWLClass(iri.addFragment("MyClass3"));
        OWLIndividual ind1 = factory.getOWLAnonymousIndividual();
        OWLIndividual ind2 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi1"));
        OWLIndividual ind3 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi2"));
        OWLObjectPropertyExpression objectProperty1 = factory.getOWLObjectProperty(iri.addFragment("objectProperty1"));
        OWLObjectPropertyExpression objectProperty2 = factory.getOWLObjectProperty(iri.addFragment("objectProperty2"));
        OWLObjectPropertyExpression objectProperty3 = factory.getOWLObjectProperty(iri.addFragment("objectProperty3"));
        OWLDataPropertyExpression dataProperty1 = factory.getOWLDataProperty(iri.addFragment("dataProperty1"));
        OWLDataPropertyExpression dataProperty2 = factory.getOWLDataProperty(iri.addFragment("dataProperty2"));
        OWLDataPropertyExpression dataProperty3 = factory.getOWLDataProperty(iri.addFragment("dataProperty3"));

        //todo

        // equivalent classes
        owl.applyChange(new AddAxiom(owl, factory.getOWLEquivalentClassesAxiom(Stream.of(clazz1, clazz2, clazz3))));

        // equivalent data properties
        owl.applyChange(new AddAxiom(owl, factory.getOWLEquivalentDataPropertiesAxiom(Stream.of(dataProperty1, dataProperty2, dataProperty3).collect(Collectors.toSet()))));

        // equivalent object properties
        owl.applyChange(new AddAxiom(owl, factory.getOWLEquivalentObjectPropertiesAxiom(Stream.of(objectProperty1, objectProperty2, objectProperty3).collect(Collectors.toSet()))));

        // same individuals
        owl.applyChange(new AddAxiom(owl, factory.getOWLSameIndividualAxiom(ind1, ind2, ind3)));

        debug((OntologyModel) owl);
        //TODO:
    }
}
