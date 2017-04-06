package ru.avicomp.ontapi.internal;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntSWRL;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * for "Rule" Axiom {@link org.semanticweb.owlapi.model.AxiomType#SWRL_RULE}
 * Specification: <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 */
class SWRLRuleTranslator extends AxiomTranslator<SWRLRule> {
    @Override
    public void write(SWRLRule axiom, OntGraphModel model) {
        Stream<OntSWRL.Atom> head = axiom.head().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        Stream<OntSWRL.Atom> body = axiom.body().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        WriteHelper.addAnnotations(model.createSWRLImp(head.collect(Collectors.toList()), body.collect(Collectors.toList())), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntSWRL.Imp.class).filter(OntObject::isLocal).map(OntObject::getRoot);
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getSubject().canAs(OntSWRL.Imp.class);
    }

    @Override
    public Wrap<SWRLRule> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntSWRL.Imp imp = statement.getSubject().as(OntSWRL.Imp.class);

        Wrap.Collection<? extends SWRLAtom> head = Wrap.Collection.create(imp.head().map(a -> ReadHelper.getSWRLAtom(a, conf.dataFactory())));
        Wrap.Collection<? extends SWRLAtom> body = Wrap.Collection.create(imp.body().map(a -> ReadHelper.getSWRLAtom(a, conf.dataFactory())));

        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        SWRLRule res = conf.dataFactory().getSWRLRule(body.objects().collect(Collectors.toList()), head.objects().collect(Collectors.toList()), annotations.getObjects());
        return Wrap.create(res, imp.content()).add(annotations.getTriples()).add(body.getTriples()).add(head.getTriples());
    }
}
