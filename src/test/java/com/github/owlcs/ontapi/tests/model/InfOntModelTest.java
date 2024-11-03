package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.BlankNodeId;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntDataRange;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.search.Filters;

import java.util.List;
import java.util.Objects;

public class InfOntModelTest {

    @Test
    void testListAllAxiomsAndGetCounts() {
        OntologyManager om = OntManagers.createDirectManager();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1")
                .addEquivalentClass(
                        m2.createOntClass("http://b#C2")
                );
        m1.createOntClass("http://a#C1");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // Declaration(Class(<http://a#C1>))
        // Declaration(Class(<http://b#C2>))
        // Declaration(Class(<http://b#C1>))
        // Declaration(Datatype(rdf:XMLLiteral))
        // EquivalentClasses(<http://b#C1> <http://b#C2>)
        // SubAnnotationPropertyOf(rdfs:isDefinedBy rdfs:isDefinedBy)
        // SubAnnotationPropertyOf(rdfs:isDefinedBy rdfs:seeAlso)
        // SubAnnotationPropertyOf(rdfs:seeAlso rdfs:seeAlso)
        // AnnotationPropertyRange(rdfs:label <http://www.w3.org/2000/01/rdf-schema#Literal>)
        // AnnotationPropertyRange(rdfs:comment <http://www.w3.org/2000/01/rdf-schema#Literal>)
        List<OWLAxiom> actual1 = o1.axioms(Imports.INCLUDED).toList();
        Assertions.assertEquals(10, actual1.size());
        Assertions.assertEquals(4, actual1.stream().filter(it -> it instanceof OWLDeclarationAxiom).count());
        Assertions.assertEquals(3, actual1.stream().filter(it -> it instanceof OWLSubAnnotationPropertyOfAxiom).count());
        Assertions.assertEquals(2, actual1.stream().filter(it -> it instanceof OWLAnnotationPropertyRangeAxiom).count());
        Assertions.assertEquals(1, actual1.stream().filter(it -> it instanceof OWLEquivalentClassesAxiom).count());

        Assertions.assertEquals(4, o1.axioms(AxiomType.DECLARATION, Imports.INCLUDED).count());
        Assertions.assertEquals(3, o1.axioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF, Imports.INCLUDED).count());
        Assertions.assertEquals(2, o1.axioms(AxiomType.ANNOTATION_PROPERTY_RANGE, Imports.INCLUDED).count());
        Assertions.assertEquals(1, o1.axioms(AxiomType.EQUIVALENT_CLASSES, Imports.INCLUDED).count());

        List<OWLLogicalAxiom> actual2 = o1.logicalAxioms(Imports.INCLUDED).toList();
        Assertions.assertEquals(1, actual2.size());

        Assertions.assertEquals(10, o1.getAxiomCount(Imports.INCLUDED));
        Assertions.assertEquals(1, o1.getLogicalAxiomCount(Imports.INCLUDED));

