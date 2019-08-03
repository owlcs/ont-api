/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;
import org.semanticweb.owlapi.util.OWLClassExpressionCollector;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstract {@link OWLOntology OWL-API Ontology} implementation with methods to read information
 * in the form of {@link OWLObject OWL Object}s from the underling graph-model.
 * It's an analogy of <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLImmutableOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl</a>.
 * <p>
 * Created by @szuev on 03.12.2016.
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public abstract class OntBaseModelImpl implements OWLOntology, BaseModel {
    // binary format to provide serialization:
    public static final OntFormat DEFAULT_SERIALIZATION_FORMAT = OntFormat.RDF_THRIFT;
    private static final long serialVersionUID = 7605836729147058594L;

    protected transient InternalModel base;
    protected transient ModelConfig config;

    protected int hashCode;

    protected OntBaseModelImpl(Graph graph, ModelConfig conf) {
        this.config = Objects.requireNonNull(conf);
        this.base = conf.createInternalModel(Objects.requireNonNull(graph));
    }

    @Override
    public InternalModel getBase() {
        return base;
    }

    @Override
    public void setBase(InternalModel m) {
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
    public OntologyID getOntologyID() {
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
        return accept(new OWLClassExpressionCollector()).stream();
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
        return base.ambiguousEntities(Imports.INCLUDED == imports)
                .map(Resource::getURI)
                .map(IRI::create)
                .collect(Collectors.toSet());
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
        Stream<? extends OWLClassAxiom> subClassOf = base.listOWLSubClassOfAxioms(clazz);
        Stream<? extends OWLClassAxiom> disjointUnion = base.listOWLAxioms(OWLDisjointUnionAxiom.class)
                .filter(a -> Objects.equals(a.getOWLClass(), clazz));
        Stream<? extends OWLClassAxiom> disjoint = base.listOWLAxioms(OWLDisjointClassesAxiom.class)
                .filter(a -> a.operands().anyMatch(clazz::equals));
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
        Stream<? extends OWLObjectPropertyAxiom> subPropertyOf = base.listOWLAxioms(OWLSubObjectPropertyOfAxiom.class)
                .filter(a -> Objects.equals(a.getSubProperty(), property));
        @SuppressWarnings("unchecked")
        Stream<? extends OWLObjectPropertyAxiom> nary = Stream.of(
                OWLEquivalentObjectPropertiesAxiom.class,
                OWLDisjointObjectPropertiesAxiom.class,
                OWLInverseObjectPropertiesAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLNaryPropertyAxiom.class::cast)
                .filter(a -> a.operands().anyMatch(o -> Objects.equals(o, property)))
                .map(OWLObjectPropertyAxiom.class::cast);
        Stream<? extends OWLObjectPropertyAxiom> unary = Stream.of(
                OWLObjectPropertyDomainAxiom.class,
                OWLObjectPropertyRangeAxiom.class,
                OWLTransitiveObjectPropertyAxiom.class,
                OWLIrreflexiveObjectPropertyAxiom.class,
                OWLReflexiveObjectPropertyAxiom.class,
                OWLSymmetricObjectPropertyAxiom.class,
                OWLFunctionalObjectPropertyAxiom.class,
                OWLInverseFunctionalObjectPropertyAxiom.class,
                OWLAsymmetricObjectPropertyAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLUnaryPropertyAxiom.class::cast)
                .filter(a -> Objects.equals(a.getProperty(), property))
                .map(OWLObjectPropertyAxiom.class::cast);
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
        Stream<? extends OWLDataPropertyAxiom> subPropertyOf = base.listOWLAxioms(OWLSubDataPropertyOfAxiom.class)
                .filter(a -> Objects.equals(a.getSubProperty(), property));
        @SuppressWarnings("unchecked")
        Stream<? extends OWLDataPropertyAxiom> nary = Stream.of(
                OWLEquivalentDataPropertiesAxiom.class,
                OWLDisjointDataPropertiesAxiom.class,
                OWLInverseObjectPropertiesAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLNaryPropertyAxiom.class::cast)
                .filter(a -> a.operands().anyMatch(o -> Objects.equals(o, property)))
                .map(OWLDataPropertyAxiom.class::cast);
        Stream<? extends OWLDataPropertyAxiom> unary = Stream.of(
                OWLDataPropertyDomainAxiom.class,
                OWLDataPropertyRangeAxiom.class,
                OWLFunctionalDataPropertyAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLUnaryPropertyAxiom.class::cast)
                .filter(a -> Objects.equals(a.getProperty(), property))
                .map(OWLDataPropertyAxiom.class::cast);
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
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property) {
        return Stream.of(axioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF).filter(a -> a.getSubProperty().equals(property)),
                axioms(AxiomType.ANNOTATION_PROPERTY_RANGE).filter(a -> a.getProperty().equals(property)),
                axioms(AxiomType.ANNOTATION_PROPERTY_DOMAIN).filter(a -> a.getProperty().equals(property)))
                .flatMap(Function.identity());
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
        Stream<? extends OWLIndividualAxiom> classAssertion = base.listOWLAxioms(OWLClassAssertionAxiom.class)
                .filter(a -> Objects.equals(a.getIndividual(), individual));
        Stream<? extends OWLIndividualAxiom> nary = Stream.of(
                OWLSameIndividualAxiom.class,
                OWLDifferentIndividualsAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLNaryIndividualAxiom.class::cast)
                .filter(a -> a.operands().anyMatch(o -> Objects.equals(o, individual)));
        Stream<? extends OWLIndividualAxiom> propertyAssertion = Stream.of(
                OWLObjectPropertyAssertionAxiom.class,
                OWLDataPropertyAssertionAxiom.class,
                OWLNegativeObjectPropertyAssertionAxiom.class,
                OWLNegativeDataPropertyAssertionAxiom.class
        ).flatMap(c -> base.listOWLAxioms(c))
                .map(OWLPropertyAssertionAxiom.class::cast)
                .filter(a -> Objects.equals(a.getSubject(), individual));
        return Stream.of(classAssertion, nary, propertyAssertion).flatMap(Function.identity());
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype) {
        return base.listOWLAxioms(OWLDatatypeDefinitionAxiom.class).filter(a -> datatype.equals(a.getDatatype()));
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
     * @param sub        if {@code true} performs searching in sub position
     * @param <A>subtype of {@link OWLAxiom}
     * @return a {@code Stream} of {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> axioms(Class<A> type, OWLObject object, boolean sub) {
        if (sub && OWLDeclarationAxiom.class.equals(type) && object instanceof OWLEntity) {
            return (Stream<A>) base.listOWLDeclarationAxioms((OWLEntity) object);
        }
        if (OWLSubObjectPropertyOfAxiom.class.equals(type) && object instanceof OWLObjectPropertyExpression) {
            return (Stream<A>) base.listOWLAxioms(OWLSubObjectPropertyOfAxiom.class)
                    .filter(a -> object.equals(sub ? a.getSubProperty() : a.getSuperProperty()));
        }
        if (OWLSubDataPropertyOfAxiom.class.equals(type) && object instanceof OWLDataPropertyExpression) {
            return (Stream<A>) base.listOWLAxioms(OWLSubDataPropertyOfAxiom.class)
                    .filter(a -> object.equals(sub ? a.getSubProperty() : a.getSuperProperty()));
        }
        if (OWLSubAnnotationPropertyOfAxiom.class.equals(type) && object instanceof OWLAnnotationProperty) {
            // the difference: this axiom type is ignored in the original OWL-API method:
            return (Stream<A>) base.listOWLAxioms(OWLSubAnnotationPropertyOfAxiom.class)
                    .filter(a -> object.equals(sub ? a.getSubProperty() : a.getSuperProperty()));
        }
        if (OWLSubClassOfAxiom.class.equals(type) && object instanceof OWLClassExpression) {
            OWLClassExpression c = (OWLClassExpression) object;
            if (c.isOWLClass() && sub) {
                return (Stream<A>) base.listOWLSubClassOfAxioms(c.asOWLClass());
            }
            return (Stream<A>) base.listOWLAxioms(OWLSubClassOfAxiom.class)
                    .filter(a -> c.equals(sub ? a.getSubClass() : a.getSuperClass()));
        }
        if (OWLEquivalentClassesAxiom.class.equals(type) && sub && object instanceof OWLClassExpression) {
            OWLClassExpression c = (OWLClassExpression) object;
            if (c.isOWLClass()) {
                return (Stream<A>) base.listOWLEquivalentClassesAxioms(c.asOWLClass());
            }
            return (Stream<A>) base.listOWLAxioms(OWLEquivalentClassesAxiom.class).filter(a -> a.contains(c));
        }
        if (OWLInverseObjectPropertiesAxiom.class.equals(type) && object instanceof OWLObjectPropertyExpression) {
            return (Stream<A>) base.listOWLAxioms(OWLInverseObjectPropertiesAxiom.class)
                    .filter(a -> object.equals(sub ? a.getFirstProperty() : a.getSecondProperty()));
        }
        if (OWLObjectPropertyAssertionAxiom.class.equals(type) && object instanceof OWLIndividual) {
            return (Stream<A>) base.listOWLAxioms(OWLObjectPropertyAssertionAxiom.class)
                    .filter(a -> object.equals(sub ? a.getSubject() : a.getObject()));
        }
        if (OWLNegativeObjectPropertyAssertionAxiom.class.equals(type) && object instanceof OWLIndividual) {
            return (Stream<A>) base.listOWLAxioms(OWLNegativeObjectPropertyAssertionAxiom.class)
                    .filter(a -> object.equals(sub ? a.getSubject() : a.getObject()));
        }
        if (OWLAnnotationAssertionAxiom.class.equals(type)) {
            if (!sub && object instanceof OWLAnnotationObject) {
                return (Stream<A>) base.listOWLAxioms(OWLAnnotationAssertionAxiom.class)
                        .filter(a -> object.equals(a.getValue()));
            }
            if (sub && object instanceof OWLAnnotationSubject) {
                return (Stream<A>) base.listOWLAnnotationAssertionAxioms((OWLAnnotationSubject) object);
            }
        }
        if (OWLDisjointUnionAxiom.class.equals(type) && object instanceof OWLClassExpression) {
            return (Stream<A>) base.listOWLAxioms(OWLDisjointUnionAxiom.class)
                    .filter(a -> sub ? object.equals(a.getOWLClass()) : a.classExpressions().anyMatch(object::equals));
        }
        if (OWLSubPropertyChainOfAxiom.class.equals(type) && object instanceof OWLObjectPropertyExpression) {
            return (Stream<A>) base.listOWLAxioms(OWLSubPropertyChainOfAxiom.class)
                    .filter(a -> sub ? object.equals(a.getSuperProperty()) :
                            a.getPropertyChain().stream().anyMatch(object::equals));
        }
        if (OWLClassAxiom.class.equals(type) && object instanceof OWLClass) {
            return (Stream<A>) axioms((OWLClass) object);
        }
        if (OWLObjectPropertyAxiom.class.equals(type) && object instanceof OWLObjectPropertyExpression) {
            return (Stream<A>) axioms((OWLObjectPropertyExpression) object);
        }
        if (OWLDataPropertyAxiom.class.equals(type) && object instanceof OWLDataProperty) {
            return (Stream<A>) axioms((OWLDataProperty) object);
        }
        if (OWLIndividualAxiom.class.equals(type) && object instanceof OWLIndividual) {
            return (Stream<A>) axioms((OWLIndividual) object);
        }
        if (OWLNaryAxiom.class.isAssignableFrom(type)) {
            return base.listOWLAxioms(type)
                    .filter(a -> ((OWLNaryAxiom) a).operands().anyMatch(o -> Objects.equals(o, object)));
        }
        // default:
        return base.listOWLAxioms(type, object);
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(Imports imports) {
        // WARNING: the class ru.avicomp.ontapi.OWLOntologyWrapper has its own implementation that overrides this one
        return AxiomType.TBoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        // WARNING: the class ru.avicomp.ontapi.OWLOntologyWrapper has its own implementation that overrides this one
        return AxiomType.ABoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        // WARNING: the class ru.avicomp.ontapi.OWLOntologyWrapper has its own implementation that overrides this one
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
        Stream<? extends OWLNaryClassAxiom> naryClassAxioms = Stream.of(OWLEquivalentClassesAxiom.class, OWLDisjointClassesAxiom.class)
                .flatMap(base::listOWLAxioms)
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
        return axioms(Filters.subAnnotationWithSub, property);
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return axioms(Filters.apDomainFilter, property);
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return axioms(Filters.apRangeFilter, property);
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        return axioms(Filters.datatypeDefFilter, datatype);
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
        return containsAxiom(axiom) || axioms(axiom.getAxiomType()).anyMatch(ax -> ax.equalsIgnoreAnnotations(axiom));
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
        return axioms(OWLDeclarationAxiom.class, subject, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity) {
        return axioms(OWLAnnotationAssertionAxiom.class, OWLAnnotationSubject.class, entity, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass clazz) {
        return axioms(OWLSubClassOfAxiom.class, OWLClass.class, clazz, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass clazz) {
        return axioms(OWLSubClassOfAxiom.class, OWLClass.class, clazz, Navigation.IN_SUPER_POSITION);
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass clazz) {
        return axioms(OWLEquivalentClassesAxiom.class, OWLClass.class, clazz, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass clazz) {
        return axioms(OWLDisjointClassesAxiom.class, OWLClass.class, clazz, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass clazz) {
        return axioms(OWLDisjointUnionAxiom.class, OWLClass.class, clazz, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass clazz) {
        return axioms(OWLHasKeyAxiom.class, OWLClass.class, clazz, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return axioms(OWLSubObjectPropertyOfAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return axioms(OWLSubObjectPropertyOfAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUPER_POSITION);
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLObjectPropertyDomainAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLObjectPropertyRangeAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLInverseObjectPropertiesAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLEquivalentObjectPropertiesAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLDisjointObjectPropertiesAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLFunctionalObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLInverseFunctionalObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLSymmetricObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLAsymmetricObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLReflexiveObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLIrreflexiveObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return axioms(OWLTransitiveObjectPropertyAxiom.class, OWLObjectPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return axioms(OWLSubDataPropertyOfAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return axioms(OWLSubDataPropertyOfAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUPER_POSITION);
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        return axioms(OWLDataPropertyDomainAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        return axioms(OWLDataPropertyRangeAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(OWLDataProperty property) {
        return axioms(OWLEquivalentDataPropertiesAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(OWLDataProperty property) {
        return axioms(OWLDisjointDataPropertiesAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return axioms(OWLFunctionalDataPropertyAxiom.class, OWLDataPropertyExpression.class,
                property, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        return axioms(OWLClassAssertionAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression ce) {
        return axioms(OWLClassAssertionAxiom.class, OWLClassExpression.class,
                ce, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(OWLIndividual individual) {
        return axioms(OWLDataPropertyAssertionAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(OWLIndividual individual) {
        return axioms(OWLObjectPropertyAssertionAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return axioms(OWLNegativeObjectPropertyAssertionAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return axioms(OWLNegativeDataPropertyAssertionAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        return axioms(OWLSameIndividualAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(OWLIndividual individual) {
        return axioms(OWLDifferentIndividualsAxiom.class, OWLIndividual.class,
                individual, Navigation.IN_SUB_POSITION);
    }


    /*
     * ======================
     * Serialization methods:
     * ======================
     */

    /**
     * Reads the object while serialization.
     * Note: only the base graph is serialized.
     *
     * @param in {@link ObjectInputStream}
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object could not be found
     * @see OntologyManagerImpl#readObject(ObjectInputStream)
     */
    @SuppressWarnings("JavadocReference")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Graph base = new GraphMem();
        RDFDataMgr.read(base, in, DEFAULT_SERIALIZATION_FORMAT.getLang());
        // set temporary model with default personality, it will be reset inside manager while its #readObject
        setBase(BaseModel.createInternalModel(base));
    }

    /**
     * Writes the object while serialization.
     * Note: only the base graph is serialized,
     * so if you serialize and then de-serialize standalone ontology it will loss all its references,
     * please use managers serialization, it will restore any links.
     * Also please note: an exception is expected if the encapsulated graph is not {@link GraphMem}.
     *
     * @param out {@link ObjectOutputStream}
     * @throws IOException     if I/O errors occur while writing to the underlying <code>OutputStream</code>
     * @throws OntApiException in case this instance encapsulates graph which is not plain in-memory graph
     */
    private void writeObject(ObjectOutputStream out) throws IOException, OntApiException {
        Graph g = base.getBaseGraph();
        if (!(g instanceof GraphMem))
            throw new OntApiException(getOntologyID() + ":: Serialization is not supported for " + g.getClass());
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
        if (!(obj instanceof OntologyModel)) {
            return false;
        }
        OntGraphModel right = ((OntologyModel) obj).asGraphModel();
        OntGraphModel left = getBase();
        return left.getID().sameAs(right.getID());
    }
}
