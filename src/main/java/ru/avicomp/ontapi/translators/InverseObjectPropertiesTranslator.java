package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLInverseObjectPropertiesAxiomImpl;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getFirstProperty(), OWL2.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntOPE.class)
                .map(subj -> subj.inverseOf().map(obj -> subj.getStatement(OWL2.inverseOf, obj)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLInverseObjectPropertiesAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntOPE f = statement.getSubject().as(OntOPE.class);
        OntOPE s = statement.getObject().as(OntOPE.class);
        return new OWLInverseObjectPropertiesAxiomImpl(RDF2OWLHelper.getObjectProperty(f), RDF2OWLHelper.getObjectProperty(s), annotations);
    }
}
