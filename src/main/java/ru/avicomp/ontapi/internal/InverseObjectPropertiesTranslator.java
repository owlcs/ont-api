package ru.avicomp.ontapi.internal;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
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
    public Stream<OntStatement> statements(OntGraphModel model) {
        // NOTE as a precaution: the first (commented) way is not correct
        // since it includes anonymous object property expressions (based on owl:inverseOf),
        // which could be treat as separated axioms, but OWL-API doesn't think so.
        /*return model.statements(null, OWL.inverseOf, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(OntOPE.class))
                .filter(s -> s.getObject().canAs(OntOPE.class));*/
        return model.ontObjects(OntOPE.class)
                .map(subj -> subj.inverseOf().map(obj -> subj.statement(OWL.inverseOf, obj)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        if (!statement.getPredicate().equals(OWL.inverseOf) || !statement.getObject().isResource()) return false;
        OntObject subject = statement.getSubject();
        OntObject object = statement.getObject().as(OntObject.class);
        // to not take into account the object property expressions:
        return (subject.isURIResource() || subject.hasType(OWL.ObjectProperty))
                && (object.isURIResource() || object.hasType(OWL.ObjectProperty));
    }

    @Override
    public Wrap<OWLInverseObjectPropertiesAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLObjectPropertyExpression> f = ReadHelper.fetchObjectPropertyExpression(statement.getSubject().as(OntOPE.class), conf.dataFactory());
        Wrap<? extends OWLObjectPropertyExpression> s = ReadHelper.fetchObjectPropertyExpression(statement.getObject().as(OntOPE.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLInverseObjectPropertiesAxiom res = conf.dataFactory().getOWLInverseObjectPropertiesAxiom(f.getObject(), s.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(f).append(s);
    }
}
