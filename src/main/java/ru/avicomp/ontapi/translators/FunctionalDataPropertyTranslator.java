package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL2;
import uk.ac.manchester.cs.owl.owlapi.OWLFunctionalDataPropertyAxiomImpl;

/**
 * example:
 * foaf:gender rdf:type owl:DatatypeProperty , owl:FunctionalProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class FunctionalDataPropertyTranslator extends AbstractPropertyTypeTranslator<OWLFunctionalDataPropertyAxiom, OntNDP> {
    @Override
    Resource getType() {
        return OWL2.FunctionalProperty;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLFunctionalDataPropertyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return new OWLFunctionalDataPropertyAxiomImpl(RDF2OWLHelper.getDataProperty(getSubject(statement)), annotations);
    }
}
