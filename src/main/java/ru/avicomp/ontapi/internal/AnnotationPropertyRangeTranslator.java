package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntConfig;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class {@link AbstractPropertyRangeTranslator}
 * Note: OWL Axiom Type is "AnnotationPropertyRangeOf", not "AnnotationPropertyRange"
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class AnnotationPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLAnnotationPropertyRangeAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLAnnotationPropertyRangeAxiom} axiom.
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        OntConfig.LoaderConfiguration conf = getConfig(model).loaderConfig();
        if (!conf.isLoadAnnotationAxioms()) return Stream.empty();
        return super.statements(model)
                .filter(s -> s.getObject().isURIResource())
                .filter(s -> ReadHelper.testAnnotationAxiomOverlaps(s, conf,
                        AxiomType.OBJECT_PROPERTY_RANGE, AxiomType.DATA_PROPERTY_RANGE));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return super.testStatement(statement) && statement.getObject().isURIResource();
    }

    @Override
    public Wrap<OWLAnnotationPropertyRangeAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<OWLAnnotationProperty> p = ReadHelper.fetchAnnotationProperty(statement.getSubject().as(getView()), conf.dataFactory());
        Wrap<IRI> d = ReadHelper.wrapIRI(statement.getObject().as(OntObject.class));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLAnnotationPropertyRangeAxiom res = conf.dataFactory().getOWLAnnotationPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
