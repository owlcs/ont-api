package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * TODO: add way to work with OntStatements.
 * Base Resource.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    @Override
    GraphModel getModel();

    /**
     * Returns the stream of all annotations attached to this object (not only to main-triple).
     * Each annotation could be plain (assertion) or bulk annotation (with/without sub-annotations).
     * <p>
     * According to OWL2-DL specification OntObject should be an uri-resource (i.e. not anonymous),
     * but we extend this behaviour for more generality.
     *
     * @return Stream of {@link OntAnnotation}s, each of them has as key {@link OntNAP} and as value any {@link RDFNode}.
     */
    Stream<OntAnnotation> annotations();

    OntAnnotation addAnnotation(OntNAP property, Resource uri);

    OntAnnotation addAnnotation(OntNAP property, Literal literal);

    OntAnnotation addAnnotation(OntNAP property, OntIndividual.Anonymous anon);

    default OntAnnotation addComment(String txt, String lang) {
        return addAnnotation(getModel().getRDFSComment(), ResourceFactory.createLangLiteral(txt, lang));
    }

    default OntAnnotation addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), ResourceFactory.createLangLiteral(txt, lang));
    }
}
