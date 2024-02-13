package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.sszuev.jena.ontapi.OntSpecification;
import com.github.sszuev.jena.ontapi.UnionGraph;
import com.github.sszuev.jena.ontapi.utils.Graphs;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;

public class JenaInferenceTest {

    @Test
    void testSpec_OWL2_FULL_MEM_RDFS_INF() {
        OntologyManager m = OntManagers.createManager();
        DataFactory df = m.getOWLDataFactory();
        m.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_FULL_MEM_RDFS_INF);

        Ontology ont = m.createOntology(IRI.create("ont-a"));
        Assertions.assertInstanceOf(InfGraph.class, ont.asGraphModel().getGraph());
        Assertions.assertInstanceOf(UnionGraph.class, ((InfGraph) ont.asGraphModel().getGraph()).getRawGraph());
        Assertions.assertTrue(
                Graphs.isGraphMem(((UnionGraph) ((InfGraph) ont.asGraphModel().getGraph()).getRawGraph()).getBaseGraph())
        );

        ont.add(df.getOWLDeclarationAxiom(df.getOWLClass("A")));
        ont.add(df.getOWLDeclarationAxiom(df.getOWLClass("B")));
        ont.add(df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B")));

        Assertions.assertEquals(3, ont.axioms().count());

        Assertions.assertEquals(21, ont.asGraphModel().statements(null, RDFS.subClassOf, null).count());
        Assertions.assertEquals(68, ont.asGraphModel().statements(null, RDF.type, null).count());
    }

    @Test
    void testSpec_OWL1_DL_MEM_TRANS_INF() {
        OntologyManager m = OntManagers.createManager();
        DataFactory df = m.getOWLDataFactory();
        m.getOntologyConfigurator().setSpecification(OntSpecification.OWL1_DL_MEM_TRANS_INF);

        Ontology a = m.createOntology(IRI.create("ont-a"));
        Ontology b = m.createOntology(IRI.create("ont-b"));
        Assertions.assertInstanceOf(InfGraph.class, b.asGraphModel().getGraph());
        Assertions.assertInstanceOf(UnionGraph.class, ((InfGraph) b.asGraphModel().getGraph()).getRawGraph());
        Assertions.assertTrue(
                Graphs.isGraphMem(((UnionGraph) ((InfGraph) b.asGraphModel().getGraph()).getRawGraph()).getBaseGraph())
        );

        m.applyChange(new AddImport(a, df.getOWLImportsDeclaration(b.getOntologyID().getOntologyIRI().orElseThrow())));

        b.add(df.getOWLDeclarationAxiom(df.getOWLClass("A")));
        b.add(df.getOWLDeclarationAxiom(df.getOWLClass("B")));
        b.add(df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B")));

        Assertions.assertEquals(3, b.axioms().count());
        Assertions.assertEquals(3, a.axioms().count());

        Assertions.assertEquals(3, b.asGraphModel().statements(null, RDFS.subClassOf, null).count());
        Assertions.assertEquals(3, b.asGraphModel().statements(null, RDF.type, null).count());

        Assertions.assertThrows(OntApiException.class,
                () -> b.add(df.getOWLHasKeyAxiom(df.getOWLObjectHasSelf(df.getOWLObjectProperty("self"))))
        );

        Assertions.assertEquals(4, b.asGraphModel().size());
        Assertions.assertEquals(6, a.asGraphModel().size());
        Assertions.assertEquals(2, a.asGraphModel().getBaseGraph().size());

        Assertions.assertEquals(2, m.models().count());

        m.models().forEach(model -> {
            Assertions.assertInstanceOf(InfGraph.class, model.getGraph());
            Assertions.assertInstanceOf(UnionGraph.class, ((InfGraph) model.getGraph()).getRawGraph());
            Assertions.assertTrue(
                    Graphs.isGraphMem(((UnionGraph) ((InfGraph) model.getGraph()).getRawGraph()).getBaseGraph())
            );
        });
    }
}
