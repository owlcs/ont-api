package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
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
        WriteHelper.writeTriple(model, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntOPE.class)
                .map(subj -> subj.inverseOf().map(obj -> subj.statement(OWL.inverseOf, obj)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLInverseObjectPropertiesAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntOPE f = statement.getSubject().as(OntOPE.class);
        OntOPE s = statement.getObject().as(OntOPE.class);
        return new OWLInverseObjectPropertiesAxiomImpl(ReadHelper.getObjectProperty(f), ReadHelper.getObjectProperty(s), annotations);
    }
}
