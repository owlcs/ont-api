package ru.avicomp.ontapi.translators;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.out.NodeFmtLib;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import uk.ac.manchester.cs.owl.owlapi.*;

/**
 * Helper to translate rdf-graph to the axioms.
 * TODO:
 * <p>
 * Created by @szuev on 25.11.2016.
 */
public class RDF2OWLHelper {

    public static final OWLDataFactory OWL_DATA_FACTORY = new OWLDataFactoryImpl();

    public static OWLEntity getEntity(OntEntity entity) {
        IRI iri = IRI.create(OntApiException.notNull(entity, "Null entity.").getURI());
        if (OntClass.class.isInstance(entity)) {
            return new OWLClassImpl(iri);
        } else if (OntDT.class.isInstance(entity)) {
            return new OWLDatatypeImpl(iri);
        } else if (OntIndividual.Named.class.isInstance(entity)) {
            return new OWLNamedIndividualImpl(iri);
        } else if (OntNAP.class.isInstance(entity)) {
            return new OWLAnnotationPropertyImpl(iri);
        } else if (OntNDP.class.isInstance(entity)) {
            return new OWLDataPropertyImpl(iri);
        } else if (OntNOP.class.isInstance(entity)) {
            return new OWLObjectPropertyImpl(iri);
        }
        throw new OntApiException("Unsupported " + entity);
    }

    public static OWLAnonymousIndividual getAnonymousIndividual(OntIndividual.Anonymous individual) {
        return getAnonymousIndividual((RDFNode) OntApiException.notNull(individual, "Null individual."));
    }

    private static OWLAnonymousIndividual getAnonymousIndividual(RDFNode anon) {
        if (!anon.isAnon()) throw new OntApiException("Not anon " + anon);
        String label = NodeFmtLib.encodeBNodeLabel(anon.asNode().getBlankNodeLabel());
        return new OWLAnonymousIndividualImpl(NodeID.getNodeID(label));
    }

