package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link SubObjectPropertyOfTranslator}, {@link SubDataPropertyOfTranslator} and {@link SubAnnotationPropertyOfTranslator}
 * Example:
 * foaf:msnChatID  rdfs:subPropertyOf foaf:nick .
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractSubPropertyTranslator<Axiom extends OWLAxiom, P extends OntPE> extends AxiomTranslator<Axiom> {

    abstract OWLPropertyExpression getSubProperty(Axiom axiom);

    abstract OWLPropertyExpression getSuperProperty(Axiom axiom);

    abstract Class<P> getView();

    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(p -> p.subPropertyOf().map(r -> p.statement(RDFS.subPropertyOf, r)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, getSubProperty(axiom), RDFS.subPropertyOf, getSuperProperty(axiom), axiom.annotations());
    }
}
