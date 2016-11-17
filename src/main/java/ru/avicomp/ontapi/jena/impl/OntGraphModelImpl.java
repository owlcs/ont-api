package ru.avicomp.ontapi.jena.impl;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Base model to work through jena only.
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work in accordance with OWL2 DL specification.
 * <p>
 * Created by @szuev on 27.10.2016.
 */
public class OntGraphModelImpl extends ModelCom implements OntGraphModel {

    /**
     * fresh ontology.
     */
    public OntGraphModelImpl() {
        this(new GraphMem());
    }

    public OntGraphModelImpl(Graph graph) {
        this(graph instanceof UnionGraph ? graph : new UnionGraph(graph), OntModelConfig.ONT_PERSONALITY);
    }

    public OntGraphModelImpl(Graph graph, OntPersonality personality) {
        super(graph instanceof UnionGraph ? graph : new UnionGraph(graph), personality);
    }

    @Override
    public OntID getID() {
        List<Resource> prev = ontologies().collect(Collectors.toList());
        Resource res;
        if (prev.isEmpty()) {
            res = createResource(); // anon id
            add(res, RDF.type, OWL2.Ontology);
        } else {
            res = prev.get(0); // choose first.
        }
        return getNodeAs(res.asNode(), OntID.class);
    }

    @Override
    public OntID setID(String uri) {
        List<Statement> tmp = ontologies().map(s -> JenaUtils.asStream(listStatements(s, null, (RDFNode) null))).flatMap(Function.identity()).distinct().collect(Collectors.toList());
        remove(tmp);
        Resource subject = uri == null ? createResource() : createResource(uri);
        add(subject, RDF.type, OWL2.Ontology);
        tmp.forEach(s -> add(subject, s.getPredicate(), s.getObject()));
        return getNodeAs(subject.asNode(), OntID.class);
    }

    private Stream<Resource> ontologies() {
        return JenaUtils.asStream(listStatements(null, RDF.type, OWL2.Ontology).mapWith(Statement::getSubject)).distinct();
    }

    public void addImport(OntGraphModelImpl m) {
        getGraph().addGraph(m.getBaseGraph());
        // todo:
    }

    public void removeImport(OntGraphModelImpl m) {
        getGraph().removeGraph(m.getBaseGraph());
        // todo:
    }

    @Override
    public UnionGraph getGraph() {
        return (UnionGraph) super.getGraph();
    }

    @Override
    public Graph getBaseGraph() {
        return getGraph().getBaseGraph();
    }

    public Model getBaseModel() {
        return ModelFactory.createModelForGraph(getBaseGraph());
    }

    @Override
    public PrefixMapping setNsPrefix(String prefix, String uri) {
        getBaseGraph().getPrefixMapping().setNsPrefix(prefix, uri);
        return this;
    }

    @Override
    public Model write(Writer writer) {
        return getBaseModel().write(writer);
    }

    @Override
    public Model write(Writer writer, String lang) {
        return getBaseModel().write(writer, lang);
    }

    @Override
    public Model write(Writer writer, String lang, String base) {
        return getBaseModel().write(writer, lang, base);
    }

    @Override
    public Model write(OutputStream out) {
        return getBaseModel().write(out);
    }

    @Override
    public Model write(OutputStream out, String lang) {
        return getBaseModel().write(out, lang);
    }

    @Override
    public Model write(OutputStream out, String lang, String base) {
        return getBaseModel().write(out, lang, base);
    }

