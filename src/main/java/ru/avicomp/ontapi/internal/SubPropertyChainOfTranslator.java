package ru.avicomp.ontapi.internal;

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
    public Wrap<OWLSubPropertyChainOfAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntOPE ope = statement.getSubject().as(OntOPE.class);
        Wrap<? extends OWLObjectPropertyExpression> subject = ReadHelper.fetchObjectPropertyExpression(ope, conf.dataFactory());
        Wrap.Collection<? extends OWLObjectPropertyExpression> members = Wrap.Collection.create(ope.superPropertyOf().map(s -> ReadHelper.fetchObjectPropertyExpression(s, conf.dataFactory())));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        // note: the input is a list. does it mean that the order is important?
        OWLSubPropertyChainOfAxiom res = conf.dataFactory().getOWLSubPropertyChainOfAxiom(members.objects().collect(Collectors.toList()), subject.getObject(), annotations.getObjects());
        return Wrap.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
