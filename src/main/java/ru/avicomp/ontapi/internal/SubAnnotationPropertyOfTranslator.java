package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractSubPropertyTranslator}
 *
 * Created by @szuev on 30.09.2016.
 */
class SubAnnotationPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubAnnotationPropertyOfAxiom, OntNAP> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLSubAnnotationPropertyOfAxiom} axiom.
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        OntLoaderConfiguration conf = getConfig(model).loaderConfig();
        if (!conf.isLoadAnnotationAxioms()) return Stream.empty();
        return super.statements(model)
                .filter(s -> ReadHelper.testAnnotationAxiomOverlaps(s, conf,
                        AxiomType.SUB_OBJECT_PROPERTY, AxiomType.SUB_DATA_PROPERTY));
    }

    @Override
    public Wrap<OWLSubAnnotationPropertyOfAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<OWLAnnotationProperty> sub = ReadHelper.fetchAnnotationProperty(statement.getSubject().as(OntNAP.class), conf.dataFactory());
        Wrap<OWLAnnotationProperty> sup = ReadHelper.fetchAnnotationProperty(statement.getObject().as(OntNAP.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLSubAnnotationPropertyOfAxiom res = conf.dataFactory().getOWLSubAnnotationPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
