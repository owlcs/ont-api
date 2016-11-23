package ru.avicomp.ontapi;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.*;

/**
 * Utils for working with owl-api {@link IRI} and jena {@link Node}
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class NodeIRIUtils {

    public static IRI toIRI(OWLObject object) {
        if (OntApiException.notNull(object, "Null owl-object specified.").isIRI()) return (IRI) object;
        if (HasIRI.class.isInstance(object)) {
            return ((HasIRI) object).getIRI();
        }
        if (OWLAnnotationObject.class.isInstance(object)) {
            return ((OWLAnnotationObject) object).asIRI().orElseThrow(() -> new OntApiException("Not iri: " + object));
        }
        if (OWLClassExpression.class.isInstance(object)) {
            return toIRI((OWLClassExpression) object);
        }
        if (OWLPropertyExpression.class.isInstance(object)) {
            return toIRI((OWLPropertyExpression) object);
        }
        throw new OntApiException("Unsupported owl-object: " + object);
    }

    private static IRI toIRI(OWLClassExpression expression) {
        HasIRI res = null;
        if (ClassExpressionType.OWL_CLASS.equals(expression.getClassExpressionType())) {
            res = (OWLClass) expression;
        }
        return OntApiException.notNull(res, "Unsupported class-expression: " + expression).getIRI();
    }

    private static IRI toIRI(OWLPropertyExpression expression) {
        if (expression.isOWLDataProperty())
            return expression.asOWLDataProperty().getIRI();
        if (expression.isOWLObjectProperty())
            return expression.asOWLObjectProperty().getIRI();
        if (expression.isOWLAnnotationProperty()) {
            return expression.asOWLAnnotationProperty().getIRI();
        }
        throw new OntApiException("Unsupported property-expression: " + expression);
    }

    public static Node toNode() {
        return NodeFactory.createBlankNode();
    }

    public static Node toNode(OWLObject object) {
        if (OWLLiteral.class.isInstance(object)) {
            return toLiteralNode((OWLLiteral) object);
        }
        if (OWLAnonymousIndividual.class.isInstance(object)) {
            NodeID id = ((OWLAnonymousIndividual) object).getID();
            return NodeFactory.createBlankNode(id.getID());
        }
        return toNode(toIRI(object));
    }

    private static Node toNode(IRI iri) {
        return NodeFactory.createURI(OntApiException.notNull(iri, "Null IRI specified.").getIRIString());
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

    public static Triple toTriple(OWLObject s, IRI p, OWLObject o) {
        return Triple.create(toNode(s), toNode(p), toNode(o));
    }

}
