package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.translators.OWL2RDFHelper;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * test for annotations.
 * <p>
 * Created by szuev on 11.10.2016.
 */
public class AnnotationsGraphTest extends GraphTestBase {

    @Test
    public void testSingleComplexAnnotation() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/1");
        // test data:
        OntIRI clazzIRI = iri.addFragment("SomeClass1");
        OntIRI annotationProperty = iri.addFragment("some-annotation-property");
        String comment = "comment here";
        String commentLang = "s";
        String label = "some-label";

        LOGGER.info("Create fresh ontology (" + iri + ").");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntGraphModel jena = owl.asGraphModel();

        OntClass ontClass = jena.createOntEntity(OntClass.class, clazzIRI.getIRIString());

        LOGGER.info("Assemble annotations using jena.");
        Resource commentURI;
        Literal label4, label2;
        Resource root = jena.createResource();
        jena.add(root, RDF.type, OWL.Annotation);
        jena.add(root, OWL.annotatedProperty, RDFS.label);
        jena.add(root, OWL.annotatedTarget, label4 = ResourceFactory.createLangLiteral(comment, commentLang));
        jena.add(root, RDFS.comment, commentURI = ResourceFactory.createResource(annotationProperty.getIRIString()));
        jena.add(root, RDFS.label, label2 = ResourceFactory.createPlainLiteral(label));
        Resource anon = jena.createResource();
        jena.add(root, OWL.annotatedSource, anon);
        jena.add(anon, RDF.type, OWL.Axiom);
        jena.add(anon, OWL.annotatedSource, ontClass);
        jena.add(anon, OWL.annotatedProperty, RDF.type);
        jena.add(anon, OWL.annotatedTarget, OWL.Class);
        jena.add(anon, RDFS.comment, commentURI);
        jena.add(anon, RDFS.label, label2);
        jena.add(anon, RDFS.label, label4);

        debug(owl);
        LOGGER.info("Check");

