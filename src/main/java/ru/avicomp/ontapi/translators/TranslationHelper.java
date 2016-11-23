package ru.avicomp.ontapi.translators;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;

/**
 * TODO: change to use {@link OntGraphModel} instead direct working with graph.
 * Helper for axiom parsing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2 Mapping from the Structural Specification to RDF Graphs</a>
 * for handling common graph triples (operator 'T') see chapter "2.1 Translation of Axioms without Annotations"
 * for handling annotations (operator 'TANN') see chapters "2.2 Translation of Annotations" and "2.3 Translation of Axioms with Annotations".
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class TranslationHelper {

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
        return ResourceFactory.createResource(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    public static Property toProperty(OWLObject object) {
        return toProperty(NodeIRIUtils.toIRI(object));
    }

    private static Property toProperty(IRI iri) {
        return ResourceFactory.createProperty(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    private static Literal toLiteral(OWLLiteral literal) {
        return new LiteralImpl(NodeIRIUtils.toLiteralNode(literal), null);
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
        throw new OntApiException("Unsupported " + entity);
    }

    public static Class<? extends OntEntity> getEntityView(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return OntClass.class;
        } else if (entity.isOWLDataProperty()) {
            return OntNDP.class;
        } else if (entity.isOWLObjectProperty()) {
            return OntNOP.class;
        } else if (entity.isOWLNamedIndividual()) {
            return OntIndividual.Named.class;
        } else if (entity.isOWLAnnotationProperty()) {
            return OntNAP.class;
        } else if (entity.isOWLDatatype()) {
            return OntDT.class;
        }
        throw new OntApiException("Unsupported " + entity);
    }

    public static Class<? extends OntFR> getFRView(OWLFacet facet) {
        switch (facet) {
            case LENGTH:
                return OntFR.Length.class;
            case MIN_LENGTH:
                return OntFR.MinLength.class;
            case MAX_LENGTH:
                return OntFR.MaxLength.class;
            case MIN_INCLUSIVE:
                return OntFR.MinInclusive.class;
            case MAX_INCLUSIVE:
                return OntFR.MaxInclusive.class;
            case MIN_EXCLUSIVE:
                return OntFR.MinExclusive.class;
            case MAX_EXCLUSIVE:
                return OntFR.MaxExclusive.class;
            case PATTERN:
                return OntFR.Pattern.class;
            case FRACTION_DIGITS:
                return OntFR.FractionDigits.class;
            case TOTAL_DIGITS:
                return OntFR.TotalDigits.class;
            case LANG_RANGE:
                return OntFR.LangRange.class;
        }
        throw new OntApiException("Unsupported " + facet);
    }

    public static void processAnnotatedTriple(OntGraphModel model, OWLObject subject, OWLObject predicate, OWLObject object, OWLAxiom axiom) {
        processAnnotatedTriple(model, subject, toProperty(predicate), object, axiom);
    }

    public static void processAnnotatedTriple(OntGraphModel model, OWLObject subject, Property predicate, OWLObject object, OWLAxiom axiom) {
        processAnnotatedTriple(model, subject, predicate, object, axiom, false);
    }

    public static void processAnnotatedTriple(OntGraphModel model, OWLObject subject, Property predicate, OWLObject object, OWLAxiom axiom, boolean addSubject) {
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        RDFNode _object = addRDFNode(model, object);
        model.add(_subject, predicate, _object);
        addAnnotations(model, _subject, predicate, _object, axiom);
    }

    public static void processAnnotatedTriple(OntGraphModel model, OWLObject subject, Property predicate, RDFNode object, OWLAxiom axiom, boolean addSubject) {
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        model.add(_subject, predicate, object);
        addAnnotations(model, _subject, predicate, object, axiom);
    }

    public static void processAnnotatedTriple(OntGraphModel model, OWLObject subject, Property predicate, Stream<? extends OWLObject> objects, OWLAxiom axiom, boolean addSubject) {
        Resource _subject = addSubject ? addRDFNode(model, subject).asResource() : toResource(subject);
        RDFNode _object = addRDFList(model, objects);
        model.add(_subject, predicate, _object);
        addAnnotations(model, _subject, predicate, _object, axiom);
    }

    public static RDFList addRDFList(OntGraphModel model, Stream<? extends OWLObject> objects) {
        return model.createList(objects.map(o -> addRDFNode(model, o)).iterator());
    }

    public static OntEntity addOntEntity(OntGraphModel model, OWLEntity entity) {
        Class<? extends OntEntity> view = getEntityView(entity);
        String uri = entity.getIRI().getIRIString();
        return model.fetchOntEntity(view, uri);
    }

    public static OntOPE.Inverse addInverseOf(OntGraphModel model, OWLObjectInverseOf io) {
        String uri = io.getInverseProperty().getNamedProperty().getIRI().getIRIString();
        return model.fetchOntEntity(OntNOP.class, uri).createInverse();
    }

    public static OntFR addFacetRestriction(OntGraphModel model, OWLFacetRestriction fr) {
        return model.createFacetRestriction(getFRView(fr.getFacet()), toLiteral(fr.getFacetValue()));
    }

    public static OntCE addClassExpression(OntGraphModel model, OWLClassExpression ce) {
        if (ce.isOWLClass()) {
            return addOntEntity(model, ce.asOWLClass()).as(OntClass.class);
        }
        ClassExpressionType type = ce.getClassExpressionType();
        CETranslator cet = OntApiException.notNull(CETranslator.valueOf(type), "Unsupported class-expression " + ce + "/" + type);
        return cet.translator.add(model, ce).as(OntCE.class);
    }

    public static OntDR addDataRange(OntGraphModel model, OWLDataRange dr) {
        if (dr.isOWLDatatype()) {
            return addOntEntity(model, dr.asOWLDatatype()).as(OntDT.class);
        }
        DataRangeType type = dr.getDataRangeType();
        DRTranslator drt = OntApiException.notNull(DRTranslator.valueOf(type), "Unsupported data-range expression " + dr + "/" + type);
        return drt.translator.add(model, dr).as(OntDR.class);
    }

    public static OntIndividual.Anonymous getAnonymousIndividual(OntGraphModel model, OWLAnonymousIndividual ai) {
        Resource res = toResource(ai.getID());
        if (!model.contains(res, null, (RDFNode) null)) {
            throw new OntApiException("Anonymous individuals should be created first.");
        }
        return res.inModel(model).as(OntIndividual.Anonymous.class);
    }

    /**
     * the main method to add OWLObject as RDFNode to the specified model.
     *
     * @param model {@link OntGraphModel}
     * @param o     {@link OWLObject}
     * @return {@link RDFNode} node, attached to the model.
     */
    public static RDFNode addRDFNode(OntGraphModel model, OWLObject o) {
        if (OWLEntity.class.isInstance(o)) {
            return addOntEntity(model, (OWLEntity) o);
        }
        if (OWLObjectInverseOf.class.isInstance(o)) {
            return addInverseOf(model, (OWLObjectInverseOf) o);
        }
        if (OWLFacetRestriction.class.isInstance(o)) {
            return addFacetRestriction(model, (OWLFacetRestriction) o);
        }
        if (OWLClassExpression.class.isInstance(o)) {
            return addClassExpression(model, (OWLClassExpression) o);
        }
        if (OWLDataRange.class.isInstance(o)) {
            return addDataRange(model, (OWLDataRange) o);
        }
        if (OWLAnonymousIndividual.class.isInstance(o)) {
            return getAnonymousIndividual(model, (OWLAnonymousIndividual) o);
        }
        if (SWRLObject.class.isInstance(o)) {
            return addSWRLObject(model, (SWRLObject) o);
        }
        return toRDFNode(o).inModel(model);
    }

    public static OntSWRL.Variable addSWRLVariable(OntGraphModel model, SWRLVariable var) {
        return model.createSWRLVariable(var.getIRI().getIRIString());
    }

    public static OntSWRL.Atom addSWRLAtom(OntGraphModel model, SWRLAtom atom) {
        SWRLAtomTranslator swrlt = OntApiException.notNull(SWRLAtomTranslator.valueOf(atom), "Unsupported swrl-atom " + atom);
        return swrlt.translator.add(model, atom).as(OntSWRL.Atom.class);
    }

    public static RDFNode addSWRLObject(OntGraphModel model, SWRLObject o) {
        if (SWRLAtom.class.isInstance(o)) {
            return addSWRLAtom(model, (SWRLAtom) o);
        } else if (SWRLArgument.class.isInstance(o)) {
            if (SWRLVariable.class.isInstance(o)) {
                return addSWRLVariable(model, (SWRLVariable) o);
            }
            if (SWRLLiteralArgument.class.isInstance(o)) {
                return addRDFNode(model, ((SWRLLiteralArgument) o).getLiteral());
            }
            if (SWRLIndividualArgument.class.isInstance(o)) {
                return addRDFNode(model, ((SWRLIndividualArgument) o).getIndividual());
            }
        }
        throw new OntApiException("Unsupported SWRL-Object: " + o);
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
     * EquivalentClasses, EquivalentObjectProperties, EquivalentDataProperties, or SameIndividual (see {@link AbstractNaryTranslator});
     * in last case call this method for each of triple from inner axiom.
     * <p>
     *
     * @param model {@link OntGraphModel}
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(OntGraphModel model, Statement statement, OWLAxiom axiom) {
        if (!axiom.isAnnotated()) return;
        Resource blank = model.createResource();
        model.add(blank, RDF.type, OWL2.Axiom);
        model.add(blank, OWL2.annotatedSource, statement.getSubject());
        model.add(blank, OWL2.annotatedProperty, statement.getPredicate());
        model.add(blank, OWL2.annotatedTarget, statement.getObject());
        addAnnotations(model, blank, axiom);
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
     * (see {@link AbstractTwoWayNaryTranslator})
     *
     * @param model {@link OntGraphModel}
     * @param root  {@link Resource} anonymous node
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(OntGraphModel model, Resource root, OWLAxiom axiom) {
        addAnnotations(model, root, axiom.annotations().collect(Collectors.toSet()));
    }

    public static void addAnnotations(OntGraphModel model, Resource root, Collection<OWLAnnotation> annotations) {
        if (annotations.isEmpty()) return;
        annotations.forEach(a -> {
            model.add(root, addRDFNode(model, a.getProperty()).as(Property.class), toRDFNode(a.getValue()));
        });
        annotations.forEach(a -> translateAnnotation(model, root, a));
    }

    public static void addAnnotations(OntGraphModel model, Resource subject, Property predicate, RDFNode object, OWLAxiom axiom) {
        addAnnotations(model, model.createStatement(subject, predicate, object), axiom);
    }

    private static void translateAnnotation(OntGraphModel model, Resource source, OWLAnnotation annotation) {
        if (annotation.annotations().count() == 0) return;
        Resource blank = model.createResource();
        model.add(blank, RDF.type, OWL2.Annotation);
        model.add(blank, OWL2.annotatedSource, source);
        model.add(blank, OWL2.annotatedProperty, toRDFNode(annotation.getProperty()));
        model.add(blank, OWL2.annotatedTarget, toRDFNode(annotation.getValue()));
        annotation.annotations().forEach(child -> model.add(blank, addRDFNode(model, child.getProperty()).as(Property.class), toRDFNode(child.getValue())));
        annotation.annotations().filter(a -> a.annotations().count() != 0).forEach(a -> translateAnnotation(model, blank, a));
    }

    /**
     * for SWRLAtom
     */
    private enum SWRLAtomTranslator {
        BUILT_IN(SWRLBuiltInAtom.class, new Translator<SWRLBuiltInAtom, OntSWRL.Atom.BuiltIn>() {
            @Override
            OntSWRL.Atom.BuiltIn translate(OntGraphModel model, SWRLBuiltInAtom atom) {
                return model.createBuiltInSWRLAtom(model.createResource(atom.getPredicate().getIRIString()),
                        atom.arguments().map(a -> addSWRLObject(model, a).as(OntSWRL.DArg.class)));
            }
        }),
        OWL_CLASS(SWRLClassAtom.class, new Translator<SWRLClassAtom, OntSWRL.Atom.OntClass>() {
            @Override
            OntSWRL.Atom.OntClass translate(OntGraphModel model, SWRLClassAtom atom) {
                return model.createClassSWRLAtom(addClassExpression(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getArgument()).as(OntSWRL.IArg.class));
            }
        }),
        DATA_PROPERTY(SWRLDataPropertyAtom.class, new Translator<SWRLDataPropertyAtom, OntSWRL.Atom.DataProperty>() {
            @Override
            OntSWRL.Atom.DataProperty translate(OntGraphModel model, SWRLDataPropertyAtom atom) {
                return model.createDataPropertySWRLAtom(addOntEntity(model, atom.getPredicate().asOWLDataProperty()).as(OntNDP.class),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DATA_RANGE(SWRLDataRangeAtom.class, new Translator<SWRLDataRangeAtom, OntSWRL.Atom.DataRange>() {
            @Override
            OntSWRL.Atom.DataRange translate(OntGraphModel model, SWRLDataRangeAtom atom) {
                return model.createDataRangeSWRLAtom(addDataRange(model, atom.getPredicate()), addSWRLObject(model, atom.getArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DIFFERENT_INDIVIDUALS(SWRLDifferentIndividualsAtom.class, new Translator<SWRLDifferentIndividualsAtom, OntSWRL.Atom.DifferentIndividuals>() {
            @Override
            OntSWRL.Atom.DifferentIndividuals translate(OntGraphModel model, SWRLDifferentIndividualsAtom atom) {
                return model.createDifferentIndividualsSWRLAtom(addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        OBJECT_PROPERTY(SWRLObjectPropertyAtom.class, new Translator<SWRLObjectPropertyAtom, OntSWRL.Atom.ObjectProperty>() {
            @Override
            OntSWRL.Atom.ObjectProperty translate(OntGraphModel model, SWRLObjectPropertyAtom atom) {
                return model.createObjectPropertySWRLAtom(addRDFNode(model, atom.getPredicate()).as(OntOPE.class),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        SAME_INDIVIDUALS(SWRLSameIndividualAtom.class, new Translator<SWRLSameIndividualAtom, OntSWRL.Atom.SameIndividuals>() {
            @Override
            OntSWRL.Atom.SameIndividuals translate(OntGraphModel model, SWRLSameIndividualAtom atom) {
                return model.createSameIndividualsSWRLAtom(addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),;

        private final Translator<? extends SWRLAtom, ? extends OntSWRL.Atom> translator;
        private final Class<? extends SWRLAtom> type;

        SWRLAtomTranslator(Class<? extends SWRLAtom> type, Translator<? extends SWRLAtom, ? extends OntSWRL.Atom> translator) {
            this.translator = translator;
            this.type = type;
        }

        private static SWRLAtomTranslator valueOf(SWRLAtom atom) {
            for (SWRLAtomTranslator t : values()) {
                if (t.type.isInstance(atom)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends SWRLAtom, TO extends OntSWRL.Atom> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, SWRLAtom atom) {
                return translate(model, (FROM) atom);
            }

            abstract TO translate(OntGraphModel model, FROM atom);
        }
    }

    /**
     * Data Range translator
     */
    private enum DRTranslator {
        ONE_OF(DataRangeType.DATA_ONE_OF, new Translator<OWLDataOneOf, OntDR.OneOf>() {
            @Override
            OntDR.OneOf translate(OntGraphModel model, OWLDataOneOf expression) {
                return model.createOneOfDataRange(expression.values().map(TranslationHelper::toLiteral));
            }
        }),
        RESTRICTION(DataRangeType.DATATYPE_RESTRICTION, new Translator<OWLDatatypeRestriction, OntDR.Restriction>() {
            @Override
            OntDR.Restriction translate(OntGraphModel model, OWLDatatypeRestriction expression) {
                return model.createRestrictionDataRange(addRDFNode(model, expression.getDatatype()).as(OntDR.class),
                        expression.facetRestrictions().map(f -> addFacetRestriction(model, f)));
            }
        }),
        COMPLEMENT_OF(DataRangeType.DATA_COMPLEMENT_OF, new Translator<OWLDataComplementOf, OntDR.ComplementOf>() {
            @Override
            OntDR.ComplementOf translate(OntGraphModel model, OWLDataComplementOf expression) {
                return model.createComplementOfDataRange(addRDFNode(model, expression.getDataRange()).as(OntDR.class));
            }
        }),
        UNION_OF(DataRangeType.DATA_UNION_OF, new Translator<OWLDataUnionOf, OntDR.UnionOf>() {
            @Override
            OntDR.UnionOf translate(OntGraphModel model, OWLDataUnionOf expression) {
                return model.createUnionOfDataRange(expression.operands().map(owlDataRange -> addRDFNode(model, owlDataRange).as(OntDR.class)));
            }
        }),
        INTERSECTION_OF(DataRangeType.DATA_INTERSECTION_OF, new Translator<OWLDataIntersectionOf, OntDR.IntersectionOf>() {
            @Override
            OntDR.IntersectionOf translate(OntGraphModel model, OWLDataIntersectionOf expression) {
                return model.createIntersectionOfDataRange(expression.operands().map(owlDataRange -> addRDFNode(model, owlDataRange).as(OntDR.class)));
            }
        }),;
        private final DataRangeType type;
        private final Translator<? extends OWLDataRange, ? extends OntDR> translator;

        DRTranslator(DataRangeType type, Translator<? extends OWLDataRange, ? extends OntDR> translator) {
            this.translator = translator;
            this.type = type;
        }

        public static DRTranslator valueOf(DataRangeType type) {
            for (DRTranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLDataRange, TO extends OntDR> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, OWLDataRange expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntGraphModel model, FROM expression);
        }
    }

    /**
     * Class Expression translator
     */
    private enum CETranslator {
        OBJECT_MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, new Translator<OWLObjectMaxCardinality, OntCE.ObjectMaxCardinality>() {
            @Override
            OntCE.ObjectMaxCardinality translate(OntGraphModel model, OWLObjectMaxCardinality expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectMaxCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MAX_CARDINALITY(ClassExpressionType.DATA_MAX_CARDINALITY, new Translator<OWLDataMaxCardinality, OntCE.DataMaxCardinality>() {
            @Override
            OntCE.DataMaxCardinality translate(OntGraphModel model, OWLDataMaxCardinality expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataMaxCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_MIN_CARDINALITY(ClassExpressionType.OBJECT_MIN_CARDINALITY, new Translator<OWLObjectMinCardinality, OntCE.ObjectMinCardinality>() {
            @Override
            OntCE.ObjectMinCardinality translate(OntGraphModel model, OWLObjectMinCardinality expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectMinCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MIN_CARDINALITY(ClassExpressionType.DATA_MIN_CARDINALITY, new Translator<OWLDataMinCardinality, OntCE.DataMinCardinality>() {
            @Override
            OntCE.DataMinCardinality translate(OntGraphModel model, OWLDataMinCardinality expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataMinCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_EXACT_CARDINALITY(ClassExpressionType.OBJECT_EXACT_CARDINALITY, new Translator<OWLObjectExactCardinality, OntCE.ObjectCardinality>() {
            @Override
            OntCE.ObjectCardinality translate(OntGraphModel model, OWLObjectExactCardinality expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_EXACT_CARDINALITY(ClassExpressionType.DATA_EXACT_CARDINALITY, new Translator<OWLDataExactCardinality, OntCE.DataCardinality>() {
            @Override
            OntCE.DataCardinality translate(OntGraphModel model, OWLDataExactCardinality expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_ALL_VALUES_FROM(ClassExpressionType.OBJECT_ALL_VALUES_FROM, new Translator<OWLObjectAllValuesFrom, OntCE.ObjectAllValuesFrom>() {
            @Override
            OntCE.ObjectAllValuesFrom translate(OntGraphModel model, OWLObjectAllValuesFrom expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntCE c = addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectAllValuesFrom(p, c);
            }
        }),
        DATA_ALL_VALUES_FROM(ClassExpressionType.DATA_ALL_VALUES_FROM, new Translator<OWLDataAllValuesFrom, OntCE.DataAllValuesFrom>() {
            @Override
            OntCE.DataAllValuesFrom translate(OntGraphModel model, OWLDataAllValuesFrom expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                OntDR d = addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataAllValuesFrom(p, d);
            }
        }),
        OBJECT_SOME_VALUES_FROM(ClassExpressionType.OBJECT_SOME_VALUES_FROM, new Translator<OWLObjectSomeValuesFrom, OntCE.ObjectSomeValuesFrom>() {
            @Override
            OntCE.ObjectSomeValuesFrom translate(OntGraphModel model, OWLObjectSomeValuesFrom expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntCE c = addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectSomeValuesFrom(p, c);
            }
        }),
        DATA_SOME_VALUES_FROM(ClassExpressionType.DATA_SOME_VALUES_FROM, new Translator<OWLDataSomeValuesFrom, OntCE.DataSomeValuesFrom>() {
            @Override
            OntCE.DataSomeValuesFrom translate(OntGraphModel model, OWLDataSomeValuesFrom expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                OntDR d = addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataSomeValuesFrom(p, d);
            }
        }),
        OBJECT_HAS_VALUE(ClassExpressionType.OBJECT_HAS_VALUE, new Translator<OWLObjectHasValue, OntCE.ObjectHasValue>() {
            @Override
            OntCE.ObjectHasValue translate(OntGraphModel model, OWLObjectHasValue expression) {
                OntOPE p = addRDFNode(model, expression.getProperty()).as(OntOPE.class);
                OntIndividual i = addRDFNode(model, expression.getFiller()).as(OntIndividual.class);
                return model.createObjectHasValue(p, i);
            }
        }),
        DATA_HAS_VALUE(ClassExpressionType.DATA_HAS_VALUE, new Translator<OWLDataHasValue, OntCE.DataHasValue>() {
            @Override
            OntCE.DataHasValue translate(OntGraphModel model, OWLDataHasValue expression) {
                OntNDP p = addRDFNode(model, expression.getProperty()).as(OntNDP.class);
                Literal l = toLiteral(expression.getFiller());
                return model.createDataHasValue(p, l);
            }
        }),
        HAS_SELF(ClassExpressionType.OBJECT_HAS_SELF, new Translator<OWLObjectHasSelf, OntCE.HasSelf>() {
            @Override
            OntCE.HasSelf translate(OntGraphModel model, OWLObjectHasSelf expression) {
                return model.createHasSelf(addRDFNode(model, expression.getProperty()).as(OntOPE.class));
            }
        }),
        UNION_OF(ClassExpressionType.OBJECT_UNION_OF, new Translator<OWLObjectUnionOf, OntCE.UnionOf>() {
            @Override
            OntCE.UnionOf translate(OntGraphModel model, OWLObjectUnionOf expression) {
                return model.createUnionOf(expression.operands().map(ce -> addRDFNode(model, ce).as(OntCE.class)));
            }
        }),
        INTERSECTION_OF(ClassExpressionType.OBJECT_INTERSECTION_OF, new Translator<OWLObjectIntersectionOf, OntCE.IntersectionOf>() {
            @Override
            OntCE.IntersectionOf translate(OntGraphModel model, OWLObjectIntersectionOf expression) {
                return model.createIntersectionOf(expression.operands().map(ce -> addRDFNode(model, ce).as(OntCE.class)));
            }
        }),
        ONE_OF(ClassExpressionType.OBJECT_ONE_OF, new Translator<OWLObjectOneOf, OntCE.OneOf>() {
            @Override
            OntCE.OneOf translate(OntGraphModel model, OWLObjectOneOf expression) {
                return model.createOneOf(expression.operands().map(ce -> addRDFNode(model, ce).as(OntIndividual.class)));
            }
        }),
        COMPLEMENT_OF(ClassExpressionType.OBJECT_COMPLEMENT_OF, new Translator<OWLObjectComplementOf, OntCE.ComplementOf>() {
            @Override
            OntCE.ComplementOf translate(OntGraphModel model, OWLObjectComplementOf expression) {
                return model.createComplementOf(addRDFNode(model, expression.getOperand()).as(OntCE.class));
            }
        }),;

        private final ClassExpressionType type;
        private final Translator<? extends OWLClassExpression, ? extends OntCE> translator;

        CETranslator(ClassExpressionType type, Translator<? extends OWLClassExpression, ? extends OntCE> translator) {
            this.type = type;
            this.translator = translator;
        }

        public static CETranslator valueOf(ClassExpressionType type) {
            for (CETranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLClassExpression, TO extends OntCE> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, OWLClassExpression expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntGraphModel model, FROM expression);
        }
    }
}
