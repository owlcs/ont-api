package ru.avicomp.ontapi.jena.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.model.*;

/**
 * TODO: move adding sub-annotations to base class.
 * <p>
 * Implementation of {@link OntAnnotation} (OWL2 Annotations)
 * see e.g. <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 * It's not an {@link OntObject} but a {@link Statement}
 * <p>
 * Created by @szuev on 12.11.2016.
 */
public class OntAStatementImpl extends OntStatementImpl implements OntAnnotation {

    public OntAStatementImpl(Resource subject, OntNAP predicate, RDFNode object, GraphModel model) {
        super(subject, predicate, object, model);
    }

    public OntAStatementImpl(Statement s) {
        super(s);
    }

    @Override
    public OntNAP getPredicate() {
        return (OntNAP) super.getPredicate();
    }

    @Override
    public boolean isAssertion() {
        return getSubject().isURIResource();
    }

    @Override
    public OntAnnotation add(OntNAP property, Literal value) {
        return addAnnotation(OntException.notNull(property, "Null property."), OntException.notNull(value, "Null literal."));
    }

    @Override
    public OntAnnotation add(OntNAP property, OntIndividual.Anonymous value) {
        return addAnnotation(OntException.notNull(property, "Null property."), OntException.notNull(value, "Null individual."));
    }

    @Override
    public OntAnnotation add(OntNAP property, Resource value) {
        return addAnnotation(OntException.notNull(property, "Null property."), OntObjectImpl.checkNamed(value));
    }

    @Override
    public void attach(OntAnnotation other) {
        addAnnotation(OntException.notNull(other, "Null annotation.").getPredicate(), other.getObject());
    }

    private OntAnnotation addAnnotation(OntNAP property, RDFNode value) {
        if (isAssertion()) { // expand to owl:Axiom form
            OntObjectImpl obj = (OntObjectImpl) getSubject().as(OntObject.class);
            Statement main = obj.getMainStatement();
            Resource subject = expand(OWL2.Axiom, main.getSubject(), main.getPredicate(), main.getObject());
            subject.addProperty(getPredicate(), getObject());
            getModel().remove(this);
            changeSubject(subject);
        }
        // expand to owl:Annotation form:
        Resource root = expand(OWL2.Annotation, getSubject(), getPredicate(), getObject());
        root.addProperty(property, value);
        return new OntAStatementImpl(root, property, value, getModel());
    }

    @Override
    public OntAnnotation removeAnnotation(OntNAP property, RDFNode value) {
        OntException.notNull(property, "Null property.");
        OntException.notNull(value, "Null value.");
        if (isAssertion()) return null;
        Resource root = findBulkForm(OWL2.Annotation, getSubject(), getPredicate(), getObject(), getModel());
        if (root == null) return null;
        OntAnnotation res = null;
        if (getModel().contains(root, property, value)) {
            res = new OntAStatementImpl(root, property, value, getModel());
            if (res.annotations().count() != 0) throw new OntException("Can't delete " + res + ": it has children");
            getModel().removeAll(root, property, value);
        }
        if (children(root, getModel()).count() == 0) {
            getModel().removeAll(root, null, null);
        }
        return res;
    }

    @Override
    public void removeAll() {
        Set<OntAnnotation> children = annotations().collect(Collectors.toSet());
        children.forEach(OntAnnotation::removeAll);
        children.forEach(a -> removeAnnotation(a.getPredicate(), a.getObject()));
    }

    @Override
    public Stream<OntAnnotation> annotations() {
        if (isAssertion()) return Stream.empty();
        Resource root = findBulkForm(OWL2.Annotation, getSubject(), getPredicate(), getObject(), getModel());
        if (root == null) return Stream.empty();
        return children(root, getModel());
    }

    public static Stream<OntAnnotation> children(Resource root, GraphModel model) {
        return JenaUtils.asStream(root.listProperties()
                .filterDrop(s -> RDF.type.equals(s.getPredicate()))
                .filterDrop(s -> OWL2.annotatedSource.equals(s.getPredicate()))
                .filterDrop(s -> OWL2.annotatedProperty.equals(s.getPredicate()))
                .filterDrop(s -> OWL2.annotatedTarget.equals(s.getPredicate()))
                .filterKeep(s -> s.getPredicate().canAs(OntNAP.class))
                .mapWith(s -> new OntAStatementImpl(s.getSubject(), s.getPredicate().as(OntNAP.class), s.getObject(), model))
                .mapWith(OntAnnotation.class::cast)).distinct();
    }

    private Resource expand(Resource type, Resource subject, Property predicate, RDFNode object) {
        Resource res = findBulkForm(type, subject, predicate, object, getModel());
        return res == null ? createBulkFrom(type, subject, predicate, object, getModel()) : res;
    }

    public static Resource findBulkForm(Resource type, Resource subject, Property predicate, RDFNode object, GraphModel model) {
        List<Resource> candidates = model.listStatements(null, OWL2.annotatedSource, subject).mapWith(Statement::getSubject).filterKeep(new UniqueFilter<>()).toList();
        for (Resource res : candidates) {
            if (!model.contains(res, RDF.type, type)) continue;
            if (!model.contains(res, OWL2.annotatedProperty, predicate)) continue;
            if (!model.contains(res, OWL2.annotatedTarget, object)) continue;
            return res;
        }
        return null;
    }

    public static Resource createBulkFrom(Resource type, Resource subject, Property predicate, RDFNode object, GraphModel model) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(OWL2.annotatedSource, subject);
        res.addProperty(OWL2.annotatedProperty, predicate);
        res.addProperty(OWL2.annotatedTarget, object);
        return res;
    }

    @Override
    public String toString() {
        return String.format("Annotation[%s %s]", getPredicate().getURI(), getObject());
    }
}
