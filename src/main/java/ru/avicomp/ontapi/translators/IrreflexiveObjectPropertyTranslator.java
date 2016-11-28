package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLIrreflexiveObjectPropertyAxiomImpl;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty", not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class IrreflexiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLIrreflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLIrreflexiveObjectPropertyAxiom axiom) {
        return OWL2.IrreflexiveProperty;
    }

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontObjects(OntOPE.class).filter(OntObject::isLocal).filter(OntOPE::isIrreflexive)
                .collect(Collectors.toMap(RDF2OWLHelper::getObjectProperty, p -> p.getStatement(RDF.type, OWL2.IrreflexiveProperty)));
    }

    @Override
    OWLIrreflexiveObjectPropertyAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLIrreflexiveObjectPropertyAxiomImpl((OWLObjectPropertyExpression) object, annotations);
    }
}
