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

import java.util.List;

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
                () -> ontology.add(df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLObjectUnionOf(df.getOWLClass("B")))));

    }
}