        Assertions.assertEquals(4, o1.getAxiomCount(AxiomType.DECLARATION, Imports.INCLUDED));
        Assertions.assertEquals(3, o1.getAxiomCount(AxiomType.SUB_ANNOTATION_PROPERTY_OF, Imports.INCLUDED));
        Assertions.assertEquals(2, o1.getAxiomCount(AxiomType.ANNOTATION_PROPERTY_RANGE, Imports.INCLUDED));
        Assertions.assertEquals(1, o1.getAxiomCount(AxiomType.EQUIVALENT_CLASSES, Imports.INCLUDED));
    }

    @Test
    void testListAxiomsByObject() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_MICRO_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1")
                .addEquivalentClass(
                        m2.createOntClass("http://b#C2")
                );
        m1.createOntClass("http://a#C1");

        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createObjectProperty("http://a#p2");
        m1.createAnnotationProperty("http://a#p3").addSubProperty(
                m1.createAnnotationProperty("http://a#p4")
        );
        m1.createDataProperty("http://a#p3").addSubProperty(
                m2.createDataProperty("http://b#p1")
        );
        OntIndividual i1 = m1.createIndividual("http://b#i1", m2.getOntClass("http://b#C2"));
        OntIndividual i2 = m2.createIndividual(null, m2.getOntClass("http://b#C1"));
        i1.addSameAsStatement(i2);
        OntDataRange d1 = m2.createDataOneOf(m2.createTypedLiteral(1), m2.createTypedLiteral(2));
        OntDataRange d2 = m1.createDatatype("1&2").addEquivalentClass(d1);

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // SubClassOf(<http://b#C2> <http://b#C1>)
        // SubClassOf(<http://b#C2> owl:Thing)
        // SubClassOf(<http://b#C2> <http://b#C2>)
        // SubClassOf(<http://b#C2> rdfs:Resource)
        // EquivalentClasses(<http://b#C1> <http://b#C2>)
        // EquivalentClasses(<http://b#C2>)
        // EquivalentClasses(<http://b#C1> <http://b#C2>)
        // EquivalentClasses(<http://b#C2>)
        Assertions.assertEquals(8, o1.axioms(df.getOWLClass("http://b#C2"), Imports.INCLUDED).count());

        // ObjectPropertyDomain(<http://a#p2> owl:Thing)
        // ObjectPropertyDomain(<http://a#p2> rdfs:Resource)
        // ObjectPropertyRange(<http://a#p2> owl:Thing)
        // ObjectPropertyRange(<http://a#p2> rdfs:Resource)
        Assertions.assertEquals(4, o1.axioms(df.getOWLObjectProperty("http://a#p2"), Imports.INCLUDED).count());

        // SubDataPropertyOf(<http://b#p1> <http://a#p3>)
        // SubDataPropertyOf(<http://b#p1> <http://b#p1>)
        // EquivalentDataProperties(<http://b#p1>)
        Assertions.assertEquals(3, o1.axioms(df.getOWLDataProperty("http://b#p1"), Imports.INCLUDED).count());

        // SubAnnotationPropertyOf(<http://a#p4> <http://a#p3>)
        // SubAnnotationPropertyOf(<http://a#p4> <http://a#p4>)
        Assertions.assertEquals(2, o1.axioms(df.getOWLAnnotationProperty("http://a#p4"), Imports.INCLUDED).count());

        // ClassAssertion(<http://b#C1> _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // ClassAssertion(<http://b#C2> _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // ClassAssertion(owl:Thing _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // ClassAssertion(rdfs:Resource _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // SameIndividual(<http://b#i1> _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // SameIndividual(<http://b#i1> _:c9613ce1-e0bd-4e2c-8855-cffc04961da4)
        // ObjectPropertyAssertion(owl:sameAs _:c9613ce1-e0bd-4e2c-8855-cffc04961da4 <http://b#i1>)
        Assertions.assertEquals(7, o1.axioms(df.getOWLAnonymousIndividual(BlankNodeId.of(i2.asNode())), Imports.INCLUDED).count());

        // DatatypeDefinition(<1&2> DataOneOf("1"^^xsd:int "2"^^xsd:int))
        // DatatypeDefinition(<1&2> <1&2>)
        Assertions.assertEquals(2, o1.axioms(df.getOWLDatatype(d2.getURI()), Imports.INCLUDED).count());
    }

    @Test
    void testListAxiomsByFilter() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_MINI_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://a#C1")
                .addSuperClass(
                        m2.createOntClass("http://b#C2")
                ).addSuperClass(
                        m2.createOntClass("http://b#C3")
                                .addSuperClass(m2.createOntClass("http://b#C4"))
                );
        m1.createOntClass("http://a#C1");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // SubClassOf(<http://b#C2> <http://b#C2>)
        // SubClassOf(<http://a#C1> <http://b#C2>)
        Assertions.assertEquals(2,
                o1.axioms(Filters.subClassWithSuper, df.getOWLClass(IRI.create("http://b#C2")), Imports.INCLUDED).count()
        );

        // SubClassOf(<http://a#C1> <http://a#C1>)
        // SubClassOf(<http://a#C1> owl:Thing)
        // SubClassOf(<http://a#C1> rdfs:Resource)
        // SubClassOf(<http://a#C1> <http://b#C3>)
        // SubClassOf(<http://a#C1> <http://b#C2>)
        // SubClassOf(<http://a#C1> <http://b#C4>)
        Assertions.assertEquals(6,
                o1.axioms(Filters.subClassWithSub, df.getOWLClass(IRI.create("http://a#C1")), Imports.INCLUDED).count()
        );
    }

    @Test
    void testListAxiomsByObjectAndPosition() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_MINI_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://a#C1")
                .addSuperClass(
                        m2.createOntClass("http://b#C2")
                ).addSuperClass(
                        m2.createOntClass("http://b#C3")
                                .addSuperClass(m2.createOntClass("http://b#C4"))
                );
        m1.createOntClass("http://a#C1");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // SubClassOf(<http://b#C2> <http://b#C2>)
        // SubClassOf(<http://a#C1> <http://b#C2>)
        Assertions.assertEquals(2,
                o1.axioms(
                        OWLSubClassOfAxiom.class,
                        OWLClass.class,
                        df.getOWLClass(IRI.create("http://b#C2")),
                        Imports.INCLUDED,
                        Navigation.IN_SUPER_POSITION
                ).count()
        );

        // SubClassOf(<http://a#C1> <http://a#C1>)
        // SubClassOf(<http://a#C1> owl:Thing)
        // SubClassOf(<http://a#C1> rdfs:Resource)
        // SubClassOf(<http://a#C1> <http://b#C3>)
        // SubClassOf(<http://a#C1> <http://b#C2>)
        // SubClassOf(<http://a#C1> <http://b#C4>)
        Assertions.assertEquals(6,
                o1.axioms(
                        OWLSubClassOfAxiom.class,
                        OWLClass.class,
                        df.getOWLClass(IRI.create("http://a#C1")),
                        Imports.INCLUDED,
                        Navigation.IN_SUB_POSITION
                ).count()
        );
    }

    @Test
    void listBoxAxioms() {
        OntologyManager om = OntManagers.createDirectManager();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1")
                .addEquivalentClass(
                        m2.createOntClass("http://b#C2")
                );
        m1.createOntClass("http://a#C1");

        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createObjectProperty("http://a#p2");
        m1.createAnnotationProperty("http://a#p3").addSubProperty(
                m1.createAnnotationProperty("http://a#p4")
        );
        m1.createDataProperty("http://a#p3").addSubProperty(
                m2.createDataProperty("http://b#p1")
        );
        OntIndividual i1 = m1.createIndividual("http://b#i1", m2.getOntClass("http://b#C2"));
        OntIndividual i2 = m2.createIndividual(null, m2.getOntClass("http://b#C1"));
        i1.addSameAsStatement(i2);
        OntDataRange d1 = m2.createDataOneOf(m2.createTypedLiteral(1), m2.createTypedLiteral(2));
        m1.createDatatype("1&2").addEquivalentClass(d1);

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // EquivalentClasses(<http://b#C1> <http://b#C2>)
        // DatatypeDefinition(<1&2> DataOneOf("1"^^xsd:int "2"^^xsd:int))
        Assertions.assertEquals(2, o1.tboxAxioms(Imports.INCLUDED).count());
        // SameIndividual(<http://b#i1> _:8f7080cb-5bb6-4097-bb2d-d678fed3cd7b)
        // ClassAssertion(<http://b#C2> <http://b#i1>)
        // ClassAssertion(<http://b#C1> _:8f7080cb-5bb6-4097-bb2d-d678fed3cd7b)
        Assertions.assertEquals(3, o1.aboxAxioms(Imports.INCLUDED).count());
        // SubDataPropertyOf(<http://b#p1> <http://a#p3>)
        // SubDataPropertyOf(<http://a#p3> <http://a#p3>)
        // SubDataPropertyOf(<http://b#p1> <http://b#p1>)
        Assertions.assertEquals(3, o1.rboxAxioms(Imports.INCLUDED).count());
    }

    @Test
    void listAxiomsIgnoreAnnotations() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1")
                .addEquivalentClassStatement(
                        m2.createOntClass("http://b#C2")
                ).addAnnotation(m2.getRDFSComment(), "xxx");
        m1.createOntClass("http://a#C1").addAnnotation(m2.getRDFSLabel(), "qqq");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // EquivalentClasses(Annotation(rdfs:comment "xxx"^^xsd:string) <http://b#C1> <http://b#C2>)
        Assertions.assertEquals(1,
                o1.axiomsIgnoreAnnotations(
                        df.getOWLEquivalentClassesAxiom(df.getOWLClass("http://b#C1"), df.getOWLClass("http://b#C2")),
                        Imports.INCLUDED
                ).count());
    }

    @Test
    void testListReferencingAxioms() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m1.createOntClass("http://a#C1").addSuperClass(
                m2.createOntClass("http://b#C1")
                        .addEquivalentClass(
                                m2.createOntClass("http://b#C2")
                        )
        );

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // SubClassOf(<http://a#C1> <http://b#C1>)
        // Declaration(Class(<http://a#C1>))
        // SubClassOf(<http://a#C1> <http://a#C1>)
        // SubClassOf(<http://a#C1> <http://a#C1>)
        Assertions.assertEquals(4, o1.referencingAxioms(df.getOWLClass("http://a#C1"), Imports.INCLUDED).count());
    }

    @Test
    void testContainsAxiom() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_MICRO_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m1.createOntClass("http://a#C1").addSuperClass(
                m2.createOntClass("http://b#C1")
                        .addEquivalentClass(
                                m2.createOntClass("http://b#C2")
                        )
        );
        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createObjectProperty("http://a#p2");
        m1.createAnnotationProperty("http://a#p3").addSubProperty(
                m1.createAnnotationProperty("http://a#p4")
        );
        m1.createDataProperty("http://a#p3").addSubProperty(
                m2.createDataProperty("http://b#p1")
        );

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        Assertions.assertTrue(
                o1.containsAxiom(
                        df.getOWLSubClassOfAxiom(df.getOWLClass("http://a#C1"), df.getOWLClass("http://b#C1")),
                        Imports.INCLUDED,
                        AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS
                )
        );
        Assertions.assertFalse(
                o1.containsAxiom(
                        df.getOWLSubClassOfAxiom(df.getOWLClass("http://b#C1"), df.getOWLClass("http://a#C1")),
                        Imports.INCLUDED,
                        AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS
                )
        );
    }

    @Test
    void testContainsObject() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m1.createOntClass("http://a#C1").addSuperClass(
                m2.createOntClass("http://b#C1")
                        .addEquivalentClass(
                                m2.createOntClass("http://b#C2")
                        )
        );
        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createObjectProperty("http://a#p2");
        m1.createAnnotationProperty("http://a#p3").addSubProperty(
                m1.createAnnotationProperty("http://a#p4")
        );
        m1.createDataProperty("http://a#p3").addSubProperty(
                m2.createDataProperty("http://b#p1")
        );

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        Assertions.assertTrue(
                o1.contains(Filters.subClassWithSub, df.getOWLClass("http://a#C1"), Imports.INCLUDED)
        );
        Assertions.assertFalse(
                o1.contains(Filters.subClassWithSuper, df.getOWLClass("http://b#C2"), Imports.INCLUDED)
        );
    }

    @Test
    void testListSignature1() {
        OntologyManager om = OntManagers.createDirectManager();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_DL_MEM_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1");
        m2.createOntClass("http://b#C2");
        m2.createDataProperty("http://b#p1");
        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createAnnotationProperty("http://a#p2");
        m1.createDataProperty("http://a#p3");
        m1.createIndividual("http://b#i2");
        m1.createIndividual(null, m2.getOntClass("http://b#C2"));
        m2.createIndividual(null, m2.getOntClass("http://b#C1"));

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        List<OWLEntity> actual = o1.signature(Imports.INCLUDED).toList();
        Assertions.assertEquals(172, actual.size());

        Assertions.assertEquals(34, actual.stream().filter(it -> it instanceof OWLClass).count());
        Assertions.assertEquals(34, o1.classesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(32, actual.stream().filter(it -> it instanceof OWLDatatype).count());
        Assertions.assertEquals(32, o1.datatypesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(94, actual.stream().filter(it -> it instanceof OWLNamedIndividual).count());
        Assertions.assertEquals(94, o1.individualsInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(3, actual.stream().filter(it -> it instanceof OWLObjectProperty).count());
        Assertions.assertEquals(3, o1.objectPropertiesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(2, actual.stream().filter(it -> it instanceof OWLDataProperty).count());
        Assertions.assertEquals(2, o1.dataPropertiesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(7, actual.stream().filter(it -> it instanceof OWLAnnotationProperty).count());
        Assertions.assertEquals(7, o1.annotationPropertiesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(2, o1.referencedAnonymousIndividuals(Imports.INCLUDED).count());

        // class and named individual
        Assertions.assertEquals(2, o1.entitiesInSignature(IRI.create("http://b#C2"), Imports.INCLUDED).count());
        // datatype and named individual
        Assertions.assertEquals(2, o1.entitiesInSignature(IRI.create(XSD.date.getURI()), Imports.INCLUDED).count());
        // 1 named individual
        Assertions.assertEquals(1, o1.entitiesInSignature(IRI.create("http://b#i2"), Imports.INCLUDED).count());
        // object property + named individual
        Assertions.assertEquals(2, o1.entitiesInSignature(IRI.create(OWL.sameAs.getURI()), Imports.INCLUDED).count());
        // data property + named individual
        Assertions.assertEquals(2, o1.entitiesInSignature(IRI.create("http://b#p1"), Imports.INCLUDED).count());
        // annotation property + named individual
        Assertions.assertEquals(2, o1.entitiesInSignature(IRI.create("http://a#p2"), Imports.INCLUDED).count());

        Assertions.assertEquals(68, o1.getPunnedIRIs(Imports.INCLUDED).size());
        Assertions.assertTrue(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create("http://b#C2")));
        Assertions.assertTrue(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create(XSD.xstring.getURI())));
        Assertions.assertFalse(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create("http://b#i2")));
        Assertions.assertTrue(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create(OWL.disjointWith.getURI())));
        Assertions.assertTrue(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create("http://b#p1")));
        Assertions.assertTrue(o1.getPunnedIRIs(Imports.INCLUDED).contains(IRI.create("http://a#p2")));
    }

    @Test
    void testListSignature2() {
        OntologyManager om = OntManagers.createDirectManager();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_DL_MEM);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1");
        m2.createOntClass("http://b#C2");
        m2.createDataProperty("http://b#p1");
        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createAnnotationProperty("http://a#p2");
        m1.createDataProperty("http://a#p3");
        m1.createIndividual("http://a#i2");
        m1.createIndividual(null, m2.getOntClass("http://b#C2"));
        m2.createIndividual(null, m2.getOntClass("http://b#C1"));

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        List<OWLEntity> actual = o1.signature(Imports.INCLUDED).toList();
        Assertions.assertEquals(6, actual.size());

        Assertions.assertEquals(3, actual.stream().filter(it -> it instanceof OWLClass).count());
        Assertions.assertEquals(3, o1.classesInSignature(Imports.INCLUDED).count());
        Assertions.assertEquals(0, actual.stream().filter(it -> it instanceof OWLDatatype).count());
        Assertions.assertEquals(0, o1.datatypesInSignature(Imports.INCLUDED).count());
        Assertions.assertEquals(0, actual.stream().filter(it -> it instanceof OWLNamedIndividual).count());
        Assertions.assertEquals(0, o1.individualsInSignature(Imports.INCLUDED).count());
        Assertions.assertEquals(0, actual.stream().filter(it -> it instanceof OWLObjectProperty).count());
        Assertions.assertEquals(0, o1.objectPropertiesInSignature(Imports.INCLUDED).count());
        Assertions.assertEquals(2, actual.stream().filter(it -> it instanceof OWLDataProperty).count());
        Assertions.assertEquals(2, o1.dataPropertiesInSignature(Imports.INCLUDED).count());
        Assertions.assertEquals(1, actual.stream().filter(it -> it instanceof OWLAnnotationProperty).count());
        Assertions.assertEquals(1, o1.annotationPropertiesInSignature(Imports.INCLUDED).count());

        Assertions.assertEquals(2, o1.referencedAnonymousIndividuals(Imports.INCLUDED).count());
    }

    @Test
    void testContainsInSignature() {
        OntologyManager om = OntManagers.createDirectManager();
        DataFactory df = om.getOWLDataFactory();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_DL_MEM_RULES_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1");
        m2.createOntClass("http://b#C2");
        m2.createDataProperty("http://b#p1");
        m2.createIndividual("http://b#i1");
        m1.createOntClass("http://a#C1");
        m1.createAnnotationProperty("http://a#p2");
        m1.createDataProperty("http://a#p3");
        m1.createIndividual("http://a#i2");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        Assertions.assertTrue(o1.containsClassInSignature(IRI.create("http://b#C1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsClassInSignature(df.getOWLThing().getIRI(), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLThing().getIRI(), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLThing(), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsClassInSignature(IRI.create("http://a#C3"), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsEntityInSignature(IRI.create("http://a#C3"), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsDatatypeInSignature(IRI.create(XSD.unsignedInt.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(XSD.unsignedInt.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLDatatype(XSD.unsignedInt.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsDatatypeInSignature(IRI.create(OWL.Nothing.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(OWL.Nothing.getURI()), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsIndividualInSignature(IRI.create("http://b#i1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsIndividualInSignature(IRI.create("http://a#i2"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsIndividualInSignature(IRI.create(OWL.NamedIndividual.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsIndividualInSignature(IRI.create(OWL.Thing.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create("http://b#i1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLNamedIndividual("http://b#i1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create("http://a#i2"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(OWL.NamedIndividual.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(OWL.Thing.getURI()), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsDataPropertyInSignature(IRI.create("http://b#p1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create("http://b#p1"), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLDataProperty("http://b#p1"), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsDatatypeInSignature(IRI.create(OWL.bottomDataProperty.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsEntityInSignature(IRI.create(OWL.bottomDataProperty.getURI()), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsObjectPropertyInSignature(IRI.create(OWL.disjointWith.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(OWL.disjointWith.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLObjectProperty(OWL.disjointWith.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsObjectPropertyInSignature(IRI.create(OWL.topObjectProperty.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsEntityInSignature(IRI.create(OWL.topObjectProperty.getURI()), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsAnnotationPropertyInSignature(IRI.create(RDFS.isDefinedBy.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(IRI.create(RDFS.isDefinedBy.getURI()), Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntityInSignature(df.getOWLAnnotationProperty(RDFS.isDefinedBy.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsAnnotationPropertyInSignature(IRI.create(OWL.topObjectProperty.getURI()), Imports.INCLUDED));
        Assertions.assertFalse(o1.containsEntityInSignature(IRI.create(OWL.topObjectProperty.getURI()), Imports.INCLUDED));

        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.CLASS, Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.DATATYPE, Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.NAMED_INDIVIDUAL, Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.OBJECT_PROPERTY, Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.DATA_PROPERTY, Imports.INCLUDED));
        Assertions.assertTrue(o1.containsEntitiesOfTypeInSignature(EntityType.ANNOTATION_PROPERTY, Imports.INCLUDED));
    }
}
