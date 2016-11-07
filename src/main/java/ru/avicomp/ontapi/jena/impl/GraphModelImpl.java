package ru.avicomp.ontapi.jena.impl;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Base model to work through jena only.
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work in accordance with OWL2 DL specification.
 * <p>
 * Created by @szuev on 27.10.2016.
 */
public class GraphModelImpl extends ModelCom {

    public GraphModelImpl(Graph graph) {
        super(graph instanceof UnionGraph ? graph : new UnionGraph(graph), OntConfiguration.getPersonality());
    }

    public void addImport(GraphModelImpl m) {
        getGraph().addGraph(m.getBaseGraph());
        // todo:
    }

    public void removeImport(GraphModelImpl m) {
        getGraph().removeGraph(m.getBaseGraph());
        // todo:
    }

    @Override
    public UnionGraph getGraph() {
        return (UnionGraph) super.getGraph();
    }

    public Graph getBaseGraph() {
        return getGraph().getBaseGraph();
    }

    public Model getBaseModel() {
        return ModelFactory.createModelForGraph(getBaseGraph());
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

    public Stream<OntEntity> ontEntities() {
        return ontObjects(OntEntity.class);
    }

    /**
     * to retrieve the stream of OntObjects
     *
     * @param type Class
     * @return Stream
     */
    protected <T extends OntObject> Stream<T> ontObjects(Class<T> type) {
        return getFactory(type).find(this).map(e -> getNodeAs(e.asNode(), type));
    }

    /**
     * to create any OntObject resource
     *
     * @param type Class
     * @param uri  String
     * @return OntObject
     */
    protected <T extends OntObject> T createOntObject(Class<T> type, String uri) {
        Resource res = uri == null ? createResource() : createResource(uri);
        return getFactory(type).create(res.asNode(), this).as(type);
    }

    public <T extends OntEntity> Stream<T> ontEntities(Class<T> type) {
        return ontObjects(type);
    }

    public Stream<OntClassEntity> listClasses() {
        return ontEntities(OntClassEntity.class).map(OntClassEntity.class::cast);
    }

    public Stream<OntAPEntity> listAnnotationProperties() {
        return ontEntities(OntAPEntity.class).map(OntAPEntity.class::cast);
    }

    public Stream<OntDPropertyImpl> listDataProperties() {
        return ontEntities(OntDPEntity.class).map(OntDPropertyImpl.class::cast);
    }

    public Stream<OntOPropertyImpl> listObjectProperties() {
        return ontEntities(OntOPEntity.class).map(OntOPropertyImpl.class::cast);
    }

    public Stream<OntDatatypeEntity> listDatatypes() {
        return ontEntities(OntDatatypeEntity.class).map(OntDatatypeEntity.class::cast);
    }

    public Stream<OntIndividualEntity> listNamedIndividuals() {
        return ontEntities(OntIndividualEntity.class).map(OntIndividualEntity.class::cast);
    }

    protected OntConfiguration.OntObjectFactory getFactory(Class<? extends OntObject> view) {
        return (OntConfiguration.OntObjectFactory) OntException.notNull(getPersonality().getImplementation(view), "Can't find factory for object " + view);
    }


    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return asStream(iterator, true, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean distinct, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        Stream<T> res = StreamSupport.stream(iterable.spliterator(), parallel);
        return distinct ? res.distinct() : res;
    }

    Stream<OntCE> classExpressions(Resource resource, Property predicate) {
        return asStream(listObjectsOfProperty(resource, predicate)).map(node -> getNodeAs(node.asNode(), OntCE.class)).distinct();
    }

    Stream<OntDR> dataRanges(Resource resource, Property predicate) {
        return GraphModelImpl.asStream(listObjectsOfProperty(resource, predicate)).map(node -> getNodeAs(node.asNode(), OntDR.class)).distinct();
    }

}
