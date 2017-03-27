package ru.avicomp.ontapi.tests;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.internal.WriteHelper;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
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
    public void testSingleComplexAnnotation() throws Exception {
        OntIRI iri = OntIRI.create("http://test.org/annotations/1");
        // test data:
        OntIRI clazzIRI = iri.addFragment("SomeClass1");
        OntIRI annotationProperty = iri.addFragment("some-annotation-property");
        String comment = "comment here";
        String commentLang = "s";
        String label = "some-label";

        LOGGER.info("Create fresh ontology (" + iri + ").");
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = manager.createOntology(iri);

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

        Set<OWLAnnotation> annotations = Stream.of(
                factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(comment, commentLang),
                        Stream.of(
                                factory.getOWLAnnotation(factory.getRDFSComment(), annotationProperty),
                                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))
                        )
                ),
                factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label))).collect(Collectors.toSet());
        OWLAxiom expected = factory.getOWLDeclarationAxiom(factory.getOWLClass(clazzIRI), annotations);

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
    public void testComplexAnnotations() throws Exception {
        OntIRI iri = OntIRI.create("http://test.org/annotations/2");
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory factory = manager.getOWLDataFactory();
        long count = manager.ontologies().count();

        OWLOntologyID id1 = iri.toOwlOntologyID(iri.addPath("1.0"));
        LOGGER.info("Create ontology " + id1);
        OntologyModel owl1 = manager.createOntology(id1);

        // plain annotations will go as assertion annotation axioms after reloading owl. so disable
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
        OWLAxiom classAxiom = factory.getOWLDeclarationAxiom(owlClass, Stream.of(root1, root2, simple2, simple1).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, classAxiom));

        OWLAnnotation individualAnn = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("INDI-ANN"), factory.getRDFSComment("indi-comment"));
        OWLAxiom individualAxiom = factory.getOWLClassAssertionAxiom(owlClass, factory.getOWLNamedIndividual(iri.addFragment("Indi")), Stream.of(individualAnn).collect(Collectors.toSet()));
        owl1.applyChange(new AddAxiom(owl1, individualAxiom));

        debug(owl1);

        OWLOntologyID id2 = iri.toOwlOntologyID(iri.addPath("2.0"));
        LOGGER.info("Create ontology " + id2 + " (empty)");
        OntologyModel owl2 = manager.createOntology(id2);
        Assert.assertEquals("Incorrect number of ontologies.", count + 2, manager.ontologies().count());

        LOGGER.info("Pass all content from " + id1 + " to " + id2 + " using jena");
        OntGraphModel source = owl1.asGraphModel();
        OntGraphModel target = owl2.asGraphModel();
        Iterator<Statement> toCopy = source.getBaseModel().listStatements().filterDrop(statement -> iri.toResource().equals(statement.getSubject()));
        toCopy.forEachRemaining(target::add);
        target.setNsPrefixes(source.getNsPrefixMap()); // just in case
        debug(owl2);

        LOGGER.info("Validate axioms");
        Assert.assertEquals("Incorrect axioms count", 5, owl2.getAxiomCount());
        Assert.assertEquals("Should be single class-assertion", 1, owl2.axioms(AxiomType.CLASS_ASSERTION).count());
        Assert.assertEquals("Should be two annotation-assertions", 2, owl2.axioms(AxiomType.ANNOTATION_ASSERTION).count());
        Assert.assertEquals("Should be two declarations(class + individual)", 2, owl2.axioms(AxiomType.DECLARATION).count());
        Assert.assertEquals("Should be two annotated axioms", 2, owl2.axioms().filter(OWLAxiom::isAnnotated).count());
        // no assertion annotations iside:
        OWLAxiom expectedClassAxiom = factory.getOWLDeclarationAxiom(owlClass, Stream.of(root1, root2).collect(Collectors.toSet()));
        Assert.assertTrue("Incorrect declaration class axiom", owl2.containsAxiom(expectedClassAxiom));
        Assert.assertTrue("Incorrect class assertion axiom", owl2.containsAxiom(individualAxiom));
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

    /**
     * WARNING: OWL-API version 5.0.3 does NOT support complex annotations for ontology header.
     * And this seems contrary to the specification.
     * ONT-API graph representation of OWLOntology provides fully correct graph for this case.
     * After reloading graph back to OWL-API loss information is expected.
     *
     * @throws Exception if smt wrong
     */
    @Test
    public void testAnnotateOntology() throws Exception {
        annotateOntologyTest(OntManagers.createONT());
    }

    private void annotateOntologyTest(OWLOntologyManager manager) throws Exception {
        long count = manager.ontologies().count();
        OntIRI iri = OntIRI.create("http://test.org/annotations/7");
        OWLOntology o1 = manager.createOntology(iri);

        OWLDataFactory df = OntManagers.getDataFactory();
        OWLAnnotationProperty annotationProperty = df.getOWLAnnotationProperty(iri.addFragment("ann-prop"));
        OWLLiteral someLiteral = df.getOWLLiteral("some-literal", "eee");
        OWLLiteral label = df.getOWLLiteral("annotation-label");
        OWLLiteral comment = df.getOWLLiteral("annotation-comment");
        OntIRI link = iri.addFragment("see-also");

        OWLAnnotation _ann1 = df.getOWLAnnotation(df.getRDFSLabel(), label);
        OWLAnnotation _ann2 = df.getOWLAnnotation(df.getRDFSComment(), comment, _ann1);
        OWLAnnotation seeAlsoAnnotation = df.getOWLAnnotation(df.getRDFSSeeAlso(), link, Stream.of(_ann1, _ann2));
        OWLAnnotation customPropertyAnnotation = df.getOWLAnnotation(annotationProperty, someLiteral);

        LOGGER.info("1) Annotate ontology.");
        o1.applyChanges(new AddOntologyAnnotation(o1, seeAlsoAnnotation));
        o1.applyChanges(new AddOntologyAnnotation(o1, customPropertyAnnotation));

        String txt = ReadWriteUtils.toString(o1, OntFormat.TURTLE);
        LOGGER.debug(txt);
        ReadWriteUtils.print(o1, OntFormat.FUNCTIONAL_SYNTAX);
        LOGGER.debug("Annotations:");
        o1.annotations().forEach(LOGGER::debug);
        // checking jena shadow
        if (manager instanceof OntologyManager) {
            OntGraphModel jena = ((OntologyModel) o1).asGraphModel();
            // test annotation see-also:
            Assert.assertTrue("Can't find rdfs:comment " + comment, jena.contains(null, RDFS.comment, WriteHelper.toRDFNode(comment)));
            Assert.assertTrue("Can't find owl:annotatedTarget " + comment, jena.contains(null, OWL.annotatedTarget, WriteHelper.toRDFNode(comment)));
            Assert.assertEquals("Should be at least two rdf:label " + label, 2, jena.listStatements(null, RDFS.label, WriteHelper.toRDFNode(label)).toList().size());
            Assert.assertTrue("Can't find rdfs:seeAlso " + link + " attached to ontology.", jena.contains(iri.toResource(), RDFS.seeAlso, WriteHelper.toResource(link)));
            Resource seeAlsoRoot = jena.statements(null, OWL.annotatedProperty, RDFS.seeAlso)
                    .map(Statement::getSubject)
                    .filter(Resource::isAnon)
                    .filter(r -> r.hasProperty(RDF.type, OWL.Axiom))
                    .findFirst().orElseThrow(() -> new AssertionError("No axiom root for rdfs:seeAlso"));
            Assert.assertTrue("Can't find owl:annotatedSource", seeAlsoRoot.hasProperty(OWL.annotatedSource, jena.getID()));
            Assert.assertTrue("Can't find  owl:annotatedTarget", seeAlsoRoot.hasProperty(OWL.annotatedTarget, WriteHelper.toResource(link)));
            // test annotation with custom property:
            Assert.assertTrue("Can't find " + annotationProperty + " " + someLiteral, jena.contains(iri.toResource(), WriteHelper.toProperty(annotationProperty), WriteHelper.toRDFNode(someLiteral)));
            Assert.assertTrue("Can't find declaration of " + annotationProperty, jena.contains(WriteHelper.toResource(annotationProperty), RDF.type, OWL.AnnotationProperty));
        }

        LOGGER.info("2) Remove " + seeAlsoAnnotation);
        o1.applyChanges(new RemoveOntologyAnnotation(o1, seeAlsoAnnotation));
        debug(o1);
        Assert.assertFalse("The annotation " + seeAlsoAnnotation + " still present", o1.annotations().anyMatch(seeAlsoAnnotation::equals));

        // test jena annotation1:
        if (manager instanceof OntologyManager) {
            OntGraphModel jena = ((OntologyModel) o1).asGraphModel();
            Assert.assertFalse("There is rdfs:comment " + comment, jena.contains(null, RDFS.comment, WriteHelper.toRDFNode(comment)));
            Assert.assertFalse("There is owl:annotatedTarget " + comment, jena.contains(null, OWL.annotatedTarget, WriteHelper.toRDFNode(comment)));
            Assert.assertEquals("There is rdf:label " + label, 0, jena.listStatements(null, RDFS.label, WriteHelper.toRDFNode(label)).toList().size());
            Assert.assertFalse("There is rdfs:seeAlso " + link, jena.contains(iri.toResource(), RDFS.seeAlso, WriteHelper.toResource(link)));
            // test annotation2:
            Assert.assertTrue("Can't find " + annotationProperty + " " + someLiteral, jena.contains(iri.toResource(), WriteHelper.toProperty(annotationProperty), WriteHelper.toRDFNode(someLiteral)));
            Assert.assertTrue("Can't find declaration of " + annotationProperty, jena.contains(WriteHelper.toResource(annotationProperty), RDF.type, OWL.AnnotationProperty));
        }

        LOGGER.info("3) Remove " + customPropertyAnnotation);
        o1.applyChanges(new RemoveOntologyAnnotation(o1, customPropertyAnnotation));
        Assert.assertFalse("The annotation " + customPropertyAnnotation + " still present", o1.annotations().anyMatch(customPropertyAnnotation::equals));
        o1.remove(df.getOWLDeclarationAxiom(annotationProperty));
        debug(o1);
        if (manager instanceof OntologyManager) {
            OntGraphModel jena = ((OntologyModel) o1).asGraphModel();
            List<Statement> rest = jena.listStatements().toList();
            LOGGER.debug("Rest statements : ");
            rest.forEach(LOGGER::debug);
            Assert.assertEquals("Expected only single triplet", 1, rest.size());
        }

        LOGGER.info("4) Reload original ontology.");
        IRI ver = IRI.create("http://version/2");
        txt += String.format("<%s> <%s> <%s> .", iri, OWL.versionIRI, ver);
        LOGGER.debug("\n" + txt + "\n");
        OWLOntology o2 = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(txt));
        Assert.assertEquals("Incorrect ontologies count", count + 2, manager.ontologies().count());
        List<OWLAnnotation> actual = o2.annotations().sorted().collect(Collectors.toList());
        List<OWLAnnotation> expected = Stream.of(seeAlsoAnnotation, customPropertyAnnotation).sorted().collect(Collectors.toList());
        Assert.assertEquals("Incorrect ontology annotations after reloading", expected, actual);
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
