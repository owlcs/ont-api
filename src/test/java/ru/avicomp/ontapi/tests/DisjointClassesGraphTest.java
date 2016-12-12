package ru.avicomp.ontapi.tests;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.OntologyModelImpl;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNOP;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * test owl:AllDisjointClasses and owl:disjointWith using jena and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class DisjointClassesGraphTest extends GraphTestBase {

    @Test
    public void test() throws OWLOntologyCreationException {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        LOGGER.info("Load ontology from file " + fileIRI);
        OntologyModel original = (OntologyModelImpl) OntManagerFactory.createONTManager().loadOntology(fileIRI);
        debug(original);

        LOGGER.info("Assemble new ontology with the same content.");
        OntIRI iri = OntIRI.create("http://test.test/complex");
        OntIRI ver = OntIRI.create("http://test.test/complex/version-iri/1.0");
        OntologyModel result = OntManagerFactory.createONTManager().createOntology(iri.toOwlOntologyID());
        OntGraphModel jena = result.asGraphModel();
        jena.setNsPrefix("", iri.getIRIString() + "#");
        jena.getID().setVersionIRI(ver.getIRIString());

        OntClass simple1 = jena.createOntEntity(OntClass.class, iri.addFragment("Simple1").getIRIString());
        OntClass simple2 = jena.createOntEntity(OntClass.class, iri.addFragment("Simple2").getIRIString());
        OntClass complex1 = jena.createOntEntity(OntClass.class, iri.addFragment("Complex1").getIRIString());
        OntClass complex2 = jena.createOntEntity(OntClass.class, iri.addFragment("Complex2").getIRIString());

        OntNOP property = jena.createOntEntity(OntNOP.class, iri.addFragment("hasSimple1").getIRIString());
        property.setFunctional(true);
        property.addRange(simple1);
        OntCE.ObjectSomeValuesFrom restriction = jena.createObjectSomeValuesFrom(property, simple2);
        complex2.addSubClassOf(restriction);
        complex2.addSubClassOf(complex1);
        complex2.addComment("comment1", "es");
        complex1.addDisjointWith(simple1);

        // bulk disjoint instead adding one by one (to have the same list of axioms):
        Resource anon = jena.createResource();
        jena.add(anon, RDF.type, OWL2.AllDisjointClasses);
        jena.add(anon, OWL2.members, jena.createList(Stream.of(complex2, simple1, simple2).iterator()));

        LOGGER.info("After rebind.");
        ReadWriteUtils.print(jena, OntFormat.TTL_RDF);

        LOGGER.info("Compare axioms.");
        result.axioms().forEach(LOGGER::debug);
        TestUtils.compareAxioms(original.axioms(), result.axioms());

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

        LOGGER.info("Compare axioms.");
        result.axioms().forEach(LOGGER::debug);
        TestUtils.compareAxioms(original.axioms().filter(axiom -> !AxiomType.DISJOINT_CLASSES.equals(axiom.getAxiomType())), result.axioms());
        debug(result);

    }
}
