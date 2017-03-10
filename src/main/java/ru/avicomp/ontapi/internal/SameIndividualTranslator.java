package ru.avicomp.ontapi.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

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

    private Set<Wrap<OWLSameIndividualAxiom>> extractStandaloneAxioms(Set<Wrap<OWLSameIndividualAxiom>> set) {
        return set.stream().filter(a -> set.stream().filter(b -> !a.equals(b))
                .noneMatch(b -> ReadHelper.isIntersect(a.getObject(), b.getObject()))).collect(Collectors.toSet());
    }

    /**
     * todo: at the moment it is not used. see description of super method.
     * Compresses nary axioms to more compact form.
     * The difference with parent method is in fact that if individual 'A' is same as individual 'B', and
     * 'B' is same as 'C' then 'A' is same as 'C'.
     *
     * @param init initial Map with Axioms as keys
     * @return shrunken map of axioms
     */
    @Override
    Set<Wrap<OWLSameIndividualAxiom>> shrink(Set<Wrap<OWLSameIndividualAxiom>> init) {
        if (init.size() < 2) {
            return new HashSet<>(init);
        }
        Set<Wrap<OWLSameIndividualAxiom>> unique = extractStandaloneAxioms(init);
        Set<Wrap<OWLSameIndividualAxiom>> res = new HashSet<>();
        res.addAll(unique);
        if (res.size() == init.size()) return res;
        Set<Wrap<OWLSameIndividualAxiom>> tmp = new HashSet<>(init);
        tmp.removeAll(unique);
        // assemble a single axiom of the remaining pairwise axioms
        Stream<OWLAnnotation> annotations = tmp.stream().map(Wrap::getObject).map(HasAnnotations::annotations).findAny().orElse(Stream.empty());
        // do operands(stream)->set->stream to avoid BootstrapMethodError
        Stream<OWLIndividual> components = tmp.stream().map(Wrap::getObject).map(axiom -> axiom.operands().collect(Collectors.toSet()).stream()).flatMap(Function.identity()).distinct();
        Set<Triple> triples = tmp.stream().map(Wrap::triples).flatMap(Function.identity()).collect(Collectors.toSet());
        OWLSameIndividualAxiom multi = create(components, annotations.collect(Collectors.toSet()));
        res.add(new Wrap<>(multi, triples));
        return res;
    }

    @Override
    Wrap<OWLSameIndividualAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLIndividual> a = ReadHelper.fetchIndividual(statement.getSubject().as(getView()), df);
        Wrap<? extends OWLIndividual> b = ReadHelper.fetchIndividual(statement.getObject().as(getView()), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLSameIndividualAxiom res = df.getOWLSameIndividualAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
