package ru.avicomp.ontapi.translators;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;
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
        return new OWLAnnotationPropertyImpl(IRI.create(OntApiException.notNull(nap, "Null annotation property.").getURI()));
    }

    public static Collection<OWLAnnotation> getBulkAnnotations(OntObject object) {
        return getBulkAnnotations(OntApiException.notNull(object, "Null ont-object.").getRoot());
    }

    public static Collection<OWLAnnotation> getBulkAnnotations(OntStatement statement) {
        return statement.annotations()
                .filter(OntStatement::hasAnnotations)
                .map(RDF2OWLHelper::toOWLAnnotation).collect(Collectors.toSet());
    }

    public static Stream<OWLAnnotation> annotations(OntStatement statement) {
        return OntApiException.notNull(statement, "Null ont-statement.")
                .annotations()
                .map(RDF2OWLHelper::toOWLAnnotation);
    }

    private static OWLAnnotation toOWLAnnotation(OntStatement s) {
        return new OWLAnnotationImpl(getAnnotationProperty(s.getPredicate().as(OntNAP.class)), getAnnotationValue(s.getObject()), annotations(s));
    }
}
