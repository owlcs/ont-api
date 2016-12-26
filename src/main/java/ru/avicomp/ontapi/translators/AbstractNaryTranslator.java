package ru.avicomp.ontapi.translators;

import java.util.*;
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

    private void write(OWLNaryAxiom<OWL> thisAxiom, Stream<OWLAnnotation> annotations, OntGraphModel model) {
        OWLObject first = thisAxiom.operands().filter(e -> !e.isAnonymous()).findFirst().
                orElseThrow(() -> new OntApiException("Can't find a single non-anonymous expression inside " + thisAxiom));
        OWLObject rest = thisAxiom.operands().filter((obj) -> !first.equals(obj)).findFirst().
                orElseThrow(() -> new OntApiException("Should be at least two expressions inside " + thisAxiom));
        OWL2RDFHelper.writeTriple(model, first, getPredicate(), rest, annotations, true);
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        axiom.asPairwiseAxioms().forEach(a -> write(a, axiom.annotations(), model));
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
        Map<Axiom, Set<Triple>> init = super.read(model);
        Map<Axiom, Set<Triple>> res = new HashMap<>();
        init.keySet().forEach(axiom -> {
            Set<Triple> value = init.get(axiom);
            axiom.splitToAnnotatedPairs().forEach(a -> {
                //noinspection unchecked
                res.put((Axiom) a, value);
            });
        });
        return res;
    }

    /**
     * Compresses collection of nary axioms to more compact form.
     * <p>
     * The mechanism is the same for all kind of nary-axioms with except of SameAs axiom.
     * Pairwise axioms could be merged to one if and only if they have the same annotations and mutually complement each other,
     * i.e. three pairwise axioms {a, b}, {a, c}, {b, c} equivalent one axiom {a, b, c}.
     * Example: classes 'A', 'B', 'C' are mutually disjoint if and only if each pair is disjoint ('A'-'B', 'B'-'C' and 'A'-'C')
     *
     * @param init initial Map with Axioms as keys and Set of Triple as values.
     * @return shrunken map of axioms.
     */
    Map<Axiom, Set<Triple>> shrink(Map<Axiom, Set<Triple>> init) {
        if (init.size() < 2) {
            return new HashMap<>(init);
        }
        Map<Set<OWLAnnotation>, Set<Axiom>> groupedByAnnotations =
                init.keySet().stream().collect(Collectors.groupingBy(a -> a.annotations().collect(Collectors.toSet()), Collectors.toSet()));
        Map<Axiom, Set<Triple>> res = new HashMap<>();
        for (Set<OWLAnnotation> annotations : groupedByAnnotations.keySet()) {
            Set<Axiom> compressed = shrink(groupedByAnnotations.get(annotations), annotations);
            compressed.forEach(axiom -> {
                //noinspection SuspiciousMethodCalls
                Set<Triple> value = axiom.splitToAnnotatedPairs().stream()
                        .map(a -> init.get(a).stream()).flatMap(Function.identity()).collect(Collectors.toSet());
                res.put(axiom, value);
            });
        }
        return res;
    }

    /**
     * Examples:
     * {a, b}, {a, d}, {b, c}                           -> {d, a}, {b, c}, {a, b}
     * {a, b}, {a, c}, {b, c}, {g, a}                   -> {a, g}, {a, b, c}
     * {a, b}, {a, c}, {a, d}, {b, c}, {b, d}, {c, d}   -> {a, b, c, d}
     * {a, b}, {a, c}, {a, d}, {b, c}, {b, f}, {f, c}   -> {a, b, c}, {b, c, f}, {d, a}
     *
     * @param init        Set of pairwise Axioms (each should contain only two operands)
     * @param annotations Set of OWLAnnotations
     * @return new Set of Axioms (see examples)
     */
    private Set<Axiom> shrink(Set<Axiom> init, Set<OWLAnnotation> annotations) {
        if (init.isEmpty()) return Collections.emptySet();
        Set<Axiom> res = new HashSet<>();
        if (init.size() == 1) {
            res.addAll(init);
            return res;
        }
        List<Axiom> split = new LinkedList<>(init);
        Axiom first = create(split.remove(0).operands(), annotations);
        while (!split.isEmpty()) {
            Axiom next = split.remove(0);
            // do operands(stream)->set->stream to avoid BootstrapMethodError
            Stream<OWL> operands = Stream.of(first.operands().collect(Collectors.toSet()), next.operands().collect(Collectors.toSet()))
                    .map(Collection::stream).flatMap(Function.identity());
            Axiom candidate = create(operands, annotations);
            if (init.containsAll(candidate.asPairwiseAxioms())) {
                first = candidate;
            } else {
                //noinspection SuspiciousMethodCalls
                split.removeAll(first.asPairwiseAxioms());
                res.add(first);
                first = create(next.operands(), annotations);
            }
            if (split.isEmpty()) {
                res.add(first);
            }
        }
        return res;
    }

    @Override
    public Map<Axiom, Set<Triple>> read(OntGraphModel model) {
        return getConfig().isCompressNaryAxioms() ? shrink(readPairwiseAxioms(model)) : super.read(model);
    }
}
