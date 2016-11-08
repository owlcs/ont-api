package ru.avicomp.ontapi.jena.impl;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {
    static final Node RDF_TYPE = RDF.type.asNode();
    static final Node OWL_DATATYPE_PROPERTY = OWL2.DatatypeProperty.asNode();
    static final Node OWL_OBJECT_PROPERTY = OWL2.ObjectProperty.asNode();

    public static OntObjectFactory objectFactory = new OntObjectFactory() {
        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return JenaUtils.asStream(eg.asGraph().find(Node.ANY, Node.ANY, Node.ANY).
                    mapWith(Triple::getSubject).filterKeep(n -> canWrap(n, eg)).mapWith(n -> wrap(n, eg)));
        }

        @Override
        public EnhNode wrap(Node n, EnhGraph eg) {
            if (canWrap(n, eg)) {
                return new OntObjectImpl(n, eg);
            }
            throw new OntException("Cannot convert node " + n + " to OntObject");
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isURI() || node.isBlank();
        }
    };

    OntObjectImpl(Resource inModel) {
        this(inModel.asNode(), (GraphModelImpl) inModel.getModel());
    }

    public OntObjectImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<Resource> types() {
        return JenaUtils.asStream(getModel().listObjectsOfProperty(this, RDF.type)
                .filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast));
    }

    boolean hasType(Resource type) {
        return types().filter(type::equals).findAny().isPresent();
    }

    @Override
    public GraphModelImpl getModel() {
        return (GraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", asNode(), getActualClass().getSimpleName());
    }

    public <T extends OntObject> T getOntProperty(Property predicate, Class<T> view) {
        Statement st = getProperty(predicate);
        return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
    }

    public <T extends OntObject> T getRequiredOntProperty(Property predicate, Class<T> view) {
        return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
    }
}
