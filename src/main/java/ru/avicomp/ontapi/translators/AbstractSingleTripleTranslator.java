package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * Base class for parse axiom which is related to single triplet.
 * sub-classes:
 *  {@link DeclarationTranslator},
 *  {@link FunctionalDataPropertyTranslator},
 *  {@link FunctionalObjectPropertyTranslator},
 *  {@link ReflexiveObjectPropertyTranslator},
 *  {@link IrreflexiveObjectPropertyTranslator},
 *  {@link AsymmetricObjectPropertyTranslator},
 *  {@link SymmetricObjectPropertyTranslator},
 *  {@link TransitiveObjectPropertyTranslator},
 *  {@link InverseFunctionalObjectPropertyTranslator},
 * <p>
 * Created by @szuev on 28.09.2016.
 */
abstract class AbstractSingleTripleTranslator<Axiom extends OWLAxiom> extends AxiomTranslator<Axiom> {

    public abstract OWLObject getSubject(Axiom axiom);

    public abstract Property getPredicate();

    public abstract RDFNode getObject(Axiom axiom);

    @Override
    public void write(Axiom axiom, OntGraphModel graph) {
        OWL2RDFHelper.writeTriple(graph, getSubject(axiom), getPredicate(), getObject(axiom), axiom.annotations(), true);
    }
}
