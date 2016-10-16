package ru.avicomp.ontapi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;

/**
 * Test Utils.
 * <p>
 * Created by @szuev on 16.10.2016.
 */
public class TestUtils {
    private static final Logger LOGGER = Logger.getLogger(TestUtils.class);

    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (o1, o2) -> NodeUtils.compareRDFTerms(o1.asNode(), o2.asNode());

    public static OntologyModel load(OWLOntologyManager manager, IRI fileIRI) {
        LOGGER.info("Load ontology model from " + fileIRI + ".");
        OWLOntology owl = null;
        try {
            owl = manager.loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals("incorrect class " + owl.getClass(), OntologyModel.class, owl.getClass());
        return (OntologyModel) owl;
    }

    public static OntologyModel putOntModelToManager(OWLOntologyManager manager, OntModel model, OntFormat convertFormat) {
        String uri = getURI(model);
        LOGGER.info("Put ontology " + uri + "(" + convertFormat + ") to manager.");
        try (InputStream is = ReadWriteUtils.toInputStream(model, convertFormat == null ? OntFormat.TTL_RDF : convertFormat)) {
            manager.loadOntologyFromOntologyDocument(is);
        } catch (IOException | OWLOntologyCreationException e) {
            Assert.fail(e.getMessage());
        }
        OntologyModel res = (OntologyModel) manager.getOntology(IRI.create(uri));
        Assert.assertNotNull("Can't find ontology " + uri, res);
        return res;
    }

    public static OntModel copyOntModel(OntModel original, String newURI) {
        String oldURI = getURI(original);
        if (newURI == null) newURI = oldURI + ".copy";
        OntModel res = ModelFactory.createOntologyModel(original.getSpecification());
        res.setNsPrefix("", newURI + "#");
        res.add(original.getBaseModel().listStatements());
        ResourceUtils.renameResource(res.getOntology(oldURI), newURI);
        return res;
    }

    public static String getURI(Model model) {
        if (model == null) return null;
        Resource ontology = findOntology(model);
        if (ontology != null) {
            return ontology.getURI();
        }
        // maybe there is a base prefix (topbraid-style)
        String res = model.getNsPrefixURI("base");
        if (res == null)
            res = model.getNsPrefixURI(""); // sometimes empty prefix is used for record doc-uri (protege, owl-api).
        if (res != null) {
            res = res.replaceAll("#$", "");
        }
        return res;
    }

    public static Resource findOntology(Model model) {
        if (model == null) return null;
        if (!OntModel.class.isInstance(model)) {
            List<Statement> statements = model.listStatements(null, RDF.type, OWL.Ontology).toList();
            return statements.size() != 1 ? null : statements.get(0).getSubject();
        }
        return getOntology((OntModel) model);
    }

    public static Ontology getOntology(OntModel model) {
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

    public static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
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

    public static boolean same(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex() && OWLAPIStreamUtils.equalStreams(a.components(), b.components());
    }
}
