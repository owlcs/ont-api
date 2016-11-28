package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLReflexiveObjectPropertyAxiomImpl;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * example:
 * :ob-prop-1 rdf:type owl:ObjectProperty, owl:ReflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class ReflexiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLReflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLReflexiveObjectPropertyAxiom axiom) {
        return OWL2.ReflexiveProperty;
    }

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontObjects(OntOPE.class).filter(OntObject::isLocal).filter(OntOPE::isReflexive)
                .collect(Collectors.toMap(RDF2OWLHelper::getObjectProperty, p -> p.getStatement(RDF.type, OWL2.ReflexiveProperty)));
    }

    @Override
    OWLReflexiveObjectPropertyAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLReflexiveObjectPropertyAxiomImpl((OWLObjectPropertyExpression) object, annotations);
    }
}
