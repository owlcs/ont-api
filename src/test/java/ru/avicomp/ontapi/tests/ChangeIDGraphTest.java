package ru.avicomp.ontapi.tests;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.OntIRI;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationImplNotAnnotated;

/**
 * testing changing OWLOntologyID through jena and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ChangeIDGraphTest extends GraphTestBase {

    private static class ApplyChangesWrapper {
        private final SetOntologyIRI op;
        private final String msg;

        private ApplyChangesWrapper(SetOntologyIRI applyChanges, String msg) {
            this.op = applyChanges;
            this.msg = msg;
        }

        void process(OWLOntology o, IRI iri) {
            op.apply(o, iri);
        }

        @Override
        public String toString() {
            return msg;
        }

        private interface SetOntologyIRI {
            void apply(OWLOntology o, IRI iri);
        }
    }

    /**
     * WARNING: this test shows that there is a bug in OWL-API (5.0.5):
     * the original way (see {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl#applyChanges(List)})
     * doesn't work correctly in OWL-API.
     * The default method {@link OWLOntology#applyChanges(List)}) is never called due to it has explicit implementation.
     * In ONT-API there is no such problem.
     */
    @Test
    public void testApplyChanges() throws Exception {
        ApplyChangesWrapper w1 = new ApplyChangesWrapper((o, iri) -> o.applyChange(new SetOntologyID(o, iri)), "OWLOntology#applyChange()");
        ApplyChangesWrapper w3 = new ApplyChangesWrapper((o, iri) -> o.getOWLOntologyManager().applyChanges(new SetOntologyID(o, iri)), "OWLOntologyManager#applyChanges(...)");
        ApplyChangesWrapper w2 = new ApplyChangesWrapper((o, iri) -> o.getOWLOntologyManager().applyChange(new SetOntologyID(o, iri)), "OWLOntologyManager#applyChange()");
        ApplyChangesWrapper w4 = new ApplyChangesWrapper((o, iri) -> o.applyChanges(new SetOntologyID(o, iri)), "OWLOntology#applyChanges(...)");
        for (ApplyChangesWrapper w : Arrays.asList(w1, w2, w3, w4)) {
            //testApplyChanges(OntManagers.createOWL(), w); // <-- will fail on w4
            testApplyChanges(OntManagers.createONT(), w);
        }
    }

    private void testApplyChanges(OWLOntologyManager m, ApplyChangesWrapper p) throws Exception {
        String msg = "Test[" + (m instanceof OntologyManager ? "ONT" : "OWL") + "] " + p;
        LOGGER.info(msg);
        IRI x = IRI.create("x");
        OWLOntology o = m.createOntology(x);
        OWLOntologyID id1 = o.getOntologyID();
        LOGGER.info("1)iri=<" + x + ">, id=<" + id1 + ">");
        Assert.assertTrue("can't find " + x, m.contains(x));
        Assert.assertTrue("can't find " + id1, m.contains(id1));
        IRI y = IRI.create("y");
        p.process(o, y);
        OWLOntologyID id2 = o.getOntologyID();
        LOGGER.info("2)iri=<" + y + ">, id=<" + id2 + ">");
        Assert.assertFalse("still " + x, m.contains(x));
        Assert.assertFalse("still " + id1, m.contains(id1));
        Assert.assertTrue("can't find " + y, m.contains(y));
        Assert.assertTrue("can't find " + id2, m.contains(id2));
        LOGGER.debug("PASS: " + msg);
    }

    @Test
    public void testDifferent() throws Exception {
        OntologyManager manager = OntManagers.createONT();

        // anon ontology
        OntologyModel anon = manager.createOntology();
        Assert.assertEquals("Should be one ontology inside jena-graph", 1, anon.asGraphModel().listStatements(null, RDF.type, OWL.Ontology).toList().size());

        LOGGER.info("Create owl ontology.");
        OntIRI iri = OntIRI.create("http://test.test/change-id");
        OntIRI clazz = iri.addFragment("SomeClass1");
        List<Resource> imports = Stream.of(ResourceFactory.createResource("http://test.test/some-import")).collect(Collectors.toList());
        Map<Property, List<RDFNode>> annotations = new HashMap<>();
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>()).add(ResourceFactory.createLangLiteral("Some comment N1", "xyx"));
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>()).add(ResourceFactory.createPlainLiteral("Some comment N2"));
        annotations.computeIfAbsent(OWL.incompatibleWith, p -> new ArrayList<>()).add(ResourceFactory.createResource("http://yyy/zzz"));

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLOntologyID id = iri.toOwlOntologyID();
        LOGGER.debug("Create ontology, ID=" + id);
        OntologyModel owl = manager.createOntology(id);
        createOntologyProperties(owl, imports, annotations);
        OWLAnnotationProperty ap1 = factory.getOWLAnnotationProperty(iri.addFragment("annotation-property-1"));
        OWLAnnotation a1 = factory.getOWLAnnotation(ap1, factory.getOWLLiteral("tess-annotation-1"));
        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(clazz), Stream.of(a1).collect(Collectors.toList()))));
        OntGraphModel jena = owl.asGraphModel();
        debug(owl);

        long numOfOnt = manager.ontologies().count();

        OWLOntologyID test1 = iri.addPath("test1").toOwlOntologyID(OntIRI.create("http://version/1.0"));
        LOGGER.info("1)Change ontology iri to " + test1 + " through owl-api.");
        owl.applyChanges(new SetOntologyID(owl, test1));
        testIRIChanged(manager, owl, jena, test1, imports, annotations);
        testHasClass(owl, jena, clazz);
        Resource ontology = jena.listStatements(null, RDF.type, OWL.Ontology).mapWith(Statement::getSubject).toList().get(0);

        OWLOntologyID test2 = iri.addPath("test2").toOwlOntologyID(test1.getVersionIRI().orElse(null));
        LOGGER.info("2)Change ontology iri to " + test2 + " through jena.");
        ResourceUtils.renameResource(ontology, OntIRI.toStringIRI(test2));
        testIRIChanged(manager, owl, jena, test2, imports, annotations);
        testHasClass(owl, jena, clazz);
        ontology = jena.listStatements(null, RDF.type, OWL.Ontology).mapWith(Statement::getSubject).toList().get(0);

        // anon:
        OWLOntologyID test3 = new OWLOntologyID(); //iri.addPath("test3").toOwlOntologyID();
        LOGGER.info("3)Change ontology iri to " + test3 + " through jena.");
        ResourceUtils.renameResource(ontology, OntIRI.toStringIRI(test3));
        testIRIChanged(manager, owl, jena, test3, imports, annotations);
        testHasClass(owl, jena, clazz);

        OWLOntologyID test4 = iri.addPath("test4").toOwlOntologyID();
        LOGGER.info("4)Change ontology iri to " + test4 + " through owl-api.");
        manager.applyChange(new SetOntologyID(owl, test4));
        testIRIChanged(manager, owl, jena, test4, imports, annotations);
        testHasClass(owl, jena, clazz);

        //anon:
        OWLOntologyID test5 = new OWLOntologyID();
        LOGGER.info("5)Change ontology iri to " + test5 + " through owl-api.");
        manager.applyChange(new SetOntologyID(owl, test5));
        testIRIChanged(manager, owl, jena, test5, imports, annotations);
        testHasClass(owl, jena, clazz);

        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());
    }

    private static void testIRIChanged(OntologyManager manager, OntologyModel owl, OntGraphModel jena, OWLOntologyID id, List<Resource> imports, Map<Property, List<RDFNode>> annotations) {
        debug(owl);

        Assert.assertTrue("Can't find ontology " + id + " by ID", manager.contains(id));
        Assert.assertTrue("Can't find ontology " + id + " in manager", manager.contains(owl));
        if (id.getOntologyIRI().isPresent()) {
            Assert.assertTrue("Can't find " + id.getOntologyIRI().get() + " in manager", manager.contains(id.getOntologyIRI().get()));
        }

        String iri = id.getOntologyIRI().isPresent() ? id.getOntologyIRI().orElse(null).getIRIString() : null;
        OntID ontID = jena.getID();
        Assert.assertNotNull("Can't find new ontology for iri " + id, ontID);
        Assert.assertNotNull("Can't find new ontology in jena", owl.asGraphModel().getID());
        Assert.assertEquals("Incorrect jena id-iri", iri, ontID.getURI());
        Assert.assertTrue("Incorrect ID expected=" + id + ", actual=" + owl.getOntologyID(), (id.isAnonymous() && owl.getOntologyID().isAnonymous()) || owl.getOntologyID().equals(id));
        // check imports:
        List<String> expected = imports.stream().map(Resource::getURI).sorted().collect(Collectors.toList());
        List<String> actualOwl = owl.importsDeclarations().map(OWLImportsDeclaration::getIRI).map(IRI::getIRIString).sorted().collect(Collectors.toList());
        List<String> actualJena = jena.getID().imports().sorted().collect(Collectors.toList());
        Assert.assertEquals("Incorrect owl imports", expected, actualOwl);
        Assert.assertEquals("Incorrect jena imports", expected, actualJena);
        // check owl-annotations:
        int count = 0;
        for (Property property : annotations.keySet()) {
            count += annotations.get(property).size();
            annotations.get(property).forEach(node -> {
                OWLAnnotation a = toOWLAnnotation(property, node);
                Assert.assertTrue("Can't find annotation " + a, owl.annotations().anyMatch(a::equals));
            });
        }
        Assert.assertEquals("Incorrect annotation count", count, owl.annotations().count());
        // check jena annotations:
        for (Property property : annotations.keySet()) {
            List<RDFNode> actualList = jena.listStatements(ontID, property, (RDFNode) null).mapWith(Statement::getObject).
                    toList().stream().sorted(Models.RDF_NODE_COMPARATOR).collect(Collectors.toList());
            List<RDFNode> expectedList = annotations.get(property).stream().sorted(Models.RDF_NODE_COMPARATOR).collect(Collectors.toList());
            Assert.assertEquals("Incorrect list of annotations", expectedList, actualList);
        }
    }

    private static void testHasClass(OntologyModel owl, OntGraphModel jena, IRI classIRI) {
        OWLEntity entity = owl.axioms(AxiomType.DECLARATION).map(OWLDeclarationAxiom::getEntity).filter(AsOWLClass::isOWLClass).findFirst().orElse(null);
        Assert.assertNotNull("Can't find any owl-class", entity);
        Assert.assertEquals("Incorrect owl-class uri", classIRI, entity.getIRI());
        List<OntClass> classes = jena.ontEntities(OntClass.class).collect(Collectors.toList());
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
        return toOWLAnnotation(OntManagers.getDataFactory(), property, node);
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
}