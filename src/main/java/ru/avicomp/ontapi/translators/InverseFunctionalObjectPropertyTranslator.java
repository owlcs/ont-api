package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLInverseFunctionalObjectPropertyAxiomImpl;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class InverseFunctionalObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLInverseFunctionalObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        return OWL.InverseFunctionalProperty;
    }

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontObjects(OntOPE.class).filter(OntObject::isLocal).filter(OntOPE::isInverseFunctional)
                .collect(Collectors.toMap(RDF2OWLHelper::getObjectProperty, p -> p.getStatement(RDF.type, OWL2.InverseFunctionalProperty)));
    }

    @Override
    OWLInverseFunctionalObjectPropertyAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLInverseFunctionalObjectPropertyAxiomImpl((OWLObjectPropertyExpression) object, annotations);
    }
}
