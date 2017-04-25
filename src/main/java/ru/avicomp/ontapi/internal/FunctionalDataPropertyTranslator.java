package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * example:
 * foaf:gender rdf:type owl:DatatypeProperty , owl:FunctionalProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class FunctionalDataPropertyTranslator extends AbstractPropertyTypeTranslator<OWLFunctionalDataPropertyAxiom, OntNDP> {
    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    public Wrap<OWLFunctionalDataPropertyAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<OWLDataProperty> p = ReadHelper.fetchDataProperty(getSubject(statement), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLFunctionalDataPropertyAxiom res = conf.dataFactory().getOWLFunctionalDataPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
