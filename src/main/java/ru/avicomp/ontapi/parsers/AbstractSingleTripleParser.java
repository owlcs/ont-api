package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Base class for parse axiom which is related to single triplet.
 * sub-classes:
 *  {@link DeclarationParser},
 *  {@link FunctionalDataPropertyParser},
 *  {@link FunctionalObjectPropertyParser},
 *  {@link ReflexiveObjectPropertyParser},
 *  {@link IrreflexiveObjectPropertyParser},
 *  {@link AsymmetricObjectPropertyParser},
 *  {@link SymmetricObjectPropertyParser},
 *  {@link TransitiveObjectPropertyParser},
 *  {@link InverseFunctionalObjectPropertyParser},
 * <p>
 * Created by @szuev on 28.09.2016.
 */
abstract class AbstractSingleTripleParser<Axiom extends OWLAxiom> extends AxiomParser<Axiom> {

    public abstract OWLObject getSubject();

    public abstract Property getPredicate();

    public abstract RDFNode getObject();

    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getSubject(), getPredicate(), getObject(), getAxiom(), true);
    }

}
