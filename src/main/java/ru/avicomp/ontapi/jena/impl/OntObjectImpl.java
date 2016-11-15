package ru.avicomp.ontapi.jena.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {
    static final Node RDF_TYPE = RDF.type.asNode();

    public static OntObjectFactory objectFactory = new OntObjectFactory() {
        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return OntFinder.ANYTHING.find(eg).filter(n -> canWrap(n, eg)).map(n -> wrap(n, eg));
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

    public OntObjectImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public Stream<Resource> types() {
        return JenaUtils.asStream(getModel().listObjectsOfProperty(this, RDF.type)
                .filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast)).distinct();
    }

    boolean hasType(Resource type) {
        return types().filter(type::equals).findAny().isPresent();
    }

    OntStatement addType(Resource type) {
        return addStatement(RDF.type, OntException.notNull(type, "Null rdf:type"));
    }

    void removeType(Resource type) {
        getModel().remove(this, RDF.type, type);
    }

    void changeType(Resource property, boolean add) {
        if (add) {
            addType(property);
        } else {
            removeType(property);
        }
    }

    public <T extends RDFNode> T getOntProperty(Property predicate, Class<T> view) {
        Statement st = getProperty(predicate);
        return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
    }

    public <T extends RDFNode> T getRequiredOntProperty(Property predicate, Class<T> view) {
        return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
    }

    @Override
    public OntStatement getRoot() {
        List<Resource> types = types().collect(Collectors.toList());
        if (types.isEmpty()) throw new OntException("Can't determine main triple: no types.");
        return new OntStatementImpl.RootImpl(this, RDF.type, types.get(0), getModel());
    }

    @Override
    public OntStatement getStatement(Property property, OntObject object) {
        return statements(property).filter(s -> s.getObject().equals(object)).findFirst().orElse(null);
    }

    @Override
    public OntStatement addStatement(Property property, RDFNode value) {
        OntStatement res = toOntStatement(getRoot(), getModel().createStatement(this, OntException.notNull(property, "Null property."), OntException.notNull(value, "Null value.")));
        getModel().add(res);
        return res;
    }

    @Override
    public void remove(Property property, RDFNode value) {
        getModel().removeAll(this, OntException.notNull(property, "Null property."), OntException.notNull(value, "Null value."));
    }

    @Override
    public Stream<OntStatement> statements(Property property) {
        return statements().filter(s -> s.getPredicate().equals(property));
    }

    @Override
    public Stream<OntStatement> statements() {
        OntStatement main = getRoot();
        return JenaUtils.asStream(listProperties()).map(s -> toOntStatement(main, s));
    }

    private OntStatement toOntStatement(OntStatement main, Statement st) {
        if (st.equals(main)) return main;
        if (st.getPredicate().canAs(OntNAP.class)) {
            // if subject is anon -> general annotation wrapper.
            return main.getSubject().isURIResource() ? new OntStatementImpl.AssertionAnnotationImpl(main, st.getPredicate().as(OntNAP.class), st.getObject()) :
                    new OntStatementImpl.CommonAnnotationImpl(main.getSubject(), st.getPredicate().as(OntNAP.class), st.getObject(), main.getModel());
        }
        return new OntStatementImpl(st.getSubject(), st.getPredicate(), st.getObject(), getModel());
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", asNode(), getActualClass().getSimpleName());
    }

    static Node checkNamed(Node res) {
        if (OntException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntException("Not uri node " + res);
    }

    static Resource checkNamed(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }
}
