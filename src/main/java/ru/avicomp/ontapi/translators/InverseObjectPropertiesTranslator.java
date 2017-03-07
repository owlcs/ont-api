package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntOPE.class)
                .map(subj -> subj.inverseOf().map(obj -> subj.statement(OWL.inverseOf, obj)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    Wrap<OWLInverseObjectPropertiesAxiom> asAxiom(OntStatement statement) {
        Wrap<? extends OWLObjectPropertyExpression> f = ReadHelper._getObjectProperty(statement.getSubject().as(OntOPE.class), getDataFactory());
        Wrap<? extends OWLObjectPropertyExpression> s = ReadHelper._getObjectProperty(statement.getObject().as(OntOPE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLInverseObjectPropertiesAxiom res = getDataFactory().getOWLInverseObjectPropertiesAxiom(f.getObject(), s.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(f).append(s);
    }
}
