package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNOP;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
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
        OWLDataFactory factory = OntManagerFactory.getDataFactory();
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        LOGGER.info("Load ontology from file " + fileIRI);
        OWLOntology original = OntManagerFactory.createONTManager().loadOntology(fileIRI);
        debug(original);

        LOGGER.info("Assemble new ontology with the same content.");
        OntIRI iri = OntIRI.create("http://test.test/complex");
        OntIRI ver = OntIRI.create("http://test.test/complex/version-iri/1.0");
        OntologyModel result = OntManagerFactory.createONTManager().createOntology(iri.toOwlOntologyID());
        OntGraphModel jena = result.asGraphModel();
        jena.setNsPrefix("", iri.getIRIString() + "#");
        jena.getID().setVersionIRI(ver.getIRIString());

        OWLClass owlSimple1 = factory.getOWLClass(iri.addFragment("Simple1"));
        OWLClass owlSimple2 = factory.getOWLClass(iri.addFragment("Simple2"));
        OWLClass owlComplex1 = factory.getOWLClass(iri.addFragment("Complex1"));
        OWLClass owlComplex2 = factory.getOWLClass(iri.addFragment("Complex2"));
        OntClass ontSimple1 = jena.createOntEntity(OntClass.class, owlSimple1.getIRI().getIRIString());
        OntClass ontSimple2 = jena.createOntEntity(OntClass.class, owlSimple2.getIRI().getIRIString());
        OntClass ontComplex1 = jena.createOntEntity(OntClass.class, owlComplex1.getIRI().getIRIString());
        OntClass ontComplex2 = jena.createOntEntity(OntClass.class, owlComplex2.getIRI().getIRIString());

        OntNOP property = jena.createOntEntity(OntNOP.class, iri.addFragment("hasSimple1").getIRIString());
        property.setFunctional(true);
        property.addRange(ontSimple1);
        OntCE.ObjectSomeValuesFrom restriction = jena.createObjectSomeValuesFrom(property, ontSimple2);
        ontComplex2.addSubClassOf(restriction);
        ontComplex2.addSubClassOf(ontComplex1);
        ontComplex2.addComment("comment1", "es");
        ontComplex1.addDisjointWith(ontSimple1);

        // bulk disjoint instead adding one by one (to have the same list of axioms):
        Resource anon = jena.createResource();
        jena.add(anon, RDF.type, OWL.AllDisjointClasses);
        jena.add(anon, OWL.members, jena.createList(Stream.of(ontComplex2, ontSimple1, ontSimple2).iterator()));

        debug(result);

        LOGGER.info("Compare axioms.");
        List<OWLAxiom> actual = result.axioms().sorted().collect(Collectors.toList());
        List<OWLAxiom> expected = original.axioms().sorted().collect(Collectors.toList());
        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

        LOGGER.info("Remove OWL:disjointWith for " + ontComplex1 + " & " + ontSimple1 + " pair.");
        jena.removeAll(ontComplex1, OWL.disjointWith, null);
        ReadWriteUtils.print(result.asGraphModel(), OntFormat.TURTLE);
        actual = result.axioms().sorted().collect(Collectors.toList());
        expected = original.axioms().sorted().collect(Collectors.toList());
        expected.remove(factory.getOWLDisjointClassesAxiom(owlComplex1, owlSimple1));

        expected.forEach(LOGGER::debug);
        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

        LOGGER.info("Remove OWL:AllDisjointClasses");
        anon = jena.listResourcesWithProperty(RDF.type, OWL.AllDisjointClasses).toList().get(0);
        RDFList list = jena.listObjectsOfProperty(anon, OWL.members).mapWith(n -> n.as(RDFList.class)).toList().get(0);
        list.removeList();
        jena.removeAll(anon, null, null);

        debug(result);
        LOGGER.info("Compare axioms.");
        actual = result.axioms().sorted().collect(Collectors.toList());
        expected = original.axioms().filter(axiom -> !AxiomType.DISJOINT_CLASSES.equals(axiom.getAxiomType())).sorted().collect(Collectors.toList());

        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

    }
}
