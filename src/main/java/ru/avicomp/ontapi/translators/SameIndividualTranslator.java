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
import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;

import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLSameIndividualAxiomImpl;

/**
 * base class {@link AbstractNaryTranslator}
 * example:
 * :indi1 owl:sameAs :indi2, :indi3 .
 * <p>
 * Created by szuev on 13.10.2016.
 */
class SameIndividualTranslator extends AbstractNaryTranslator<OWLSameIndividualAxiom, OWLIndividual, OntIndividual> {
    @Override
    public Property getPredicate() {
        return OWL.sameAs;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    OWLSameIndividualAxiom create(Stream<OWLIndividual> components, Set<OWLAnnotation> annotations) {
        return new OWLSameIndividualAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    OWLSameIndividualAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(RDF2OWLHelper::getIndividual), annotations);
    }

    private Set<OWLSameIndividualAxiom> extractStandaloneAxioms(Set<OWLSameIndividualAxiom> set) {
        return set.stream().filter(a -> set.stream().filter(b -> !a.equals(b))
                .noneMatch(b -> RDF2OWLHelper.isIntersect(a, b))).collect(Collectors.toSet());
    }

    /**
     * Compresses nary axioms to more compact form.
     * The difference with parent method is in fact that if individual 'A' is same as individual 'B', and
     * 'B' is same as 'C' then 'A' is same as 'C'.
     *
     * @param init initial Map with Axioms as keys
     * @return shrunken map of axioms
     */
    @Override
    Map<OWLSameIndividualAxiom, Set<Triple>> shrink(Map<OWLSameIndividualAxiom, Set<Triple>> init) {
        Set<OWLSameIndividualAxiom> unique = extractStandaloneAxioms(init.keySet());
        Map<OWLSameIndividualAxiom, Set<Triple>> res = new HashMap<>();
        unique.forEach(a -> res.put(a, init.get(a)));
        if (res.size() == init.size()) return res;
        Map<OWLSameIndividualAxiom, Set<Triple>> tmp = new HashMap<>(init);
        unique.forEach(tmp::remove);
        // assemble a single axiom of the remaining pairwise axioms
        Stream<OWLAnnotation> annotations = tmp.keySet().stream().map(HasAnnotations::annotations).findAny().orElse(Stream.empty());
        // do operands(stream)->set->stream to avoid BootstrapMethodError
        Stream<OWLIndividual> components = tmp.keySet().stream().map(axiom -> axiom.operands().collect(Collectors.toSet()).stream()).flatMap(Function.identity()).distinct();
        Set<Triple> triples = tmp.values().stream().map(Collection::stream).flatMap(Function.identity()).collect(Collectors.toSet());
        OWLSameIndividualAxiom multi = create(components, annotations.collect(Collectors.toSet()));
        res.put(multi, triples);
        return res;
    }
}
