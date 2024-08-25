package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @see <a href="https://www.w3.org/TR/owl2-profiles/#OWL_2_QL">OWL 2 QL</a>
 */
public class OWL2QLProfileTest {

    @Test
    void testObjectUnionOf() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createOntClass("a").addSuperClass(data.createObjectUnionOf(data.createOntClass("b")));

        OntologyManager manager = OntManagers.createManager();
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph(),
                manager.getOntologyLoaderConfiguration().setSpecification(OntSpecification.OWL2_QL_MEM));

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION, AxiomType.DECLARATION), actual1);

        Assertions.assertThrows(OntApiException.class,
                () -> ontology.add(df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLObjectUnionOf(df.getOWLClass("B"))))
        );
    }

    @Test
    void testHasKey() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createOntClass("a").addHasKey(data.createObjectProperty("p1"), data.createDataProperty("p2"));

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_QL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        Assertions.assertThrows(OntApiException.class,
                () -> ontology.add(
                        df.getOWLHasKeyAxiom(df.getOWLClass("A"), df.getOWLObjectProperty(df.getOWLClass("P"))))
        );

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));
    }

    @Test
    void testDisjointUnion() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createOntClass("a")
                .addDisjointUnion(data.createOntClass("b"));

        OntologyManager manager = OntManagers.createManager();
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph(),
                manager.getOntologyLoaderConfiguration().setSpecification(OntSpecification.OWL2_QL_MEM));

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION, AxiomType.DECLARATION), actual1);

        Assertions.assertThrows(OntApiException.class,
                () -> ontology.add(
                        df.getOWLDisjointUnionAxiom(df.getOWLClass("A"), List.of(df.getOWLClass("B")))
                )
        );

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION, AxiomType.DECLARATION), actual2);
    }

    @Test
    void testPropertyChain() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createObjectProperty("p1")
                .addPropertyChain(data.createObjectProperty("p2"), data.createObjectProperty("p3"));

        OntologyManager manager = OntManagers.createManager();
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph(),
                manager.getOntologyLoaderConfiguration().setSpecification(OntSpecification.OWL2_QL_MEM));

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        Assertions.assertThrows(OntApiException.class,
                () -> ontology.add(
                        df.getOWLSubPropertyChainOfAxiom(List.of(df.getOWLObjectProperty("B")), df.getOWLObjectProperty("A"))
                )
        );
        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));
    }

    @Test
    void testPropertyDomainAxiom() {
        OntModel data = OntModelFactory.createModel();
        data.createObjectProperty("p1")
                .addDomain(data.createObjectMaxCardinality(data.createObjectProperty("p2"), 42, data.createOntClass("C")));

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_QL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLDataPropertyDomainAxiom(
                df.getOWLDataProperty("P1"),
                df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("P3"), df.getOWLThing())
        );
        OWLAxiom ax2 = df.getOWLObjectPropertyDomainAxiom(
                df.getOWLObjectProperty("P2"),
                df.getOWLClass("X")
        );

        Assertions.assertThrows(OntApiException.class, () -> ontology.add(ax1));

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));

        ontology.add(ax2);

        List<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(4, actual3.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.OBJECT_PROPERTY_DOMAIN), new HashSet<>(actual3));
    }
}
