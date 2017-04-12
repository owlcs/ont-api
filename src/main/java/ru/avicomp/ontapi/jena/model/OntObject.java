package ru.avicomp.ontapi.jena.model;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * Base Ont Resource.
 * The analogue of {@link org.apache.jena.ontology.OntResource}
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    @Override
    OntGraphModel getModel();

    /**
     * Determines is ont-object resource local defined (i.e. does not belong to any graph from imports).
     *
     * @return true if this resource is local to the base graph.
     */
    boolean isLocal();

    /**
     * returns the root statement, i.e. the main triple in model which determines this object.
     * usually it is declaration (statement with predicate rdf:type)
     *
     * @return OntStatement or null
     */
    OntStatement getRoot();

    /**
     * returns the content of object: all characteristic statements,
     * i.e. all those statements which determine this object.
     * For noncomposite objects the result would contain only the root statement.
     * For composite (usually anonymous, e.g. disjoint section, class expression, etc) objects
     * the result would contain all statements in the graph but without statements related to the components.
     *
     * @return Stream of associated with this object statements
     */
    Stream<OntStatement> content();

    OntStatement addStatement(Property property, RDFNode value);

    /**
     * returns the <b>first</b> statement for specified property and object.
     *
     * @param property {@link Property}
     * @param object   {@link RDFNode}
     * @return {@link Optional} around {@link OntStatement}
     */
    Optional<OntStatement> statement(Property property, RDFNode object);

    @Override
    OntStatement getRequiredProperty(Property property);

    /**
     * returns the <b>first</b> statement for specified property.
     *
     * @param property {@link Property}
     * @return {@link Optional} around {@link OntStatement}
     */
    Optional<OntStatement> statement(Property property);

    void remove(Property property, RDFNode object);

    Stream<OntStatement> statements(Property property);

    /**
     * returns all statements related to this object (i.e. with subject=this)
     *
     * @return Stream of all statements.
     */
    Stream<OntStatement> statements();


    /**
     * gets stream of all objects attached on property to this ont-object
     *
     * @param predicate {@link Property} predicate
     * @param view      Interface to find and cast
     * @return Stream of objects ({@link RDFNode}s)
     */
    <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> view);


    /**
     * returns all declarations (statements with rdf:type predicate)
     *
     * @return Stream of {@link Resource}s
     */
    Stream<Resource> types();

    /**
     * Answers if this object has specified rdf:type
     *
     * @param type {@link Resource} to test
     * @return true if it has.
     */
    boolean hasType(Resource type);

    /**
     * Returns the stream of all annotations attached to this object (not only to main-triple).
     * Each annotation could be plain (assertion) or bulk annotation (with/without sub-annotations).
     * Sub-annotations are not included to this stream.
     * <p>
     * According to OWL2-DL specification OntObject should be an uri-resource (i.e. not anonymous),
     * but we extend this behaviour for more generality.
     *
     * @return Stream of {@link OntStatement}s, each of them has as key {@link OntNAP} and as value any {@link RDFNode}.
     */
    default Stream<OntStatement> annotations() {
        return statements().map(OntStatement::annotations).flatMap(Function.identity());
    }

    /**
     * add annotation assertion.
     * it could be expanded to bulk form by adding sub-annotation
     *
     * @param property Named annotation property.
     * @param value    RDFNode (uri-resource, literal or anonymous individual)
     * @return OntStatement for newly added annotation.
     * @throws OntJenaException in case input is wrong.
     */
    default OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return getRoot().addAnnotation(property, value);
    }

    default OntStatement addComment(String txt, String lang) {
        return addAnnotation(getModel().getRDFSComment(), ResourceFactory.createLangLiteral(txt, lang));
    }

    default OntStatement addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), ResourceFactory.createLangLiteral(txt, lang));
    }

    default void clearAnnotations() {
        Set<OntStatement> annotated = statements().filter(OntStatement::hasAnnotations).collect(Collectors.toSet());
        annotated.forEach(OntStatement::clearAnnotations);
        annotations().forEach(a -> removeAll(a.getPredicate()));
    }
}
