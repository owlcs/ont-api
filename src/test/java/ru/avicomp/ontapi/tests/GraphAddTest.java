package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.SomeValuesFromRestriction;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
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
public class GraphAddTest {
    private static final Logger LOGGER = Logger.getLogger(GraphAddTest.class);

    @Test
    public void addImportTest() throws OWLOntologyCreationException {
        OntIRI iri = OntIRI.create("http://test.test/add-import");
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.asGraphModel();

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
        OntModel jena = owl.asGraphModel();

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

    @Test
    public void addTest() throws OWLOntologyCreationException {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        LOGGER.info("The file " + fileIRI);

        OntologyModel original = (OntologyModel) OntManagerFactory.createOWLOntologyManager().loadOntology(fileIRI);
        debug(original);

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

        //complex2.addDisjointWith(simple2);
        //complex2.addDisjointWith(simple1);
        // bulk disjoint instead adding one by one (to have the same list of axioms):
        Resource anon = jena.createResource();
        jena.add(anon, RDF.type, OWL2.AllDisjointClasses);
        jena.add(anon, OWL2.members, jena.createList(Stream.of(complex2, simple1, simple2).iterator()));
        LOGGER.debug("Result ontology: ");
        ReadWriteUtils.print(jena, OntFormat.TTL_RDF);
        jena.rebind(); // rebind because we have several bulk axioms.
        result.axioms().forEach(LOGGER::debug);
        compareAxioms(original.axioms(), result.axioms());
    }

    private static void compareAxioms(Stream<? extends OWLAxiom> axioms1, Stream<? extends OWLAxiom> axioms2) {
        List<OWLAxiom> list1 = axioms1.sorted().collect(Collectors.toList());
        List<OWLAxiom> list2 = axioms2.sorted().collect(Collectors.toList());
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

    private void debug(OntologyModel ontology) {
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        LOGGER.debug("Jena: ");
        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TTL_RDF);
    }
}
