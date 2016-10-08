package ru.avicomp.ontapi.tests;

import java.util.stream.Stream;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.SomeValuesFromRestriction;
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
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

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
}
