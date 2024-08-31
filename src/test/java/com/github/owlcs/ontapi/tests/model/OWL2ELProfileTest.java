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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @see <a href="https://www.w3.org/TR/owl2-profiles/#OWL_2_EL">OWL 2 EL</a>
 */
public class OWL2ELProfileTest {

    @Test
    void testDataAllValuesFrom() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createOntClass("a")
                .addSuperClass(data.createDataAllValuesFrom(data.createDataProperty("p"), data.getDatatype(XSD.xstring)));

        OntologyManager manager = OntManagers.createManager();
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph(),
                manager.getOntologyLoaderConfiguration().setSpecification(OntSpecification.OWL2_EL_MEM));

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(List.of(AxiomType.DECLARATION, AxiomType.DECLARATION), actual1);

        Assertions.assertThrows(OntApiException.class,
                () -> ontology.add(df.getOWLSubClassOfAxiom(df.getOWLClass("A"),
                        df.getOWLDataAllValuesFrom(df.getOWLDataProperty("P"), df.getStringOWLDatatype()))));
    }

    @Test
    void testPropertyTypeAxioms() {
        OntModel data = OntModelFactory.createModel();
        data.setID("ont");
        data.createObjectProperty("p1").setAsymmetric(true);
        data.createObjectProperty("p2").setSymmetric(true);
        data.createObjectProperty("p3").setFunctional(true);
        data.createObjectProperty("p4").setInverseFunctional(true);
        data.createObjectProperty("p5").setIrreflexive(true);

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_EL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<? extends AxiomType<?>> actual1 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(5, actual1.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual1));

        OWLAxiom ax1 = df.getOWLAsymmetricObjectPropertyAxiom(df.getOWLObjectProperty("P"));
        OWLAxiom ax2 = df.getOWLSymmetricObjectPropertyAxiom(df.getOWLObjectProperty("P"));
        OWLAxiom ax3 = df.getOWLFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("P"));
        OWLAxiom ax4 = df.getOWLInverseFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("P"));
        OWLAxiom ax5 = df.getOWLIrreflexiveObjectPropertyAxiom(df.getOWLObjectProperty("P"));

        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax1));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax2));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax3));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax4));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax5));

        List<? extends AxiomType<?>> actual2 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(5, actual2.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION), new HashSet<>(actual2));

        OWLAxiom ax6 = df.getOWLReflexiveObjectPropertyAxiom(df.getOWLObjectProperty("P"));
        ontology.add(ax6);
        List<? extends AxiomType<?>> actual3 = ontology.axioms().map(OWLAxiom::getAxiomType).toList();
        Assertions.assertEquals(6, actual3.size());
        Assertions.assertEquals(Set.of(AxiomType.DECLARATION, AxiomType.REFLEXIVE_OBJECT_PROPERTY), new HashSet<>(actual3));
    }

    @Test
    void testDifferentIndividualsAxiom() {
        OntModel data = OntModelFactory.createModel();
        data.createOntClass("A").createIndividual().addDifferentIndividual(data.createIndividual("i1"));
        data.createOntClass("B").createIndividual("i2").addDifferentIndividual(data.createIndividual("i3"));

        data.createDifferentIndividuals(
                data.createIndividual("i4"),
                data.createOntClass("C").createIndividual(),
                data.createOntClass("D").createIndividual("i5"),
                data.createOntClass("E").createIndividual()
        );

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_EL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<OWLAxiom> actual1 = ontology.axioms()
                .filter(it -> !AxiomType.DECLARATION.equals(it.getAxiomType()) && !AxiomType.CLASS_ASSERTION.equals(it.getAxiomType()))
                .toList();
        Assertions.assertEquals(2, actual1.size());
        Assertions.assertEquals(
                Set.of("DifferentIndividuals(<i2> <i3>)", "DifferentIndividuals(<i4> <i5>)"),
                actual1.stream().map(Object::toString).collect(Collectors.toSet())
        );

        OWLAxiom ax1 = df.getOWLDifferentIndividualsAxiom(df.getOWLNamedIndividual("i6"), df.getOWLAnonymousIndividual());
        OWLAxiom ax2 = df.getOWLDifferentIndividualsAxiom(
                df.getOWLNamedIndividual("i6"), df.getOWLNamedIndividual("i7"), df.getOWLNamedIndividual("i8"));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax1));

        ontology.add(ax2);

        List<OWLAxiom> actual2 = ontology.axioms()
                .filter(it -> !AxiomType.DECLARATION.equals(it.getAxiomType()) && !AxiomType.CLASS_ASSERTION.equals(it.getAxiomType()))
                .toList();
        Assertions.assertEquals(3, actual2.size());
        Assertions.assertEquals(
                Set.of("DifferentIndividuals(<i2> <i3>)", "DifferentIndividuals(<i4> <i5>)", "DifferentIndividuals(<i6> <i7> <i8>)"),
                actual2.stream().map(Object::toString).collect(Collectors.toSet())
        );
    }

    @Test
    void testDisjointPropertiesAxioms() {
        OntModel data = OntModelFactory.createModel();
        data.createObjectProperty("p1").addDisjointProperty(data.createObjectProperty("p2"));
        data.createDisjointDataProperties(
                data.createDataProperty("p3"), data.createDataProperty("p4"), data.createDataProperty("p5")
        );

        OntologyManager manager = OntManagers.createManager();
        manager.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_EL_MEM);
        DataFactory df = manager.getOWLDataFactory();
        Ontology ontology = manager.addOntology(data.getGraph());

        List<OWLAxiom> actual1 = ontology.axioms()
                .filter(it -> !AxiomType.DECLARATION.equals(it.getAxiomType()))
                .toList();
        Assertions.assertEquals(0, actual1.size());

        OWLAxiom ax1 = df.getOWLDisjointDataPropertiesAxiom(df.getOWLDataProperty("p6"));
        OWLAxiom ax2 = df.getOWLDisjointDataPropertiesAxiom(df.getOWLDataProperty("p7"), df.getOWLDataProperty("p8"));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax1));
        Assertions.assertThrows(OntApiException.Unsupported.class, () -> ontology.add(ax2));
    }
}
