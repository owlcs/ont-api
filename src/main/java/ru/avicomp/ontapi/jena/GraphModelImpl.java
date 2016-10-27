package ru.avicomp.ontapi.jena;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.DisjointUnion;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.translators.rdf2axiom.GraphParseHelper;
import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;

/**
 * New strategy here.
 * TODO:
 * Created by @szuev on 26.10.2016.
 */
public class GraphModelImpl extends ModelCom {
    private Graph union;

    public GraphModelImpl(Graph base) {
        super(base);
    }

    public Graph getUnionGraph() {
        return union == null ? graph : union;
    }

    public void addImport(Graph g) {
        MultiUnion imports = union == null ? new MultiUnion() : (MultiUnion) ((DisjointUnion) union).getR();
        imports.addGraph(g);
        union = new DisjointUnion(graph, imports);
    }

    public void removeImport(Graph g) {
        MultiUnion imports = (MultiUnion) ((DisjointUnion) OntException.notNull(union, "No imported graphs")).getR();
        imports.removeGraph(g);
        union = imports.isEmpty() ? null : new DisjointUnion(graph, imports);
    }

    public OWLOntologyID getID() {
        return GraphParseHelper.getOWLOntologyID(graph);
    }

    public ExtendedIterator<OWLEntity> entities() {
        return GraphParseHelper.entities(graph);
    }

    public ExtendedIterator<OWLEntity> allEntities() {
        return GraphParseHelper.entities(getUnionGraph());
    }

    public ExtendedIterator<OWLDeclarationAxiom> declarationAxioms() {
        return entities().mapWith(entity -> new OWLDeclarationAxiomImpl(entity, getAnnotations(entity, graph)));
    }

    /**
     * move to some translators
     * {@link OWLDataPropertyDomainAxiom}, {@link OWLAnnotationPropertyDomainAxiom}, {@link OWLObjectPropertyDomainAxiom}
     *
     * @return Set
     */
    public Set<OWLAxiom> getDomainAxioms() {
        Set<OWLProperty> props = allEntities().filterKeep(owlEntity -> owlEntity.isOWLDataProperty() || owlEntity.isOWLObjectProperty() || owlEntity.isOWLAnnotationProperty()).mapWith(OWLProperty.class::cast).toSet();
        Set<Triple> triples = graph.find(Node.ANY, RDFS.domain.asNode(), Node.ANY).toSet();
        //todo:
        return null;
    }

    public Collection<OWLAnnotation> getAnnotations(OWLObject object, Graph graph) {
        //todo
        return Collections.emptySet();
    }

}
