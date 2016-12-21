package ru.avicomp.ontapi.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.OWL2;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import uk.ac.manchester.cs.owl.owlapi.OWLAnonymousIndividualImpl;

/**
 * Test Utils.
 * <p>
 * Created by @szuev on 16.10.2016.
 */
public class TestUtils {
    private static final Logger LOGGER = Logger.getLogger(TestUtils.class);

    private static final OWLAnonymousIndividual ANONYMOUS_INDIVIDUAL = new OWLAnonymousIndividualImpl(NodeID.getNodeID());

    public static OntologyModel createModel(OntIRI iri) {
        return createModel(iri.toOwlOntologyID());
    }

    public static OntologyModel createModel(OWLOntologyID id) {
        return createModel(OntManagerFactory.createONTManager(), id);
    }

    public static OntologyModel createModel(OntologyManager manager, OWLOntologyID id) {
        LOGGER.info("Create ontology " + id);
        return manager.createOntology(id);
    }

    public static OntGraphModel copyOntModel(OntGraphModel original, String newURI) {
        String oldURI = getURI(original);
        if (newURI == null) newURI = oldURI + ".copy";
        OntGraphModel res = new OntGraphModelImpl();
        res.setNsPrefix("", newURI + "#");
        res.add(original.getBaseModel().listStatements());
        res.setID(newURI);
        return res;
    }

    public static String getURI(Model model) {
        if (model == null) return null;
        Resource ontology = findOntologyResource(model);
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

    private static Resource findOntologyResource(Model model) {
        if (model == null) return null;
        if (OntGraphModel.class.isInstance(model)) {
            return ((OntGraphModel) model).getID();
        }
        if (OntModel.class.isInstance(model)) {
            return getOntology((OntModel) model);
        }
        List<Statement> statements = model.listStatements(null, RDF.type, OWL.Ontology).toList();
        return statements.size() != 1 ? null : statements.get(0).getSubject();
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

    public static Triple createTriple(Resource r, Property p, RDFNode o) {
        return Triple.create(r.asNode(), p.asNode(), o.asNode());
    }

    public static void compareAxioms(Stream<? extends OWLAxiom> expected, Stream<? extends OWLAxiom> actual) {
        compareAxioms(toMap(expected), toMap(actual));
    }

    public static void compareAxioms(Map<AxiomType, List<OWLAxiom>> expected, Map<AxiomType, List<OWLAxiom>> actual) {
        LOGGER.debug("[Compare] Expected axioms: ");
        expected.values().forEach(LOGGER::debug);
        LOGGER.debug("[Compare] Actual axioms: ");
        actual.values().forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect axiom types:", expected.keySet(), actual.keySet());
        List<String> errors = new ArrayList<>();
        for (AxiomType type : expected.keySet()) {
            List<OWLAxiom> exList = expected.get(type);
            List<OWLAxiom> acList = actual.get(type);
            if (exList.size() != acList.size()) {
                errors.add(String.format("[%s]incorrect axioms list: %d != %d", type, exList.size(), acList.size()));
                continue;
            }
            for (int i = 0; i < exList.size(); i++) {
                OWLAxiom a = exList.get(i);
                OWLAxiom b = acList.get(i);
                if (same(a, b)) continue;
                errors.add(String.format("[%s]%s != %s", type, a, b));
            }
        }
        errors.forEach(LOGGER::error);
        Assert.assertTrue("There are " + errors.size() + " errors", errors.isEmpty());
    }

    public static Map<AxiomType, List<OWLAxiom>> toMap(Stream<? extends OWLAxiom> stream) {
        return toMap(stream.collect(Collectors.toList()));
    }

    public static Map<AxiomType, List<OWLAxiom>> toMap(List<? extends OWLAxiom> list) {
        Set<AxiomType> types = list.stream().map(OWLAxiom::getAxiomType).collect(Collectors.toSet());
        Map<AxiomType, List<OWLAxiom>> res = new HashMap<>();
        types.forEach(type -> {
            List<OWLAxiom> value = res.computeIfAbsent(type, t -> new ArrayList<>());
            List<OWLAxiom> byType = list.stream().filter(a -> type.equals(a.getAxiomType())).sorted().collect(Collectors.toList());
            value.addAll(byType);
        });
        return res;
    }

    public static boolean same(OWLAxiom a, OWLAxiom b) {
        return a.typeIndex() == b.typeIndex() && OWLAPIStreamUtils.equalStreams(replaceAnonymous(a.components()), replaceAnonymous(b.components()));
    }

    private static Stream<?> replaceAnonymous(Stream<?> stream) {
        return stream.map(o -> o instanceof OWLAnonymousIndividual ? ANONYMOUS_INDIVIDUAL : o);
    }

    public static void setDefaultPrefixes(OntGraphModel m) {
        m.setNsPrefix("owl", OWL2.getURI());
        m.setNsPrefix("rdfs", RDFS.getURI());
        m.setNsPrefix("rdf", RDF.getURI());
        m.setNsPrefix("xsd", XSD.getURI());
    }
}
