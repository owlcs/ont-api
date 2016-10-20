package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.SWRLRule;

/**
 * for "Rule" Axiom {@link org.semanticweb.owlapi.model.AxiomType#SWRL_RULE}
 * Specification: <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 */
class SWRLRuleParser extends AxiomParser<SWRLRule> {
    @Override
    public void process(Graph graph) {
        //TODO:
        // annotation as for anonymous node
        getAxiom().head();
    }

}
