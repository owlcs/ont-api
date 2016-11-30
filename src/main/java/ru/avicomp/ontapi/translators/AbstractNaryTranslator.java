package ru.avicomp.ontapi.translators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * Base class for following axioms:
 *      EquivalentClasses ({@link EquivalentClassesTranslator}),
 *      EquivalentObjectProperties ({@link EquivalentObjectPropertiesTranslator}),
 *      EquivalentDataProperties ({@link EquivalentDataPropertiesTranslator}),
 *      SameIndividual ({@link SameIndividualTranslator}).
 * Also for {@link AbstractTwoWayNaryTranslator} with following subclasses:
 *      DisjointClasses ({@link DisjointClassesTranslator}),
 *      DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator}),
 *      DisjointDataProperties ({@link DisjointDataPropertiesTranslator}),
 *      DifferentIndividuals ({@link DifferentIndividualsTranslator}).
 * <p>
 * How to annotate see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
 * <p>
 * Created by szuev on 13.10.2016.
 */
abstract class AbstractNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>, OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AxiomTranslator<Axiom> {

    private void write(Axiom parentAxiom, OWLNaryAxiom<OWL> thisAxiom, OntGraphModel model) {
        OWLObject first = thisAxiom.operands().filter(e -> !e.isAnonymous()).findFirst().
                orElseThrow(() -> new OntApiException("Can't find a single non-anonymous expression inside " + thisAxiom));
        OWLObject rest = thisAxiom.operands().filter((obj) -> !first.equals(obj)).findFirst().
                orElseThrow(() -> new OntApiException("Should be at least two expressions inside " + thisAxiom));
        OWL2RDFHelper.writeTriple(model, first, getPredicate(), rest, parentAxiom.annotations(), true);
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        axiom.asPairwiseAxioms().forEach(a -> write(axiom, a, model));
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(o -> o.statements(getPredicate()))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }

    abstract Property getPredicate();

    abstract Class<ONT> getView();

    abstract Axiom create(Stream<OWL> components, Set<OWLAnnotation> annotations);

    Stream<ONT> components(OntStatement statement) {
        return Stream.of(statement.getSubject().as(getView()), statement.getObject().as(getView()));
    }

    private Map<Axiom, Set<Triple>> readPairwiseAxioms(OntGraphModel model) {
        return super.read(model);
    }

    private Set<Axiom> extractStandaloneAxioms(Set<Axiom> set) {
        return set.stream()
                .filter(a -> set.stream().filter(b -> !a.equals(b))
                        .noneMatch(b -> RDF2OWLHelper.isIntersect(a, b))).collect(Collectors.toSet());
    }

    private Map<Axiom, Set<Triple>> shrink(Map<Axiom, Set<Triple>> init) {
        Set<Axiom> unique = extractStandaloneAxioms(init.keySet());
        Map<Axiom, Set<Triple>> res = new HashMap<>();
        unique.forEach(a -> res.put(a, init.get(a)));
        if (res.size() == init.size()) return res;
        Map<Axiom, Set<Triple>> tmp = new HashMap<>(init);
        unique.forEach(tmp::remove);
        // assemble a single axiom of the remaining pairwise axioms
        Stream<OWLAnnotation> annotations = tmp.keySet().stream().map(HasAnnotations::annotations).findAny().orElse(Stream.empty());
        // do operands(stream)->set->stream to avoid BootstrapMethodError
        Stream<OWL> components = tmp.keySet().stream().map(axiom -> axiom.operands().collect(Collectors.toSet()).stream()).flatMap(Function.identity()).distinct();
        Set<Triple> triples = tmp.values().stream().map(Collection::stream).flatMap(Function.identity()).collect(Collectors.toSet());
        Axiom multi = create(components, annotations.collect(Collectors.toSet()));
        res.put(multi, triples);
        return res;
    }

    @Override
    public Map<Axiom, Set<Triple>> read(OntGraphModel model) {
        return shrink(readPairwiseAxioms(model));
    }
}
