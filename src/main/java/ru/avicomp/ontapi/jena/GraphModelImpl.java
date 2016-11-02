package ru.avicomp.ontapi.jena;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Base model to work through jena only.
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
        return Arrays.stream(OntEntity.Type.values()).map(this::ontEntities).flatMap(Function.identity());
    }

    public Stream<EntityImpl> ontEntities(OntEntity.Type type) {
        return byTypes(extractType(type)).filter(GraphModelImpl::isURI).map(Statement::getSubject).map(r -> wrapEntity(type, r));
    }

    public Stream<OntClassEntity> listClasses() {
        return ontEntities(OntEntity.Type.CLASS).map(OntClassEntity.class::cast);
    }

    public Stream<OntAPEntity> listAnnotationProperties() {
        return ontEntities(OntEntity.Type.ANNOTATION_PROPERTY).map(OntAPEntity.class::cast);
    }

    public Stream<DPropertyImpl> listDataProperties() {
        return ontEntities(OntEntity.Type.DATA_PROPERTY).map(DPropertyImpl.class::cast);
    }

    public Stream<OPropertyImpl> listObjectProperties() {
        return ontEntities(OntEntity.Type.OBJECT_PROPERTY).map(OPropertyImpl.class::cast);
    }

    public Stream<OntDatatypeEntity> listDatatypes() {
        return ontEntities(OntEntity.Type.DATATYPE).map(OntDatatypeEntity.class::cast);
    }

    public Stream<OntIndividualEntity> listNamedIndividuals() {
        return ontEntities(OntEntity.Type.INDIVIDUAL).map(OntIndividualEntity.class::cast);
    }

    protected ExtendedIterator<Statement> findByType(Resource type) {
        return listStatements(null, RDF.type, type);
    }

    protected Stream<Statement> byType(Resource type) {
        return ModelHelper.asStream(findByType(type));
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

    private static boolean isURI(Statement statement) {
        return statement.getSubject().isURIResource();
    }

    private static Resource checkEntityResource(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }

    private Stream<OntCE> classExpressions(Resource resource, Property predicate) {
        return ModelHelper.asStream(listObjectsOfProperty(resource, predicate).mapWith(node -> ModelHelper.toCE(GraphModelImpl.this, node.asResource())));
    }

    private static Resource extractType(OntEntity.Type type) {
        switch (type) {
            case CLASS:
                return OWL.Class;
            case ANNOTATION_PROPERTY:
                return OWL.AnnotationProperty;
            case DATA_PROPERTY:
                return OWL.DatatypeProperty;
            case OBJECT_PROPERTY:
                return OWL.ObjectProperty;
            case DATATYPE:
                return RDFS.Datatype;
            case INDIVIDUAL:
                return OWL2.NamedIndividual;
            default:
                throw new OntException("Unsupported entity type " + type);
        }
    }

    private EntityImpl wrapEntity(OntEntity.Type type, Resource r) {
        switch (type) {
            case CLASS:
                return new ClassImpl(r);
            case ANNOTATION_PROPERTY:
                return new APropertyImpl(r);
            case DATA_PROPERTY:
                return new DPropertyImpl(r);
            case OBJECT_PROPERTY:
                return new OPropertyImpl(r);
            case DATATYPE:
                return new DatatypeImpl(r);
            case INDIVIDUAL:
                return new NamedIndividualImpl(r);
            default:
                throw new OntException("Unsupported entity type " + type);
        }
    }

    abstract class ObjectResourceImpl extends ResourceImpl implements OntObject {

        ObjectResourceImpl(Resource resource) {
            super(resource.asNode(), GraphModelImpl.this);
        }

        @Override
        public GraphModelImpl getModel() {
            return GraphModelImpl.this;
        }

        @Override
        public Stream<Resource> types() {
            return ModelHelper.asStream(listStatements(this, RDF.type, (RDFNode) null).mapWith(Statement::getObject).filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast));
        }
    }

    abstract class EntityImpl extends ObjectResourceImpl implements OntEntity {
        EntityImpl(Resource r) {
            super(checkEntityResource(r));
        }

        @Override
        public boolean isLocal() {
            return isInBaseModel(this, RDF.type, extractType(getOntType()));
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", getURI(), getOntType());
        }
    }

    class ClassImpl extends EntityImpl implements OntClassEntity {
        ClassImpl(Resource r) {
            super(r);
        }

        @Override
        public Stream<OntCE> subClassOf() {
            return GraphModelImpl.this.classExpressions(this, RDFS.subClassOf);
        }
    }

    private class APropertyImpl extends EntityImpl implements OntAPEntity {
        APropertyImpl(Resource r) {
            super(r);
        }

        @Override
        public Stream<Resource> domain() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.domain).mapWith(RDFNode::asResource));
        }

        @Override
        public Stream<Resource> range() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.range).mapWith(RDFNode::asResource));
        }
    }

    private class DPropertyImpl extends EntityImpl implements OntDPEntity {
        DPropertyImpl(Resource r) {
            super(r);
        }

        @Override
        public Stream<OntCE> domain() {
            return GraphModelImpl.this.classExpressions(this, RDFS.domain);
        }

        @Override
        public Stream<OntDR> range() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.range).mapWith(node -> ModelHelper.toDR(GraphModelImpl.this, node.asResource())));
        }
    }

    private class OPropertyImpl extends EntityImpl implements OntOPEntity {
        OPropertyImpl(Resource r) {
            super(r);
        }

        @Override
        public Stream<OntCE> domain() {
            return GraphModelImpl.this.classExpressions(this, RDFS.domain);
        }

        @Override
        public Stream<OntCE> range() {
            return GraphModelImpl.this.classExpressions(this, RDFS.range);
        }
    }

    private class DatatypeImpl extends EntityImpl implements OntDatatypeEntity {
        DatatypeImpl(Resource r) {
            super(r);
        }
    }

    private class NamedIndividualImpl extends EntityImpl implements OntIndividualEntity {
        NamedIndividualImpl(Resource r) {
            super(r);
        }
    }

    class UnionOf extends CollectionOfCEImpl implements OntCE.UnionOf {
        UnionOf(Resource r) {
            super(r, OWL.unionOf);
        }
    }

    class IntersectionOf extends CollectionOfCEImpl implements OntCE.IntersectionOf {
        IntersectionOf(Resource r) {
            super(r, OWL.intersectionOf);
        }
    }

    class OneOf extends CollectionOfCEImpl implements OntCE.OneOf {
        OneOf(Resource r) {
            super(r, OWL.oneOf);
        }
    }

    abstract class CEImpl extends ObjectResourceImpl implements OntCE {
        final Property predicate;

        CEImpl(Resource resource, Property predicate) {
            super(resource);
            this.predicate = predicate;
        }

        @Override
        public Stream<OntCE> subClassOf() {
            return GraphModelImpl.this.classExpressions(this, RDFS.subClassOf);
        }
    }

    abstract class CollectionOfCEImpl extends CEImpl implements OntCE.Components<OntCE> {

        CollectionOfCEImpl(Resource r, Property predicate) {
            super(r, predicate);
        }

        @Override
        public Stream<OntCE> components() {
            // ignore anything but Class Expressions
            return ModelHelper.asStream(listObjectsOfProperty(this, predicate)
                    .mapWith(n -> n.as(RDFList.class))
                    .mapWith(list -> ModelHelper.asStream(getModel(), list).filter(o -> ModelHelper.isCE(getModel(), o)).map(OntCE.class::cast)))
                    .flatMap(Function.identity()).distinct();
        }
    }
}
