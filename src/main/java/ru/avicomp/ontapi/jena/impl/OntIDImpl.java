package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Streams;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Ontology ID
 * Created by szuev on 09.11.2016.
 */
public class OntIDImpl extends OntObjectImpl implements OntID {
    public static Configurable<OntObjectFactory> idFactory = m -> new OntObjectFactory() {
        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            if (canWrap(node, eg)) {
                return new OntIDImpl(node, eg);
            }
            throw new OntJenaException("Not an ontology node " + node);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF.type.asNode(), OWL.Ontology.asNode());
        }
    };

    public OntIDImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public String getVersionIRI() {
        Statement st = getProperty(OWL.versionIRI);
        if (st == null || !st.getObject().isURIResource()) return null;
        return st.getObject().asResource().getURI();
    }

    @Override
    public void setVersionIRI(String uri) {
        removeAll(OWL.versionIRI);
        if (uri != null) {
            addProperty(OWL.versionIRI, getModel().createResource(uri));
        }
    }

    @Override
    public void addImport(String uri) {
        if (OntJenaException.notNull(uri, "Null uri specified.").equals(getURI())) {
            throw new OntJenaException("Can't import itself: " + uri);
        }
        addImportResource(getModel().createResource(uri));
    }

    @Override
    public void removeImport(String uri) {
        removeImportResource(getModel().createResource(uri));
    }

    @Override
    public Stream<String> imports() {
        return importResources().map(Resource::getURI);
    }

    public Stream<Resource> importResources() {
        return Streams.asStream(listProperties(OWL.imports)
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isURIResource)
                .mapWith(RDFNode::asResource));
    }

    public void addImportResource(Resource uri) {
        addProperty(OWL.imports, uri);
    }

    public void removeImportResource(Resource uri) {
        remove(OWL.imports, uri);
    }
}
