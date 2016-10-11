package ru.avicomp.ontapi;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * utils for converting owl-api iri to jena node
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class NodeIRIUtils {

    public static Node toNode() {
        return NodeFactory.createBlankNode();
    }

    public static Node toNode(IRI iri) {
        return NodeFactory.createURI(OntException.notNull(iri, "Null iri specified.").getIRIString());
    }

    public static Node toNode(HasIRI hasIRI) {
        return toNode(hasIRI.getIRI());
    }

    public static Node toNode(OWLAnnotationValue value) {
        if (OntException.notNull(value, "Null value specified.").isIRI()) {
            return toNode((IRI) value);
        }
        if (OWLLiteral.class.isInstance(value)) {
            return toLiteralNode((OWLLiteral) value);
        }
        throw new OntException("Unsupported object type: " + value);
    }

    public static Triple toTriple(IRI s, IRI p, IRI o) {
        return Triple.create(toNode(s), toNode(p), toNode(o));
    }

    public static Triple toTriple(OWLAnnotationValue s, IRI p, OWLAnnotationValue o) {
        Node subject;
        if (s.isIRI()) {
            subject = toNode((IRI) s);
        } else {
            throw new OntException("Unsupported subject type: " + s);
        }
        Node object;
        if (o.isIRI()) {
            object = toNode((IRI) o);
        } else if (OWLLiteral.class.isInstance(o)) {
            object = toLiteralNode((OWLLiteral) o);
        } else {
            throw new OntException("Unsupported object type: " + s);
        }
        Node predicate = toNode(p);
        return Triple.create(subject, predicate, object);
    }

    public static IRI fromResource(Resource resource) {
        return IRI.create(resource.getURI());
    }

    public static Node toLiteralNode(OWLLiteral owlLiteral) {
        return toLiteralNode(owlLiteral.getLiteral(), owlLiteral.getLang(), owlLiteral.getDatatype().getIRI());
    }

    public static Node toLiteralNode(String value, String lang, String datatypeURI) {
        RDFDatatype type = TypeMapper.getInstance().getTypeByName(datatypeURI);
        return NodeFactory.createLiteral(value, lang, type);
    }

    public static Node toLiteralNode(String value, String lang, IRI dataTypeIRI) {
        return toLiteralNode(value, lang, dataTypeIRI.getIRIString());
    }
}
