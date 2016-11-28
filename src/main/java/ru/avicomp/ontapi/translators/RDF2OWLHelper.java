package ru.avicomp.ontapi.translators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

/**
 * Helper to translate rdf-graph to the axioms.
 * TODO:
 * <p>
 * Created by @szuev on 25.11.2016.
 */
public class RDF2OWLHelper {

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
        String label = NodeFmtLib.encodeBNodeLabel(OntApiException.notNull(individual, "Null individual.").asNode().getBlankNodeLabel());
        return new OWLAnonymousIndividualImpl(NodeID.getNodeID(label));
    }

    public static OWLLiteral getLiteral(Literal literal) {
        String txt = OntApiException.notNull(literal, "Null literal").getLexicalForm();
        String lang = literal.getLanguage();
        OWLDatatype dt = new OWLDatatypeImpl(IRI.create(literal.getDatatypeURI()));
        return new OWLLiteralImpl(txt, lang, dt);
    }

    public static OWLAnnotationValue getAnnotationValue(RDFNode node) {
        if (OntApiException.notNull(node, "Null node").isLiteral()) {
            return getLiteral(node.asLiteral());
        }
        if (node.isURIResource()) {
            return IRI.create(node.asResource().getURI());
        }
        if (node.canAs(OntIndividual.Anonymous.class)) {
            return getAnonymousIndividual(node.as(OntIndividual.Anonymous.class));
        }
        throw new OntApiException("Not an OWLAnnotationValue " + node);
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

    public static Set<OWLTripleSet<OWLAnnotation>> getBulkAnnotations(OntObject object) {
        return getBulkAnnotations(OntApiException.notNull(object, "Null ont-object.").getRoot());
    }

    public static Set<OWLTripleSet<OWLAnnotation>> getBulkAnnotations(OntStatement statement) {
        return statement.annotations()
                .filter(OntStatement::hasAnnotations)
                .map(RDF2OWLHelper::getHierarchicalAnnotations).collect(Collectors.toSet());
    }

    private static OWLTripleSet<OWLAnnotation> getHierarchicalAnnotations(OntStatement a) {
        OntObject ann = a.getSubject().as(OntObject.class);
        Set<Triple> triples = new HashSet<>();
        Stream.of(RDF.type, OWL2.annotatedSource, OWL2.annotatedProperty, OWL2.annotatedTarget)
                .forEach(p -> triples.add(ann.getRequiredProperty(p).asTriple()));
        triples.add(a.asTriple());

        OWLAnnotationProperty p = getAnnotationProperty(a.getPredicate().as(OntNAP.class));
        OWLAnnotationValue v = getAnnotationValue(a.getObject());
        Set<OWLTripleSet<OWLAnnotation>> children = a.annotations().map(RDF2OWLHelper::getHierarchicalAnnotations).collect(Collectors.toSet());
        OWLAnnotation res = new OWLAnnotationImpl(p, v, children.stream().map(OWLTripleSet::getObject));
        triples.addAll(children.stream().map(OWLTripleSet::getTriples).map(Collection::stream).flatMap(Function.identity()).collect(Collectors.toSet()));
        return new OWLTripleSet<>(res, triples);
    }

}
