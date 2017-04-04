package ru.avicomp.ontapi.transforms;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * To convert OWL 1 DL => OWL 2 DL
 * <p>
 * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_RDF_Graphs_to_the_Structural_Specification'>Chapter 3</a>
 * also <a href='https://www.w3.org/TR/owl2-quick-reference/'>4.2 Additional Vocabulary in OWL 2 RDF Syntax</a>
 */
@SuppressWarnings("WeakerAccess")
public class OWLTransform extends Transform {
    protected static final boolean PROCESS_INDIVIDUALS_DEFAULT = false;
    private boolean processIndividuals;

    public OWLTransform(Graph graph) {
        this(graph, PROCESS_INDIVIDUALS_DEFAULT);
    }

    protected OWLTransform(Graph graph, boolean processIndividuals) {
        super(graph);
        this.processIndividuals = processIndividuals;
    }

    @Override
    public void perform() {
        // table 5:
        Stream.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class)
                .map(p -> statements(null, RDF.type, p))
                .flatMap(Function.identity())
                .forEach(s -> undeclare(s.getSubject(), RDFS.Class));

        // table 5:
        Stream.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty,
                OWL.TransitiveProperty, OWL.DatatypeProperty, OWL.AnnotationProperty, OWL.OntologyProperty)
                .map(p -> statements(null, RDF.type, p))
                .flatMap(Function.identity())
                .forEach(s -> undeclare(s.getSubject(), RDF.Property));

        // definitely ObjectProperty (table 6, supplemented by owl:AsymmetricProperty, owl:ReflexiveProperty, owl:IrreflexiveProperty):
        Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty,
                OWL.AsymmetricProperty, OWL.ReflexiveProperty, OWL.IrreflexiveProperty)
                .map(p -> statements(null, RDF.type, p))
                .flatMap(Function.identity())
                .forEach(s -> declare(s.getSubject(), OWL.ObjectProperty));

        // replace owl:OntologyProperty with owl:AnnotationProperty (table 6)
        changeType(OWL.OntologyProperty, OWL.AnnotationProperty);
        // replace owl:DataRange(as deprecated) with rdfs:Datatype (see quick-reference guide)
        changeType(OWL.DataRange, RDFS.Datatype);

        fixInvalidURIs();
        fixOntology();
        fixAxioms();
        if (processIndividuals) {
            fixNamedIndividuals();
        }
    }

    /**
     * merge several owl:Ontology to single one.
     * as primary choose the one that has the largest number of triplets.
     * if there is no any owl:Ontology -> add new anonymous owl:Ontology
     */
    protected void fixOntology() {
        Model m = getBaseModel();
        // choose or create the new one:
        Resource ontology = Graphs.getOntology(getBaseGraph())
                .map(m::getRDFNode).map(RDFNode::asResource)
                .orElseGet(() -> m.createResource().addProperty(RDF.type, OWL.Ontology));
        // move all content from other ontologies to the selected one
        Stream<Resource> other = statements(null, RDF.type, OWL.Ontology)
                .map(Statement::getSubject)
                .filter(s -> !ontology.equals(s));
        List<Statement> rest = other
                .map(o -> statements(o, null, null))
                .flatMap(Function.identity()).collect(Collectors.toList());
        rest.forEach(s -> ontology.addProperty(s.getPredicate(), s.getObject()));
        // remove all other ontologies
        m.remove(rest);
    }

    protected void fixAxioms() {
        fixClassExpressions();
        fixPropertyChains();
    }

    /**
     * to replace
     * {@link WRONG_OWL#propertyChain} -> {@link OWL#propertyChainAxiom}
     * {@link WRONG_OWL#DataProperty} -> {@link OWL#DatatypeProperty}
     *
     * @see WRONG_OWL
     */
    public void fixInvalidURIs() {
        Set<Statement> propertyChains = statements(null, OWLTransform.WRONG_OWL.propertyChain, null).collect(Collectors.toSet());
        propertyChains.forEach(s -> {
            getBaseModel().remove(s);
            getBaseModel().add(s.getSubject(), OWL.propertyChainAxiom, s.getObject());
        });
        Set<Statement> dataProperties = statements(null, org.apache.jena.vocabulary.RDF.type, OWLTransform.WRONG_OWL.DataProperty).collect(Collectors.toSet());
        dataProperties.forEach(s -> {
            getBaseModel().remove(s);
            getBaseModel().add(s.getSubject(), org.apache.jena.vocabulary.RDF.type, OWL.DatatypeProperty);
        });
    }

    /**
     * As shown by OWL-API-contract SubPropertyChainOfAxiom could be expressed in the following form:
     * <pre>
     * [    rdfs:subPropertyOf  :r ;
     *      owl:propertyChain   ( :p :q )
     * ] .
     * </pre>
     * Unfortunately I could not find specification for this case,
     * but I believe that this is an example of some rudimental OWL dialect, so it must be fixed here.
     */
    public void fixPropertyChains() {
        Set<Resource> anons = statements(null, OWL.propertyChainAxiom, null)
                .map(Statement::getSubject)
                .filter(RDFNode::isAnon)
                .filter(s -> s.hasProperty(RDFS.subPropertyOf) && s.getPropertyResourceValue(RDFS.subPropertyOf).isURIResource())
                .filter(s -> s.getPropertyResourceValue(OWL.propertyChainAxiom).canAs(RDFList.class))
                .collect(Collectors.toSet());
        anons.forEach(a -> {
            Resource subj = a.getRequiredProperty(RDFS.subPropertyOf).getObject().asResource();
            Resource obj = a.getRequiredProperty(OWL.propertyChainAxiom).getObject().asResource();
            getBaseModel().add(subj, OWL.propertyChainAxiom, obj);
            getBaseModel().removeAll(a, RDFS.subPropertyOf, null);
            getBaseModel().removeAll(a, OWL.propertyChainAxiom, null);
        });
    }

    /**
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_Axioms'>Chapter 3.2.5, Table 18</a>
     * Warning: there is also an interpretation of empty rdf:List as owl:Nothing or owl:Thing in table.
     * But I don't see that OWL-API works such way (it treats rdf:nil as just an empty collection).
     * So we skip also fixing for this case.
     * Plus - it works currently only for named owl:Class resources.
     */
    protected void fixClassExpressions() {
        Stream.of(OWL.complementOf, OWL.unionOf, OWL.intersectionOf, OWL.oneOf)
                .forEach(property -> {
                    Set<Resource> classes = statements(null, property, null)
                            .map(Statement::getSubject)
                            .filter(RDFNode::isURIResource)
                            .collect(Collectors.toSet());
                    classes.forEach(c -> moveToEquivalentClass(c, property));
                });
    }

    protected void moveToEquivalentClass(Resource subject, Property predicate) {
        List<Statement> statements = statements(subject, predicate, null).collect(Collectors.toList());
        Model m = getBaseModel();
        Resource newRoot = m.createResource();
        newRoot.addProperty(RDF.type, OWL.Class);
        m.add(subject, OWL.equivalentClass, newRoot);
        statements.forEach(s -> newRoot.addProperty(s.getPredicate(), s.getObject()));
        m.remove(statements);
    }

    protected void fixNamedIndividuals() {
        Set<Statement> statements = statements(null, RDF.type, null)
                .filter(s -> s.getSubject().isURIResource())
                .filter(s -> s.getObject().isResource())
                .filter(s -> !builtIn.reservedResources().contains(s.getObject().asResource())).collect(Collectors.toSet());
        statements.forEach(s -> declare(s.getSubject(), OWL.NamedIndividual));
    }

    /**
     * Vocabulary with wrong OWL URIs.
     * As shown by OWL-API-contract-tests the OWL-API ontology could contain inappropriate URIs (properties and resources).
     * Usually they are in RDF/XML format.
     * Currently I see only two such cases:
     * - owl:propertyChain
     * - owl:DataProperty
     * There are no such URIs in the OWL2 specification. Instead there are following things:
     * - owl:propertyChainAxiom
     * - owl:DatatypeProperty
     * Unfortunately I could not find documentation about these things.
     *
     * @see OWL the correct vocabulary
     */
    public static class WRONG_OWL {
        public final static String NS = OWL.NS;
        public static final Property propertyChain = property("propertyChain");
        public static final Resource DataProperty = resource("DataProperty");

        protected static Resource resource(String local) {
            return ResourceFactory.createResource(NS + local);
        }

        protected static Property property(String local) {
            return ResourceFactory.createProperty(NS + local);
        }
    }
}
