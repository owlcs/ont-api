package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLAsymmetricObjectPropertyAxiomImpl;

/**
 * object property with owl:AsymmetricProperty type
 *
 * Created by @szuev on 18.10.2016.
 */
class AsymmetricObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLAsymmetricObjectPropertyAxiom, OntOPE> {

    @Override
    OWLAsymmetricObjectPropertyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return new OWLAsymmetricObjectPropertyAxiomImpl(RDF2OWLHelper.getObjectProperty(getSubject(statement)), annotations);
    }

    @Override
    Resource getType() {
        return OWL2.AsymmetricProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }
}
