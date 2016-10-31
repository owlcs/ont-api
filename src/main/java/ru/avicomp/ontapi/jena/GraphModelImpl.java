package ru.avicomp.ontapi.jena;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;

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

    public Stream<OntEntity> listEntities(EntityType type) {
        return byTypes(type.getType()).filter(GraphModelImpl::isURI).map(Statement::getSubject).filter(new UniqueFilter<>()).map(r -> newInstance(type, r));
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
        return asStream(iterator, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public static boolean isURI(Statement statement) {
        return statement.getSubject().isURIResource();
    }

    public enum EntityType {
        CLASS(ClassEntity.class, OWL.Class),
        ANNOTATION_PROPERTY(AnnotationPropertyEntity.class, OWL.AnnotationProperty),
        DATA_PROPERTY(DataPropertyEntity.class, OWL.DatatypeProperty),
        OBJECT_PROPERTY(ObjectPropertyEntity.class, OWL.ObjectProperty),
        DATATYPE(DatatypeEntity.class, RDFS.Datatype),
        INDIVIDUAL(IndividualEntity.class, OWL2.NamedIndividual);

        private Class<? extends OntEntity> getView() {
            return view;
        }

        private Class<? extends OntEntity> view;
        private Resource type;

        EntityType(Class<? extends OntEntity> view, Resource type) {
            this.type = type;
            this.view = view;
        }

        public Resource getType() {
            return type;
        }
    }

    private OntEntity newInstance(EntityType type, Resource s) {
        try {
            return type.getView().getDeclaredConstructor(GraphModelImpl.class, Resource.class).newInstance(this, s);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new OntException(e);
        }
    }

    public abstract class OntEntity extends ResourceImpl {
        private final EntityType type;

        OntEntity(EntityType type, Resource r) {
            super(r.asNode(), GraphModelImpl.this);
            this.type = OntException.notNull(type, "Null type");
        }

        public Resource getType() {
            return type.getType();
        }

        public boolean isLocal() {
            return isInBaseModel(this, RDF.type, getType());
        }

        public boolean isClass() {
            return EntityType.CLASS.equals(type);
        }

        public boolean isProperty() {
            return false;
        }

        public boolean isAnnotationProperty() {
            return false;
        }

        public boolean isDataProperty() {
            return false;
        }

        public boolean isObjectProperty() {
            return false;
        }

        public boolean isDatatype() {
            return EntityType.DATATYPE.equals(type);
        }

        public boolean isIndividual() {
            return EntityType.INDIVIDUAL.equals(type);
        }
    }

    public abstract class PropertyEntity extends OntEntity {
        PropertyEntity(EntityType type, Resource r) {
            super(type, r);
        }

        @Override
        public boolean isProperty() {
            return true;
        }

        private Set<Resource> getTypes() {
            return listStatements(this, RDF.type, (RDFNode) null).mapWith(Statement::getObject).filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast).toSet();
        }

        @Override
        public boolean isAnnotationProperty() {
            return getTypes().contains(EntityType.ANNOTATION_PROPERTY.getType());
        }

        @Override
        public boolean isDataProperty() {
            return getTypes().contains(EntityType.DATA_PROPERTY.getType());
        }

        @Override
        public boolean isObjectProperty() {
            return getTypes().contains(EntityType.OBJECT_PROPERTY.getType());
        }

        public AnnotationPropertyEntity asAnnotationProperty() {
            if (isAnnotationProperty()) {
                return new AnnotationPropertyEntity(this);
            }
            throw new OntException("Not annotation property.");
        }

        public DataPropertyEntity asDataProperty() {
            if (isDataProperty()) {
                return new DataPropertyEntity(this);
            }
            throw new OntException("Not data property.");
        }

        public ObjectPropertyEntity asObjectProperty() {
            if (isObjectProperty()) {
                return new ObjectPropertyEntity(this);
            }
            throw new OntException("Not object property.");
        }
    }

    public class ClassEntity extends OntEntity {
        ClassEntity(Resource r) {
            super(EntityType.CLASS, r);
        }
    }

    public class AnnotationPropertyEntity extends PropertyEntity {
        AnnotationPropertyEntity(Resource r) {
            super(EntityType.ANNOTATION_PROPERTY, r);
        }
    }

    public class DataPropertyEntity extends PropertyEntity {
        DataPropertyEntity(Resource r) {
            super(EntityType.DATA_PROPERTY, r);
        }
    }

    public class ObjectPropertyEntity extends PropertyEntity {
        ObjectPropertyEntity(Resource r) {
            super(EntityType.OBJECT_PROPERTY, r);
        }
    }

    public class DatatypeEntity extends OntEntity {
        DatatypeEntity(Resource r) {
            super(EntityType.DATATYPE, r);
        }
    }

    public class IndividualEntity extends OntEntity {
        IndividualEntity(Resource r) {
            super(EntityType.INDIVIDUAL, r);
        }
    }
}
