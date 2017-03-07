package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

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
    Wrap<OWLHasKeyAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        OntCE ce = statement.getSubject().as(OntCE.class);
        Wrap<? extends OWLClassExpression> subject = ReadHelper.getClassExpression(ce, df);
        Wrap.Collection<? extends OWLPropertyExpression> members = Wrap.Collection.create(ce.hasKey()
                .filter(p -> p.canAs(OntOPE.class) || p.canAs(OntNDP.class)) // only P or R (!)
                .map(p -> ReadHelper.getProperty(p, df)));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLHasKeyAxiom res = df.getOWLHasKeyAxiom(subject.getObject(), members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
