package ru.avicomp.ontapi.jena.impl;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

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
        super(graph instanceof UnionGraph ? graph : new UnionGraph(graph));
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
        return Arrays.stream(OntEntityImpl.Type.values()).map(OntEntityImpl.Type::getView).map(this::ontEntities).flatMap(Function.identity());
    }

    public <T extends OntEntity> Stream<T> ontEntities(Class<T> type) {
        return byTypes(OntEntityImpl.getRDFType(type)).filter(GraphModelImpl::isURI).map(Statement::getSubject).map(r -> OntEntityImpl.wrapEntity(type, r));
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

    protected ExtendedIterator<Statement> findByType(Resource type) {
        return listStatements(null, RDF.type, type);
    }

    protected Stream<Statement> byType(Resource type) {
        return asStream(findByType(type));
    }

    protected Stream<Statement> byTypes(Resource... types) {
        Stream<Statement> res = null;
        for (Resource t : types) {
            if (res == null) {
                res = byType(t);
            } else {
                res = Stream.concat(res, byType(t));
            }
        }
        return res;
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return asStream(iterator, true, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean distinct, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        Stream<T> res = StreamSupport.stream(iterable.spliterator(), parallel);
        return distinct ? res.distinct() : res;
    }

    public Stream<RDFNode> asStream(RDFList list) {
        return list.asJavaList().stream().map(this::wrapRDFNode).distinct();
    }

    private static boolean isURI(Statement statement) {
        return statement.getSubject().isURIResource();
    }

    Stream<OntCE> classExpressions(Resource resource, Property predicate) {
        return asStream(listObjectsOfProperty(resource, predicate).mapWith(node -> wrapCE(node.asResource())));
    }

    public <T extends OntCE> T createClass(String uri, Class<T> view) {
        Resource resource = createResource(uri);
        if (view.isAssignableFrom(OntClassEntity.class)) {
        }
        // todo:
        return null;
    }

    public boolean isCE(RDFNode node) {
        return node.isResource() && (contains(node.asResource(), RDF.type, OWL.Class) || contains(node.asResource(), RDF.type, OWL.Restriction));
    }

    public RDFNode wrapRDFNode(RDFNode node) {
        if (node.isLiteral()) return node;
        return toOntObject(node);
    }

    public OntObject toOntObject(RDFNode node) {
        if (isCE(node)) {
            return wrapCE(node.asResource());
        }
        throw new OntException("Unsupported resource " + node);
    }

    public OntCE wrapCE(Resource resource) {
        if (resource.isURIResource()) {
            return new OntClassImpl(resource);
        }
        if (contains(resource, OWL.unionOf)) {
            return new OntCEImpl.UnionOfImpl(resource);
        }
        if (contains(resource, OWL.intersectionOf)) {
            return new OntCEImpl.IntersectionOfImpl(resource);
        }
        if (contains(resource, OWL.oneOf)) {
            return new OntCEImpl.OneOfImpl(resource);
        }
        throw new OntException("Unsupported class expression " + resource);
    }

    public OntDR wrapDR(Resource resource) {
        throw new OntException("Unsupported data-range expression " + resource);
    }


}
