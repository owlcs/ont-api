/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.internal.InternalGraphModel;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.IsAnonymous;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationObject;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomCollection;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPrimitive;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstract {@link OWLOntology OWL-API Ontology} implementation with methods to read information
 * in the form of {@link OWLObject OWL Object}s from the underling graph-model.
 * It's an analogy of
 * <a href="https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLImmutableOntologyImpl.java">uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl</a>.
 * <p>
 * Created by @ssz on 03.12.2016.
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public abstract class OntBaseModelImpl implements OWLOntology, OntBaseModel {
    // binary format to provide serialization:
    public static final OntFormat DEFAULT_SERIALIZATION_FORMAT = OntFormat.RDF_THRIFT;
    @Serial
    private static final long serialVersionUID = 7605836729147058594L;

    protected transient InternalGraphModel base;
    protected transient ModelConfig config;

    protected int hashCode;

    protected OntBaseModelImpl(Graph graph, ModelConfig conf) {
        this.config = Objects.requireNonNull(conf);
        this.base = OntBaseModel.createInternalGraphModel(graph, conf.getSpecification(), conf,
                conf.getManager().getOWLDataFactory(), conf.getManagerCaches());
    }

    @Override
    public InternalGraphModel getGraphModel() {
        return base;
    }

    @Override
    public void setGraphModel(InternalGraphModel m) {
        this.base = Objects.requireNonNull(m);
    }

    @Override
    public ModelConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(ModelConfig conf) {
        this.config = Objects.requireNonNull(conf);
    }

    @Override
    public OntologyManager getOWLOntologyManager() {
        return getConfig().getManager();
    }

    /**
     * Gets the data factory.
     *
     * @return {@link DataFactory}
     */
    public DataFactory getDataFactory() {
        return getOWLOntologyManager().getOWLDataFactory();
    }

    @Override
    public void setOWLOntologyManager(@Nullable OWLOntologyManager manager) {
        throw new OntApiException.Unsupported("Misuse: attempt to set new manager: " + manager);
    }

    /**
     * Gets Ontology ID.
     *
     * @return the {@link OWLOntologyID}
     */
    @Override
    public ID getOntologyID() {
        return this.base.getOntologyID();
    }

    /**
     * Sets Ontology ID.
     * For internal usage only: the outer interface must be "immutable".
     *
     * @param id {@link OWLOntologyID Ontology ID}
     */
    protected void setOntologyID(OWLOntologyID id) {
        this.base.setOntologyID(id);
        this.hashCode = 0;
    }

    @Override
    public boolean isAnonymous() {
        return base.getID().isAnon();
    }

    @Override
    public boolean isEmpty() {
        return base.isOntologyEmpty();
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        return base.listOWLAnnotations();
    }

    /*
     * =============================
     * Methods to work with imports:
     * =============================
     */

    @Override
    public Stream<OWLOntology> imports() {
        return getOWLOntologyManager().imports(this);
    }

    @Override
    public Stream<OWLImportsDeclaration> importsDeclarations() {
        return base.listOWLImportDeclarations();
    }

    @Override
    public Stream<IRI> directImportsDocuments() {
        return importsDeclarations().map(OWLImportsDeclaration::getIRI);
    }

    @Override
    public Stream<OWLOntology> directImports() {
        return getOWLOntologyManager().directImports(this);
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        return getOWLOntologyManager().importsClosure(this);
    }

    /*
     * ==========================
     * To work with OWL-entities:
     * ==========================
     */

    @Override
    public Stream<OWLClass> classesInSignature() {
        return base.listOWLClasses();
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        return base.listOWLClassExpressions();
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return base.listOWLAnonymousIndividuals();
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        return anonymousIndividuals();
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        return base.listOWLNamedIndividuals();
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        return base.listOWLDataProperties();
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return base.listOWLObjectProperties();
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return base.listOWLAnnotationProperties();
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        return base.listOWLDatatypes();
    }

    @Override
    public Stream<OWLEntity> signature() {
        return Stream.of(classesInSignature(), objectPropertiesInSignature(), dataPropertiesInSignature(),
                        individualsInSignature(), datatypesInSignature(), annotationPropertiesInSignature())
                .flatMap(Function.identity());
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(@Nullable IRI iri) {
        return base.listOWLEntities(iri);
    }

    @Override
    public Set<IRI> getPunnedIRIs(Imports imports) {
        return base.listPunningIRIs(Imports.INCLUDED == imports).collect(Collectors.toSet());
    }

    @Override
    public boolean isDeclared(OWLEntity entity) {
        return base.containsOWLDeclaration(entity);
    }

    @Override
    public boolean containsReference(OWLEntity entity) {
        return referencingAxioms(entity).findFirst().isPresent();
    }

    @Override
    public boolean containsClassInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLClass(iri));
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLObjectProperty(iri));
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLDataProperty(iri));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLAnnotationProperty(iri));
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLDatatype(iri));
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri) {
        return base.containsOWLEntity(getDataFactory().getOWLNamedIndividual(iri));
    }

    @Override
    public boolean containsEntityInSignature(IRI iri) {
        return containsClassInSignature(iri)
                || containsObjectPropertyInSignature(iri)
                || containsDataPropertyInSignature(iri)
                || containsIndividualInSignature(iri)
                || containsDatatypeInSignature(iri)
                || containsAnnotationPropertyInSignature(iri);
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return containsClassInSignature(entity.getIRI());
        }
        if (entity.isOWLDatatype()) {
            return containsDatatypeInSignature(entity.getIRI());
        }
        if (entity.isOWLNamedIndividual()) {
            return containsIndividualInSignature(entity.getIRI());
        }
        if (entity.isOWLAnnotationProperty()) {
            return containsAnnotationPropertyInSignature(entity.getIRI());
        }
        if (entity.isOWLObjectProperty()) {
            return containsObjectPropertyInSignature(entity.getIRI());
        }
        if (entity.isOWLDataProperty()) {
            return containsDataPropertyInSignature(entity.getIRI());
        }
        throw new OntApiException.IllegalArgument("Unsupported entity: " + entity);
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type) {
        if (EntityType.CLASS.equals(type)) {
            return classesInSignature().findFirst().isPresent();
        }
        if (EntityType.DATA_PROPERTY.equals(type)) {
            return dataPropertiesInSignature().findFirst().isPresent();
        }
        if (EntityType.OBJECT_PROPERTY.equals(type)) {
            return objectPropertiesInSignature().findFirst().isPresent();
        }
        if (EntityType.ANNOTATION_PROPERTY.equals(type)) {
            return annotationPropertiesInSignature().findFirst().isPresent();
        }
        if (EntityType.DATATYPE.equals(type)) {
            return datatypesInSignature().findFirst().isPresent();
        }
        if (EntityType.NAMED_INDIVIDUAL.equals(type)) {
            return individualsInSignature().findFirst().isPresent();
        }
        throw new IllegalArgumentException("Entity type " + type + " is not valid for entity presence check");
    }

    /*
     * =======================
     * To work with OWL-Axioms
     * =======================
     */

    @Override
    public Stream<OWLAxiom> axioms() {
        return base.listOWLAxioms();
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> axiomType) {
        return base.listOWLAxioms(axiomType);
    }

    /**
     * Gets the axioms that form the definition/description of a class.
     * The results include:
     * <ul>
     * <li>Subclass axioms where the subclass is equal to the specified class</li>
     * <li>Equivalent class axioms where the specified class is an operand in the equivalent class axiom</li>
     * <li>Disjoint class axioms where the specified class is an operand in the disjoint class axiom</li>
     * <li>Disjoint union axioms, where the specified class is the named class that is equivalent to the disjoint union</li>
     * </ul>
     * This method may walk over the whole axiom cache in the {@link #base internal model} or read graph directly, as it sees fit.
     *
     * @param clazz The class whose describing axioms are to be retrieved
     * @return A {@code Stream} of class axioms that describe the class
     */
    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz) {
        Stream<? extends OWLClassAxiom> subClassOf = base.listOWLSubClassOfAxiomsBySubject(clazz);
        Stream<? extends OWLClassAxiom> disjointUnion = base.listOWLDisjointUnionAxioms(clazz);
        Stream<? extends OWLClassAxiom> disjoint = base.listOWLDisjointClassesAxioms(clazz);
        Stream<? extends OWLClassAxiom> equivalent = base.listOWLEquivalentClassesAxioms(clazz);
        return Stream.of(subClassOf, disjointUnion, disjoint, equivalent).flatMap(Function.identity());
    }

    /**
     * Gets the axioms that form the definition/description of an object property.
     * The result set of object property axioms includes:
     * <ul>
     * <li>1) Sub-property axioms where the sub property is the specified property</li>
     * <li>2) Equivalent property axioms where the axiom contains the specified property</li>
     * <li>3) Equivalent property axioms that contain the inverse of the specified property</li>
     * <li>4) Disjoint property axioms that contain the specified property</li>
     * <li>5) Domain axioms that specify a domain of the specified property</li>
     * <li>6) Range axioms that specify a range of the specified property</li>
     * <li>7) Any property characteristic axiom (i.e. Functional, Symmetric, Reflexive etc.) whose subject is the specified property</li>
     * <li>8) Inverse properties axioms that contain the specified property</li>
     * </ul>
     * <b>Note: either condition *3* or OWL-API-5.1.4 implementation (owlapi-impl) are wrong as shown by tests.</b>
     *
     * @param property The property whose defining axioms are to be retrieved
     * @return A {@code Stream} of object property axioms that describe the specified property
     */
    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property) {
        Stream<? extends OWLObjectPropertyAxiom> subPropertyOf = base.listOWLSubObjectPropertyOfAxiomsBySubject(property);
        Stream<? extends OWLObjectPropertyAxiom> nary = Stream.of(
                base.listOWLEquivalentObjectPropertiesAxioms(property),
                base.listOWLDisjointObjectPropertiesAxioms(property),
                base.listOWLInverseObjectPropertiesAxioms(property)).flatMap(Function.identity());
        Stream<? extends OWLObjectPropertyAxiom> unary = Stream.of(
                base.listOWLObjectPropertyDomainAxioms(property),
                base.listOWLObjectPropertyRangeAxioms(property),
                base.listOWLTransitiveObjectPropertyAxioms(property),
                base.listOWLIrreflexiveObjectPropertyAxioms(property),
                base.listOWLReflexiveObjectPropertyAxioms(property),
                base.listOWLSymmetricObjectPropertyAxioms(property),
                base.listOWLAsymmetricObjectPropertyAxioms(property),
                base.listOWLFunctionalObjectPropertyAxioms(property),
                base.listOWLInverseFunctionalObjectPropertyAxioms(property)).flatMap(Function.identity());
        return Stream.of(subPropertyOf, nary, unary).flatMap(Function.identity());
    }

    /**
     * Gets the axioms that form the definition/description of a data property.
     * The result set of data property axioms includes:
     * <ul>
     * <li>Sub-property axioms where the sub property is the specified property</li>
     * <li>Equivalent property axioms where the axiom contains the specified property</li>
     * <li>Disjoint property axioms that contain the specified property</li>
     * <li>Domain axioms that specify a domain of the specified property</li>
     * <li>Range axioms that specify a range of the specified property</li>
     * <li>Functional data property characteristic axiom whose subject is the specified property</li>
     * </ul>
     *
     * @param property The property whose defining axioms are to be retrieved
     * @return A {@code Stream} of data property axioms
     */
    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property) {
        Stream<? extends OWLDataPropertyAxiom> subPropertyOf = base.listOWLSubDataPropertyOfAxiomsBySubject(property);
        Stream<? extends OWLDataPropertyAxiom> nary = Stream.of(
                base.listOWLEquivalentDataPropertiesAxioms(property),
                base.listOWLDisjointDataPropertiesAxioms(property)).flatMap(Function.identity());
        Stream<? extends OWLDataPropertyAxiom> unary = Stream.of(
                base.listOWLDataPropertyDomainAxioms(property),
                base.listOWLDataPropertyRangeAxioms(property),
                base.listOWLFunctionalDataPropertyAxioms(property)).flatMap(Function.identity());
        return Stream.of(subPropertyOf, nary, unary).flatMap(Function.identity());
    }

    /**
     * Gets the axioms that form the definition/description of an annotation property.
     * The result set of annotation property axioms includes:
     * <ul>
     * <li>Annotation subPropertyOf axioms where the specified property is the sub property</li>
     * <li>Annotation property domain axioms that specify a domain for the specified property</li>
     * <li>Annotation property range axioms that specify a range for the specified property</li>
     * </ul>
     *
     * @param property The property whose definition axioms are to be retrieved
     * @return A {@code Stream} of annotation axioms
     */
    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property) {
        return Stream.of(
                base.listOWLSubAnnotationPropertyOfAxiomsBySubject(property),
                base.listOWLAnnotationPropertyDomainAxioms(property),
                base.listOWLAnnotationPropertyRangeAxioms(property)).flatMap(Function.identity());
    }

    /**
     * Gets the axioms that form the definition/description of an individual.
     * Conditions:
     * <ul>
     * <li>Individual type assertions that assert the type of the specified individual</li>
     * <li>Same individuals axioms that contain the specified individual</li>
     * <li>Different individuals axioms that contain the specified individual</li>
     * <li>Object property assertion axioms whose subject is the specified individual</li>
     * <li>Data property assertion axioms whose subject is the specified individual</li>
     * <li>Negative object property assertion axioms whose subject is the specified individual</li>
     * <li>Negative data property assertion axioms whose subject is the specified individual</li>
     * </ul>
     *
     * @param individual The individual whose defining axioms are to be retrieved
     * @return A {@code Stream} of individual axioms
     */
    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual) {
        Stream<? extends OWLIndividualAxiom> classAssertion = base.listOWLClassAssertionAxioms(individual);
        Stream<? extends OWLIndividualAxiom> nary = Stream.concat(base.listOWLSameIndividualAxioms(individual),
                base.listOWLDifferentIndividualsAxioms(individual));
        Stream<? extends OWLIndividualAxiom> propertyAssertion = Stream.of(
                base.listOWLObjectPropertyAssertionAxioms(individual),
                base.listOWLDataPropertyAssertionAxioms(individual),
                base.listOWLNegativeObjectPropertyAssertionAxioms(individual),
                base.listOWLNegativeDataPropertyAssertionAxioms(individual)).flatMap(Function.identity());
        return Stream.of(classAssertion, nary, propertyAssertion).flatMap(Function.identity());
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype) {
        return base.listOWLDatatypeDefinitionAxioms(datatype);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends OWLAxiom> Stream<A> axioms(OWLAxiomSearchFilter filter, Object key) {
        return base.listOWLAxioms(filter.getAxiomTypes()).filter(a -> filter.pass(a, key)).map(x -> (A) x);
    }

    /**
     * The generic search method: results all axioms which refer the given object.
     * This method may walk over the whole axiom cache in the {@link #base internal model} or read graph directly,
     * as it sees fit.
     * Functionally it differs from the original OWL-API method: it can handle a wider class of cases.
     * For internal usage only.
     *
     * @param type     {@link Class Class&lt;OWLAxiom&gt;}, not null, type of axiom
     * @param view     {@link Class Class&lt;OWLObject&gt;} anything, ignored
     * @param object   {@link OWLObject} to find occurrences
     * @param position {@link Navigation} used in conjunction with {@code object} for some several kinds of axioms
     * @param <A>      subtype of {@link OWLAxiom}
     * @return A {@code Stream} of {@link OWLAxiom}s
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLImmutableOntologyImpl.java#L544'>uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl#axioms(Class, Class, OWLObject, Navigation)</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/Internals.java#L495'>uk.ac.manchester.cs.owl.owlapi.Internals#get(Class, Class, Navigation)</a>
     */
    @Override
    public <A extends OWLAxiom> Stream<A> axioms(Class<A> type,
                                                 @Nullable Class<? extends OWLObject> view, // not used
                                                 OWLObject object,
                                                 @Nullable Navigation position) {
        return axioms(type, object, Navigation.IN_SUB_POSITION == position);
    }

    /**
     * The generic search method.
     *
     * @param type       {@link Class Class&lt;OWLAxiom&gt;}, not null, type of axiom
     * @param object     {@link OWLObject} to find occurrences
     * @param sub        if {@code true} performs searching in subject position
     *                   (only for cases where it makes sense, usually this flag is ignored)
     * @param <A>subtype of {@link OWLAxiom}
     * @return a {@code Stream} of {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> axioms(Class<A> type, OWLObject object, boolean sub) {
        if (OWLDeclarationAxiom.class.equals(type) && object instanceof OWLEntity) {
            return (Stream<A>) base.listOWLDeclarationAxioms((OWLEntity) object);
        }
        if (object instanceof OWLClassExpression) {
            return (Stream<A>) axiomsByClassExpression(type, (OWLClassExpression) object, sub);
        }
        if (object instanceof OWLDataRange) {
            return (Stream<A>) axiomsByDataRange(type, (OWLDataRange) object, sub);
        }
        if (object instanceof OWLIndividual) {
            return (Stream<A>) axiomsByIndividual(type, (OWLIndividual) object, sub);
        }
        if (object instanceof OWLObjectPropertyExpression) {
            return (Stream<A>) axiomsByObjectProperty(type, (OWLObjectPropertyExpression) object, sub);
        }
        if (object instanceof OWLDataProperty) {
            return (Stream<A>) axiomsByDataProperty(type, (OWLDataProperty) object, sub);
        }
        if (object instanceof OWLAnnotationProperty) {
            return (Stream<A>) axiomsByAnnotationProperty(type, (OWLAnnotationProperty) object, sub);
        }
        if (OWLAnnotationAssertionAxiom.class.equals(type)) {
            if (object instanceof OWLAnnotationSubject) {
                return (Stream<A>) base.listOWLAnnotationAssertionAxioms((OWLAnnotationSubject) object);
            }
            if (object instanceof OWLAnnotationObject) {
                return (Stream<A>) base.listOWLAxioms(OWLAnnotationAssertionAxiom.class).filter(a -> object.equals(a.getValue()));
            }
        }
        throw new OntApiException.IllegalArgument();
    }

    private Stream<? extends OWLAxiom> axiomsByClassExpression(Class<? extends OWLAxiom> type,
                                                               OWLClassExpression clazz,
                                                               boolean subject) throws ClassCastException {
        if (OWLSubClassOfAxiom.class.equals(type)) {
            if (clazz.isOWLClass()) {
                return subject ?
                        base.listOWLSubClassOfAxiomsBySubject(clazz.asOWLClass()) :
                        base.listOWLSubClassOfAxiomsByObject(clazz.asOWLClass());
            }
            return base.listOWLAxioms(OWLSubClassOfAxiom.class)
                    .filter(a -> clazz.equals(subject ? a.getSubClass() : a.getSuperClass()));
        }
        if (OWLEquivalentClassesAxiom.class.equals(type)) {
            if (clazz.isOWLClass()) {
                return base.listOWLEquivalentClassesAxioms(clazz.asOWLClass());
            }
            return base.listOWLAxioms(OWLEquivalentClassesAxiom.class).filter(a -> a.contains(clazz));
        }
        if (OWLDisjointClassesAxiom.class.equals(type)) {
            if (clazz.isOWLClass()) {
                return base.listOWLDisjointClassesAxioms(clazz.asOWLClass());
            }
            return base.listOWLAxioms(OWLDisjointClassesAxiom.class).filter(a -> a.contains(clazz));
        }
        if (OWLDisjointUnionAxiom.class.equals(type)) {
            // CN owl:disjointUnionOf ( C1 ... Cn )
            if (subject) {
                return base.listOWLDisjointUnionAxioms(clazz.asOWLClass());
            }
            return base.listOWLAxioms(OWLDisjointUnionAxiom.class).filter(a -> a.classExpressions().anyMatch(clazz::equals));
        }
        if (OWLHasKeyAxiom.class.equals(type)) {
            // C owl:hasKey ( P1 ... Pm R1 ... Rn )
            if (clazz.isOWLClass()) {
                return base.listOWLHasKeyAxioms(clazz.asOWLClass());
            }
            return base.listOWLAxioms(OWLHasKeyAxiom.class).filter(x -> x.getClassExpression().equals(clazz));
        }
        if (OWLClassAssertionAxiom.class.equals(type)) {
            return base.listOWLClassAssertionAxioms(clazz);
        }
        return base.listOWLAxioms(type, clazz);
    }

    private Stream<? extends OWLAxiom> axiomsByDataRange(Class<? extends OWLAxiom> type,
                                                         OWLDataRange range,
                                                         boolean subject) throws ClassCastException {
        if (OWLDatatypeDefinitionAxiom.class.equals(type)) {
            if (subject) {
                return base.listOWLDatatypeDefinitionAxioms(range.asOWLDatatype());
            }
            return base.listOWLAxioms(OWLDatatypeDefinitionAxiom.class).filter(x -> x.getDataRange().equals(range));
        }
        return base.listOWLAxioms(type, range);
    }

    private Stream<? extends OWLAxiom> axiomsByIndividual(Class<? extends OWLAxiom> type,
                                                          OWLIndividual individual,
                                                          boolean subject) {
        if (OWLClassAssertionAxiom.class.equals(type)) {
            return base.listOWLClassAssertionAxioms(individual);
        }
        if (OWLSameIndividualAxiom.class.equals(type)) {
            return base.listOWLSameIndividualAxioms(individual);
        }
        if (OWLDifferentIndividualsAxiom.class.equals(type)) {
            return base.listOWLDifferentIndividualsAxioms(individual);
        }
        if (OWLObjectPropertyAssertionAxiom.class.equals(type)) {
            if (subject) {
                return base.listOWLObjectPropertyAssertionAxioms(individual);
            }
            return base.listOWLAxioms(OWLObjectPropertyAssertionAxiom.class).filter(x -> x.getObject().equals(individual));
        }
        if (OWLNegativeObjectPropertyAssertionAxiom.class.equals(type)) {
            if (subject) {
                return base.listOWLNegativeObjectPropertyAssertionAxioms(individual);
            }
            return base.listOWLAxioms(OWLNegativeObjectPropertyAssertionAxiom.class).filter(x -> x.getObject().equals(individual));
        }
        if (OWLDataPropertyAssertionAxiom.class.equals(type)) {
            return base.listOWLDataPropertyAssertionAxioms(individual);
        }
        if (OWLNegativeDataPropertyAssertionAxiom.class.equals(type)) {
            return base.listOWLNegativeDataPropertyAssertionAxioms(individual);
        }
        return base.listOWLAxioms(type, individual);
    }

    private Stream<? extends OWLAxiom> axiomsByObjectProperty(Class<? extends OWLAxiom> type,
                                                              OWLObjectPropertyExpression property,
                                                              boolean subject) {
        if (OWLSubObjectPropertyOfAxiom.class.equals(type)) {
            return subject ?
                    base.listOWLSubObjectPropertyOfAxiomsBySubject(property) :
                    base.listOWLSubObjectPropertyOfAxiomsByObject(property);
        }
        if (OWLEquivalentObjectPropertiesAxiom.class.equals(type)) {
            return base.listOWLEquivalentObjectPropertiesAxioms(property);
        }
        if (OWLDisjointObjectPropertiesAxiom.class.equals(type)) {
            return base.listOWLDisjointObjectPropertiesAxioms(property);
        }
        if (OWLInverseObjectPropertiesAxiom.class.equals(type)) {
            return base.listOWLInverseObjectPropertiesAxioms(property);
        }
        if (OWLObjectPropertyDomainAxiom.class.equals(type)) {
            return base.listOWLObjectPropertyDomainAxioms(property);
        }
        if (OWLObjectPropertyRangeAxiom.class.equals(type)) {
            return base.listOWLObjectPropertyRangeAxioms(property);
        }
        if (OWLTransitiveObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLTransitiveObjectPropertyAxioms(property);
        }
        if (OWLFunctionalObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLFunctionalObjectPropertyAxioms(property);
        }
        if (OWLInverseFunctionalObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLInverseFunctionalObjectPropertyAxioms(property);
        }
        if (OWLSymmetricObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLSymmetricObjectPropertyAxioms(property);
        }
        if (OWLAsymmetricObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLAsymmetricObjectPropertyAxioms(property);
        }
        if (OWLReflexiveObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLReflexiveObjectPropertyAxioms(property);
        }
        if (OWLIrreflexiveObjectPropertyAxiom.class.equals(type)) {
            return base.listOWLIrreflexiveObjectPropertyAxioms(property);
        }
        return base.listOWLAxioms(type, property);
    }

    private Stream<? extends OWLAxiom> axiomsByDataProperty(Class<? extends OWLAxiom> type,
                                                            OWLDataProperty property,
                                                            boolean subject) {
        if (OWLSubDataPropertyOfAxiom.class.equals(type)) {
            return subject ?
                    base.listOWLSubDataPropertyOfAxiomsBySubject(property) :
                    base.listOWLSubDataPropertyOfAxiomsByObject(property);
        }
        if (OWLEquivalentDataPropertiesAxiom.class.equals(type)) {
            return base.listOWLEquivalentDataPropertiesAxioms(property);
        }
        if (OWLDisjointDataPropertiesAxiom.class.equals(type)) {
            return base.listOWLDisjointDataPropertiesAxioms(property);
        }
        if (OWLDataPropertyDomainAxiom.class.equals(type)) {
            return base.listOWLDataPropertyDomainAxioms(property);
        }
        if (OWLDataPropertyRangeAxiom.class.equals(type)) {
            return base.listOWLDataPropertyRangeAxioms(property);
        }
        if (OWLFunctionalDataPropertyAxiom.class.equals(type)) {
            return base.listOWLFunctionalDataPropertyAxioms(property);
        }
        return base.listOWLAxioms(type, property);
    }

    private Stream<? extends OWLAxiom> axiomsByAnnotationProperty(Class<? extends OWLAxiom> type,
                                                                  OWLAnnotationProperty property,
                                                                  boolean subject) {
        if (OWLSubAnnotationPropertyOfAxiom.class.equals(type)) {
            return subject ?
                    base.listOWLSubAnnotationPropertyOfAxiomsBySubject(property) :
                    base.listOWLSubAnnotationPropertyOfAxiomsByObject(property);
        }
        if (OWLAnnotationPropertyDomainAxiom.class.equals(type)) {
            return base.listOWLAnnotationPropertyDomainAxioms(property);
        }
        if (OWLAnnotationPropertyRangeAxiom.class.equals(type)) {
            return base.listOWLAnnotationPropertyRangeAxioms(property);
        }
        return base.listOWLAxioms(type, property);
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(Imports imports) {
        return AxiomType.TBoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        return AxiomType.ABoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        return AxiomType.RBoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return base.listOWLLogicalAxioms();
    }

    @Override
    public Stream<OWLClassAxiom> generalClassAxioms() {
        Stream<OWLSubClassOfAxiom> subClassOfAxioms = base.listOWLAxioms(OWLSubClassOfAxiom.class)
                .filter(a -> a.getSubClass().isAnonymous());
        Stream<? extends OWLNaryClassAxiom> naryClassAxioms = Stream.of(
                        OWLEquivalentClassesAxiom.class,
                        OWLDisjointClassesAxiom.class).flatMap(base::listOWLAxioms)
                .filter(a -> a.classExpressions().allMatch(IsAnonymous::isAnonymous));
        return Stream.concat(subClassOfAxioms, naryClassAxioms);
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom) {
        return axioms(axiom.getAxiomType()).map(OWLAxiom.class::cast).filter(ax -> ax.equalsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive) {
        return base.listOWLAxioms(primitive);
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        return base.listOWLSubAnnotationPropertyOfAxiomsBySubject(property);
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return base.listOWLAnnotationPropertyDomainAxioms(property);
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return base.listOWLAnnotationPropertyRangeAxioms(property);
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        return base.listOWLDatatypeDefinitionAxioms(datatype);
    }

    @Override
    public int getAxiomCount() {
        return (int) base.getOWLAxiomCount();
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType) {
        return (int) axioms(axiomType).count();
    }

    @Override
    public int getLogicalAxiomCount() {
        return (int) logicalAxioms().count();
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom) {
        return base.contains(axiom);
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom axiom) {
        return base.containsIgnoreAnnotations(axiom);
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object key) {
        return base.listOWLAxioms(filter.getAxiomTypes()).anyMatch(a -> filter.pass(a, key));
    }

    @Override
    public boolean containsClassInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsClassInSignature(iri));
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsObjectPropertyInSignature(iri));
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsDataPropertyInSignature(iri));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsAnnotationPropertyInSignature(iri));
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsDatatypeInSignature(iri));
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsIndividualInSignature(iri));
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom, Imports imports, AxiomAnnotations ignoreAnnotations) {
        return imports.stream(this).anyMatch(o -> ignoreAnnotations.contains(o, axiom));
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object key, Imports imports) {
        return imports.stream(this).anyMatch(o -> o.contains(filter, key));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key, Imports imports) {
        if (Imports.EXCLUDED == imports) {
            return axioms(filter, key);
        }
        return imports.stream(this).flatMap(o -> o.axioms(filter, key));
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        if (Imports.EXCLUDED == imports) {
            return axiomsIgnoreAnnotations(axiom);
        }
        return imports.stream(this).flatMap(o -> o.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public int getAxiomCount(Imports imports) {
        return imports.stream(this).mapToInt(OWLAxiomCollection::getAxiomCount).sum();
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType, Imports imports) {
        return imports.stream(this).mapToInt(o -> o.getAxiomCount(axiomType)).sum();
    }

    @Override
    public int getLogicalAxiomCount(Imports imports) {
        return imports.stream(this).mapToInt(OWLAxiomCollection::getLogicalAxiomCount).sum();
    }

    /*
     * ===============================================================================
     * The overridden default methods from org.semanticweb.owlapi.model.OWLAxiomIndex:
     * ===============================================================================
     */

    @Override
    public Stream<OWLDeclarationAxiom> declarationAxioms(OWLEntity subject) {
        return base.listOWLDeclarationAxioms(subject);
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity) {
        return base.listOWLAnnotationAssertionAxioms(entity);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass clazz) {
        return base.listOWLSubClassOfAxiomsBySubject(clazz);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass clazz) {
        return base.listOWLSubClassOfAxiomsByObject(clazz);
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass clazz) {
        return base.listOWLEquivalentClassesAxioms(clazz);
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass clazz) {
        return base.listOWLDisjointClassesAxioms(clazz);
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass clazz) {
        return base.listOWLDisjointUnionAxioms(clazz);
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass clazz) {
        return base.listOWLHasKeyAxioms(clazz);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression clazz) {
        return base.listOWLClassAssertionAxioms(clazz);
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return base.listOWLSubObjectPropertyOfAxiomsBySubject(property);
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return base.listOWLSubObjectPropertyOfAxiomsByObject(property);
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLObjectPropertyDomainAxioms(property);
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLObjectPropertyRangeAxioms(property);
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLInverseObjectPropertiesAxioms(property);
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLEquivalentObjectPropertiesAxioms(property);
    }

    @Override
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLDisjointObjectPropertiesAxioms(property);
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLFunctionalObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLInverseFunctionalObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLSymmetricObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLAsymmetricObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLReflexiveObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLIrreflexiveObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return base.listOWLTransitiveObjectPropertyAxioms(property);
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return base.listOWLSubDataPropertyOfAxiomsBySubject(property);
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return base.listOWLSubDataPropertyOfAxiomsByObject((OWLDataProperty) property);
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        return base.listOWLDataPropertyDomainAxioms(property);
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        return base.listOWLDataPropertyRangeAxioms(property);
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(OWLDataProperty property) {
        return base.listOWLEquivalentDataPropertiesAxioms(property);
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(OWLDataProperty property) {
        return base.listOWLDisjointDataPropertiesAxioms(property);
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return base.listOWLFunctionalDataPropertyAxioms((OWLDataProperty) property);
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(OWLIndividual individual) {
        return base.listOWLDataPropertyAssertionAxioms(individual);
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(OWLIndividual individual) {
        return base.listOWLObjectPropertyAssertionAxioms(individual);
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return base.listOWLNegativeObjectPropertyAssertionAxioms(individual);
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return base.listOWLNegativeDataPropertyAssertionAxioms(individual);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        return base.listOWLClassAssertionAxioms(individual);
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        return base.listOWLSameIndividualAxioms(individual);
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(OWLIndividual individual) {
        return base.listOWLDifferentIndividualsAxioms(individual);
    }

    /**
     * Reads the object while serialization.
     * Note: only the base graph is serialized.
     *
     * @param in {@link ObjectInputStream}
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object could not be found
     * @see OntologyManagerImpl#readObject(ObjectInputStream)
     */
    @Serial
    @SuppressWarnings("JavadocReference")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Graph base = GraphMemFactory.createDefaultGraph();
        RDFDataMgr.read(base, in, DEFAULT_SERIALIZATION_FORMAT.getLang());
        // set temporary model with default personality, it will be reset inside manager while its #readObject
        setGraphModel(OntBaseModel.createInternalGraphModel(base));
    }

    /**
     * Writes the object while serialization.
     * Note: only the base graph is serialized,
     * so if you serialize and then de-serialize standalone ontology it will lose all its references,
     * please use managers serialization, it will restore any links.
     * Also, please note: an exception is expected if the encapsulated graph is not {@code  GraphMem}.
     *
     * @param out {@link ObjectOutputStream}
     * @throws IOException     if I/O errors occur while writing to the underlying <code>OutputStream</code>
     * @throws OntApiException in case this instance encapsulates graph which is not plain in-memory graph
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException, OntApiException {
        Graph g = base.getBaseGraph();
        if (!Graphs.isGraphMem(g)) {
            throw new OntApiException(getOntologyID() + ":: Serialization is not supported for " + g.getClass());
        }
        out.defaultWriteObject();
        // serialize only base graph (it will be wrapped as UnionGraph):
        RDFDataMgr.write(out, g, DEFAULT_SERIALIZATION_FORMAT.getLang());
    }

    /**
     * Overridden {@link OWLObjectImpl#toString()} in order not to force the axioms loading.
     * For brief information there should be a separate method and the original implementation of toString is not very good idea in our case.
     *
     * @return String
     */
    @Override
    public String toString() {
        return String.format("Ontology(%s)", getOntologyID());
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? hashCode = initHashCode() : hashCode;
    }

    @Override
    public int compareTo(@Nullable OWLObject o) {
        return OWLObjectImpl.DEFAULT_COMPARATOR.compare(this, Objects.requireNonNull(o));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Ontology)) {
            return false;
        }
        OntModel right = ((Ontology) obj).asGraphModel();
        OntModel left = getGraphModel();
        return left.id().filter(id -> right.id().filter(id::sameAs).isPresent()).isPresent();
    }
}
