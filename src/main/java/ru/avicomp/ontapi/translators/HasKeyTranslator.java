package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntOPE;
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
        Set<OWLPropertyExpression> properties = subject.hasKey().map(ReadHelper::getProperty).collect(Collectors.toSet());
        return new OWLHasKeyAxiomImpl(ReadHelper.getClassExpression(subject), properties, annotations);
    }

    @Override
    Wrap<OWLHasKeyAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        OntCE ce = statement.getSubject().as(OntCE.class);
        Wrap<? extends OWLClassExpression> subject = ReadHelper._getClassExpression(ce, df);
        Wrap.Collection<? extends OWLPropertyExpression> members = Wrap.Collection.create(ce.hasKey()
                .filter(p -> p.canAs(OntOPE.class) || p.canAs(OntNDP.class)) // only P or R (!)
                .map(p -> ReadHelper._getProperty(p, df)));
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLHasKeyAxiom res = df.getOWLHasKeyAxiom(subject.getObject(), members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
