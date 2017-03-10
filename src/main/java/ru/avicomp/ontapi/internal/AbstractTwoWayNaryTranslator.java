package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * This is for following axioms with two or more than two entities:
 * <p>
 * DisjointClasses ({@link DisjointClassesTranslator}),
 * DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator}),
 * DisjointDataProperties ({@link DisjointDataPropertiesTranslator}),
 * DifferentIndividuals ({@link DifferentIndividualsTranslator}).
 * <p>
 * Each of these axioms could be written in two ways: as single triple (or sequence of single triples) or as special anonymous node with rdf:List inside.
 * <p>
 * Created by szuev on 12.10.2016.
 */
abstract class AbstractTwoWayNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>, OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AbstractNaryTranslator<Axiom, OWL, ONT> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        Set<OWL> operands = axiom.operands().collect(Collectors.toSet());
        Set<OWLAnnotation> annotations = axiom.annotations().collect(Collectors.toSet());
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            return;
        }
        if (operands.size() == 2) { // single triple classic way
            write(axiom, annotations, model);
        } else { // OWL2 anonymous node
            Resource root = model.createResource();
            model.add(root, RDF.type, getMembersType());
            model.add(root, getMembersPredicate(), WriteHelper.addRDFList(model, operands.stream()));
            OntDisjoint<ONT> res = root.as(getDisjointView());
            WriteHelper.addAnnotations(res, annotations.stream());
        }
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return Stream.concat(
                super.statements(model),
                model.ontObjects(getDisjointView()).map(OntObject::getRoot).filter(OntStatement::isLocal)
        );
    }

    abstract Resource getMembersType();

    abstract Property getMembersPredicate();

    abstract Class<? extends OntDisjoint<ONT>> getDisjointView();
}