    public static OWLIndividual getIndividual(OntIndividual individual) {
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return new OWLNamedIndividualImpl(IRI.create(individual.getURI()));
        }
        return getAnonymousIndividual(individual.as(OntIndividual.Anonymous.class));
    }

    /**
     * NOTE: different implementations of {@link OWLLiteral} have different mechanism to calculate hash.
     * For example {@link OWLLiteralImplInteger}.hashCode != {@link OWLLiteralImpl}.hashCode
     * So even if {@link OWLLiteral}s equal there is no guarantee that {@link Set}s of {@link OWLLiteral}s equal too.
     *
     * @param literal {@link Literal} - jena literal.
     * @return {@link OWLLiteralImpl} - OWL-API literal.
     */
    public static OWLLiteral getLiteral(Literal literal) {
        String txt = OntApiException.notNull(literal, "Null literal").getLexicalForm();
        String lang = literal.getLanguage();
        OWLDatatype dt = new OWLDatatypeImpl(IRI.create(literal.getDatatypeURI()));
        return new OWLLiteralImpl(txt, lang, dt);
    }

    public static OWLAnnotationSubject getAnnotationSubject(Resource resource) {
        if (OntApiException.notNull(resource, "Null resource").isURIResource()) {
            return IRI.create(resource.getURI());
        }
        if (resource.isAnon()) {
            return getAnonymousIndividual(Models.asAnonymousIndividual(resource));
        }
        throw new OntApiException("Not an AnnotationSubject " + resource);
    }

    public static OWLAnnotationValue getAnnotationValue(RDFNode node) {
        if (OntApiException.notNull(node, "Null node").isLiteral()) {
            return getLiteral(node.asLiteral());
        }
        if (node.isURIResource()) {
            return IRI.create(node.asResource().getURI());
        }
        if (node.isAnon()) {
            return getAnonymousIndividual(Models.asAnonymousIndividual(node));
        }
        throw new OntApiException("Not an AnnotationValue " + node);
    }

    public static OWLPropertyExpression getProperty(OntPE property) {
        if (OntApiException.notNull(property, "Null property.").canAs(OntNAP.class)) {
            return RDF2OWLHelper.getAnnotationProperty(property.as(OntNAP.class));
        }
        if (property.canAs(OntNDP.class)) {
            return RDF2OWLHelper.getDataProperty(property.as(OntNDP.class));
        }
        if (property.canAs(OntOPE.class)) {
            return RDF2OWLHelper.getObjectProperty(property.as(OntOPE.class));
        }
        throw new OntApiException("Unsupported property " + property);
    }

    public static OWLAnnotationProperty getAnnotationProperty(OntNAP nap) {
        IRI iri = IRI.create(OntApiException.notNull(nap, "Null annotation property.").getURI());
        return new OWLAnnotationPropertyImpl(iri);
    }

    public static OWLDataProperty getDataProperty(OntNDP nap) {
        IRI iri = IRI.create(OntApiException.notNull(nap, "Null data property.").getURI());
        return new OWLDataPropertyImpl(iri);
    }

    public static OWLObjectPropertyExpression getObjectProperty(OntOPE ope) {
        OntApiException.notNull(ope, "Null object property.");
        OWLObjectPropertyExpression res;
        if (ope.isAnon()) { //todo: handle inverse of inverseOf
            OWLObjectProperty op = new OWLObjectPropertyImpl(IRI.create(ope.as(OntOPE.Inverse.class).getDirect().getURI()));
            res = op.getInverseProperty();
        } else {
            res = new OWLObjectPropertyImpl(IRI.create(ope.getURI()));
        }
        return res;
    }

    public static OWLDatatype getDatatype(OntDT dt) {
        IRI iri = IRI.create(OntApiException.notNull(dt, "Null datatype.").getURI());
        return new OWLDatatypeImpl(iri);
    }

    public static Stream<OWLAnnotation> annotations(OntObject object) {
        return annotations(OntApiException.notNull(object, "Null ont-object.").getRoot());
    }

    public static Stream<OWLAnnotation> annotations(OntStatement statement) {
        return statement.annotations()
                .map(a -> new OWLAnnotationImpl(getAnnotationProperty(a.getPredicate().as(OntNAP.class)),
                        getAnnotationValue(a.getObject()),
                        annotations(a)));
    }

    public static Set<AxiomTranslator.Triples<OWLAnnotation>> getAnnotations(OntObject object) {
        return getBulkAnnotations(OntApiException.notNull(object, "Null ont-object.").getRoot());
    }

    public static Set<AxiomTranslator.Triples<OWLAnnotation>> getAnnotations(OntStatement statement) {
        if (isEntityDeclaration(statement) && statement.annotations().noneMatch(OntStatement::hasAnnotations)) {
            // for compatibility with OWL-API skip plain annotations attached to an entity:
            // they would go separately as annotation-assertions.
            return Collections.emptySet();
        }
        return getBulkAnnotations(statement);
    }

    public static Set<AxiomTranslator.Triples<OWLAnnotation>> getBulkAnnotations(OntStatement statement) {
        return statement.annotations().map(a -> a.hasAnnotations() ?
                getHierarchicalAnnotations(a) :
                getPlainAnnotation(a)).collect(Collectors.toSet());
    }

    public static boolean isEntityDeclaration(OntStatement statement) {
        return statement.isRoot() && statement.isDeclaration() && statement.getSubject().isURIResource();
    }

    private static AxiomTranslator.Triples<OWLAnnotation> getPlainAnnotation(OntStatement a) {
        OWLAnnotation res = new OWLAnnotationImpl(getAnnotationProperty(a.getPredicate().as(OntNAP.class)),
                getAnnotationValue(a.getObject()), Stream.empty());
        return new AxiomTranslator.Triples<>(res, a.asTriple());
    }

    private static AxiomTranslator.Triples<OWLAnnotation> getHierarchicalAnnotations(OntStatement a) {
        OntObject ann = a.getSubject().as(OntObject.class);
        Set<Triple> triples = new HashSet<>();
        Stream.of(RDF.type, OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                .forEach(p -> triples.add(ann.getRequiredProperty(p).asTriple()));
        triples.add(a.asTriple());

        OWLAnnotationProperty p = getAnnotationProperty(a.getPredicate().as(OntNAP.class));
        OWLAnnotationValue v = getAnnotationValue(a.getObject());

        Set<AxiomTranslator.Triples<OWLAnnotation>> children = a.annotations().map(RDF2OWLHelper::getHierarchicalAnnotations).collect(Collectors.toSet());
        OWLAnnotation res = new OWLAnnotationImpl(p, v, children.stream().map(AxiomTranslator.Triples::getObject));
        children.stream().map(AxiomTranslator.Triples::getTriples).forEach(triples::addAll);
        return new AxiomTranslator.Triples<>(res, triples);
    }

    public static OWLFacetRestriction getFacetRestriction(OntFR fr) {
        OWLLiteral literal = getLiteral(OntApiException.notNull(fr, "Null facet restriction.").getValue());
        if (OntFR.Length.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.LENGTH, literal);
        if (OntFR.MinLength.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.MIN_LENGTH, literal);
        if (OntFR.MaxLength.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.MAX_LENGTH, literal);
        if (OntFR.MinInclusive.class.isInstance(fr))
            return new OWLFacetRestrictionImpl(OWLFacet.MIN_INCLUSIVE, literal);
        if (OntFR.MaxInclusive.class.isInstance(fr))
            return new OWLFacetRestrictionImpl(OWLFacet.MAX_INCLUSIVE, literal);
        if (OntFR.MinExclusive.class.isInstance(fr))
            return new OWLFacetRestrictionImpl(OWLFacet.MIN_EXCLUSIVE, literal);
        if (OntFR.MaxExclusive.class.isInstance(fr))
            return new OWLFacetRestrictionImpl(OWLFacet.MAX_EXCLUSIVE, literal);
        if (OntFR.Pattern.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.PATTERN, literal);
        if (OntFR.FractionDigits.class.isInstance(fr))
            return new OWLFacetRestrictionImpl(OWLFacet.FRACTION_DIGITS, literal);
        if (OntFR.TotalDigits.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.TOTAL_DIGITS, literal);
        if (OntFR.LangRange.class.isInstance(fr)) return new OWLFacetRestrictionImpl(OWLFacet.LANG_RANGE, literal);
        throw new OntApiException("Unsupported facet restriction " + fr);
    }

    private static OWLDataRange fetchDataRange(OntDR dr) {
        if (dr == null)
            return new OWLDatatypeImpl(OWLRDFVocabulary.RDFS_LITERAL.getIRI());
        return getDataRange(dr);
    }

    public static OWLDataRange getDataRange(OntDR dr) {
        if (OntApiException.notNull(dr, "Null data range.").isURIResource()) {
            return getDatatype(dr.as(OntDT.class));
        }
        if (OntDR.Restriction.class.isInstance(dr)) {
            OntDR.Restriction _dr = (OntDR.Restriction) dr;
            return new OWLDatatypeRestrictionImpl(getDatatype(_dr.getDatatype()), _dr.facetRestrictions().map(RDF2OWLHelper::getFacetRestriction).collect(Collectors.toSet()));
        }
        if (OntDR.ComplementOf.class.isInstance(dr)) {
            OntDR.ComplementOf _dr = (OntDR.ComplementOf) dr;
            return new OWLDataComplementOfImpl(getDataRange(_dr.getDataRange()));
        }
        if (OntDR.UnionOf.class.isInstance(dr)) {
            OntDR.UnionOf _dr = (OntDR.UnionOf) dr;
            return new OWLDataUnionOfImpl(_dr.dataRanges().map(RDF2OWLHelper::getDataRange));
        }
        if (OntDR.IntersectionOf.class.isInstance(dr)) {
            OntDR.IntersectionOf _dr = (OntDR.IntersectionOf) dr;
            return new OWLDataIntersectionOfImpl(_dr.dataRanges().map(RDF2OWLHelper::getDataRange));
        }
        if (OntDR.OneOf.class.isInstance(dr)) {
            OntDR.OneOf _dr = (OntDR.OneOf) dr;
            return new OWLDataOneOfImpl(_dr.values().map(RDF2OWLHelper::getLiteral));
        }
        throw new OntApiException("Unsupported data range expression " + dr);
    }

    private static OWLClassExpression fetchClassExpression(OntCE ce) {
        if (ce == null)
            return new OWLClassImpl(OWLRDFVocabulary.OWL_THING.getIRI());
        return getClassExpression(ce);
    }

    public static OWLClassExpression getClassExpression(OntCE ce) {
        if (OntApiException.notNull(ce, "Null class expression.").isURIResource()) {
            return new OWLClassImpl(IRI.create(ce.getURI()));
        }
        if (OntCE.ObjectSomeValuesFrom.class.isInstance(ce)) {
            OntCE.ObjectSomeValuesFrom _ce = (OntCE.ObjectSomeValuesFrom) ce;
            return new OWLObjectSomeValuesFromImpl(getObjectProperty(_ce.getOnProperty()), getClassExpression(_ce.getValue()));
        }
        if (OntCE.DataSomeValuesFrom.class.isInstance(ce)) {
            OntCE.DataSomeValuesFrom _ce = (OntCE.DataSomeValuesFrom) ce;
            return new OWLDataSomeValuesFromImpl(getDataProperty(_ce.getOnProperty()), getDataRange(_ce.getValue()));
        }
        if (OntCE.ObjectAllValuesFrom.class.isInstance(ce)) {
            OntCE.ObjectAllValuesFrom _ce = (OntCE.ObjectAllValuesFrom) ce;
            return new OWLObjectAllValuesFromImpl(getObjectProperty(_ce.getOnProperty()), getClassExpression(_ce.getValue()));
        }
        if (OntCE.DataAllValuesFrom.class.isInstance(ce)) {
            OntCE.DataAllValuesFrom _ce = (OntCE.DataAllValuesFrom) ce;
            return new OWLDataAllValuesFromImpl(getDataProperty(_ce.getOnProperty()), getDataRange(_ce.getValue()));
        }
        if (OntCE.ObjectHasValue.class.isInstance(ce)) {
            OntCE.ObjectHasValue _ce = (OntCE.ObjectHasValue) ce;
            return new OWLObjectHasValueImpl(getObjectProperty(_ce.getOnProperty()), getIndividual(_ce.getValue()));
        }
        if (OntCE.DataHasValue.class.isInstance(ce)) {
            OntCE.DataHasValue _ce = (OntCE.DataHasValue) ce;
            return new OWLDataHasValueImpl(getDataProperty(_ce.getOnProperty()), getLiteral(_ce.getValue()));
        }
        if (OntCE.ObjectMinCardinality.class.isInstance(ce)) {
            OntCE.ObjectMinCardinality _ce = (OntCE.ObjectMinCardinality) ce;
            return new OWLObjectMinCardinalityImpl(getObjectProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchClassExpression(_ce.getValue()));
        }
        if (OntCE.DataMinCardinality.class.isInstance(ce)) {
            OntCE.DataMinCardinality _ce = (OntCE.DataMinCardinality) ce;
            return new OWLDataMinCardinalityImpl(getDataProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchDataRange(_ce.getValue()));
        }
        if (OntCE.ObjectMaxCardinality.class.isInstance(ce)) {
            OntCE.ObjectMaxCardinality _ce = (OntCE.ObjectMaxCardinality) ce;
            return new OWLObjectMaxCardinalityImpl(getObjectProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchClassExpression(_ce.getValue()));
        }
        if (OntCE.DataMaxCardinality.class.isInstance(ce)) {
            OntCE.DataMaxCardinality _ce = (OntCE.DataMaxCardinality) ce;
            return new OWLDataMaxCardinalityImpl(getDataProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchDataRange(_ce.getValue()));
        }
        if (OntCE.ObjectCardinality.class.isInstance(ce)) {
            OntCE.ObjectCardinality _ce = (OntCE.ObjectCardinality) ce;
            return new OWLObjectExactCardinalityImpl(getObjectProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchClassExpression(_ce.getValue()));
        }
        if (OntCE.DataCardinality.class.isInstance(ce)) {
            OntCE.DataCardinality _ce = (OntCE.DataCardinality) ce;
            return new OWLDataExactCardinalityImpl(getDataProperty(_ce.getOnProperty()), _ce.getCardinality(), fetchDataRange(_ce.getValue()));
        }
        if (OntCE.HasSelf.class.isInstance(ce)) {
            OntCE.HasSelf _ce = (OntCE.HasSelf) ce;
            return new OWLObjectHasSelfImpl(getObjectProperty(_ce.getOnProperty()));
        }
        if (OntCE.UnionOf.class.isInstance(ce)) {
            OntCE.UnionOf _ce = (OntCE.UnionOf) ce;
            return new OWLObjectUnionOfImpl(_ce.components().map(RDF2OWLHelper::getClassExpression));
        }
        if (OntCE.IntersectionOf.class.isInstance(ce)) {
            OntCE.IntersectionOf _ce = (OntCE.IntersectionOf) ce;
            return new OWLObjectIntersectionOfImpl(_ce.components().map(RDF2OWLHelper::getClassExpression));
        }
        if (OntCE.OneOf.class.isInstance(ce)) {
            OntCE.OneOf _ce = (OntCE.OneOf) ce;
            return new OWLObjectOneOfImpl(_ce.components().map(RDF2OWLHelper::getIndividual));
        }
        if (OntCE.ComplementOf.class.isInstance(ce)) {
            OntCE.ComplementOf _ce = (OntCE.ComplementOf) ce;
            return new OWLObjectComplementOfImpl(getClassExpression(_ce.getValue()));
        }
        throw new OntApiException("Unsupported class expression " + ce);
    }

    public static SWRLVariable getSWRLVariable(OntSWRL.Variable var) {
        if (!OntApiException.notNull(var, "Null swrl var").isURIResource()) {
            throw new OntApiException("Anonymous swrl var " + var);
        }
        // not public access:
        return OWL_DATA_FACTORY.getSWRLVariable(IRI.create(var.getURI()));
    }

    public static SWRLDArgument getSWRLiteralDArg(OntSWRL.DArg arg) {
        if (OntApiException.notNull(arg, "Null SWRL-D arg").isLiteral()) {
            return new SWRLLiteralArgumentImpl(getLiteral(arg.asLiteral()));
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return getSWRLVariable(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-D arg " + arg);
    }

    public static SWRLIArgument getSWRLIndividualArg(OntSWRL.IArg arg) {
        if (OntApiException.notNull(arg, "Null SWRL-I arg").canAs(OntIndividual.class)) {
            return new SWRLIndividualArgumentImpl(getIndividual(arg.as(OntIndividual.class)));
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return getSWRLVariable(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-I arg " + arg);
    }

    public static SWRLAtom getSWRLAtom(OntSWRL.Atom atom) {
        OntApiException.notNull(atom, "Null SWRL atom");
        if (OntSWRL.Atom.BuiltIn.class.isInstance(atom)) {
            OntSWRL.Atom.BuiltIn a = (OntSWRL.Atom.BuiltIn) atom;
            IRI i = IRI.create(a.getPredicate().getURI());
            return new SWRLBuiltInAtomImpl(i, a.arguments().map(RDF2OWLHelper::getSWRLiteralDArg).collect(Collectors.toList()));
        }
        if (OntSWRL.Atom.OntClass.class.isInstance(atom)) {
            OntSWRL.Atom.OntClass a = (OntSWRL.Atom.OntClass) atom;
            return new SWRLClassAtomImpl(getClassExpression(a.getPredicate()), getSWRLIndividualArg(a.getArg()));
        }
        if (OntSWRL.Atom.DataProperty.class.isInstance(atom)) {
            OntSWRL.Atom.DataProperty a = (OntSWRL.Atom.DataProperty) atom;
            return new SWRLDataPropertyAtomImpl(getDataProperty(a.getPredicate()), getSWRLIndividualArg(a.getFirstArg()), getSWRLiteralDArg(a.getSecondArg()));
        }
        if (OntSWRL.Atom.ObjectProperty.class.isInstance(atom)) {
            OntSWRL.Atom.ObjectProperty a = (OntSWRL.Atom.ObjectProperty) atom;
            return new SWRLObjectPropertyAtomImpl(getObjectProperty(a.getPredicate()), getSWRLIndividualArg(a.getFirstArg()), getSWRLIndividualArg(a.getSecondArg()));
        }
        if (OntSWRL.Atom.DataRange.class.isInstance(atom)) {
            OntSWRL.Atom.DataRange a = (OntSWRL.Atom.DataRange) atom;
            return new SWRLDataRangeAtomImpl(getDataRange(a.getPredicate()), getSWRLiteralDArg(a.getArg()));
        }
        if (OntSWRL.Atom.DifferentIndividuals.class.isInstance(atom)) {
            OntSWRL.Atom.DifferentIndividuals a = (OntSWRL.Atom.DifferentIndividuals) atom;
            OWLObjectProperty property = new OWLObjectPropertyImpl(IRI.create(OWL.differentFrom.getURI())); // it is not true object property.
            return new SWRLDifferentIndividualsAtomImpl(property, getSWRLIndividualArg(a.getFirstArg()), getSWRLIndividualArg(a.getSecondArg()));
        }
        if (OntSWRL.Atom.SameIndividuals.class.isInstance(atom)) {
            OntSWRL.Atom.SameIndividuals a = (OntSWRL.Atom.SameIndividuals) atom;
            OWLObjectProperty property = new OWLObjectPropertyImpl(IRI.create(OWL.sameAs.getURI())); // it is not true object property.
            return new SWRLSameIndividualAtomImpl(property, getSWRLIndividualArg(a.getFirstArg()), getSWRLIndividualArg(a.getSecondArg()));
        }
        throw new OntApiException("Unsupported SWRL atom " + atom);
    }

    /**
     * answers true if two nary axioms intersect, i.e. they have the same annotations and some components are included in both axioms.
     *
     * @param left  OWLNaryAxiom left axiom
     * @param right OWLNaryAxiom right axiom
     * @return true if axioms intersect.
     */
    public static boolean isIntersect(OWLNaryAxiom left, OWLNaryAxiom right) {
        if (!OWLAPIStreamUtils.equalStreams(left.annotations(), right.annotations())) return false;
        Set set1 = ((Stream<?>) left.operands()).collect(Collectors.toSet());
        Set set2 = ((Stream<?>) right.operands()).collect(Collectors.toSet());
        return !Collections.disjoint(set1, set2);
    }

    /**
     * A helper object, which helps
     * to find all (owl-)annotations and triples related to the specified statement.
     * todo: it seems it is incorrect - the set should contain main triple, declaration triple and annotations triples.
     */
    public static class AxiomStatement {
        private final OntStatement statement;
        private final Set<Triple> triples;
        private final Set<OWLAnnotation> annotations;

        public AxiomStatement(OntStatement main) {
            this.statement = OntApiException.notNull(main, "Null statement.");
            this.triples = new HashSet<>();
            this.annotations = new HashSet<>();
            triples.add(main.asTriple());
            Resource subject = main.getSubject();
            RDFNode object = main.getObject();
            Stream<Statement> associated;

            if (subject.isAnon()
                    //) { // todo: seems this place degrades the performance. need to change whole mechanism (separate to each translator)
                    && !subject.canAs(OntIndividual.Anonymous.class)) { // for anonymous axioms (e.g. disjoint all)
                associated = Models.getAssociatedStatements(subject).stream();
            } else if (object.isAnon()) { // e.g. anon class expression in statement subClassOf
                associated = Models.getAssociatedStatements(object.asResource()).stream();
            } else {
                associated = Stream.empty();
            }
            associated.map(Statement::asTriple).forEach(triples::add);
            RDF2OWLHelper.getAnnotations(main).forEach(a -> {
                triples.addAll(a.getTriples());
                annotations.add(a.getObject());
            });
        }

        public OntStatement getStatement() {
            return statement;
        }

        public Set<Triple> getTriples() {
            return triples;
        }

        public Set<OWLAnnotation> getAnnotations() {
            return annotations;
        }

    }

}
