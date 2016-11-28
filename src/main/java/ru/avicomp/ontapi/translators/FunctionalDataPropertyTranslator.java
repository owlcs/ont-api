package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLFunctionalDataPropertyAxiomImpl;

/**
 * example:
 * foaf:gender rdf:type owl:DatatypeProperty , owl:FunctionalProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class FunctionalDataPropertyTranslator extends AbstractFunctionalPropertyTranslator<OWLFunctionalDataPropertyAxiom> {

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontObjects(OntNDP.class).filter(OntObject::isLocal).filter(OntNDP::isFunctional)
                .collect(Collectors.toMap(RDF2OWLHelper::getDataProperty, p -> p.getStatement(RDF.type, OWL2.FunctionalProperty)));
    }

    @Override
    OWLFunctionalDataPropertyAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLFunctionalDataPropertyAxiomImpl((OWLDataPropertyExpression) object, annotations);
    }
}
