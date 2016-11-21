package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.IsAnonymous;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntException;

/**
 * Base class for following axioms:
 *  EquivalentClasses ({@link EquivalentClassesTranslator}),
 *  EquivalentObjectProperties ({@link EquivalentObjectPropertiesTranslator}),
 *  EquivalentDataProperties ({@link EquivalentDataPropertiesTranslator}),
 *  SameIndividual ({@link SameIndividualTranslator}).
 *
 *  How to annotate see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
 *
 * Created by szuev on 13.10.2016.
 */
abstract class AbstractNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<? extends IsAnonymous>> extends AxiomTranslator<Axiom> {

    private void process(Axiom parentAxiom, OWLNaryAxiom<? extends IsAnonymous> thisAxiom, Graph graph) {
        OWLObject first = thisAxiom.operands().filter(e -> !e.isAnonymous()).findFirst().
                orElseThrow(() -> new OntException("Can't find a single non-anonymous expression inside " + thisAxiom));
        OWLObject rest = thisAxiom.operands().filter((obj) -> !first.equals(obj)).findFirst().
                orElseThrow(() -> new OntException("Should be at least two expressions inside " + thisAxiom));
        TranslationHelper.processAnnotatedTriple(graph, first, getPredicate(), rest, parentAxiom, true);
    }

    @Override
    public void write(Axiom axiom, Graph graph) {
        axiom.asPairwiseAxioms().forEach(a -> process(axiom, a, graph));
    }

    public abstract Property getPredicate();
}
