package ru.avicomp.ontapi.parsers;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

import static ru.avicomp.ontapi.NodeIRIUtils.toLiteralNode;

/**
 * utils for axiom parsers
 *
 * Created by @szuev on 28.09.2016.
 */
public class ParseUtils {

    public static IRI toIRI(OWLClassExpression expression) {
        HasIRI res = null;
        if (ClassExpressionType.OWL_CLASS.equals(expression.getClassExpressionType())) {
            res = (OWLClass) expression;
        }
        return OntException.notNull(res, "Can't parse " + expression).getIRI();
    }

    public static IRI toIRI(OWLPropertyExpression expression) {
        if (expression.isOWLDataProperty())
            return expression.asOWLDataProperty().getIRI();
        if (expression.isOWLObjectProperty())
            return expression.asOWLObjectProperty().getIRI();
        throw new OntException("Can't parse " + expression);
    }

    private static Resource addObjectRestriction(Model model, OWLObjectRestriction restriction, OWLClassExpressionRDFType type) {
        RDFNode object;
        if (OWLClassExpressionRDFType.HAS_SELF.equals(type)) {
            Node literal = toLiteralNode(String.valueOf(Boolean.TRUE), null, XSDVocabulary.BOOLEAN.getIRI());
            object = model.getRDFNode(literal);
        } else if (HasFiller.class.isInstance(restriction)) {
            OWLObject filter = ((HasFiller) restriction).getFiller();
            object = toResource(model, filter);
        } else {
            throw new OntException("Unsupported restriction " + restriction);
        }
        OWLObjectPropertyExpression property = restriction.getProperty();
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Restriction);
        model.add(res, OWL.onProperty, ResourceFactory.createResource(toIRI(property).getIRIString()));
        model.add(res, type.getPredicate(), object);
        return res;
    }


    private static Resource addObjectCardinalityRestriction(Model model, OWLObjectCardinalityRestriction restriction, OWLClassExpressionRDFType type) {
        OWLObjectPropertyExpression property = restriction.getProperty();
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Restriction);
        model.add(res, OWL.onProperty, ResourceFactory.createResource(toIRI(property).getIRIString()));
        Node literal = toLiteralNode(String.valueOf(restriction.getCardinality()), null, XSDVocabulary.NON_NEGATIVE_INTEGER.getIRI());
        RDFNode object = model.getRDFNode(literal);
        Property predicate = type.getPredicate();
        model.add(res, predicate, object);
        return res;
    }

    private static Resource addClassCollectionOf(Model model, HasOperands<? extends OWLObject> classExpression, OWLClassExpressionRDFType type) {
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Class);
        model.add(res, type.getPredicate(), model.createList(toResourceIterator(model, classExpression.operands())));
        return res;
    }

    private static Resource addObjectComplementOf(Model model, OWLObjectComplementOf classExpression, OWLClassExpressionRDFType type) {
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Class);
        model.add(res, type.getPredicate(), toResource(model, classExpression.getOperand()));
        return res;
    }

    public static Iterator<? extends RDFNode> toResourceIterator(Model model, Stream<? extends OWLObject> stream) {
        return stream.map(o -> toResource(model, o)).iterator();
    }

    public static Resource addClassExpression(Model model, OWLClassExpression expression) {
        ClassExpressionType expressionType = expression.getClassExpressionType();
        OWLClassExpressionRDFType type = OntException.notNull(OWLClassExpressionRDFType.valueOf(expressionType), "Unsupported type " + expressionType);
        OWLClassExpressionRDFType.MethodType methodType = type.getMethodType();
        if (OWLClassExpressionRDFType.MethodType.RESTRICTION_CARDINALITY.equals(methodType)) {
            return addObjectCardinalityRestriction(model, (OWLObjectCardinalityRestriction) expression, type);
        }
        if (OWLClassExpressionRDFType.MethodType.RESTRICTION.equals(methodType)) {
            return addObjectRestriction(model, (OWLObjectRestriction) expression, type);
        }
        if (OWLClassExpressionRDFType.MethodType.COLLECTION.equals(methodType)) {
            return addClassCollectionOf(model, (HasOperands<? extends OWLObject>) expression, type);
        }
        if (OWLClassExpressionRDFType.MethodType.SINGLETON.equals(methodType)) {
            return addObjectComplementOf(model, (OWLObjectComplementOf) expression, type);
        }
        return null;
    }

    public static Resource toResource(Model model, OWLObject o) {
        if (HasIRI.class.isInstance(o)) {
            return NodeIRIUtils.toResource(((HasIRI) o).getIRI());
        }
        if (OWLClassExpression.class.isInstance(o)) {
            return addClassExpression(model, (OWLClassExpression) o);
        }
        throw new OntException("Unsupported owl-object type: " + o);
    }

    public enum OWLClassExpressionRDFType {
        MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, OWL.maxCardinality, MethodType.RESTRICTION_CARDINALITY),
        MIN_CARDINALITY(ClassExpressionType.OBJECT_MIN_CARDINALITY, OWL.minCardinality, MethodType.RESTRICTION_CARDINALITY),
        EXACT_CARDINALITY(ClassExpressionType.OBJECT_EXACT_CARDINALITY, OWL.cardinality, MethodType.RESTRICTION_CARDINALITY),
        ALL_VALUES_FROM(ClassExpressionType.OBJECT_ALL_VALUES_FROM, OWL.allValuesFrom, MethodType.RESTRICTION),
        SOME_VALUES_FROM(ClassExpressionType.OBJECT_SOME_VALUES_FROM, OWL.someValuesFrom, MethodType.RESTRICTION),
        HAS_VALUE(ClassExpressionType.OBJECT_HAS_VALUE, OWL.hasValue, MethodType.RESTRICTION),
        HAS_SELF(ClassExpressionType.OBJECT_HAS_SELF, OWL2.hasSelf, MethodType.RESTRICTION),
        UNION_OF(ClassExpressionType.OBJECT_UNION_OF, OWL.unionOf, MethodType.COLLECTION),
        INTERSECTION_OF(ClassExpressionType.OBJECT_INTERSECTION_OF, OWL.intersectionOf, MethodType.COLLECTION),
        ONE_OF(ClassExpressionType.OBJECT_ONE_OF, OWL.oneOf, MethodType.COLLECTION),
        COMPLEMENT_OF(ClassExpressionType.OBJECT_COMPLEMENT_OF, OWL.complementOf, MethodType.SINGLETON),;
        private ClassExpressionType type;
        private Property predicate;
        private MethodType methodType;


        OWLClassExpressionRDFType(ClassExpressionType type, Property predicate, MethodType methodType) {
            this.type = type;
            this.predicate = predicate;
            this.methodType = methodType;
        }

        public static OWLClassExpressionRDFType valueOf(ClassExpressionType type) {
            for (OWLClassExpressionRDFType t : values()) {
                if (t.getType().equals(type)) return t;
            }
            return null;
        }

        public ClassExpressionType getType() {
            return type;
        }

        public Property getPredicate() {
            return predicate;
        }

        public MethodType getMethodType() {
            return methodType;
        }

        enum MethodType {
            RESTRICTION_CARDINALITY,
            RESTRICTION,
            COLLECTION,
            SINGLETON,
        }
    }

}
