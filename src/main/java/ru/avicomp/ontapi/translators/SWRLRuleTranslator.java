package ru.avicomp.ontapi.translators;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntSWRL;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.SWRLRuleImpl;

/**
 * for "Rule" Axiom {@link org.semanticweb.owlapi.model.AxiomType#SWRL_RULE}
 * Specification: <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 */
class SWRLRuleTranslator extends AxiomTranslator<SWRLRule> {
    @Override
    public void write(SWRLRule axiom, OntGraphModel model) {
        Stream<OntSWRL.Atom> head = axiom.head().map(atom -> OWL2RDFHelper.addSWRLAtom(model, atom));
        Stream<OntSWRL.Atom> body = axiom.body().map(atom -> OWL2RDFHelper.addSWRLAtom(model, atom));
        OWL2RDFHelper.addAnnotations(model.createSWRLImp(head.collect(Collectors.toList()), body.collect(Collectors.toList())), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntSWRL.Imp.class).filter(OntObject::isLocal).map(OntObject::getRoot);
    }

    @Override
    SWRLRule create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntSWRL.Imp imp = statement.getSubject().as(OntSWRL.Imp.class);
        List<SWRLAtom> head = imp.head().map(RDF2OWLHelper::getSWRLAtom).collect(Collectors.toList());
        List<SWRLAtom> body = imp.body().map(RDF2OWLHelper::getSWRLAtom).collect(Collectors.toList());
        return new SWRLRuleImpl(head, body, annotations);
    }
}
