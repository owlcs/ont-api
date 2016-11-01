package ru.avicomp.ontapi.jena;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
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

    public Stream<OntEntity> listEntities() {
        return Arrays.stream(EntityType.values()).map(this::listEntities).flatMap(Function.identity());
    }

    private Stream<EntityImpl> listEntities(EntityType type) {
        return byTypes(type.getType()).filter(GraphModelImpl::isURI).map(Statement::getSubject).distinct().map(r -> newOntEntity(type, r));
    }

    public Stream<OntClassEntity> listClasses() {
        return listEntities(EntityType.CLASS).map(OntClassEntity.class::cast);
    }

    public Stream<OntAPEntity> listAnnotationProperties() {
        return listEntities(EntityType.ANNOTATION_PROPERTY).map(OntAPEntity.class::cast);
    }

    public Stream<DPropertyImpl> listDataProperties() {
        return listEntities(EntityType.DATA_PROPERTY).map(DPropertyImpl.class::cast);
    }

    public Stream<OPropertyImpl> listObjectProperties() {
        return listEntities(EntityType.OBJECT_PROPERTY).map(OPropertyImpl.class::cast);
    }

    public Stream<OntDatatypeEntity> listDatatypes() {
        return listEntities(EntityType.DATATYPE).map(OntDatatypeEntity.class::cast);
    }

    public Stream<OntIndividualEntity> listIndividuals() {
        return listEntities(EntityType.INDIVIDUAL).map(OntIndividualEntity.class::cast);
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

    private enum EntityType {
        CLASS(ClassImpl.class, OWL.Class),
        ANNOTATION_PROPERTY(APropertyImpl.class, OWL.AnnotationProperty),
        DATA_PROPERTY(DPropertyImpl.class, OWL.DatatypeProperty),
        OBJECT_PROPERTY(OPropertyImpl.class, OWL.ObjectProperty),
        DATATYPE(DatatypeImpl.class, RDFS.Datatype),
        INDIVIDUAL(IndividualImpl.class, OWL2.NamedIndividual);

        private Class<? extends EntityImpl> getView() {
            return view;
        }

        private Class<? extends EntityImpl> view;
        private Resource type;

        EntityType(Class<? extends EntityImpl> view, Resource type) {
            this.type = type;
            this.view = view;
        }

        public Resource getType() {
            return type;
        }
    }

    private EntityImpl newOntEntity(EntityType type, Resource s) {
        try {
            return type.getView().getDeclaredConstructor(GraphModelImpl.class, Resource.class).newInstance(this, s);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new OntException(e);
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
    }

    private static Resource checkEntityResource(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }

    abstract class EntityImpl extends ObjectResourceImpl implements OntEntity {
        private final EntityType type;

        EntityImpl(EntityType type, Resource r) {
            super(checkEntityResource(r));
            this.type = OntException.notNull(type, "Null type");
        }

        protected Set<Resource> getTypes() {
            return listStatements(this, RDF.type, (RDFNode) null).mapWith(Statement::getObject).filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast).toSet();
        }

        EntityType getEntityType() {
            return type;
        }

        @Override
        public boolean isLocal() {
            return isInBaseModel(this, RDF.type, getEntityType().getType());
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", getURI(), type);
        }
    }

    private Stream<OntCE> classExpressions(Resource resource, Property predicate) {
        return ModelHelper.asStream(listObjectsOfProperty(resource, predicate).mapWith(node -> ModelHelper.toCE(GraphModelImpl.this, node.asResource())));
    }

    class ClassImpl extends EntityImpl implements OntClassEntity {
        ClassImpl(Resource r) {
            super(EntityType.CLASS, r);
        }

        @Override
        public Stream<OntCE> subClassOf() {
            return GraphModelImpl.this.classExpressions(this, RDFS.subClassOf);
        }
    }

    private class APropertyImpl extends EntityImpl implements OntAPEntity {
        APropertyImpl(Resource r) {
            super(EntityType.ANNOTATION_PROPERTY, r);
        }

        @Override
        public Stream<Resource> getDomain() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.domain).mapWith(RDFNode::asResource));
        }

        @Override
        public Stream<Resource> getRange() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.range).mapWith(RDFNode::asResource));
        }
    }

    private class DPropertyImpl extends EntityImpl implements OntDPEntity {
        DPropertyImpl(Resource r) {
            super(EntityType.DATA_PROPERTY, r);
        }

        @Override
        public Stream<OntCE> getDomain() {
            return GraphModelImpl.this.classExpressions(this, RDFS.domain);
        }

        @Override
        public Stream<OntDR> getRange() {
            return ModelHelper.asStream(listObjectsOfProperty(this, RDFS.range).mapWith(node -> ModelHelper.toDR(GraphModelImpl.this, node.asResource())));
        }
    }

    private class OPropertyImpl extends EntityImpl implements OntOPEntity {
        OPropertyImpl(Resource r) {
            super(EntityType.OBJECT_PROPERTY, r);
        }

        @Override
        public Stream<OntCE> getDomain() {
            return GraphModelImpl.this.classExpressions(this, RDFS.domain);
        }

        @Override
        public Stream<OntCE> getRange() {
            return GraphModelImpl.this.classExpressions(this, RDFS.range);
        }
    }

    private class DatatypeImpl extends EntityImpl implements OntDatatypeEntity {
        DatatypeImpl(Resource r) {
            super(EntityType.DATATYPE, r);
        }
    }

    private class IndividualImpl extends EntityImpl implements OntIndividualEntity {
        IndividualImpl(Resource r) {
            super(EntityType.INDIVIDUAL, r);
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

    public class CollectionOfCEImpl extends CEImpl {

        public CollectionOfCEImpl(Resource r, Property predicate) {
            super(r, predicate);
        }

        public Stream<OntCE> list() {
            // ignore anything but Class Expressions
            return ModelHelper.asStream(listObjectsOfProperty(this, predicate)
                    .mapWith(n -> n.as(RDFList.class))
                    .mapWith(list -> ModelHelper.asStream(getModel(), list).filter(o -> ModelHelper.isCE(getModel(), o)).map(OntCE.class::cast)))
                    .flatMap(Function.identity()).distinct();
        }
    }
}
