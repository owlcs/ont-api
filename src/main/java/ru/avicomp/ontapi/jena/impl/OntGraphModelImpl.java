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
    public <T extends OntEntity> Stream<T> ontEntities(Class<T> type) {
        return ontObjects(type);
    }

    @Override
    protected OntPersonality getPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    Stream<OntCE> classExpressions(Resource resource, Property predicate) {
        return JenaUtils.asStream(listObjectsOfProperty(resource, predicate)).map(node -> getNodeAs(node.asNode(), OntCE.class)).distinct();
    }

    Stream<OntDR> dataRanges(Resource resource, Property predicate) {
        return JenaUtils.asStream(listObjectsOfProperty(resource, predicate)).map(node -> getNodeAs(node.asNode(), OntDR.class)).distinct();
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

}
