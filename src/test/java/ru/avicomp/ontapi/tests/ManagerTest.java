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
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.internal.ReadHelper;
import ru.avicomp.ontapi.internal.Wrap;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test core ({@link OntManagers})
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

        Assert.assertNotSame("The same manager", OntManagers.createONT(), OntManagers.createONT());
        Assert.assertNotSame("The same concurrent manager", OntManagers.createConcurrentONT(), OntManagers.createConcurrentONT());

        OntologyManagerImpl m1 = (OntologyManagerImpl) OntManagers.createONT();
        Assert.assertFalse("Concurrent", m1.isConcurrent());

        OntologyModel ont1 = m1.loadOntology(fileIRI);
        OntologyModel ont2 = m1.createOntology(id);
        Assert.assertEquals("Incorrect num of ontologies", 2, m1.ontologies().count());
        Stream.of(ont1, ont2).forEach(o -> {
            Assert.assertEquals("Incorrect impl", OntologyModelImpl.class, ont1.getClass());
            Assert.assertNotEquals("Incorrect impl", OntologyModelImpl.Concurrent.class, ont1.getClass());
        });

        OntologyManagerImpl m2 = (OntologyManagerImpl) OntManagers.createConcurrentONT();
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
        OntologyManager m1 = OntManagers.createONT();
        OntologyManager m2 = OntManagers.createONT();
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

        GraphTransformers.Store store = new GraphTransformers.Store().add((GraphTransformers.Maker) graph -> null);
        OntConfig.LoaderConfiguration conf3 = m1.getOntologyLoaderConfiguration().setGraphTransformers(store);
        Assert.assertNotEquals("Graph transform action store is changed", store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
        m1.setOntologyLoaderConfiguration(conf3);
        Assert.assertEquals("Can't set transform action store.", store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
    }

    @Test
    public void testSerialization() throws Exception {
        OWLOntologyManager origin =
                OntManagers.createONT();
        //OntManagers.createOWL();
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
        ManagerTest.compareManagersTest(origin, copy);

        if (OntologyManager.class.isInstance(origin)) {
            ManagerTest.editTest((OntologyManager) origin, (OntologyManager) copy);
        }
    }

    @Test
    public void testCoping() throws Exception {
        copyTest(OntManagers.createONT(), OntManagers.createOWL(), OntologyCopy.SHALLOW);
        copyTest(OntManagers.createONT(), OntManagers.createOWL(), OntologyCopy.DEEP);
        copyTest(OntManagers.createOWL(), OntManagers.createONT(), OntologyCopy.SHALLOW);
        copyTest(OntManagers.createOWL(), OntManagers.createONT(), OntologyCopy.DEEP);
        copyTest(OntManagers.createONT(), OntManagers.createONT(), OntologyCopy.SHALLOW);
        copyTest(OntManagers.createONT(), OntManagers.createONT(), OntologyCopy.DEEP);
    }

    @Test
    public void testMoving() throws Exception {
        LOGGER.info("1) Move OWL -> ONT");
        OWLDataFactory df = OntManagers.getDataFactory();
        OWLOntologyManager m1 = OntManagers.createOWL();
        OWLOntologyManager m2 = OntManagers.createONT();
        OWLOntologyManager m3 = OntManagers.createONT();
        IRI iri1 = IRI.create("http://test/1");
        OWLOntology o1 = m1.createOntology(iri1);
        o1.add(df.getOWLSubClassOfAxiom(df.getOWLClass("a"), df.getOWLClass("b")));
        try {
            m2.copyOntology(o1, OntologyCopy.MOVE);
            Assert.fail("Moving from OWL to ONT should be disabled");
        } catch (OWLOntologyCreationException e) {
            LOGGER.info(e);
        }
        Assert.assertEquals("Incorrect ont-count in source", 1, m1.ontologies().count());
        Assert.assertEquals("Incorrect ont-count in destinaction", 0, m2.ontologies().count());

        LOGGER.info("2) Move ONT -> OWL");
        IRI iri2 = IRI.create("http://test/2");
        IRI docIRI = IRI.create("file://nothing");
        OWLDocumentFormat format = OntFormat.JSON_LD.createOwlFormat();
        OWLOntology o2 = m2.createOntology(iri2);
        m2.setOntologyFormat(o2, format);
        m2.setOntologyDocumentIRI(o2, docIRI);
        o2.add(df.getOWLEquivalentClassesAxiom(df.getOWLClass("a"), df.getOWLClass("b")));

        try {
            m1.copyOntology(o2, OntologyCopy.MOVE);
            Assert.fail("Expected exception while moving from ONT -> OWL");
        } catch (OntApiException a) {
            LOGGER.info(a);
        }
        // check ONT manager
        // And don't care about OWL manager, we can't help him anymore.
        Assert.assertTrue("Can't find " + iri2, m2.contains(iri2));
        Assert.assertTrue("Can't find " + o2, m2.contains(o2));
        Assert.assertSame("Incorrect manager!", m2, o2.getOWLOntologyManager());
        Assert.assertEquals("Incorrect document IRI", docIRI, m2.getOntologyDocumentIRI(o2));
        Assert.assertEquals("Incorrect format", format, m2.getOntologyFormat(o2));

        LOGGER.info("3) Move ONT -> ONT");
        Assert.assertSame("Not same ontology!", o2, m3.copyOntology(o2, OntologyCopy.MOVE));
        Assert.assertTrue("Can't find " + iri2, m3.contains(iri2));
        Assert.assertFalse("There is still " + iri2, m2.contains(iri2));
        Assert.assertTrue("Can't find " + o2, m3.contains(o2));
        Assert.assertFalse("There is still " + o2, m2.contains(o2));
        Assert.assertSame("Not the same ontology", o2, m3.getOntology(iri2));
        Assert.assertSame("Incorrect manager!", m3, o2.getOWLOntologyManager());
        Assert.assertEquals("Incorrect document IRI", docIRI, m3.getOntologyDocumentIRI(o2));
        Assert.assertEquals("Incorrect format", format, m3.getOntologyFormat(o2));
        Assert.assertNull("Still have ont-format", m2.getOntologyFormat(o2));
        try {
            Assert.fail("Expected exception, but found some doc iri " + m2.getOntologyDocumentIRI(o2));
        } catch (UnknownOWLOntologyException u) {
            LOGGER.info(u);
        }
    }

    @Test
    public void testDifferentLoadStrategies() throws Exception {
        IRI sp = IRI.create("http://spinrdf.org/sp");
        IRI spin = IRI.create("http://spinrdf.org/spin");
        OWLOntologyIRIMapper mapSp = new SimpleIRIMapper(sp, IRI.create(ReadWriteUtils.getResourceFile("spin", "sp.ttl")));
        OWLOntologyIRIMapper mapSpin = new SimpleIRIMapper(spin, IRI.create(ReadWriteUtils.getResourceFile("spin", "spin.ttl")));

        LOGGER.info("1) Test load some web ontology for a case when only file scheme is allowed.");
        OntologyManager m1 = OntManagers.createONT();
        OntConfig.LoaderConfiguration conf = m1.getOntologyLoaderConfiguration().setSupportedSchemes(Stream.of(OntConfig.DefaultScheme.FILE).collect(Collectors.toSet()));
        m1.setOntologyLoaderConfiguration(conf);
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(sp));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntBuildingFactoryImpl.ConfigMismatchException) {
                LOGGER.info(e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }

        LOGGER.info("2) Add mapping and try to load again.");
        m1.getIRIMappers().add(mapSp);
        m1.loadOntology(sp);
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.info("3) Load new web-ontology which depends on this existing one.");
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(spin));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntBuildingFactoryImpl.ConfigMismatchException) {
                LOGGER.info(e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.info("4) Try to load new web-ontology with file mapping which depends on some other web-ontology.");
        OntologyManager m2 = OntManagers.createONT();
        m2.setOntologyLoaderConfiguration(conf);
        m2.getIRIMappers().add(mapSpin);
        try {
            Assert.fail("No exception while loading " + m2.loadOntology(spin));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.info(e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Manager should be empty", 0, m2.ontologies().count());

        LOGGER.info("5) Set ignore broken imports and try to load again.");
        m2.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m2.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m2.ontologies().count());

        LOGGER.info("6) Set ignore some import and load ontology with dependencies.");
        OntologyManager m3 = OntManagers.createONT();
        m3.getIRIMappers().add(mapSp);
        m3.getIRIMappers().add(mapSpin);
        m3.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION).addIgnoredImport(sp));
        m3.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m3.ontologies().count());

        LOGGER.info("7) Default way to load.");
        OntologyManager m4 = OntManagers.createONT();
        m4.getIRIMappers().add(mapSp);
        m4.getIRIMappers().add(mapSpin);
        m4.loadOntology(spin);
        Assert.assertEquals("Should be two ontologies inside.", 2, m4.ontologies().count());

    }

    private static void copyTest(OWLOntologyManager from, OWLOntologyManager to, OntologyCopy mode) throws Exception {
        LOGGER.info("Copy (" + mode + ") " + from.getClass().getInterfaces()[0].getSimpleName() + " -> " + to.getClass().getInterfaces()[0].getSimpleName());
        long fromCount = from.ontologies().count();
        long toCount = to.ontologies().count();

        OWLDataFactory df = from.getOWLDataFactory();
        IRI iri = IRI.create("test" + System.currentTimeMillis());
        LOGGER.debug("Create ontology " + iri);
        OWLClass clazz = df.getOWLClass("x");
        OWLOntology o1 = from.createOntology(iri);
        o1.add(df.getOWLDeclarationAxiom(clazz));

        to.copyOntology(o1, OntologyCopy.DEEP);
        Assert.assertEquals("Incorrect ontologies count inside OWL-manager", fromCount + 1, from.ontologies().count());
        Assert.assertEquals("Incorrect ontologies count inside ONT-manager", toCount + 1, to.ontologies().count());
        Assert.assertTrue("Can't find " + iri, to.contains(iri));
        OWLOntology o2 = to.getOntology(iri);
        Assert.assertNotNull("Can't find " + to, o2);
        Assert.assertNotSame("Should not be same", o1, o2);
        Set<OWLClass> classes = o2.classesInSignature().collect(Collectors.toSet());
        Assert.assertEquals("Should be single class inside", 1, classes.size());
        Assert.assertTrue("Can't find " + clazz, classes.contains(clazz));
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

    private static void editTest(OntologyManager origin, OntologyManager copy) {
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
        OWLDataFactory df = copy.getOWLDataFactory();
        Assert.assertNotNull(ont);
        List<OWLClass> newOWLClasses = newClasses.stream()
                .map(ce -> ReadHelper.fetchClassExpression(ce, df))
                .map(Wrap::getObject)
                .map(AsOWLClass::asOWLClass).collect(Collectors.toList());
        LOGGER.debug("OWL-Classes: " + newOWLClasses);
        newOWLClasses.forEach(c ->
                Assert.assertTrue("Can't find " + c + " inside copied ontology", ont.containsReference(c, Imports.INCLUDED))
        );
    }

    public static void compareManagersTest(OWLOntologyManager expected, OWLOntologyManager actual) {
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
            compareEntitiesTest(origin, test);
        });
    }

    @SuppressWarnings("ConstantConditions")
    public static void compareEntitiesTest(OWLOntology expectedOnt, OWLOntology actualOnt) {
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
