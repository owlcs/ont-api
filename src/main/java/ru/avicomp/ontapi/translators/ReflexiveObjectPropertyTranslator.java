package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLReflexiveObjectPropertyAxiomImpl;

/**
 * base classes {@link AbstractPropertyTypeTranslator}
 * example:
 * :ob-prop-1 rdf:type owl:ObjectProperty, owl:ReflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class ReflexiveObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLReflexiveObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL2.ReflexiveProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    OWLReflexiveObjectPropertyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return new OWLReflexiveObjectPropertyAxiomImpl(RDF2OWLHelper.getObjectProperty(getSubject(statement)), annotations);
    }
}
