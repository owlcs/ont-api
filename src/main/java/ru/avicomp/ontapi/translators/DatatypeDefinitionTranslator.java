package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;

import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLDatatypeDefinitionAxiomImpl;

/**
 * example:
 * :data-type-3 rdf:type rdfs:Datatype ; owl:equivalentClass [ rdf:type rdfs:Datatype ; owl:unionOf ( :data-type-1  :data-type-2 ) ] .
 * <p>
 * Created by @szuev on 18.10.2016.
 */
class DatatypeDefinitionTranslator extends AxiomTranslator<OWLDatatypeDefinitionAxiom> {
    @Override
    public void write(OWLDatatypeDefinitionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getDatatype(), OWL.equivalentClass, axiom.getDataRange(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntDT.class)
                .map(p -> p.equivalentClass().map(r -> p.statement(OWL.equivalentClass, r)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLDatatypeDefinitionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLDatatype dt = ReadHelper.getDatatype(statement.getSubject().as(OntDT.class));
        OWLDataRange dr = ReadHelper.getDataRange(statement.getObject().as(OntDR.class));
        return new OWLDatatypeDefinitionAxiomImpl(dt, dr, annotations);
    }
}
