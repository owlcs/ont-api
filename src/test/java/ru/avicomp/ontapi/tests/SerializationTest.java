package ru.avicomp.ontapi.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Testing Serialization of manager and ontology,
 * <p>
 * Created by @szuev on 08.02.2017.
 */
public class SerializationTest {

    private static final Logger LOGGER = Logger.getLogger(SerializationTest.class);

    @Test
    public void test() throws Exception {
        OWLOntologyManager origin =
                OntManagerFactory.createONTManager();
        //OntManagerFactory.createOWLManager();
        setUp(origin);
        debug(origin);

        LOGGER.info("|====================|");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(origin);
        stream.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(in);
        OWLOntologyManager copy = (OWLOntologyManager) inStream.readObject();
        fixAfterSerialization(origin, copy);
        debug(copy);
        test(origin, copy);
    }

    private static void fixAfterSerialization(OWLOntologyManager origin, OWLOntologyManager copy) {
        if (OntologyManager.class.isInstance(copy)) {
            return;
        }
        // OWL-API 5.0.5:
        copy.setOntologyWriterConfiguration(origin.getOntologyWriterConfiguration());
    }

    private static void debug(OWLOntologyManager m) {
        m.ontologies().forEach(LOGGER::info);
        ReadWriteUtils.print(m.getOntology(OntIRI.create("urn:iri.com#1")));
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    public static OWLOntologyManager setUp(OWLOntologyManager m) throws OWLOntologyCreationException {
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

    public static void test(OWLOntologyManager expected, OWLOntologyManager actual) {
        Assert.assertEquals("Incorrect number of ontologies.", expected.ontologies().count(), actual.ontologies().count());
        actual.ontologies().forEach(test -> {
            OWLOntologyID id = test.getOntologyID();
            LOGGER.debug("Test <" + id + ">");
            OWLOntology origin = expected.getOntology(id);
            assertNotNull("Can't find init ontology with id " + id, origin);
            AxiomType.AXIOM_TYPES.forEach(t -> {
                Set<OWLAxiom> expectedAxiom = origin.axioms(t).collect(toSet());
                Set<OWLAxiom> actualAxiom = test.axioms(t).collect(toSet());
                assertThat(String.format("Incorrect axioms for type <%s> and %s (expected=%d, actual=%d)", t, id, expectedAxiom.size(), actualAxiom.size()),
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
            testEntities(origin, test);
        });
    }

    @SuppressWarnings("ConstantConditions")
    private static void testEntities(OWLOntology expectedOnt, OWLOntology actualOnt) {
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
