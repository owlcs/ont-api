package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLTransitiveObjectPropertyAxiomImpl;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * Example:
 * gr:equal rdf:type owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class TransitiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLTransitiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLTransitiveObjectPropertyAxiom axiom) {
        return OWL.TransitiveProperty;
    }

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontObjects(OntOPE.class).filter(OntObject::isLocal).filter(OntOPE::isTransitive)
                .collect(Collectors.toMap(RDF2OWLHelper::getObjectProperty, p -> p.getStatement(RDF.type, OWL2.TransitiveProperty)));
    }

    @Override
    OWLTransitiveObjectPropertyAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLTransitiveObjectPropertyAxiomImpl((OWLObjectPropertyExpression) object, annotations);
    }
}
