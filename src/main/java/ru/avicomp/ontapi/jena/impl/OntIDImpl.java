package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntID;

/**
 * Ontology ID
 * Created by szuev on 09.11.2016.
 */
public class OntIDImpl extends OntObjectImpl implements OntID {
    public static OntObjectFactory idFactory = new OntObjectFactory() {
        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            if (canWrap(node, eg)) {
                return new OntIDImpl(node, eg);
            }
            throw new OntApiException("Not an ontology node " + node);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF_TYPE, OWL2.Ontology.asNode());
        }
    };

    public OntIDImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public String getVersionIRI() {
        Statement st = getProperty(OWL2.versionIRI);
        if (st == null || !st.getObject().isURIResource()) return null;
        return st.getObject().asResource().getURI();
    }

    @Override
    public void setVersionIRI(String uri) {
        removeAll(OWL2.versionIRI);
        if (uri != null) {
            addProperty(OWL2.versionIRI, getModel().createResource(uri));
        }
    }
}
