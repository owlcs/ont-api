package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.List;
import java.util.Objects;

public class ListRawAxiomsTest {

    @Test
    void testListAllAxioms() {
        OntologyManager om = OntManagers.createDirectManager();
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        OntModel m1 = om.createGraphModel("http://a#A");
        OntModel m2 = om.createGraphModel("http://b#B");
        m1.addImport(m2);

        m2.createOntClass("http://b#C1");
        m2.createOntClass("http://b#C2");
        m1.createOntClass("http://a#C1");

        Ontology o1 = Objects.requireNonNull(om.getOntology(IRI.create("http://a#A")));

        // Declaration(Class(<http://a#C1>))
        // Declaration(Class(<http://b#C2>))
        // Declaration(Class(<http://b#C1>))
        // Declaration(Datatype(rdf:XMLLiteral))
        // SubAnnotationPropertyOf(rdfs:isDefinedBy rdfs:isDefinedBy)
        // SubAnnotationPropertyOf(rdfs:isDefinedBy rdfs:seeAlso)
        // SubAnnotationPropertyOf(rdfs:seeAlso rdfs:seeAlso)
        // AnnotationPropertyRange(rdfs:label <http://www.w3.org/2000/01/rdf-schema#Literal>)
        // AnnotationPropertyRange(rdfs:comment <http://www.w3.org/2000/01/rdf-schema#Literal>)
        List<OWLAxiom> actual = o1.axioms(Imports.INCLUDED).toList();
        Assertions.assertEquals(9, actual.size());
        Assertions.assertEquals(4, actual.stream().filter(it -> it instanceof OWLDeclarationAxiom).count());
        Assertions.assertEquals(3, actual.stream().filter(it -> it instanceof OWLSubAnnotationPropertyOfAxiom).count());
        Assertions.assertEquals(2, actual.stream().filter(it -> it instanceof OWLAnnotationPropertyRangeAxiom).count());
    }
}
