package ru.avicomp.ontapi.translators;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLSubPropertyChainAxiomImpl;

/**
 * base class : {@link AbstractSubChainedTranslator}
 * for SubPropertyChainOf axiom
 * example: owl:topObjectProperty owl:propertyChainAxiom ( :ob-prop-1 :ob-prop-2 ) .
 * <p>
 * Created by @szuev on 18.10.2016.
 */
class SubPropertyChainOfTranslator extends AbstractSubChainedTranslator<OWLSubPropertyChainOfAxiom, OntOPE> {
    @Override
    OWLObject getSubject(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Property getPredicate() {
        return OWL.propertyChainAxiom;
    }

    @Override
    Stream<? extends OWLObject> getObjects(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getPropertyChain().stream();
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    OWLSubPropertyChainOfAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntOPE subject = statement.getSubject().as(OntOPE.class);
        List<OWLObjectPropertyExpression> children = subject.superPropertyOf().map(ReadHelper::getObjectProperty).collect(Collectors.toList());
        return new OWLSubPropertyChainAxiomImpl(children, ReadHelper.getObjectProperty(subject), annotations);
    }
}
