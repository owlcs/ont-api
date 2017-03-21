package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * To perform preliminary fixing: transform the RDFS ontological graph to the OWL ontological graph.
 * After this conversion is completed there would be a valid owl-dl-ontology but maybe with missing declarations and
 * with some RDFS-garbage (rdfs:Class, rdf:Property).
 * It seems it can be considered as an OWL1 (till rdfs:Class, rdf:Property, etc would be removed by {@link OWLTransform}).
 * Note: currently it just adds the owl:AnnotationProperty type to all instances of rdf:Property and owl:Class to all rdfs:Class.
 * <p>
 * This transformer is optional:
 * if ontology graph already contains one of the five main owl-declarations (owl:Class,
 * owl:ObjectProperty, owl:DatatypeProperty, owl:AnnotationProperty, owl:NamedIndividual) and doesn't contain rdf:Property and rdfs:Class
 * then it can't be pure RDFS-ontology and we believe that there is nothing to do.
 * <p>
 * For some additional info see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a> and
 * <a href='https://www.w3.org/TR/2012/REC-owl2-overview-20121211/#Relationship_to_OWL_1'>some words about OWL 1</a>
 */
@SuppressWarnings("WeakerAccess")
public class RDFSTransform extends Transform {

    public RDFSTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        Model m = getBaseModel();
        m.listResourcesWithProperty(RDF.type, RDF.Property).forEachRemaining(this::declareRDFProperty);
        m.listResourcesWithProperty(RDF.type, RDFS.Class).forEachRemaining(this::declareRDFSClass);
    }

    private void declareRDFProperty(Resource resource) {
        resource.addProperty(RDF.type, OWL.AnnotationProperty);
    }

    private void declareRDFSClass(Resource resource) {
        resource.addProperty(RDF.type, OWL.Class);
    }

    @Override
    public boolean test() {
        return isRDFS() && !isOWL();
    }

    private boolean isRDFS() {
        return containsType(RDFS.Class) || containsType(RDF.Property);
    }

    private boolean isOWL() {
        return containsType(OWL.Class) || containsType(OWL.NamedIndividual)
                || containsType(OWL.AnnotationProperty) || containsType(OWL.DatatypeProperty) || containsType(OWL.ObjectProperty);
    }

}
