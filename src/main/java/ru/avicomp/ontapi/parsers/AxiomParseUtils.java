package ru.avicomp.ontapi.parsers;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

/**
 * utils for axiom parsing.
 * TODO:
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class AxiomParseUtils {

    public static Model createModel(Graph graph) {
        return ModelFactory.createModelForGraph(graph);
    }

    public static RDFNode toRDFNode(OWLObject object) {
        if (object instanceof OWLLiteral) {
            return toLiteral((OWLLiteral) object);
        }
        return toResource(object);
    }

    public static Resource toResource(OWLObject object) {
        if (OWLIndividual.class.isInstance(object)) {
            return toResource((OWLIndividual) object);
        }
        return toResource(NodeIRIUtils.toIRI(object));
    }

    private static Resource toResource(OWLIndividual individual) {
        return individual.isAnonymous() ? toResource(individual.asOWLAnonymousIndividual().getID()) : toResource(individual.asOWLNamedIndividual().getIRI());
    }

    private static Resource toResource(NodeID id) {
        return new ResourceImpl(NodeFactory.createBlankNode(id.getID()), null);
    }

    private static Resource toResource(IRI iri) {
        return ResourceFactory.createResource(OntException.notNull(iri, "Null iri").getIRIString());
    }

    public static Property toProperty(OWLObject object) {
        return toProperty(NodeIRIUtils.toIRI(object));
    }

    private static Property toProperty(IRI iri) {
        return ResourceFactory.createProperty(OntException.notNull(iri, "Null iri").getIRIString());
    }

    private static Literal toLiteral(OWLLiteral literal) {
        return new LiteralImpl(NodeIRIUtils.toLiteralNode(literal), null);
    }

    private static Iterator<? extends RDFNode> toResourceIterator(Model model, Stream<? extends OWLObject> stream) {
        return stream.map(o -> addRDFNode(model, o)).iterator();
    }

    public static Resource getType(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return OWL.Class;
        } else if (entity.isOWLDataProperty()) {
            return OWL.DatatypeProperty;
        } else if (entity.isOWLObjectProperty()) {
            return OWL.ObjectProperty;
        } else if (entity.isOWLNamedIndividual()) {
            return OWL2.NamedIndividual;
        } else if (entity.isOWLAnnotationProperty()) {
            return OWL.AnnotationProperty;
        } else if (entity.isOWLDatatype()) {
            return RDFS.Datatype;
        }
        throw new OntException("Unsupported " + entity);
    }

    public static void processAnnotatedTriple(Graph graph, OWLObject subject, OWLObject predicate, OWLObject object, OWLAxiom axiom) {
        processAnnotatedTriple(graph, subject, toProperty(predicate), object, axiom);
    }

    public static void processAnnotatedTriple(Graph graph, OWLObject subject, Property predicate, OWLObject object, OWLAxiom axiom) {
        Model model = createModel(graph);
        Resource _subject = toResource(subject);
        RDFNode _object = addRDFNode(model, object);
        model.add(_subject, predicate, _object);
        AnnotationsParseUtils.addAnnotations(model, _subject, predicate, _object, axiom);
    }

    public static RDFList addRDFList(Model model, Stream<? extends OWLObject> objects) {
        return model.createList(toResourceIterator(model, objects));
    }

    public static RDFNode addRDFNode(Model model, OWLObject o) {
        if (OWLEntity.class.isInstance(o)) {
            OWLEntity entity = (OWLEntity) o;
            Resource res = toResource(entity);
            if (!entity.isBuiltIn()) {
                model.add(res, RDF.type, getType((OWLEntity) o));
            }
            return res.inModel(model);
        }
        if (OWLClassExpression.class.isInstance(o)) {
            return CETranslator.addClassExpression(model, (OWLClassExpression) o);
        }
        return toRDFNode(o).inModel(model);
    }

    private enum CETranslator {
        OBJECT_MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, new RestrictionCardinality<OWLObjectMaxCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.maxCardinality;
            }
        }),
        DATA_MAX_CARDINALITY(ClassExpressionType.DATA_MAX_CARDINALITY, new RestrictionCardinality<OWLDataMaxCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.maxCardinality;
            }
        }),
        OBJECT_MIN_CARDINALITY(ClassExpressionType.OBJECT_MIN_CARDINALITY, new RestrictionCardinality<OWLObjectMinCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.minCardinality;
            }
        }),
        DATA_MIN_CARDINALITY(ClassExpressionType.DATA_MIN_CARDINALITY, new RestrictionCardinality<OWLDataMinCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.minCardinality;
            }
        }),
        OBJECT_EXACT_CARDINALITY(ClassExpressionType.OBJECT_EXACT_CARDINALITY, new RestrictionCardinality<OWLObjectExactCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.cardinality;
            }
        }),
        DATA_EXACT_CARDINALITY(ClassExpressionType.DATA_EXACT_CARDINALITY, new RestrictionCardinality<OWLDataExactCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.cardinality;
            }
        }),
        OBJECT_ALL_VALUES_FROM(ClassExpressionType.OBJECT_ALL_VALUES_FROM, new Restriction<OWLObjectAllValuesFrom>() {
            @Override
            Property getPredicate() {
                return OWL.allValuesFrom;
            }
        }),
        DATA_ALL_VALUES_FROM(ClassExpressionType.DATA_ALL_VALUES_FROM, new Restriction<OWLDataAllValuesFrom>() {
            @Override
            Property getPredicate() {
                return OWL.allValuesFrom;
            }
        }),
        OBJECT_SOME_VALUES_FROM(ClassExpressionType.OBJECT_SOME_VALUES_FROM, new Restriction<OWLObjectSomeValuesFrom>() {
            @Override
            Property getPredicate() {
                return OWL.someValuesFrom;
            }
        }),
        DATA_SOME_VALUES_FROM(ClassExpressionType.DATA_SOME_VALUES_FROM, new Restriction<OWLDataSomeValuesFrom>() {
            @Override
            Property getPredicate() {
                return OWL.someValuesFrom;
            }
        }),
        OBJECT_HAS_VALUE(ClassExpressionType.OBJECT_HAS_VALUE, new Restriction<OWLObjectHasValue>() {
            @Override
            Property getPredicate() {
                return OWL.hasValue;
            }
        }),
        DATA_HAS_VALUE(ClassExpressionType.DATA_HAS_VALUE, new Restriction<OWLDataHasValue>() {
            @Override
            Property getPredicate() {
                return OWL.hasValue;
            }
        }),
        HAS_SELF(ClassExpressionType.OBJECT_HAS_SELF, new Restriction<OWLObjectHasSelf>() {
            @Override
            Property getPredicate() {
                return OWL2.hasSelf;
            }
        }),
        UNION_OF(ClassExpressionType.OBJECT_UNION_OF, new CollectionOf<OWLObjectUnionOf>() {
            @Override
            Property getPredicate() {
                return OWL.unionOf;
            }
        }),
        INTERSECTION_OF(ClassExpressionType.OBJECT_INTERSECTION_OF, new CollectionOf<OWLObjectIntersectionOf>() {
            @Override
            Property getPredicate() {
                return OWL.intersectionOf;
            }
        }),
        ONE_OF(ClassExpressionType.OBJECT_ONE_OF, new CollectionOf<OWLObjectOneOf>() {
            @Override
            Property getPredicate() {
                return OWL.oneOf;
            }
        }),
        COMPLEMENT_OF(ClassExpressionType.OBJECT_COMPLEMENT_OF, new ComponentsOf() {
            @Override
            Property getPredicate() {
                return OWL.complementOf;
            }
        }),;

        private final ClassExpressionType type;
        private final Translator<? extends OWLClassExpression> translator;

        CETranslator(ClassExpressionType type, Translator<? extends OWLClassExpression> translator) {
            this.type = type;
            this.translator = translator;
        }

        public static CETranslator valueOf(ClassExpressionType type) {
            for (CETranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        public static Resource addClassExpression(Model model, OWLClassExpression expression) {
            CETranslator cet = OntException.notNull(valueOf(expression.getClassExpressionType()),
                    "Unsupported expression " + expression + "/" + expression.getClassExpressionType());
            return cet.translator.add(model, expression);
        }

        private static abstract class Translator<CE extends OWLClassExpression> {
            @SuppressWarnings("unchecked")
            private Resource add(Model model, OWLClassExpression expression) {
                return translate(model, (CE) expression);
            }

            abstract Resource translate(Model model, CE expression);

            abstract Property getPredicate();
        }

        private static abstract class Restriction<RestrictionCE extends OWLRestriction> extends Translator<RestrictionCE> {
            @Override
            Resource translate(Model model, RestrictionCE expression) {
                RDFNode object;
                if (ClassExpressionType.OBJECT_HAS_SELF.equals(expression.getClassExpressionType())) {
                    Node literal = NodeIRIUtils.toLiteralNode(String.valueOf(Boolean.TRUE), null, XSDVocabulary.BOOLEAN.getIRI());
                    object = model.getRDFNode(literal);
                } else if (HasFiller.class.isInstance(expression)) {
                    OWLObject filter = ((HasFiller) expression).getFiller();
                    object = addRDFNode(model, filter);
                } else {
                    throw new OntException("Unsupported restriction " + expression);
                }
                OWLPropertyExpression property = expression.getProperty();
                Resource res = model.createResource();
                model.add(res, RDF.type, OWL.Restriction);
                model.add(res, OWL.onProperty, toResource(property));
                model.add(res, getPredicate(), object);
                return res;
            }
        }

        private static abstract class RestrictionCardinality<RestrictionCardinalityCE extends OWLCardinalityRestriction> extends Restriction<RestrictionCardinalityCE> {
            private Node getCardinalityLiteral(HasCardinality restriction) {
                return NodeIRIUtils.toLiteralNode(String.valueOf(restriction.getCardinality()), null, XSDVocabulary.NON_NEGATIVE_INTEGER.getIRI());
            }

            @Override
            Resource translate(Model model, RestrictionCardinalityCE expression) {
                OWLPropertyExpression property = expression.getProperty();
                Resource res = model.createResource();
                model.add(res, RDF.type, OWL.Restriction);
                model.add(res, OWL.onProperty, toResource(property));
                model.add(res, getPredicate(), model.getRDFNode(getCardinalityLiteral(expression)));
                return res;
            }
        }

        private static abstract class CollectionOf<OperandsCE extends OWLClassExpression & HasOperands<? extends OWLObject>> extends Translator<OperandsCE> {
            @Override
            Resource translate(Model model, OperandsCE expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, OWL.Class);
                model.add(res, getPredicate(), addRDFList(model, expression.operands()));
                return res;
            }
        }

        private static abstract class ComponentsOf extends Translator<OWLObjectComplementOf> {
            @Override
            Resource translate(Model model, OWLObjectComplementOf expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, OWL.Class);
                model.add(res, getPredicate(), addRDFNode(model, expression.getOperand()));
                return res;
            }
        }
    }
}
