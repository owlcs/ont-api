package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * domain for annotation property.
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class AnnotationPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLAnnotationPropertyDomainAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLAnnotationPropertyDomainAxiom} axiom.
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        OntLoaderConfiguration conf = getConfig(model).loaderConfig();
        if (!conf.isLoadAnnotationAxioms()) return Stream.empty();
        return super.statements(model)
                .filter(s -> s.getObject().isURIResource())
                .filter(s -> ReadHelper.testAnnotationAxiomOverlaps(s, conf, AxiomType.OBJECT_PROPERTY_DOMAIN, AxiomType.DATA_PROPERTY_DOMAIN));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return super.testStatement(statement) && statement.getObject().isURIResource();
    }

    @Override
    public InternalObject<OWLAnnotationPropertyDomainAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<OWLAnnotationProperty> p = ReadHelper.fetchAnnotationProperty(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<IRI> d = ReadHelper.wrapIRI(statement.getObject().as(OntObject.class));
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLAnnotationPropertyDomainAxiom res = conf.dataFactory().getOWLAnnotationPropertyDomainAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
