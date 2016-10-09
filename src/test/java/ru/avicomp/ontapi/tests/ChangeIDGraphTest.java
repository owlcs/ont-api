package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationImplNotAnnotated;

/**
 * testing changing OWLOntologyID through jena and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ChangeIDGraphTest extends GraphTestBase {

    private static void testIRIChanged(OntologyModel owl, OntModel jena, OWLOntologyID id, List<Resource> imports, Map<Property, List<RDFNode>> annotations) {
        debug(owl);
        String iri = id.getOntologyIRI().isPresent() ? id.getOntologyIRI().orElse(null).getIRIString() : null;
        Ontology ont = getOntology(jena);
        Assert.assertNotNull("Can't find new ontology for iri " + id, ont);
        Assert.assertNotNull("Can't find new ontology in jena", getOntology(owl.asGraphModel()));
        Assert.assertEquals("Incorrect jena id-iri", iri, ont.getURI());
        Assert.assertTrue("Incorrect owl id-iri", (id.isAnonymous() && owl.getOntologyID().isAnonymous()) || owl.getOntologyID().equals(id));
        // check imports:
        List<String> expected = imports.stream().map(Resource::getURI).sorted().collect(Collectors.toList());
        List<String> actualOwl = owl.importsDeclarations().map(OWLImportsDeclaration::getIRI).map(IRI::getIRIString).sorted().collect(Collectors.toList());
        List<String> actualJena = ont.listImports().mapWith(Resource::getURI).toList().stream().sorted().collect(Collectors.toList());
        Assert.assertEquals("Incorrect owl imports", expected, actualOwl);
        Assert.assertEquals("Incorrect jena imports", expected, actualJena);
        // check owl-annotations:
        int count = 0;
        for (Property property : annotations.keySet()) {
            count += annotations.get(property).size();
            annotations.get(property).forEach(node -> {
                OWLAnnotation a = toOWLAnnotation(property, node);
                Assert.assertTrue("Can't find annotation " + a, owl.annotations().filter(a::equals).findFirst().isPresent());
            });
        }
        Assert.assertEquals("Incorrect annotation count", count, owl.annotations().count());
        // check jena annotations:
        for (Property property : annotations.keySet()) {
            List<RDFNode> actualList = jena.listStatements(ont, property, (RDFNode) null).mapWith(Statement::getObject).
                    toList().stream().sorted(RDF_NODE_COMPARATOR).collect(Collectors.toList());
            List<RDFNode> expectedList = annotations.get(property).stream().sorted(RDF_NODE_COMPARATOR).collect(Collectors.toList());
            Assert.assertEquals("Incorrect list of annotations", expectedList, actualList);
        }
    }

    private static void testHasClass(OntologyModel owl, OntModel jena, IRI classIRI) {
        OWLDeclarationAxiom axiom = owl.axioms(AxiomType.DECLARATION).findFirst().orElse(null);
        Assert.assertNotNull("Can't find any owl-class", axiom);
        Assert.assertEquals("Incorrect owl-class uri", classIRI, axiom.getEntity().getIRI());
        List<OntClass> classes = jena.listClasses().toList();
        Assert.assertFalse("Can't find any jena-class", classes.isEmpty());
        Assert.assertEquals("Incorrect jena-class uri", classIRI.getIRIString(), classes.get(0).getURI());
    }

    private static void createOntologyProperties(OntologyModel owl, List<Resource> imports, Map<Property, List<RDFNode>> annotations) {
        OWLOntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        imports.forEach(r -> manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(OntIRI.create(r.getURI())))));
        for (Property property : annotations.keySet()) {
            annotations.get(property).forEach(node -> manager.applyChange(new AddOntologyAnnotation(owl, toOWLAnnotation(factory, property, node))));
        }
    }

    private static OWLAnnotation toOWLAnnotation(Property property, RDFNode node) {
        return toOWLAnnotation(OWLManager.createOWLOntologyManager().getOWLDataFactory(), property, node);
    }

    private static OWLAnnotation toOWLAnnotation(OWLDataFactory factory, Property property, RDFNode node) {
        OWLAnnotationProperty p = factory.getOWLAnnotationProperty(OntIRI.create(property));
        OWLAnnotationValue v = null;
        if (node.isURIResource()) {
            v = OntIRI.create(node.asResource());
        } else if (node.isLiteral()) {
            Literal literal = node.asLiteral();
            v = factory.getOWLLiteral(literal.getLexicalForm(), literal.getLanguage());
        } else {
            Assert.fail("Unknown node " + node);
        }
        return new OWLAnnotationImplNotAnnotated(p, v);
    }

    @Test
    public void test() throws OWLOntologyCreationException {
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();

        // anon ontology
        OntologyModel anon = (OntologyModel) manager.createOntology();
        Assert.assertEquals("Should be one ontology inside jena-graph", 1, anon.asGraphModel().listOntologies().toList().size());

        LOGGER.info("Create owl ontology.");
        OntIRI iri = OntIRI.create("http://test.test/change-id");
        OntIRI clazz = iri.addFragment("SomeClass1");
        List<Resource> imports = Stream.of(ResourceFactory.createResource("http://test.test/some-import")).collect(Collectors.toList());
        Map<Property, List<RDFNode>> annotations = new HashMap<>();
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>()).add(ResourceFactory.createLangLiteral("Some comment N1", "xyx"));
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>()).add(ResourceFactory.createPlainLiteral("Some comment N2"));
        annotations.computeIfAbsent(OWL.incompatibleWith, p -> new ArrayList<>()).add(ResourceFactory.createResource("http://yyy/zzz"));

        OWLDataFactory factory = manager.getOWLDataFactory();
        OntologyModel owl = (OntologyModel) manager.createOntology(iri.toOwlOntologyID());
        createOntologyProperties(owl, imports, annotations);
        OWLAnnotationProperty ap1 = factory.getOWLAnnotationProperty(iri.addFragment("annotation-property-1"));
        OWLAnnotation a1 = factory.getOWLAnnotation(ap1, factory.getOWLLiteral("tess-annotation-1"));
        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(clazz), Stream.of(a1).collect(Collectors.toList()))));
        OntModel jena = owl.asGraphModel();
        debug(owl);

        long numOfOnt = manager.ontologies().count();

        OWLOntologyID test1 = iri.addPath("test1").toOwlOntologyID(OntIRI.create("http://version/1.0"));
        LOGGER.info("1)Change ontology iri to " + test1 + " through owl-api.");
        owl.applyChanges(new SetOntologyID(owl, test1));
        testIRIChanged(owl, jena, test1, imports, annotations);
        testHasClass(owl, jena, clazz);
        Ontology ontology = jena.listOntologies().toList().get(0);

        OWLOntologyID test2 = iri.addPath("test2").toOwlOntologyID(test1.getVersionIRI().orElse(null));
        LOGGER.info("2)Change ontology iri to " + test2 + " through jena.");
        ResourceUtils.renameResource(ontology, OntIRI.toStringIRI(test2));
        testIRIChanged(owl, jena, test2, imports, annotations);
        testHasClass(owl, jena, clazz);
        ontology = jena.listOntologies().toList().get(0);

        // anon:
        OWLOntologyID test3 = new OWLOntologyID(); //iri.addPath("test3").toOwlOntologyID();
        LOGGER.info("3)Change ontology iri to " + test3 + " through jena.");
        ResourceUtils.renameResource(ontology, OntIRI.toStringIRI(test3));
        testIRIChanged(owl, jena, test3, imports, annotations);
        testHasClass(owl, jena, clazz);

        OWLOntologyID test4 = iri.addPath("test4").toOwlOntologyID();
        LOGGER.info("4)Change ontology iri to " + test4 + " through owl-api.");
        manager.applyChange(new SetOntologyID(owl, test4));
        testIRIChanged(owl, jena, test4, imports, annotations);
        testHasClass(owl, jena, clazz);

        //anon:
        OWLOntologyID test5 = new OWLOntologyID();
        LOGGER.info("5)Change ontology iri to " + test5 + " through owl-api.");
        manager.applyChange(new SetOntologyID(owl, test5));
        testIRIChanged(owl, jena, test5, imports, annotations);
        testHasClass(owl, jena, clazz);

        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());
    }
}