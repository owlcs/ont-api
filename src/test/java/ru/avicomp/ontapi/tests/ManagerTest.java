package ru.avicomp.ontapi.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.jena.converters.GraphTransformConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.translators.RDF2OWLHelper;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test core ({@link ru.avicomp.ontapi.OntManagerFactory})
 * (+ testing serialization_
 * <p>
 * Created by szuev on 22.12.2016.
 */
public class ManagerTest {

    private static final Logger LOGGER = Logger.getLogger(ManagerTest.class);

    @Test
    public void testBasics() throws OWLOntologyCreationException {
        final IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        final OWLOntologyID id = OntIRI.create("http://dummy").toOwlOntologyID();

        Assert.assertNotSame("The same manager", OntManagerFactory.createONTManager(), OntManagerFactory.createONTManager());
        Assert.assertNotSame("The same concurrent manager", OntManagerFactory.createONTConcurrentManager(), OntManagerFactory.createONTConcurrentManager());

        OntologyManagerImpl m1 = (OntologyManagerImpl) OntManagerFactory.createONTManager();
        Assert.assertFalse("Concurrent", m1.isConcurrent());

        OntologyModel ont1 = m1.loadOntology(fileIRI);
        OntologyModel ont2 = m1.createOntology(id);
        Assert.assertEquals("Incorrect num of ontologies", 2, m1.ontologies().count());
        Stream.of(ont1, ont2).forEach(o -> {
            Assert.assertEquals("Incorrect impl", OntologyModelImpl.class, ont1.getClass());
            Assert.assertNotEquals("Incorrect impl", OntologyModelImpl.Concurrent.class, ont1.getClass());
        });

        OntologyManagerImpl m2 = (OntologyManagerImpl) OntManagerFactory.createONTConcurrentManager();
        Assert.assertTrue("Not Concurrent", m2.isConcurrent());
        OntologyModel ont3 = m2.loadOntology(fileIRI);
        OntologyModel ont4 = m2.createOntology(id);
        Assert.assertEquals("Incorrect num of ontologies", 2, m2.ontologies().count());
        Stream.of(ont3, ont4).forEach(o -> {
            Assert.assertNotEquals("Incorrect impl", OntologyModelImpl.class, ont3.getClass());
            Assert.assertEquals("Incorrect impl", OntologyModelImpl.Concurrent.class, ont3.getClass());
        });
    }

    @Test
    public void testConfigs() {
        OntologyManager m1 = OntManagerFactory.createONTManager();
        OntologyManager m2 = OntManagerFactory.createONTManager();
        OntConfig.LoaderConfiguration conf1 = m1.getOntologyLoaderConfiguration();
        conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        OntConfig.LoaderConfiguration conf2 = m2.getOntologyLoaderConfiguration();
        conf2.setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT);
        Assert.assertNotEquals("The same loader configs", conf1, conf2);
        Assert.assertEquals("Not the same personalities", conf1.getPersonality(), conf2.getPersonality());
        m1.setOntologyLoaderConfiguration(conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_LAX));
        m2.setOntologyLoaderConfiguration(conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT));
        Assert.assertNotEquals("The same personalities", m1.getOntologyLoaderConfiguration().getPersonality(),
                m2.getOntologyLoaderConfiguration().getPersonality());

        boolean doTransformation = !conf1.isPerformTransformation();
        m1.getOntologyLoaderConfiguration().setPerformTransformation(doTransformation);
        Assert.assertNotEquals("The 'perform transformation' flag is changed", doTransformation, m1.getOntologyLoaderConfiguration().isPerformTransformation());
        m1.setOntologyLoaderConfiguration(conf2.setPerformTransformation(doTransformation));
        Assert.assertEquals("The same 'perform transformation' flag", doTransformation, m1.getOntologyLoaderConfiguration().isPerformTransformation());

        GraphTransformConfig.Store store = new GraphTransformConfig.Store().add((GraphTransformConfig.Maker) graph -> null);
        OntConfig.LoaderConfiguration conf3 = m1.getOntologyLoaderConfiguration().setGraphTransformers(store);
        Assert.assertNotEquals("Graph transform action store is changed", store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
        m1.setOntologyLoaderConfiguration(conf3);
        Assert.assertEquals("Can't set transform action store.", store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
    }

    @Test
    public void testSerialization() throws Exception {
        OWLOntologyManager origin =
                OntManagerFactory.createONTManager();
        //OntManagerFactory.createOWLManager();
        ManagerTest.setUpManager(origin);
        ManagerTest.debug(origin);

        LOGGER.info("|====================|");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(origin);
        stream.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(in);
        OWLOntologyManager copy = (OWLOntologyManager) inStream.readObject();
        ManagerTest.fixAfterSerialization(origin, copy);
        ManagerTest.debug(copy);
        ManagerTest.testCompareManagers(origin, copy);

        if (OntologyManager.class.isInstance(origin)) {
            ManagerTest.testEdit((OntologyManager) origin, (OntologyManager) copy);
        }
    }

    private static void fixAfterSerialization(OWLOntologyManager origin, OWLOntologyManager copy) {
        if (OntologyManager.class.isInstance(copy)) {
            return;
        }
        // OWL-API 5.0.5:
        copy.setOntologyWriterConfiguration(origin.getOntologyWriterConfiguration());
    }

    static void debug(OWLOntologyManager m) {
        m.ontologies().forEach(o -> {
            LOGGER.debug("<" + o.getOntologyID() + ">:");
            ReadWriteUtils.print(o);
        });
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    private static OWLOntologyManager setUpManager(OWLOntologyManager m) throws OWLOntologyCreationException {
        OWLDataFactory f = m.getOWLDataFactory();

        OWLOntology a1 = m.createOntology();
        a1.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class1"))));
        OWLOntology a2 = m.createOntology();
        a2.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class2"))));

        OWLOntology i1 = m.createOntology(IRI.create("urn:iri.com#1"));
        i1.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class3"))));
        OWLOntology i2 = m.createOntology(IRI.create("urn:iri.com#2"));
        i2.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class4"))));
        OWLOntology i3 = m.createOntology(IRI.create("urn:iri.com#3"));
        i3.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class5"))));

        i1.applyChange(new AddImport(i1, f.getOWLImportsDeclaration(i2.getOntologyID().getOntologyIRI().get())));
        i2.applyChange(new AddImport(i2, f.getOWLImportsDeclaration(i3.getOntologyID().getOntologyIRI().get())));
        i1.applyChange(new AddImport(i1, f.getOWLImportsDeclaration(IRI.create("urn:some.import"))));
        a1.applyChange(new AddImport(a1, f.getOWLImportsDeclaration(i1.getOntologyID().getOntologyIRI().get())));

        i2.getFormat().asPrefixOWLDocumentFormat().setPrefix("test", "urn:iri.com");
        return m;
    }

    private static void testEdit(OntologyManager origin, OntologyManager copy) {
        String uri = "urn:iri.com#1";
        OntGraphModel o1 = origin.getGraphModel(uri);
        OntGraphModel o2 = copy.getGraphModel(uri);

        List<OntClass> classes1 = o1.listClasses().collect(Collectors.toList());
        // create two new classes inside original manager (in two models).
        o1.createOntEntity(OntClass.class, "http://some/new#Class1");
        origin.getGraphModel("urn:iri.com#3").createOntEntity(OntClass.class, "http://some/new#Class2");
        List<OntClass> classes2 = o2.listClasses().collect(Collectors.toList());
        // check that in the second (copied) manager there is no changes:
        Assert.assertEquals("incorrect classes", classes1, classes2);

        // create two new classes inside copied manager.
        Set<OntClass> classes3 = o2.listClasses().collect(Collectors.toSet());
        OntClass cl3 = o2.createOntEntity(OntClass.class, "http://some/new#Class3");
        OntClass cl4 = copy.getGraphModel("urn:iri.com#3").createOntEntity(OntClass.class, "http://some/new#Class4");
        List<OntClass> newClasses = Arrays.asList(cl3, cl4);
        classes3.addAll(newClasses);
        Set<OntClass> classes4 = o2.listClasses().collect(Collectors.toSet());
        Assert.assertEquals("incorrect classes", classes3, classes4);
        newClasses.forEach(c ->
                Assert.assertFalse("Found " + c + " inside original ontology", o1.containsResource(c))
        );
        OntologyModel ont = copy.getOntology(IRI.create(uri));
        Assert.assertNotNull(ont);
        List<OWLClass> newOWLClasses = newClasses.stream()
                .map(RDF2OWLHelper::getClassExpression)
                .map(AsOWLClass::asOWLClass).collect(Collectors.toList());
        LOGGER.debug("OWL-Classes: " + newOWLClasses);
        newOWLClasses.forEach(c ->
                Assert.assertTrue("Can't find " + c + " inside copied ontology", ont.containsReference(c, Imports.INCLUDED))
        );
    }

    public static void testCompareManagers(OWLOntologyManager expected, OWLOntologyManager actual) {
        Assert.assertEquals("Incorrect number of ontologies.", expected.ontologies().count(), actual.ontologies().count());
        actual.ontologies().forEach(test -> {
            OWLOntologyID id = test.getOntologyID();
            LOGGER.debug("Test <" + id + ">");
            OWLOntology origin = expected.getOntology(id);
            Assert.assertNotNull("Can't find init ontology with id " + id, origin);
            AxiomType.AXIOM_TYPES.forEach(t -> {
                Set<OWLAxiom> expectedAxiom = origin.axioms(t).collect(Collectors.toSet());
                Set<OWLAxiom> actualAxiom = test.axioms(t).collect(Collectors.toSet());
                Assert.assertThat(String.format("Incorrect axioms for type <%s> and %s (expected=%d, actual=%d)", t, id, expectedAxiom.size(), actualAxiom.size()),
                        actualAxiom, IsEqual.equalTo(expectedAxiom));
            });

            Set<OWLImportsDeclaration> expectedImports = origin.importsDeclarations().collect(Collectors.toSet());
            Set<OWLImportsDeclaration> actualImports = test.importsDeclarations().collect(Collectors.toSet());
            Assert.assertEquals("Incorrect imports for " + id, expectedImports, actualImports);
            OWLDocumentFormat expectedFormat = origin.getFormat();
            OWLDocumentFormat actualFormat = test.getFormat();
            Assert.assertEquals("Incorrect formats for " + id, expectedFormat, actualFormat);
            Map<String, String> expectedPrefixes = expectedFormat != null && expectedFormat.isPrefixOWLDocumentFormat() ?
                    expectedFormat.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap() : null;
            Map<String, String> actualPrefixes = actualFormat != null && actualFormat.isPrefixOWLDocumentFormat() ?
                    actualFormat.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap() : null;
            Assert.assertEquals("Incorrect prefixes for " + id, expectedPrefixes, actualPrefixes);
            testCompareEntities(origin, test);
        });
    }

    @SuppressWarnings("ConstantConditions")
    public static void testCompareEntities(OWLOntology expectedOnt, OWLOntology actualOnt) {
        for (Imports i : Imports.values()) {
            Set<OWLEntity> actualEntities = actualOnt.signature(i).collect(Collectors.toSet());
            Set<OWLEntity> expectedEntities = expectedOnt.signature(i).collect(Collectors.toSet());
            LOGGER.debug("OWL entities: " + actualEntities);
            Assert.assertEquals("Incorrect owl entities", expectedEntities, actualEntities);
        }
        if (OntologyModel.class.isInstance(actualOnt) && OntologyModel.class.isInstance(expectedOnt)) {  // ont
            List<OntEntity> actualEntities = ((OntologyModel) actualOnt).asGraphModel().ontEntities().collect(Collectors.toList());
            List<OntEntity> expectedEntities = ((OntologyModel) expectedOnt).asGraphModel().ontEntities().collect(Collectors.toList());
            LOGGER.debug("ONT entities: " + actualEntities);
            Assert.assertEquals("Incorrect ont entities", expectedEntities, actualEntities);
        }
    }

}
