package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @see <a href="https://www.w3.org/TR/owl2-profiles/#OWL_2_RL">OWL2 RL</a>
 */
public class OWL2RLProfileTest {

    @Test
    void testObjectOneOf() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createOntClass("a").addSuperClass(data.createObjectOneOf());

        OntologyManager manager = OntManagers.createManager();
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph(),
                manager.getOntologyLoaderConfiguration().setSpecification(OntSpecification.OWL2_RL_MEM));

        // ObjectOneOf cannot be superclass
        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION), actual1);

        Assertions.assertThrows(OntApiException.Unsupported.class,
                () -> ontology.add(df.getOWLSubClassOfAxiom(df.getOWLClass("b"), df.getOWLObjectOneOf()))
        );

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION), actual2);

        // ObjectOneOf can be subclass
        ontology.add(df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(), df.getOWLClass("b")));
        Set<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.SUBCLASS_OF), actual3);
    }

    @Test
    void testObjectIntersectionOf() {
        // superObjectIntersectionOf := 'ObjectIntersectionOf' '(' superClassExpression superClassExpression { superClassExpression } ')'
        // subObjectIntersectionOf := 'ObjectIntersectionOf' '(' subClassExpression subClassExpression { subClassExpression } ')'

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_RL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.createOntology();

        OWLClassExpression withDataSomeValuesOf =
                df.getOWLObjectIntersectionOf(
                        df.getOWLClass("x"),
                        df.getOWLDataSomeValuesFrom(df.getOWLDataProperty("p"), df.getStringOWLDatatype())
                );
        OWLClassExpression withDataAllValuesOf =
                df.getOWLObjectIntersectionOf(
                        df.getOWLDataAllValuesFrom(df.getOWLDataProperty("p"), df.getStringOWLDatatype()),
                        df.getOWLClass("f")
                );

        // can't be superObjectIntersectionOf
        OWLAxiom ax1 = df.getOWLSubClassOfAxiom(df.getOWLClass("x"), withDataSomeValuesOf);
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax1));
        List<OWLAxiom> actual1 = ontology.axioms().toList();
        Assertions.assertEquals(List.of(), actual1);

        // can be subObjectIntersectionOf
        OWLAxiom ax2 = df.getOWLSubClassOfAxiom(withDataSomeValuesOf, df.getOWLClass("x"));
        ontology.add(ax2);

        List<OWLAxiom> actual2 = ontology.axioms().toList();
        Assertions.assertEquals(List.of(ax2), actual2);

        // can't be subObjectIntersectionOf
        OWLAxiom ax3 = df.getOWLSubClassOfAxiom(withDataAllValuesOf, df.getOWLClass("x"));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax3));

        // can be subObjectIntersectionOf
        OWLAxiom ax4 = df.getOWLSubClassOfAxiom(df.getOWLClass("x"), withDataAllValuesOf);
        ontology.add(ax4);
        Set<OWLAxiom> actual4 = ontology.axioms().collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(ax2, ax4), actual4);
    }

    @Test
    void testHasKey() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createDataAllValuesFrom(data.createDataProperty("p1"), data.getDatatype(XSD.xstring))
                .addHasKey(data.createDataProperty("p2"));

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_RL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(2, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLHasKeyAxiom(
                df.getOWLDataAllValuesFrom(df.getOWLDataProperty("P1"), df.getIntegerOWLDatatype()),
                df.getOWLObjectProperty(df.getOWLClass("P2")));
        OWLAxiom ax2 = df.getOWLHasKeyAxiom(
                df.getOWLDataSomeValuesFrom(df.getOWLDataProperty("P1"), df.getIntegerOWLDatatype()),
                df.getOWLObjectProperty(df.getOWLClass("P2")));

        Assertions.assertThrows(OntApiException.class, () -> ontology.add(ax1));

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(2, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));

        ontology.add(ax2);

        List<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual3.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.HAS_KEY), new HashSet<>(actual3));
    }

    @Test
    void testClassAssertions() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createObjectOneOf(data.createIndividual("i1")).createIndividual("i2");

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_RL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(2, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLClassAssertionAxiom(
                df.getOWLDataMaxCardinality(42, df.getOWLDataProperty("D")),
                df.getOWLNamedIndividual("I")
        );
        OWLAxiom ax2 = df.getOWLClassAssertionAxiom(
                df.getOWLDataAllValuesFrom(df.getOWLDataProperty("p"), df.getIntegerOWLDatatype()),
                df.getOWLAnonymousIndividual()
        );

        Assertions.assertThrows(OntApiException.class, () -> ontology.add(ax1));

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(2, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));

        ontology.add(ax2);
        List<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual3.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.CLASS_ASSERTION), new HashSet<>(actual3));
    }

    @Test
    void testPropertyDomainAxiom() {
        OntModel data = OntModelFactory.createModel();
        data.createObjectProperty("p1")
                .addDomain(data.createObjectMaxCardinality(data.createObjectProperty("p2"), 42, data.createOntClass("C")));

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_RL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLDataPropertyDomainAxiom(
                df.getOWLDataProperty("P1"),
                df.getOWLObjectUnionOf(df.getOWLClass("X"))
        );
        OWLAxiom ax2 = df.getOWLObjectPropertyDomainAxiom(
                df.getOWLObjectProperty("P2"),
                df.getOWLObjectComplementOf(df.getOWLClass("X"))
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

    @Test
    void testPropertyRangeAxiom() {
        OntModel data = OntModelFactory.createModel();
        data.createObjectProperty("p1")
                .addRange(data.createObjectUnionOf(data.createOntClass("a"), data.createOntClass("b")));

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_RL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLObjectPropertyRangeAxiom(
                df.getOWLObjectProperty("P1"),
                df.getOWLObjectOneOf(df.getOWLNamedIndividual("I"))
        );
        OWLAxiom ax2 = df.getOWLObjectPropertyRangeAxiom(
                df.getOWLObjectProperty("P2"),
                df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("P3"), df.getOWLClass("C"))
        );

        Assertions.assertThrows(OntApiException.class, () -> ontology.add(ax1));

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(3, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));

        ontology.add(ax2);

        List<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(4, actual3.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.OBJECT_PROPERTY_RANGE), new HashSet<>(actual3));
    }
}
