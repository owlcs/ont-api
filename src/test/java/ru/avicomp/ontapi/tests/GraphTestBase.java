package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * base and utility class for graph-tests
 * <p>
 * Created by @szuev on 02.10.2016.
 */
abstract class GraphTestBase {
    static final Logger LOGGER = Logger.getLogger(GraphTestBase.class);

    static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (o1, o2) -> NodeUtils.compareRDFTerms(o1.asNode(), o2.asNode());

    static Ontology getOntology(OntModel model) {
        List<Ontology> ontologies = model.listOntologies().toList();
        Assert.assertFalse("No ontologies at all", ontologies.isEmpty());
        if (ontologies.size() == 1) return ontologies.get(0);
        List<OntModel> imports = model.listSubModels(true).toList();
        for (OntModel i : imports) {
            ontologies.removeAll(i.listOntologies().toList());
        }
        if (ontologies.size() != 1)
            Assert.fail("More then one jena-ontology inside model : " + ontologies.size());
        return ontologies.get(0);
    }

    static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
        List<OWLAxiom> list1 = expected.sorted().collect(Collectors.toList());
        List<OWLAxiom> list2 = actual.sorted().collect(Collectors.toList());
        Assert.assertEquals("Not equal axioms streams count", list1.size(), list2.size());
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < list1.size(); i++) {
            OWLAxiom a = list1.get(i);
            OWLAxiom b = list2.get(i);
            if (same(a, b)) continue;
            errors.add(String.format("%s != %s", a, b));
        }
        errors.forEach(LOGGER::error);
        Assert.assertTrue("There are " + errors.size() + " errors", errors.isEmpty());
    }

    static boolean same(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex() && OWLAPIStreamUtils.equalStreams(a.components(), b.components());
    }

    static void debug(OntologyModel ontology) {
        LOGGER.info("DEBUG:");
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        LOGGER.debug("Jena: ");
        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TTL_RDF);
    }
}
