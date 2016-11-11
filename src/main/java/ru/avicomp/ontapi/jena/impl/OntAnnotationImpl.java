package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL2;

import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntAnnotation;

/**
 * for OWL2 Annotations
 * see e.g. <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 * <p>
 * Created by @szuev on 11.11.2016.
 */
public class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {

    private static final OntFinder ANNOTATION_FINDER = g -> JenaUtils.asStream(g.asGraph().find(Node.ANY, RDF_TYPE, OWL2.Axiom.asNode()).
            andThen(g.asGraph().find(Node.ANY, RDF_TYPE, OWL2.Annotation.asNode())).mapWith(Triple::getSubject)).distinct();

    public static OntObjectFactory factory = new CommonOntObjectFactory(new OntMaker.Default(OntAnnotationImpl.class),
            ANNOTATION_FINDER, OntFilter.BLANK.and(new OntFilter.HasType(OWL2.Axiom).or(new OntFilter.HasType(OWL2.Annotation))));

    public OntAnnotationImpl(Node n, EnhGraph m) {
        super(n, m);
    }

}
