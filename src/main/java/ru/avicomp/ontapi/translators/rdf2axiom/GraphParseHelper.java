package ru.avicomp.ontapi.translators.rdf2axiom;

import java.util.*;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntException;
import uk.ac.manchester.cs.owl.owlapi.*;

/**
 * To convert RDF -> Axiom
 * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_RDF_Graphs_to_the_Structural_Specification'>Mapping from RDF Graphs to the Structural Specification</a>
 * Created by szuev on 25.10.2016.
 */
@Deprecated
public class GraphParseHelper {

    public static OWLOntologyID getOWLOntologyID(Graph graph) {
        List<Triple> ontologies = graph.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()).toList();
        if (ontologies.isEmpty()) {
            return new OWLOntologyID();
        }
        // in case many ontologies in the single doc OWL-API gets the first one.
        if (ontologies.size() != 1) throw new OntException("Multiple ontologies are not supported.");
        Node subject = ontologies.get(0).getSubject();
        IRI iri = IRI.create(subject.getURI());
        IRI versionIRI = null;
        List<Triple> versions = graph.find(subject, OWL2.versionIRI.asNode(), Node.ANY).toList();
        if (!versions.isEmpty()) {
            if (versions.size() != 1) throw new OntException("Should be one versionIRI triple.");
            versionIRI = IRI.create(versions.get(0).getObject().getURI());
        }
        return new OWLOntologyID(iri, versionIRI);
    }

    public static ExtendedIterator<OWLImportsDeclaration> imports(Node nodeIRI, Graph graph) {
        return graph.find(nodeIRI, OWL.imports.asNode(), Node.ANY).
                mapWith(t -> IRI.create(t.getObject().getURI())).
                mapWith(OWLImportsDeclarationImpl::new);
    }

    public static ExtendedIterator<OWLImportsDeclaration> imports(OWLOntologyID id, Graph graph) {
        Optional<IRI> iri = id.getOntologyIRI();
        return imports(iri.isPresent() ? NodeFactory.createURI(iri.get().getIRIString()) : null, graph);
    }

    public static ExtendedIterator<OWLDeclarationAxiom> declarationAxioms(Graph graph) {
        return entities(graph).mapWith(entity -> new OWLDeclarationAxiomImpl(entity, getAnnotations(entity, graph)));
    }

    public static Collection<OWLAnnotation> getAnnotations(OWLObject object, Graph graph) {
        //todo
        return Collections.emptySet();
    }

    public static ExtendedIterator<OWLEntity> entities(Graph graph) {
        return graph.find(Node.ANY, RDF.type.asNode(), Node.ANY).mapWith(GraphParseHelper::toEntity).filterDrop(Objects::isNull);
    }

    public static ExtendedIterator<OWLProperty> properties(Graph graph) {
        return graph.find(Node.ANY, RDF.type.asNode(), Node.ANY).filterKeep(t -> t.getSubject().isURI()).
                mapWith(GraphParseHelper::toProperty).filterDrop(Objects::isNull).mapWith(OWLProperty.class::cast);
    }

    private static boolean isPropertyType(Node object) {
        return OWL.DatatypeProperty.asNode().equals(object) || OWL.ObjectProperty.asNode().equals(object) || OWL.AnnotationProperty.asNode().equals(object);
    }

    public static OWLEntity toEntity(Triple triple) {
        Node object = triple.getObject();
        if (OWL.Class.asNode().equals(object)) {
            return new OWLClassImpl(IRI.create(triple.getSubject().getURI()));
        }
        if (OWL2.NamedIndividual.asNode().equals(object)) {
            return new OWLNamedIndividualImpl(IRI.create(triple.getSubject().getURI()));
        }
        if (RDFS.Datatype.asNode().equals(object)) {
            return new OWLDatatypeImpl(IRI.create(triple.getSubject().getURI()));
        }
        return toProperty(triple);
    }

    public static OWLEntity toProperty(Triple triple) {
        Node object = triple.getObject();
        if (OWL.DatatypeProperty.asNode().equals(object)) {
            return new OWLDataPropertyImpl(IRI.create(triple.getSubject().getURI()));
        }
        if (OWL.AnnotationProperty.asNode().equals(object)) {
            return new OWLAnnotationPropertyImpl(IRI.create(triple.getSubject().getURI()));
        }
        if (OWL.ObjectProperty.asNode().equals(object)) {
            return new OWLObjectPropertyImpl(IRI.create(triple.getSubject().getURI()));
        }
        return null;
    }
}
