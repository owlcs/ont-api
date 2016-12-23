package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLHasKeyAxiomImpl;

/**
 * base class : {@link AbstractSubChainedTranslator}
 * for HasKey axiom.
 * example:
 * :MyClass1 owl:hasKey ( :ob-prop-1 ) .
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class HasKeyTranslator extends AbstractSubChainedTranslator<OWLHasKeyAxiom, OntCE> {
    @Override
    OWLObject getSubject(OWLHasKeyAxiom axiom) {
        return axiom.getClassExpression();
    }

    @Override
    Property getPredicate() {
        return OWL.hasKey;
    }

    @Override
    Stream<? extends OWLObject> getObjects(OWLHasKeyAxiom axiom) {
        return axiom.propertyExpressions();
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
    }

    @Override
    OWLHasKeyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntCE subject = statement.getSubject().as(OntCE.class);
        Set<OWLPropertyExpression> properties = subject.hasKey().map(RDF2OWLHelper::getProperty).collect(Collectors.toSet());
        return new OWLHasKeyAxiomImpl(RDF2OWLHelper.getClassExpression(subject), properties, annotations);
    }
}
