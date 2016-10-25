package ru.avicomp.ontapi.parsers.axiom2rdf;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
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
import ru.avicomp.ontapi.vocabulary.SWRL;

/**
 * Helper for axiom parsing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2 Mapping from the Structural Specification to RDF Graphs</a>
 * for handling common graph triples (operator 'T') see chapter "2.1 Translation of Axioms without Annotations"
 * for handling annotations (operator 'TANN') see chapters "2.2 Translation of Annotations" and "2.3 Translation of Axioms with Annotations".
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class TranslationHelper {

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
        processAnnotatedTriple(graph, subject, predicate, object, axiom, false);
    }

    public static void processAnnotatedTriple(Graph graph, OWLObject subject, Property predicate, OWLObject object, OWLAxiom axiom, boolean addSubject) {
        Model model = createModel(graph);
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        RDFNode _object = addRDFNode(model, object);
        model.add(_subject, predicate, _object);
        addAnnotations(model, _subject, predicate, _object, axiom);
    }

    public static void processAnnotatedTriple(Graph graph, OWLObject subject, Property predicate, RDFNode object, OWLAxiom axiom, boolean addSubject) {
        Model model = createModel(graph);
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        model.add(_subject, predicate, object);
        addAnnotations(model, _subject, predicate, object, axiom);
    }

    public static void processAnnotatedTriple(Graph graph, OWLObject subject, Property predicate, Stream<? extends OWLObject> objects, OWLAxiom axiom, boolean addSubject) {
        Model model = createModel(graph);
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        RDFNode _object = addRDFList(model, objects);
        model.add(_subject, predicate, _object);
        addAnnotations(model, _subject, predicate, _object, axiom);
    }

    public static RDFList addRDFList(Model model, Stream<? extends OWLObject> objects) {
        return model.createList(toResourceIterator(model, objects));
    }

    /**
     * the main method to add OWLObject as RDFNode to the specified model.
     *
     * @param model {@link Model}
     * @param o     {@link OWLObject}
     * @return {@link RDFNode} node, attached to the model.
     */
    public static RDFNode addRDFNode(Model model, OWLObject o) {
        if (OWLEntity.class.isInstance(o)) {
            OWLEntity entity = (OWLEntity) o;
            Resource res = toResource(entity);
            if (!entity.isBuiltIn()) {
                model.add(res, RDF.type, getType((OWLEntity) o));
            }
            return res.inModel(model);
        }
        if (OWLObjectInverseOf.class.isInstance(o)) {
            OWLObjectInverseOf io = (OWLObjectInverseOf) o;
            Resource res = model.createResource();
            model.add(res, OWL.inverseOf, addRDFNode(model, io.getInverse()));
            return res;
        }
        if (OWLFacetRestriction.class.isInstance(o)) {
            OWLFacetRestriction fr = (OWLFacetRestriction) o;
            Resource res = model.createResource();
            model.add(res, toProperty(fr.getFacet().getIRI()), addRDFNode(model, fr.getFacetValue()));
            return res;
        }
        if (OWLClassExpression.class.isInstance(o)) {
            return CETranslator.add(model, (OWLClassExpression) o);
        }
        if (OWLDataRange.class.isInstance(o)) {
            return DRTranslator.add(model, (OWLDataRange) o);
        }
        if (OWLAnonymousIndividual.class.isInstance(o)) {
            Resource res = toResource(((OWLAnonymousIndividual) o).getID());
            if (!model.contains(res, null, (RDFNode) null)) {
                throw new OntException("Anonymous individuals should be created first.");
            }
            return res.inModel(model);
        }
        if (SWRLObject.class.isInstance(o)) {
            return addRDFNode(model, (SWRLObject) o);
        }
        return toRDFNode(o).inModel(model);
    }

    public static RDFNode addRDFNode(Model model, SWRLObject o) {
        if (SWRLAtom.class.isInstance(o)) {
            return SWRLAtomTranslator.add(model, (SWRLAtom) o);
        } else if (SWRLArgument.class.isInstance(o)) {
            if (SWRLVariable.class.isInstance(o)) {
                Resource res = toResource(((SWRLVariable) o).getIRI()).inModel(model);
                res.addProperty(RDF.type, SWRL.Variable);
                return res;
            }
            if (SWRLLiteralArgument.class.isInstance(o)) {
                return addRDFNode(model, ((SWRLLiteralArgument) o).getLiteral());
            }
            if (SWRLIndividualArgument.class.isInstance(o)) {
                return addRDFNode(model, ((SWRLIndividualArgument) o).getIndividual());
            }
        }
        throw new OntException("Unsupported SWRL-Object: " + o);
    }

    /**
     * recursive operator TANN
     * see specification:
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_Generate_a_Main_Triple'>2.3 Translation of Axioms with Annotations</a> and
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
     * <p>
     * This is the case if ax' is of type
     * SubClassOf,
     * SubObjectPropertyOf without a property chain as the subproperty expression,
     * SubDataPropertyOf, ObjectPropertyDomain, DataPropertyDomain, ObjectPropertyRange, DataPropertyRange,
     * InverseObjectProperties, FunctionalObjectProperty, FunctionalDataProperty, InverseFunctionalObjectProperty,
     * ReflexiveObjectProperty, IrreflexiveObjectProperty, SymmetricObjectProperty, AsymmetricObjectProperty,
     * TransitiveObjectProperty, ClassAssertion, ObjectPropertyAssertion, DataPropertyAssertion, Declaration,
     * DisjointObjectProperties with two properties,
     * DisjointDataProperties with two properties,
     * DisjointClasses with two classes,
     * DifferentIndividuals with two individuals, or
     * AnnotationAssertion.
     * DisjointUnion, SubObjectPropertyOf with a subproperty chain, or HasKey
     * Also for
     * EquivalentClasses, EquivalentObjectProperties, EquivalentDataProperties, or SameIndividual (see {@link AbstractNaryParser});
     * in last case call this method for each of triple from inner axiom.
     * <p>
     *
     * @param graph Graph
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(Graph graph, Triple triple, OWLAxiom axiom) {
        if (!axiom.isAnnotated()) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Axiom.asNode()));
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), triple.getSubject()));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), triple.getPredicate()));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), triple.getObject()));
        addAnnotations(graph, blank, axiom);
    }

    /**
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_Represented_by_Blank_Nodes'>2.3.3 Axioms Represented by Blank Nodes </a>
     * for following axioms with more than two entities:
     * NegativeObjectPropertyAssertion,
     * NegativeDataPropertyAssertion,
     * DisjointClasses,
     * DisjointObjectProperties,
     * DisjointDataProperties,
     * DifferentIndividuals.
     * (see {@link AbstractTwoWayNaryParser})
     *
     * @param graph Graph
     * @param root  {@link org.apache.jena.graph.Node_Blank} anonymous node
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(Graph graph, Node root, OWLAxiom axiom) {
        addAnnotations(graph, root, axiom.annotations().collect(Collectors.toSet()));
    }

    public static void addAnnotations(Graph graph, Node root, Collection<OWLAnnotation> annotations) {
        if (annotations.isEmpty()) return;
        annotations.forEach(a -> {
            graph.add(Triple.create(root, toNode(graph, a.getProperty()), NodeIRIUtils.toNode(a.getValue())));
        });
        annotations.forEach(a -> translateAnnotation(graph, root, a));
    }

    public static void addAnnotations(Model model, Resource subject, Property predicate, RDFNode object, OWLAxiom axiom) {
        addAnnotations(model.getGraph(), Triple.create(subject.asNode(), predicate.asNode(), object.asNode()), axiom);
    }

    private static void translateAnnotation(Graph graph, Node source, OWLAnnotation annotation) {
        if (annotation.annotations().count() == 0) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Annotation.asNode()));
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), source));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), NodeIRIUtils.toNode(annotation.getProperty())));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), NodeIRIUtils.toNode(annotation.getValue())));
        annotation.annotations().forEach(child -> {
            graph.add(Triple.create(blank, toNode(graph, child.getProperty()), NodeIRIUtils.toNode(child.getValue())));
        });
        annotation.annotations().filter(a -> a.annotations().count() != 0).forEach(a -> translateAnnotation(graph, blank, a));
    }

    private static Node toNode(Graph graph, OWLAnnotationProperty property) {
        Node res = NodeIRIUtils.toNode(property);
        if (res.isURI() && !property.isBuiltIn()) {
            graph.add(Triple.create(res, RDF.type.asNode(), OWL.AnnotationProperty.asNode()));
        }
        return res;
    }

    /**
     * for SWRLAtom
     */
    private enum SWRLAtomTranslator {
        BUILT_IN(SWRLBuiltInAtom.class, new BuiltIn()),
        OWL_CLASS(SWRLClassAtom.class, new OWLClass()),
        DATA_PROPERTY(SWRLDataPropertyAtom.class, new DataProperty()),
        DATA_RANGE(SWRLDataRangeAtom.class, new DataRange()),
        DIFFERENT_INDIVIDUALS(SWRLDifferentIndividualsAtom.class, new DifferentIndividuals()),
        OBJECT_PROPERTY(SWRLObjectPropertyAtom.class, new ObjectProperty()),
        SAME_INDIVIDUALS(SWRLSameIndividualAtom.class, new SameIndividuals()),;
        private final Translator<? extends SWRLAtom> translator;
        private final Class<? extends SWRLAtom> type;

        SWRLAtomTranslator(Class<? extends SWRLAtom> type, Translator<? extends SWRLAtom> translator) {
            this.translator = translator;
            this.type = type;
        }

        private static SWRLAtomTranslator valueOf(SWRLAtom atom) {
            for (SWRLAtomTranslator t : values()) {
                if (t.type.isInstance(atom)) return t;
            }
            return null;
        }

        public static Resource add(Model model, SWRLAtom atom) {
            SWRLAtomTranslator swrlt = OntException.notNull(valueOf(atom), "Unsupported swrl-atom " + atom);
            return swrlt.translator.add(model, atom);
        }

        private static abstract class Translator<Atom extends SWRLAtom> {
            @SuppressWarnings("unchecked")
            private Resource add(Model model, SWRLAtom atom) {
                return translate(model, (Atom) atom);
            }

            abstract Resource translate(Model model, Atom atom);
        }

        private static class BuiltIn extends Translator<SWRLBuiltInAtom> {
            /**
             * see {@link org.semanticweb.owlapi.rdf.model.AbstractTranslator#visit(SWRLBuiltInAtom)}
             *
             * @param model Model
             * @param atom  SWRLBuiltInAtom
             * @return Resource
             */
            @Override
            Resource translate(Model model, SWRLBuiltInAtom atom) {
                // todo: it differs from OWL-API output.
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.BuiltinAtom);
                model.add(res, SWRL.builtin, addRDFNode(model, atom.getPredicate()));
                model.add(toResource(atom.getPredicate()), RDF.type, SWRL.Builtin);
                // is using rdf:List in such way correct?
                model.add(res, SWRL.arguments, addRDFList(model, atom.arguments()));
                return res;
            }
        }

        private static class OWLClass extends Translator<SWRLClassAtom> {
            @Override
            Resource translate(Model model, SWRLClassAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.ClassAtom);
                model.add(res, SWRL.classPredicate, addRDFNode(model, atom.getPredicate()));
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getArgument()));
                return res;
            }
        }

        private static class DataProperty extends Translator<SWRLDataPropertyAtom> {
            @Override
            Resource translate(Model model, SWRLDataPropertyAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.DatavaluedPropertyAtom);
                model.add(res, SWRL.propertyPredicate, addRDFNode(model, atom.getPredicate()));
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getFirstArgument()));
                model.add(res, SWRL.argument2, addRDFNode(model, atom.getSecondArgument()));
                return res;
            }
        }

        private static class DataRange extends Translator<SWRLDataRangeAtom> {
            @Override
            Resource translate(Model model, SWRLDataRangeAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.DataRangeAtom);
                model.add(res, SWRL.dataRange, addRDFNode(model, atom.getPredicate()));
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getArgument()));
                return res;
            }
        }

        private static class DifferentIndividuals extends Translator<SWRLDifferentIndividualsAtom> {
            @Override
            Resource translate(Model model, SWRLDifferentIndividualsAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.DifferentIndividualsAtom);
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getFirstArgument()));
                model.add(res, SWRL.argument2, addRDFNode(model, atom.getSecondArgument()));
                return res;
            }
        }

        private static class ObjectProperty extends Translator<SWRLObjectPropertyAtom> {
            @Override
            Resource translate(Model model, SWRLObjectPropertyAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.IndividualPropertyAtom);
                model.add(res, SWRL.propertyPredicate, addRDFNode(model, atom.getPredicate()));
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getFirstArgument()));
                model.add(res, SWRL.argument2, addRDFNode(model, atom.getSecondArgument()));
                return res;
            }
        }

        private static class SameIndividuals extends Translator<SWRLSameIndividualAtom> {
            @Override
            Resource translate(Model model, SWRLSameIndividualAtom atom) {
                Resource res = model.createResource();
                model.add(res, RDF.type, SWRL.SameIndividualAtom);
                model.add(res, SWRL.argument1, addRDFNode(model, atom.getFirstArgument()));
                model.add(res, SWRL.argument2, addRDFNode(model, atom.getSecondArgument()));
                return res;
            }
        }
    }

    /**
     * Data Range translator
     */
    private enum DRTranslator {
        ONE_OF(DataRangeType.DATA_ONE_OF, new OneOf()),
        RESTRICTION(DataRangeType.DATATYPE_RESTRICTION, new DatatypeRestriction()),
        COMPLEMENT_OF(DataRangeType.DATA_COMPLEMENT_OF, new ComplementOf()),
        UNION_OF(DataRangeType.DATA_UNION_OF, new NaryDataRange<OWLDataUnionOf>() {
            @Override
            Property getPredicate() {
                return OWL.unionOf;
            }
        }),
        INTERSECTION_OF(DataRangeType.DATA_INTERSECTION_OF, new NaryDataRange<OWLDataIntersectionOf>() {
            @Override
            Property getPredicate() {
                return OWL.intersectionOf;
            }
        }),;
        private final DataRangeType type;
        private final Translator<? extends OWLDataRange> translator;

        DRTranslator(DataRangeType type, Translator<? extends OWLDataRange> translator) {
            this.translator = translator;
            this.type = type;
        }

        public static DRTranslator valueOf(DataRangeType type) {
            for (DRTranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        public static Resource add(Model model, OWLDataRange expression) {
            DataRangeType type = expression.getDataRangeType();
            DRTranslator drt = OntException.notNull(valueOf(type), "Unsupported data-range expression " + expression + "/" + type);
            return drt.translator.add(model, expression);
        }

        private static abstract class Translator<DR extends OWLDataRange> {
            @SuppressWarnings("unchecked")
            private Resource add(Model model, OWLDataRange expression) {
                return translate(model, (DR) expression);
            }

            abstract Resource translate(Model model, DR expression);
        }

        private static class OneOf extends Translator<OWLDataOneOf> {
            @Override
            Resource translate(Model model, OWLDataOneOf expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, RDFS.Datatype);
                model.add(res, OWL.oneOf, addRDFList(model, expression.values()));
                return res;
            }
        }

        private static class DatatypeRestriction extends Translator<OWLDatatypeRestriction> {
            @Override
            Resource translate(Model model, OWLDatatypeRestriction expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, RDFS.Datatype);
                model.add(res, OWL2.onDatatype, addRDFNode(model, expression.getDatatype()));
                model.add(res, OWL2.withRestrictions, addRDFList(model, expression.facetRestrictions()));
                return res;
            }
        }

        private static class ComplementOf extends Translator<OWLDataComplementOf> {
            @Override
            Resource translate(Model model, OWLDataComplementOf expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, RDFS.Datatype);
                model.add(res, OWL2.datatypeComplementOf, addRDFNode(model, expression.getDataRange()));
                return res;
            }
        }

        private static abstract class NaryDataRange<NaryDR extends OWLNaryDataRange> extends Translator<NaryDR> {
            @Override
            Resource translate(Model model, NaryDR expression) {
                Resource res = model.createResource();
                model.add(res, RDF.type, RDFS.Datatype);
                model.add(res, getPredicate(), addRDFList(model, expression.operands()));
                return res;
            }

            abstract Property getPredicate();
        }
    }

    /**
     * Class Expression translator
     */
    private enum CETranslator {
        OBJECT_MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, new RestrictionCardinality<OWLObjectMaxCardinality>() {
            @Override
            Property getPredicate() {
                return OWL.maxCardinality;
            }
        }),
        DATA_MAX_CARDINALITY(ClassExpressionType.DATA_MAX_CARDINALITY, new DataRestrictionCardinality<OWLDataMaxCardinality>() {
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
        DATA_MIN_CARDINALITY(ClassExpressionType.DATA_MIN_CARDINALITY, new DataRestrictionCardinality<OWLDataMinCardinality>() {
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
        DATA_EXACT_CARDINALITY(ClassExpressionType.DATA_EXACT_CARDINALITY, new DataRestrictionCardinality<OWLDataExactCardinality>() {
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

        public static Resource add(Model model, OWLClassExpression expression) {
            ClassExpressionType type = expression.getClassExpressionType();
            CETranslator cet = OntException.notNull(valueOf(type), "Unsupported class-expression " + expression + "/" + type);
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

        private static abstract class DataRestrictionCardinality<DataRestrictionCardinalityCE extends OWLDataCardinalityRestriction> extends RestrictionCardinality<DataRestrictionCardinalityCE> {
            @Override
            Resource translate(Model model, DataRestrictionCardinalityCE expression) {
                Resource res = super.translate(model, expression);
                model.add(res, OWL2.onDataRange, addRDFNode(model, expression.getFiller()));
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