    public boolean isInBaseModel(Statement stmt) {
        return isInBaseModel(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
    }

    protected boolean isInBaseModel(Resource s, Property p, RDFNode o) {
        return getBaseGraph().contains(s.asNode(), p.asNode(), o.asNode());
    }

    @Override
    public OntGraphModelImpl remove(Resource s, Property p, RDFNode o) { // todo: removing is allowed only for base graph
        graph.delete(Triple.create(s.asNode(), p.asNode(), o.asNode()));
        return this;
    }

    @Override
    public Stream<OntEntity> ontEntities() {
        return ontObjects(OntEntity.class);
    }

    /**
     * to retrieve the stream of {@link OntObject}s
     *
     * @param type Class
     * @return Stream
     */
    @Override
    public <T extends OntObject> Stream<T> ontObjects(Class<T> type) {
        return getPersonality().getOntImplementation(type).find(this).map(e -> getNodeAs(e.asNode(), type));
    }

    @Override
    public <T extends OntEntity> T getOntEntity(Class<T> type, String uri) {
        Node n = NodeFactory.createURI(OntException.notNull(uri, "Null uri."));
        try { // returns not null in case it is present in graph or built-in.
            return getNodeAs(n, type);
        } catch (ConversionException ignore) {
            // ignore
            return null;
        }
    }

    /**
     * to create any OntObject resource
     *
     * @param type Class
     * @param uri  String
     * @return OntObject
     */
    public <T extends OntObject> T createOntObject(Class<T> type, String uri) {
        Resource res = uri == null ? createResource() : createResource(uri);
        return getPersonality().getOntImplementation(type).create(res.asNode(), this).as(type);
    }

    @Override
    public <T extends OntEntity> T createOntEntity(Class<T> type, String uri) {
        return createOntObject(type, uri);
    }

    @Override
    public void removeOntObject(OntObject obj) {
        obj.clearAnnotations();
        removeAll(obj, null, null);
    }

    @Override
    public <T extends OntEntity> Stream<T> ontEntities(Class<T> type) {
        return ontObjects(type);
    }

    @Override
    protected OntPersonality getPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    @Override
    public OntDisjoint.Classes createDisjointClasses(Stream<OntCE> classes) {
        return OntDisjointImpl.createDisjointClasses(this, classes);
    }

    @Override
    public OntDisjoint.Individuals createDifferentIndividuals(Stream<OntIndividual> individuals) {
        return OntDisjointImpl.createDifferentIndividuals(this, individuals);
    }

    @Override
    public OntDisjoint.ObjectProperties createDisjointObjectProperties(Stream<OntOPE> properties) {
        return OntDisjointImpl.createDisjointObjectProperties(this, properties);
    }

    @Override
    public OntDisjoint.DataProperties createDisjointDataProperties(Stream<OntNDP> properties) {
        return OntDisjointImpl.createDisjointDataProperties(this, properties);
    }

    @Override
    public <T extends OntFR> T createFacetRestriction(Class<T> view, Literal literal) {
        return OntFRImpl.create(this, view, literal);
    }

    @Override
    public OntDR.OneOf createOneOfDataRange(Stream<Literal> values) {
        return OntDRImpl.createOneOf(this, values);
    }

    @Override
    public OntDR.Restriction createRestrictionDataRange(OntDR property, Stream<OntFR> values) {
        return OntDRImpl.createRestriction(this, property, values);
    }

    @Override
    public OntDR.ComplementOf createComplementOfDataRange(OntDR other) {
        return OntDRImpl.createComplementOf(this, other);
    }

    @Override
    public OntDR.UnionOf createUnionOfDataRange(Stream<OntDR> values) {
        return OntDRImpl.createUnionOf(this, values);
    }

    @Override
    public OntDR.IntersectionOf createIntersectionOfDataRange(Stream<OntDR> values) {
        return OntDRImpl.createIntersectionOf(this, values);
    }

    @Override
    public OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectSomeValuesFrom.class, onProperty, other, OWL2.someValuesFrom);
    }

    @Override
    public OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataSomeValuesFrom.class, onProperty, other, OWL2.someValuesFrom);
    }

    @Override
    public OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectAllValuesFrom.class, onProperty, other, OWL2.allValuesFrom);
    }

    @Override
    public OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataAllValuesFrom.class, onProperty, other, OWL2.allValuesFrom);
    }

    @Override
    public OntCE.ObjectHasValue createObjectHasValue(OntOPE onProperty, OntIndividual other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectHasValue.class, onProperty, other, OWL2.hasValue);
    }

    @Override
    public OntCE.DataHasValue createDataHasValue(OntNDP onProperty, Literal other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataHasValue.class, onProperty, other, OWL2.hasValue);
    }

    @Override
    public OntCE.ObjectMinCardinality createObjectMinCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMinCardinality createDataMinCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectMaxCardinality createObjectMaxCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMaxCardinality createDataMaxCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectCardinality createObjectCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataCardinality createDataCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.UnionOf createUnionOf(Stream<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.UnionOf.class, OWL2.unionOf, classes);
    }

    @Override
    public OntCE.IntersectionOf createIntersectionOf(Stream<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.IntersectionOf.class, OWL2.intersectionOf, classes);
    }

    @Override
    public OntCE.OneOf createOneOf(Stream<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.OneOf.class, OWL2.oneOf, classes);
    }

    @Override
    public OntCE.HasSelf createHasSelf(OntOPE onProperty) {
        return OntCEImpl.createHasSelf(this, onProperty);
    }

    @Override
    public OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Stream<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntException.Unsupported(OntCE.NaryDataAllValuesFrom.class);
    }

    @Override
    public OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Stream<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntException.Unsupported(OntCE.NaryDataSomeValuesFrom.class);
    }

}
