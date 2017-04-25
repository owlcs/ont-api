package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Object property with owl:IrreflexiveProperty type
 *
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty" ({@link org.semanticweb.owlapi.model.AxiomType#IRREFLEXIVE_OBJECT_PROPERTY}), not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
public class IrreflexiveObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLIrreflexiveObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.IrreflexiveProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public Wrap<OWLIrreflexiveObjectPropertyAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.fetchObjectPropertyExpression(getSubject(statement), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLIrreflexiveObjectPropertyAxiom res = conf.dataFactory().getOWLIrreflexiveObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
