package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.*;

/**
 * Examples:
 * foaf:LabelProperty vs:term_status "unstable" .
 * foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 * pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionTranslator extends AxiomTranslator<OWLAnnotationAssertionAxiom> {
    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom.annotations());
    }

    /**
     * annotation assertion: the rule "s A t":
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Annotations</a>
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        OntID id = model.getID();
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isAnnotation)
                .filter(s -> testAnnotationSubject(s.getSubject(), id));
    }

    private static boolean testAnnotationSubject(Resource candidate, OntID id) {
        return !candidate.equals(id) && (candidate.isURIResource() || candidate.canAs(OntIndividual.Anonymous.class));
    }

    @Override
    Wrap<OWLAnnotationAssertionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLAnnotationSubject> s = ReadHelper.getAnnotationSubject(statement.getSubject(), df);
        Wrap<OWLAnnotationProperty> p = ReadHelper.getAnnotationProperty(statement.getPredicate().as(OntNAP.class), df);
        Wrap<? extends OWLAnnotationValue> v = ReadHelper.getAnnotationValue(statement.getObject(), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLAnnotationAssertionAxiom res = df.getOWLAnnotationAssertionAxiom(p.getObject(), s.getObject(), v.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(s).append(p).append(v);
    }

}
