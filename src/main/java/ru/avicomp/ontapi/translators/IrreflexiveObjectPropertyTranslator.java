package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLIrreflexiveObjectPropertyAxiomImpl;

/**
 * Object property with owl:IrreflexiveProperty type
 *
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty" ({@link org.semanticweb.owlapi.model.AxiomType#IRREFLEXIVE_OBJECT_PROPERTY}), not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class IrreflexiveObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLIrreflexiveObjectPropertyAxiom, OntOPE> {
    @Override
    OWLIrreflexiveObjectPropertyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return new OWLIrreflexiveObjectPropertyAxiomImpl(ReadHelper.getObjectProperty(getSubject(statement)), annotations);
    }

    @Override
    Resource getType() {
        return OWL.IrreflexiveProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }
}
