package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Base class to read and write axiom which is related to simple typed triple associated with object or data property.
 * List of sub-classes:
 * {@link FunctionalDataPropertyTranslator},
 * {@link FunctionalObjectPropertyTranslator},
 * {@link ReflexiveObjectPropertyTranslator},
 * {@link IrreflexiveObjectPropertyTranslator},
 * {@link AsymmetricObjectPropertyTranslator},
 * {@link SymmetricObjectPropertyTranslator},
 * {@link TransitiveObjectPropertyTranslator},
 * {@link InverseFunctionalObjectPropertyTranslator},
 * <p>
 * Created by @szuev on 28.09.2016.
 */
abstract class AbstractPropertyTypeTranslator<Axiom extends OWLAxiom & HasProperty, P extends OntPE> extends AxiomTranslator<Axiom> {

    abstract Resource getType();

    abstract Class<P> getView();

    P getSubject(OntStatement s) {
        return s.getSubject().as(getView());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(o -> o.statement(RDF.type, getType()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getProperty(), RDF.type, getType(), axiom.annotations());
    }
}