        OWLAxiom expected = factory.getOWLDeclarationAxiom(factory.getOWLClass(clazzIRI), Stream.of(
                factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(comment, commentLang), Stream.of(
                        factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                        factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))
                )),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))
        ).collect(Collectors.toSet()));

        LOGGER.info("Current axioms:");
        owl.axioms().forEach(LOGGER::debug);
        TestUtils.compareAxioms(Stream.of(expected), owl.axioms());

        LOGGER.info("Reload ontology.");
        OWLOntology reload = ReadWriteUtils.convertJenaToOWL(OntManagers.createOWL(), jena, null);
        LOGGER.info("Axioms after reload:");
        reload.axioms().forEach(LOGGER::debug);
        TestUtils.compareAxioms(Stream.of(expected), reload.axioms());
    }

    /**
     * test complex woody annotations.
     */
    @Test
    public void testComplexAnnotations() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/2");
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory factory = manager.getOWLDataFactory();
        long count = manager.ontologies().count();

        OWLOntologyID id1 = iri.toOwlOntologyID(iri.addPath("1.0"));
        LOGGER.info("Create ontology " + id1);
        OntologyModel owl1 = TestUtils.createModel(manager, id1);

        // plain annotations will go as assertion annotation axioms after reloading owl. so disable
        //OWLAnnotation simple1 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("PLAIN-1"));
        //OWLAnnotation simple2 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("PLAIN-2"));

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
        OWLAxiom axiom = factory.getOWLDeclarationAxiom(owlClass, Stream.of(root1, root2
                //        , simple2, simple1
        ).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, axiom));

        OWLAnnotation indiAnn = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("INDI-ANN"), factory.getRDFSComment("indi-comment"));
        OWLAxiom indiAxiom = factory.getOWLClassAssertionAxiom(owlClass, factory.getOWLNamedIndividual(iri.addFragment("Indi")), Stream.of(indiAnn).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, indiAxiom));

        debug(owl1);

        OWLOntologyID id2 = iri.toOwlOntologyID(iri.addPath("2.0"));
        LOGGER.info("Create ontology " + id2 + " (empty)");
        OntologyModel owl2 = TestUtils.createModel(manager, id2);
        Assert.assertEquals("Incorrect number of ontologies.", count + 2, manager.ontologies().count());

        LOGGER.info("Pass all content from " + id1 + " to " + id2 + " using jena");
        OntGraphModel source = owl1.asGraphModel();
        OntGraphModel target = owl2.asGraphModel();
        Iterator<Statement> toCopy = source.getBaseModel().listStatements().filterDrop(statement -> iri.toResource().equals(statement.getSubject()));
        toCopy.forEachRemaining(target::add);
        target.setNsPrefixes(source.getNsPrefixMap()); // just in case
        debug(owl2);

        LOGGER.info("Compare axioms"); // note! there is one more axiom in new ontology: Declaration(NamedIndividual(<http://test.org/annotations/2#Indi>))
        TestUtils.compareAxioms(owl1.axioms(), owl2.axioms().filter(OWLAxiom::isAnnotated));

        checkAxioms(owl1);
    }

    /**
     * test DifferentIndividuals, DisjointClasses, NegativeObjectPropertyAssertion, DisjointObjectProperties axioms.
     */
    @Test
    public void testBulkNaryAnnotatedAxioms() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/3");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
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

        debug(owl);

        // TODO: WARNING: ANNOTATIONS is not supported for DifferentIndividuals AXIOM by the OWL API (version 5.0.3)
        // TODO: Also for class-assertion if it binds anonymous individual
        // TODO: need to rewrite owl-loader to fix it.
        checkAxioms(owl, AxiomType.DIFFERENT_INDIVIDUALS, AxiomType.CLASS_ASSERTION);
    }

    /**
     * test "nary" annotated axioms: EquivalentClasses, EquivalentDataProperties, EquivalentObjectProperties, SameIndividual
     */
    @Test
    public void testNaryAnnotatedAxioms() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/4");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLClass clazz1 = factory.getOWLClass(iri.addFragment("MyClass1"));
        OWLClass clazz2 = factory.getOWLClass(iri.addFragment("MyClass2"));
        OWLClass clazz3 = factory.getOWLClass(iri.addFragment("MyClass3"));
        OWLIndividual ind1 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi1")); //factory.getOWLAnonymousIndividual();
        OWLIndividual ind2 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi2"));
        OWLIndividual ind3 = factory.getOWLNamedIndividual(iri.addFragment("MyIndi3"));
        OWLObjectPropertyExpression objectProperty1 = factory.getOWLObjectProperty(iri.addFragment("objectProperty1"));
        OWLObjectPropertyExpression objectProperty2 = factory.getOWLObjectProperty(iri.addFragment("objectProperty2"));
        OWLObjectPropertyExpression objectProperty3 = factory.getOWLObjectProperty(iri.addFragment("objectProperty3"));
        OWLDataPropertyExpression dataProperty1 = factory.getOWLDataProperty(iri.addFragment("dataProperty1"));
        OWLDataPropertyExpression dataProperty2 = factory.getOWLDataProperty(iri.addFragment("dataProperty2"));
        OWLDataPropertyExpression dataProperty3 = factory.getOWLDataProperty(iri.addFragment("dataProperty3"));
        OWLAnnotationProperty annotationProperty1 = factory.getOWLAnnotationProperty(iri.addFragment("annotationProperty1"));
        OWLAnnotationProperty annotationProperty2 = factory.getOWLAnnotationProperty(iri.addFragment("annotationProperty2"));

        // individuals should be processed first:
        owl.applyChanges(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz1, ind1)));
        owl.applyChanges(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz2, ind2)));
        owl.applyChanges(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(clazz3, ind3)));

        List<OWLNaryAxiom> axioms = new ArrayList<>();
        // equivalent classes
        OWLAnnotation ann1 = factory.getOWLAnnotation(annotationProperty1, factory.getOWLLiteral("property annotation N1"),
                factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("equivalent classes annotation N1")));
        axioms.add(factory.getOWLEquivalentClassesAxiom(
                Stream.of(clazz1, clazz2, clazz3).collect(Collectors.toSet()),
                Stream.of(ann1).collect(Collectors.toSet())));

        // equivalent data properties
        OWLAnnotation ann2 = factory.getOWLAnnotation(annotationProperty2, iri.addFragment("annotationValue1"));
        axioms.add(factory.getOWLEquivalentDataPropertiesAxiom(
                Stream.of(dataProperty1, dataProperty2, dataProperty3).collect(Collectors.toSet()),
                Stream.of(ann2).collect(Collectors.toSet())));

        // equivalent object properties
        OWLAnnotation ann3 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("equivalent object properties annotation"));
        axioms.add(factory.getOWLEquivalentObjectPropertiesAxiom(
                Stream.of(objectProperty1, objectProperty2, objectProperty3).collect(Collectors.toSet()),
                Stream.of(ann3).collect(Collectors.toSet())));

        // same individuals
        OWLAnnotation ann4 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("same individuals annotation", "fr"));
        axioms.add(factory.getOWLSameIndividualAxiom(
                Stream.of(ind1, ind2, ind3).collect(Collectors.toSet()),
                Stream.of(ann4).collect(Collectors.toSet())));

        LOGGER.info("Add pairwise axioms");

        axioms.forEach(a -> owl.applyChange(new AddAxiom(owl, a)));

        debug(owl);

        checkAxioms(owl);
    }

    /**
     * test axioms with a sub chain: DisjointUnion, SubPropertyChainOf, HasKey
     */
    @Test
    public void testAnnotatedAxiomsWithSubChain() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/5");
        OntologyModel owl = TestUtils.createModel(iri);

        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLClass clazz1 = factory.getOWLClass(iri.addFragment("MyClass1"));
        OWLClass clazz2 = factory.getOWLClass(iri.addFragment("MyClass2"));
        OWLClass clazz3 = factory.getOWLClass(iri.addFragment("MyClass3"));
        OWLClass clazz4 = factory.getOWLClass(iri.addFragment("MyClass4"));
        OWLObjectProperty op1 = factory.getOWLObjectProperty(iri.addFragment("ob-prop-1"));
        OWLObjectProperty op2 = factory.getOWLObjectProperty(iri.addFragment("ob-prop-2"));
        OWLObjectProperty top = factory.getOWLTopObjectProperty();

        OWLClassExpression ce1 = factory.getOWLObjectUnionOf(clazz3, clazz4);
        OWLObjectPropertyExpression ope1 = factory.getOWLObjectInverseOf(op2);

        List<OWLAxiom> axioms = new ArrayList<>();

        // disjointUnion
        OWLAnnotation ann1 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("some comment"));
        axioms.add(factory.getOWLDisjointUnionAxiom(clazz1, Stream.of(clazz2, ce1), Stream.of(ann1).collect(Collectors.toSet())));

        // SubPropertyChainOf
        OWLAnnotation ann2 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("sub-label", "xx"));
        axioms.add(factory.getOWLSubPropertyChainOfAxiom(Stream.of(op1, ope1).collect(Collectors.toList()), top, Stream.of(ann2).collect(Collectors.toSet())));

        // hasKey
        OWLAnnotation ann3 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), iri.addFragment("click-me/please"));
        OWLClassExpression ce2 = factory.getOWLObjectUnionOf(clazz2, ce1);
        axioms.add(factory.getOWLHasKeyAxiom(ce2, Stream.of(op1, op2).collect(Collectors.toSet()), Stream.of(ann3).collect(Collectors.toSet())));

        // hasKey
        OWLAnnotation ann4 = factory.getOWLAnnotation(factory.getRDFSIsDefinedBy(), iri.addFragment("do-not-click/please"));
        axioms.add(factory.getOWLHasKeyAxiom(clazz1, Stream.of(op1).collect(Collectors.toSet()), Stream.of(ann4).collect(Collectors.toSet())));

        LOGGER.info("Add annotated axioms");
        axioms.forEach(a -> owl.applyChanges(new AddAxiom(owl, a)));

        debug(owl);

        checkAxioms(owl);
    }

    @Test
    public void testSWRLRuleAnnotation() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/6");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLAnnotation _ann1 = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("label", "swrl"));
        OWLAnnotation _ann2 = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("comment", "swrl"), _ann1);
        OWLAnnotation annotation1 = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), iri.addPath("link").addFragment("some"), Stream.of(_ann1, _ann2));
        OWLAnnotation annotation2 = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(iri.addPath("ann").addFragment("prop")), factory.getOWLLiteral("ann-prop-lit", "s"));
        OWLAxiom axiom = factory.getSWRLRule(Collections.emptyList(), Collections.emptyList(), Stream.of(annotation1, annotation2).collect(Collectors.toList()));
        owl.applyChanges(new AddAxiom(owl, axiom));

        debug(owl);

        // Does't work due incorrect working with complex annotations in OWL-API (just try to reload ontology using only original OWL-API ver 5.0.3)
        // TODO: need to change OWL-API rdf-loader.
        //checkAxioms(owl);
    }

    @Test
    public void testAnnotateOntology() {
        OntIRI iri = OntIRI.create("http://test.org/annotations/7");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLAnnotationProperty property = factory.getOWLAnnotationProperty(iri.addFragment("ann-prop"));
        OWLLiteral someLiteral = factory.getOWLLiteral("some-literal", "eee");
        OWLLiteral label = factory.getOWLLiteral("annotation-label");
        OWLLiteral comment = factory.getOWLLiteral("annotation-comment");
        OntIRI link = iri.addFragment("see-also");

        // WARNING: OWL-API version 5.0.3 does NOT support complex annotations for ontology.
        // and this is contrary to the specification.
        // But our graph representation of OWLOntology provides fully correct graph for this case.
        // After reloading graph back to OWL-API loss information is expected.
        // TODO: need fix OWL-API graph loader also.
        OWLAnnotation _ann1 = factory.getOWLAnnotation(factory.getRDFSLabel(), label);
        OWLAnnotation _ann2 = factory.getOWLAnnotation(factory.getRDFSComment(), comment, _ann1);
        OWLAnnotation seeAlsoAnnotation = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), link, Stream.of(_ann1, _ann2));

        OWLAnnotation customPropertyAnnotation = factory.getOWLAnnotation(property, someLiteral);

        LOGGER.info("Annotate ontology.");
        owl.applyChanges(new AddOntologyAnnotation(owl, seeAlsoAnnotation));
        owl.applyChanges(new AddOntologyAnnotation(owl, customPropertyAnnotation));

        debug(owl);
        ReadWriteUtils.print(owl, OntFormat.FUNCTIONAL_SYNTAX);
        LOGGER.info("Annotations:");
        owl.annotations().forEach(LOGGER::debug);

        // checking
        OntGraphModel jena = owl.asGraphModel();
        // test annotation see-also:
        Assert.assertTrue("Can't find rdfs:comment " + comment, jena.contains(null, RDFS.comment, OWL2RDFHelper.toRDFNode(comment)));
        Assert.assertTrue("Can't find owl:annotatedTarget " + comment, jena.contains(null, OWL.annotatedTarget, OWL2RDFHelper.toRDFNode(comment)));
        Assert.assertEquals("Should be at least two rdf:label " + label, 2, jena.listStatements(null, RDFS.label, OWL2RDFHelper.toRDFNode(label)).toList().size());
        Assert.assertFalse("There is rdfs:seeAlso " + link + " attached to ontology.", jena.contains(iri.toResource(), RDFS.seeAlso, OWL2RDFHelper.toResource(link)));
        Resource seeAlsoRoot = jena.listStatements(null, RDFS.seeAlso, OWL2RDFHelper.toResource(link))
                .mapWith(Statement::getSubject).filterKeep(Resource::isAnon).toList().stream().findAny().orElse(null);
        Assert.assertNotNull("Can't find rdfs:seeAlso entrance.", seeAlsoRoot);
        Assert.assertTrue("Can't find owl:annotatedProperty", jena.contains(null, OWL.annotatedProperty, RDFS.seeAlso));
        Assert.assertTrue("Can't find owl:annotatedSource", jena.contains(null, OWL.annotatedSource, seeAlsoRoot));
        Assert.assertTrue("Can't find owl:annotatedTarget", jena.contains(null, OWL.annotatedTarget, OWL2RDFHelper.toResource(link)));

        // test annotation with custom property:
        Assert.assertTrue("Can't find " + property + " " + someLiteral, jena.contains(iri.toResource(), OWL2RDFHelper.toProperty(property), OWL2RDFHelper.toRDFNode(someLiteral)));
        Assert.assertTrue("Can't find declaration of " + property, jena.contains(OWL2RDFHelper.toResource(property), RDF.type, OWL.AnnotationProperty));

        LOGGER.info("Remove " + seeAlsoAnnotation);
        owl.applyChanges(new RemoveOntologyAnnotation(owl, seeAlsoAnnotation));
        debug(owl);
        // test annotation1:
        Assert.assertFalse("There is rdfs:comment " + comment, jena.contains(null, RDFS.comment, OWL2RDFHelper.toRDFNode(comment)));
        Assert.assertFalse("There is owl:annotatedTarget " + comment, jena.contains(null, OWL.annotatedTarget, OWL2RDFHelper.toRDFNode(comment)));
        Assert.assertEquals("There is rdf:label " + label, 0, jena.listStatements(null, RDFS.label, OWL2RDFHelper.toRDFNode(label)).toList().size());
        Assert.assertFalse("There is rdfs:seeAlso " + link, jena.contains(iri.toResource(), RDFS.seeAlso, OWL2RDFHelper.toResource(link)));
        // test annotation2:
        Assert.assertTrue("Can't find " + property + " " + someLiteral, jena.contains(iri.toResource(), OWL2RDFHelper.toProperty(property), OWL2RDFHelper.toRDFNode(someLiteral)));
        Assert.assertTrue("Can't find declaration of " + property, jena.contains(OWL2RDFHelper.toResource(property), RDF.type, OWL.AnnotationProperty));

        LOGGER.info("Remove " + customPropertyAnnotation);
        owl.applyChanges(new RemoveOntologyAnnotation(owl, customPropertyAnnotation));
        owl.remove(factory.getOWLDeclarationAxiom(property));
        debug(owl);
        List<Statement> rest = jena.listStatements().toList();
        LOGGER.debug("Rest statements : ");
        rest.forEach(LOGGER::debug);
        Assert.assertEquals("Expected only single triplet", 1, rest.size());
    }

    @Override
    Stream<OWLAxiom> filterAxioms(OWLOntology ontology, AxiomType... excluded) {
        List<OWLAxiom> res = new ArrayList<>();
        super.filterAxioms(ontology, excluded).filter(OWLAxiom::isAnnotated).forEach(axiom -> {
            if (axiom instanceof OWLNaryAxiom) {
                //noinspection unchecked
                res.addAll(((OWLNaryAxiom) axiom).splitToAnnotatedPairs());
            } else {
                res.add(axiom);
            }
        });
        return res.stream();
    }
}
