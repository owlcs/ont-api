package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * todo:
 * Created by @szuev on 02.10.2016.
 */
public class GraphTest {
    private static final Logger LOGGER = Logger.getLogger(GraphTest.class);

    @Test
    public void importsTest() throws OWLOntologyCreationException {
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

    @Test
    public void changeIDTest() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/change-id");
        OntIRI clazz = iri.addFragment("SomeClass1");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();

        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        long numOfOnt = manager.ontologies().count();
        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(clazz))));
        OntModel jena = owl.asGraphModel();
        debug(owl);

        OntIRI test1 = iri.addPath("test1");
        LOGGER.info("Change ontology iri to " + test1 + " through owl-api");
        owl.applyChanges(new SetOntologyID(owl, test1.toOwlOntologyID()));
        testIRIChanged(owl, jena, test1, clazz);

        OntIRI test2 = iri.addPath("test2");
        LOGGER.info("Change ontology iri to " + test2 + " through jena");
        ResourceUtils.renameResource(jena.getOntology(test1.getIRIString()), test2.getIRIString());
        testIRIChanged(owl, jena, test2, clazz);

        OntIRI test3 = iri.addPath("test3");
        LOGGER.info("Change ontology iri to " + test3 + " through jena");
        ResourceUtils.renameResource(jena.getOntology(test2.getIRIString()), test3.getIRIString());
        testIRIChanged(owl, jena, test3, clazz);

        OntIRI test4 = iri.addPath("test4");
        LOGGER.info("Change ontology iri to " + test4 + " through owl-api");
        manager.applyChange(new SetOntologyID(owl, test4.toOwlOntologyID()));
        testIRIChanged(owl, jena, test4, clazz);

        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());
    }

    private static void testIRIChanged(OntologyModel owl, OntModel jena, IRI ontologyIRI, IRI classIRI) {
        debug(owl);
        Assert.assertNotNull("Can't find new ontology for iri " + ontologyIRI, jena.getOntology(ontologyIRI.getIRIString()));
        Assert.assertNotNull("Can't find new ontology in jena", owl.asGraphModel().getOntology(ontologyIRI.getIRIString()));
        Assert.assertEquals("Incorrect owl id iri", ontologyIRI, owl.getOntologyID().getOntologyIRI().orElse(null));
        // check class still has the same uri:
        OWLDeclarationAxiom axiom = owl.axioms(AxiomType.DECLARATION).findFirst().orElse(null);
        Assert.assertNotNull("Can't find any owl-class", axiom);
        Assert.assertEquals("Incorrect owl-class uri", classIRI, axiom.getEntity().getIRI());
        List<OntClass> classes = jena.listClasses().toList();
        Assert.assertFalse("Can't find any jena-class", classes.isEmpty());
        Assert.assertEquals("Incorrect jena-class uri", classIRI.getIRIString(), classes.get(0).getURI());
    }

    @Test
    public void individualsTest() throws OWLOntologyCreationException {
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

    @Test
    public void disjointClassesTest() throws OWLOntologyCreationException {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        LOGGER.info("Load ontology from file " + fileIRI);
        OntologyModel original = (OntologyModel) OntManagerFactory.createOWLOntologyManager().loadOntology(fileIRI);
        debug(original);

        LOGGER.info("Assemble new ontology with the same content.");
        OntIRI iri = OntIRI.create("http://test.test/complex");
        OntIRI ver = OntIRI.create("http://test.test/complex/version-iri/1.0");
        OntologyModel result = (OntologyModel) OntManagerFactory.createOWLOntologyManager().createOntology(iri.toOwlOntologyID());
        OntModel jena = result.asGraphModel();
        jena.setNsPrefix("", iri.getIRIString() + "#");
        jena.add(jena.getOntology(iri.getIRIString()), OWL2.versionIRI, ver.toResource());

        OntClass simple1 = jena.createClass(iri.addFragment("Simple1").getIRIString());
        OntClass simple2 = jena.createClass(iri.addFragment("Simple2").getIRIString());
        OntClass complex1 = jena.createClass(iri.addFragment("Complex1").getIRIString());
        OntClass complex2 = jena.createClass(iri.addFragment("Complex2").getIRIString());

        ObjectProperty property = jena.createObjectProperty(iri.addFragment("hasSimple1").getIRIString(), true);
        property.addRange(simple1);
        SomeValuesFromRestriction restriction = jena.createSomeValuesFromRestriction(null, property, simple2);
        complex2.addSuperClass(restriction);
        complex2.addSuperClass(complex1);
        complex2.addComment("comment1", "es");
        complex1.addDisjointWith(simple1);

        // bulk disjoint instead adding one by one (to have the same list of axioms):
        Resource anon = jena.createResource();
        jena.add(anon, RDF.type, OWL2.AllDisjointClasses);
        jena.add(anon, OWL2.members, jena.createList(Stream.of(complex2, simple1, simple2).iterator()));

        jena.rebind(); // rebind because we have several bulk axioms.
        LOGGER.info("After rebind.");
        ReadWriteUtils.print(jena, OntFormat.TTL_RDF);

        LOGGER.info("Compare axioms.");
        result.axioms().forEach(LOGGER::debug);
        compareAxioms(original.axioms(), result.axioms());

        LOGGER.info("Remove OWL:disjointWith");
        ReadWriteUtils.print(jena, OntFormat.TTL_RDF);
        jena.removeAll(complex1, OWL.disjointWith, null);
        ReadWriteUtils.print(result.asGraphModel(), OntFormat.TTL_RDF);
        Assert.assertEquals("Incorrect axiom count", original.axioms().count() - 1, result.axioms().count());

        LOGGER.info("Remove OWL:AllDisjointClasses");
        anon = jena.listResourcesWithProperty(RDF.type, OWL2.AllDisjointClasses).toList().get(0);
        RDFList list = jena.listObjectsOfProperty(anon, OWL2.members).mapWith(n -> n.as(RDFList.class)).toList().get(0);
        list.removeList();
        jena.removeAll(anon, null, null);

        LOGGER.info("Events");
        result.getEventStore().getEvents().forEach(LOGGER::debug);

        LOGGER.info("Compare axioms.");
        result.axioms().forEach(LOGGER::debug);
        compareAxioms(original.axioms().filter(axiom -> !AxiomType.DISJOINT_CLASSES.equals(axiom.getAxiomType())), result.axioms());
        debug(result);

    }

    private static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
        List<OWLAxiom> list1 = expected.sorted().collect(Collectors.toList());
        List<OWLAxiom> list2 = actual.sorted().collect(Collectors.toList());
        Assert.assertEquals("Not equal axioms streams count", list1.size(), list2.size());
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < list1.size(); i++) {
            OWLAxiom a = list1.get(i);
            OWLAxiom b = list2.get(i);
            if (equals(a, b)) continue;
            errors.add(String.format("%s != %s", a, b));
        }
        errors.forEach(LOGGER::error);
        Assert.assertTrue("There are " + errors.size() + " errors", errors.isEmpty());
    }

    public static boolean equals(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex() && OWLAPIStreamUtils.equalStreams(a.components(), b.components());
    }

    private static void debug(OntologyModel ontology) {
        LOGGER.debug("DEBUG");
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        LOGGER.debug("Jena: ");
        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TTL_RDF);
    }
}
