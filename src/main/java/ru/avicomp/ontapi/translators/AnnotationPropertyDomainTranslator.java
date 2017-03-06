package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyDomainAxiomImpl;

/**
 * domain for annotation property.
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class AnnotationPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLAnnotationPropertyDomainAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    @Override
    OWLAnnotationPropertyDomainAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationProperty p = ReadHelper.getAnnotationProperty(statement.getSubject().as(getView()));
        IRI d = IRI.create(statement.getObject().asResource().getURI());
        return new OWLAnnotationPropertyDomainAxiomImpl(p, d, annotations);
    }

    /**
     * todo: invite config option to skip annotation domain in favor of other domain in case there is a punning with other property
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return super.statements(model);
    }

    @Override
    Wrap<OWLAnnotationPropertyDomainAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLAnnotationProperty> p = ReadHelper._getAnnotationProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<IRI> d = ReadHelper.wrapIRI(statement.getObject().as(OntObject.class));
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLAnnotationPropertyDomainAxiom res = getDataFactory().getOWLAnnotationPropertyDomainAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
