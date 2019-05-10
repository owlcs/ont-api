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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A common interface for any Ontology <b>O</b>bject <b>P</b>roperty <b>E</b>xpression.
 * In OWL2 there are two types of object property expressions:
 * named object property (entity) and InverseOf anonymous property expression.
 * Range values for this property expression are restricted to individuals
 * (as distinct from datatype valued {@link OntNDP properties}).
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public interface OntOPE extends OntDOP {

    /**
     * Adds a negative property assertion ontology object.
     *
     * @param source {@link OntIndividual}
     * @param target {@link OntIndividual}
     * @return {@link OntNPA.ObjectAssertion}
     * @see OntNDP#addNegativeAssertion(OntIndividual, Literal)
     */
    OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target);

    /**
     * Creates a property chain logical constructions
     * as a {@link OntList ontology []-list} of {@link OntOPE Object Property Expression}s
     * that is attached to this Object Property Expression
     * at object positions with the predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom}.
     * The resulting rdf-list will consist of all the elements of the specified collection
     * in the given order with the possibility of duplication.
     * Note: Any {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
     *
     * @param properties {@link Collection} (preferably {@link List}) of {@link OntOPE object property expression}s
     * @return {@link OntList} of {@link OntOPE}s
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#a_SubObjectPropertyOfChain'>9.2.1 Object Subproperties</a>
     * @see #addPropertyChainAxiomStatement(Collection)
     * @see #addPropertyChain(Collection)
     * @see #listPropertyChains()
     * @see #findPropertyChain(RDFNode)
     * @since 1.3.0
     */
    OntList<OntOPE> createPropertyChain(Collection<OntOPE> properties);

    /**
     * Returns a {@code Stream} over all property chain {@link OntList ontology list}s
     * that are attached to this Object Property Expression
     * on the predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom}.
     *
     * @return {@code Stream} of {@link OntList}s with generic-parameter {@code OntOPE}
     * @since 1.3.0
     */
    Stream<OntList<OntOPE>> listPropertyChains();

    /**
     * Deletes the given property chain list including all its annotations.
     *
     * @param list {@link Resource} can be {@link OntList} or {@link RDFList}
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if in the whole graph there is no property chain with the specified []-list
     * @see #clearPropertyChains()
     * @see #createPropertyChain(Collection)
     * @since 1.3.0
     */
    OntOPE removePropertyChain(Resource list) throws OntJenaException;

    /**
     * {@inheritDoc}
     * Note: a {@code PropertyChain} is not included into consideration:
     * even this property is a member of some chain ({@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code Pj} is this property), it does not mean it has the same super property ({@code P}).
     *
     * @return <b>distinct</b> {@code Stream} of object property expressions
     */
    @Override
    Stream<OntOPE> listSuperProperties(boolean direct);

    /**
     * {@inheritDoc}
     * Note: a {@code PropertyChain} is not included into consideration,
     * even this property is a super property of some chain ({@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code P} is this property), each of chain members is not considered as sub property of this property.
     *
     * @return <b>distinct</b> {@code Stream} of object property expressions
     */
    @Override
    Stream<OntOPE> listSubProperties(boolean direct);

    /**
     * Returns all ranges.
     * The statement pattern is {@code P rdfs:range C}, where {@code P} is this object property,
     * and {@code C} is one of the return class expressions.
     *
     * @return {@code Stream} of {@link OntCE}s
     */
    @Override
    default Stream<OntCE> range() {
        return objects(RDFS.range, OntCE.class);
    }

    /**
     * Returns disjoint properties (statement: {@code P1 owl:propertyDisjointWith P2}).
     *
     * @return {@code Stream} of {@link OntOPE}s
     * @see OntNDP#disjointWith()
     * @see OntDisjoint.ObjectProperties
     */
    @Override
    default Stream<OntOPE> disjointWith() {
        return objects(OWL.propertyDisjointWith, OntOPE.class);
    }

    /**
     * Lists all direct super properties, the pattern is {@code P1 rdfs:subPropertyOf P2}.
     *
     * @return {@code Stream} of {@link OntOPE}s
     * @see #addSuperProperty(OntOPE)
     * @see OntPE#removeSuperProperty(Resource)
     * @see OntPE#listSuperProperties(boolean)
     * @see #listPropertyChains()
     */
    @Override
    default Stream<OntOPE> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntOPE.class);
    }

    /**
     * Returns all equivalent object properties
     * (i.e. {@code Pi owl:equivalentProperty Pj}, where {@code Pi} - this property).
     *
     * @return {@code Stream} of {@link OntOPE}s.
     * @see OntNDP#equivalentProperty()
     */
    @Override
    default Stream<OntOPE> equivalentProperty() {
        return objects(OWL.equivalentProperty, OntOPE.class);
    }

    /**
     * Lists all object properties from the right part of the statement {@code _:this owl:inverseOf P}.
     *
     * @return {@code Stream} of {@link OntOPE}s.
     * @see OntNOP#createInverse()
     */
    default Stream<OntOPE> inverseOf() {
        return objects(OWL.inverseOf, OntOPE.class);
    }

    /**
     * Returns all associated negative object property assertions.
     *
     * @return {@code Stream} of {@link OntNPA.ObjectAssertion}s
     * @see OntNDP#negativeAssertions()
     */
    @Override
    default Stream<OntNPA.ObjectAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.ObjectAssertion.class).filter(a -> OntOPE.this.equals(a.getProperty()));
    }

    /**
     * Returns all associated negative object property assertions for the specified source individual.
     *
     * @param source {@link OntIndividual}
     * @return {@code Stream} of {@link OntNPA.ObjectAssertion}s
     * @see OntNDP#negativeAssertions(OntIndividual)
     */
    default Stream<OntNPA.ObjectAssertion> negativeAssertions(OntIndividual source) {
        return negativeAssertions()
                .filter(a -> a.getSource().equals(source));
    }

    /**
     * Finds the <b>first</b> {@code PropertyChain} logical construction
     * attached to this property for the specified []-list as object.
     *
     * @param list {@link RDFNode}
     * @return {@code Optional} around the {@link OntList ontology []-list}
     * with {@link OntOPE object property expression}s as items
     * @since 1.3.0
     */
    default Optional<OntList<OntOPE>> findPropertyChain(RDFNode list) {
        try (Stream<OntList<OntOPE>> res = listPropertyChains().filter(r -> Objects.equals(r, list))) {
            return res.findFirst();
        }
    }

    /**
     * Deletes all property chain lists including their annotations.
     *
     * @return <b>this</b> instance to allow cascading calls
     * @see #removePropertyChain(Resource)
     * @see #createPropertyChain(Collection)
     * @since 1.3.0
     */
    default OntOPE clearPropertyChains() {
        listPropertyChains().collect(Collectors.toList()).forEach(this::removePropertyChain);
        return this;
    }

    /**
     * Lists all members from the right part of statement {@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code P} is this property.
     * Note: the result ignores repetitions, e.g. for
     * {@code SubObjectPropertyOf( ObjectPropertyChain( :hasParent :hasParent ) :hasGrandparent )},
     * it returns only {@code :hasParent} property.
     *
     * @return <b>distinct</b> {@code Stream} of all sub-{@link OntOPE object properties},
     * possible empty in case of nil-list or if there is no property-chains at all
     * @see #listPropertyChains()
     * @since 1.4.0
     */
    default Stream<OntOPE> fromPropertyChain() {
        return listPropertyChains().flatMap(OntList::members).distinct();
    }

    /**
     * Creates a property chain {@link OntList ontology list}
     * and returns the statement {@code P owl:propertyChainAxiom ( P1 ... Pn )} to allow the addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param properties Array of {@link OntOPE}s without {@code null}s
     * @return {@link OntStatement} to provide the ability to add annotations subsequently
     * @see #createPropertyChain(Collection)
     * @see #addPropertyChainAxiomStatement(Collection)
     * @since 1.4.0
     */
    default OntStatement addPropertyChainAxiomStatement(OntOPE... properties) {
        return addPropertyChainAxiomStatement(Arrays.asList(properties));
    }

    /**
     * Adds a new sub-property-of chain statement.
     *
     * @param properties Collection of {@link OntOPE}s
     * @return {@link OntStatement} (i.e. {@code _:this owl:propertyChainAxiom ( ... )})
     * to provide the ability to add annotations subsequently
     * @see #createPropertyChain(Collection)
     * @see #addPropertyChainAxiomStatement(OntOPE...)
     * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>
     * @since 1.4.0
     */
    default OntStatement addPropertyChainAxiomStatement(Collection<OntOPE> properties) {
        return createPropertyChain(properties).getRoot();
    }

    /**
     * Creates the {@code P rdf:type owl:InverseFunctionalProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setInverseFunctional(boolean)
     * @since 1.4.0
     */
    default OntStatement addInverseFunctionalDeclaration() {
        return addStatement(RDF.type, OWL.InverseFunctionalProperty);
    }

    /**
     * Creates the {@code P rdf:type owl:TransitiveProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setTransitive(boolean)
     * @since 1.4.0
     */
    default OntStatement addTransitiveDeclaration() {
        return addStatement(RDF.type, OWL.TransitiveProperty);
    }

    /**
     * Creates the {@code P rdf:type owl:SymmetricProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setSymmetric(boolean)
     * @since 1.4.0
     */
    default OntStatement addSymmetricDeclaration() {
        return addStatement(RDF.type, OWL.SymmetricProperty);
    }

    /**
     * Creates the {@code P rdf:type owl:AsymmetricProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setAsymmetric(boolean)
     * @since 1.4.0
     */
    default OntStatement addAsymmetricDeclaration() {
        return addStatement(RDF.type, OWL.AsymmetricProperty);
    }

    /**
     * Creates the {@code P rdf:type owl:ReflexiveProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setReflexive(boolean)
     * @since 1.4.0
     */
    default OntStatement addReflexiveDeclaration() {
        return addStatement(RDF.type, OWL.ReflexiveProperty);
    }

    /**
     * Creates the {@code P rdf:type owl:IrreflexiveProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setIrreflexive(boolean)
     * @since 1.4.0
     */
    default OntStatement addIrreflexiveDeclaration() {
        return addStatement(RDF.type, OWL.IrreflexiveProperty);
    }

    /**
     * Adds a property range (i.e. {@code P rdfs:range C} statement).
     *
     * @param range {@link OntCE}, not {@code null}
     * @return {@link OntStatement} to allow processing annotations
     * @see #addRange(OntCE)
     */
    default OntStatement addRangeStatement(OntCE range) {
        return addStatement(RDFS.range, range);
    }

    /**
     * Adds the given property as super property returning a new statement to annotate.
     * The triple pattern is {@code this rdfs:subPropertyOf property}).
     *
     * @param property {@link OntOPE}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     */
    default OntStatement addSubPropertyOfStatement(OntOPE property) {
        return addStatement(RDFS.subPropertyOf, property);
    }

    /**
     * Adds a new inverse-of statement.
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addInverseProperty(OntOPE)
     * @see #inverseOf()
     * @since 1.4.0
     */
    default OntStatement addInverseOfStatement(OntOPE other) {
        return addStatement(OWL.inverseOf, other);
    }

    /**
     * Creates and returns a new {@link OWL#equivalentProperty owl:equivalentProperty} statement
     * with the given property as an object and this property as a subject.
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addEquivalentProperty(OntOPE)
     * @see #removeEquivalentProperty(Resource)
     * @see OntNDP#addEquivalentPropertyStatement(OntNDP)
     * @since 1.4.0
     */
    default OntStatement addEquivalentPropertyStatement(OntOPE other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    /**
     * Adds a disjoint object property (i.e. the {@code _:this owl:propertyDisjointWith @other} statement).
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addDisjointProperty(OntOPE)
     * @see #removeDisjointProperty(Resource)
     * @see OntNDP#addPropertyDisjointWithStatement(OntNDP)
     * @see OntDisjoint.ObjectProperties
     * @since 1.4.0
     */
    default OntStatement addPropertyDisjointWithStatement(OntOPE other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    /**
     * Adds the given property as super property returning this property itself.
     *
     * @param property {@link OntNDP}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see OntPE#removeSuperProperty(Resource)
     * @since 1.4.0
     */
    default OntOPE addSuperProperty(OntOPE property) {
        addSubPropertyOfStatement(property);
        return this;
    }

    /**
     * Adds a statement with the {@link RDFS#range} as predicate
     * and the specified {@link OntCE class expression} as an object.
     *
     * @param range {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addRangeStatement(OntCE)
     */
    default OntOPE addRange(OntCE range) {
        addRangeStatement(range);
        return this;
    }

    /**
     * Adds a statement with the {@link RDFS#domain} as predicate
     * and the specified {@link OntCE class expression} as an object.
     *
     * @param ce {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDomainStatement(OntCE)
     */
    @Override
    default OntOPE addDomain(OntCE ce) {
        addDomainStatement(ce);
        return this;
    }

    /**
     * Adds a new {@link OWL#equivalentProperty owl:equivalentProperty} statement.
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentPropertyStatement(OntOPE)
     * @see OntDOP#removeEquivalentProperty(Resource)
     * @see OntNDP#addEquivalentProperty(OntNDP)
     */
    default OntOPE addEquivalentProperty(OntOPE other) {
        addEquivalentPropertyStatement(other);
        return this;
    }

    /**
     * Adds a new {@link OWL#propertyDisjointWith owl:propertyDisjointWith} statement
     * for this and the specified property.
     *
     * @param other {@link OntNDP}, not {@code null}
     * @return {@link OntNDP} <b>this</b> instance to allow cascading calls
     * @see #addPropertyDisjointWithStatement(OntOPE)
     * @see OntNDP#addDisjointProperty(OntNDP)
     * @see OntDOP#removeDisjointProperty(Resource)
     * @see OntDisjoint.ObjectProperties
     * @since 1.4.0
     */
    default OntOPE addDisjointProperty(OntOPE other) {
        addPropertyDisjointWithStatement(other);
        return this;
    }

    /**
     * Adds a new inverse-of statement, returns this property instance.
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addInverseOfStatement(OntOPE)
     * @see #removeInverseProperty(Resource)
     * @since 1.4.0
     */
    default OntOPE addInverseProperty(OntOPE other) {
        addInverseOfStatement(other);
        return this;
    }

    /**
     * Adds a new sub-property-of chain statement and returns this object itself.
     * Note: the method saves a collection order with possible duplicates.
     *
     * @param properties an {@code Array} of {@link OntOPE object properties}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addPropertyChainAxiomStatement(Collection)
     * @see #addPropertyChain(Collection)
     * @since 1.4.0
     */
    default OntOPE addPropertyChain(OntOPE... properties) {
        return addPropertyChain(Arrays.asList(properties));
    }

    /**
     * Adds a new sub-property-of chain statement and returns this object itself.
     * Note: the method saves a collection order with possible duplicates.
     *
     * @param properties a {@code Collection} of {@link OntOPE object properties}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addPropertyChainAxiomStatement(Collection)
     * @see #addPropertyChain(OntOPE...)
     * @since 1.4.0
     */
    default OntOPE addPropertyChain(Collection<OntOPE> properties) {
        createPropertyChain(properties);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE removeSuperProperty(Resource property) {
        remove(RDFS.subPropertyOf, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE removeEquivalentProperty(Resource property) {
        remove(OWL.equivalentProperty, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE removeDisjointProperty(Resource property) {
        remove(OWL.propertyDisjointWith, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE removeRange(Resource range) {
        remove(RDFS.range, range);
        return this;
    }

    /**
     * Removes the statement with the predicate {@link OWL#inverseOf owl:inverseOf}
     * and the given object property as object.
     * If the argument is {@code null}, all {@code owl:inverseOf} statements will be removed for this object property.
     * No-op in case there is no {@code owl:inverseOf} statements.
     *
     * @param other {@link OntOPE} or {@code null}  to remove all {@code owl:inverseOf} statements
     * @return <b>this</b> instance to allow cascading calls
     * @see #addInverseProperty(OntOPE)
     * @since 1.4.0
     */
    default OntOPE removeInverseProperty(Resource other) {
        remove(OWL.inverseOf, other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntOPE setFunctional(boolean functional) {
        if (functional) {
            addFunctionalDeclaration();
        } else {
            remove(RDF.type, OWL.FunctionalProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:InverseFunctionalProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param inverseFunctional if {@code true} the property must be inverse-functional
     * @return <b>this</b> instance to allow cascading calls
     * @see #addInverseFunctionalDeclaration()
     */
    default OntOPE setInverseFunctional(boolean inverseFunctional) {
        if (inverseFunctional) {
            addInverseFunctionalDeclaration();
        } else {
            remove(RDF.type, OWL.InverseFunctionalProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:TransitiveProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param transitive if {@code true} the property must be transitive
     * @return <b>this</b> instance to allow cascading calls
     * @see #addTransitiveDeclaration()
     */
    default OntOPE setTransitive(boolean transitive) {
        if (transitive) {
            addTransitiveDeclaration();
        } else {
            remove(RDF.type, OWL.TransitiveProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:SymmetricProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param symmetric if {@code true} the property must be symmetric
     * @return <b>this</b> instance to allow cascading calls
     * @see #addSymmetricDeclaration()
     */
    default OntOPE setSymmetric(boolean symmetric) {
        if (symmetric) {
            addSymmetricDeclaration();
        } else {
            remove(RDF.type, OWL.SymmetricProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:AsymmetricProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param asymmetric if {@code true} the property must be asymmetric
     * @return <b>this</b> instance to allow cascading calls
     * @see #addAsymmetricDeclaration()
     */
    default OntOPE setAsymmetric(boolean asymmetric) {
        if (asymmetric) {
            addAsymmetricDeclaration();
        } else {
            remove(RDF.type, OWL.AsymmetricProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:ReflexiveProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param reflexive if {@code true} the property must be reflexive
     * @return <b>this</b> instance to allow cascading calls
     * @see #addReflexiveDeclaration()
     */
    default OntOPE setReflexive(boolean reflexive) {
        if (reflexive) {
            addReflexiveDeclaration();
        } else {
            remove(RDF.type, OWL.ReflexiveProperty);
        }
        return this;
    }

    /**
     * Adds or removes the {@code P rdf:type owl:IrreflexiveProperty} statement depending on the given flag
     * and returns this property to allow cascading calls.
     * Note: the statement is removed along with all its annotations.
     *
     * @param irreflexive if {@code true} the property must be irreflexive
     * @return <b>this</b> instance to allow cascading calls
     * @see #addIrreflexiveDeclaration()
     */
    default OntOPE setIrreflexive(boolean irreflexive) {
        if (irreflexive) {
            addIrreflexiveDeclaration();
        } else {
            remove(RDF.type, OWL.IrreflexiveProperty);
        }
        return this;
    }

    /**
     * Finds the <b>first</b> object property
     * from the right part of the statements {@code _:x owl:inverseOf PN} or {@code P1 owl:inverseOf P2}.
     * Here {@code _:x} is an anonymous object property expression (i.e. {@link Inverse Inverse Object Property}),
     * {@code PN} is a {@link OntNOP named object property}
     * and {@code P1}, {@code P2} are any object property expressions.
     * What exactly is the first statement is defined at the level of model; in general it is unpredictable.
     *
     * @return {@code Optional} of {@link OntOPE}
     * @see #inverseOf()
     * @see Inverse#getDirect()
     * @since 1.4.0
     */
    default Optional<OntOPE> findInverseProperty() {
        try (Stream<OntOPE> res = inverseOf()) {
            return res.findFirst();
        }
    }

    /**
     * @return {@code true} iff it is an inverse functional property
     */
    default boolean isInverseFunctional() {
        return hasType(OWL.InverseFunctionalProperty);
    }

    /**
     * @return {@code true} iff it is a transitive property
     */
    default boolean isTransitive() {
        return hasType(OWL.TransitiveProperty);
    }

    /**
     * @return {@code true} iff it is a symmetric property
     */
    default boolean isSymmetric() {
        return hasType(OWL.SymmetricProperty);
    }

    /**
     * @return {@code true} iff it is an asymmetric property
     */
    default boolean isAsymmetric() {
        return hasType(OWL.AsymmetricProperty);
    }

    /**
     * @return {@code true} iff it is a reflexive property
     */
    default boolean isReflexive() {
        return hasType(OWL.ReflexiveProperty);
    }

    /**
     * @return {@code true} iff it is an irreflexive property
     */
    default boolean isIrreflexive() {
        return hasType(OWL.IrreflexiveProperty);
    }

    /**
     * Add a super-property of this property (i.e. {@code _:this rdfs:subPropertyOf @superProperty} statement).
     *
     * @param superProperty {@link OntOPE}
     * @return {@link OntStatement}
     * @deprecated (since 1.4.0) use the method {@link #addSubPropertyOfStatement(OntOPE)}
     */
    @Deprecated
    default OntStatement addSubPropertyOf(OntOPE superProperty) {
        return addSubPropertyOfStatement(superProperty);
    }

    /**
     * Creates a property chain {@link OntList ontology list}
     * and returns statement {@code P owl:propertyChainAxiom ( P1 ... Pn )} to allow the addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param properties Array of {@link OntOPE}s without {@code null}s
     * @return {@link OntStatement}
     * @since 1.3.0
     * @deprecated since 1.4.0; please use the method {@link #addPropertyChainAxiomStatement(OntOPE...)}
     */
    @Deprecated
    default OntStatement addSuperPropertyOf(OntOPE... properties) {
        return addPropertyChainAxiomStatement(properties);
    }

    /**
     * Adds new sub-property-of chain.
     *
     * @param properties Collection of {@link OntOPE}s
     * @return the {@link OntStatement} ({@code _:this owl:propertyChainAxiom ( ... )})
     * @deprecated since 1.4.0; please use the method {@link #addPropertyChainAxiomStatement(Collection)}
     */
    @Deprecated
    default OntStatement addSuperPropertyOf(Collection<OntOPE> properties) {
        return addPropertyChainAxiomStatement(properties);
    }

    /**
     * Adds a new inverse-of statement.
     *
     * @param other {@link OntOPE}
     * @return {@link OntStatement}
     * @deprecated since 1.4.0; use the method {@link #addInverseOfStatement(OntOPE)}
     */
    @Deprecated
    default OntStatement addInverseOf(OntOPE other) {
        return addInverseOfStatement(other);
    }

    /**
     * Gets the <b>first</b> object property
     * from the right part of the statements {@code _:x owl:inverseOf PN} or {@code P1 owl:inverseOf P2}.
     * What exactly is the first statement is defined at the level of graph; in general it is unpredictable.
     *
     * @return {@link OntOPE} or {@code null}
     * @deprecated since 1.4.0; use the method {@link #findInverseProperty()}
     */
    @Deprecated
    default OntOPE getInverseOf() {
        return findInverseProperty().orElse(null);
    }

    /**
     * Removes the statement with predicate {@code owl:inverseOf} and the given object property as object.
     *
     * @param other {@link OntOPE}
     * @deprecated since 1.4.0; use the method {@link #removeInverseProperty(Resource)}
     */
    @Deprecated
    default void removeInverseOf(OntOPE other) {
        removeInverseProperty(other);
    }

    /**
     * Clears all {@code P1 owl:propertyDisjointWith P2} statements
     * for the specified object property (subject, {@code P1}).
     *
     * @param other {@link OntOPE}, not {@code null}
     * @deprecated since 1.4.0; use the method {@link #removeDisjointProperty(Resource)}
     */
    @Deprecated
    default void removeDisjointWith(OntOPE other) {
        removeDisjointProperty(other);
    }

    /**
     * Adds a disjoint object property (i.e. {@code _:this owl:propertyDisjointWith @other} statement).
     *
     * @param other {@link OntOPE}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @deprecated since 1.4.0; use the method {@link #addDisjointProperty(OntOPE)}
     */
    @Deprecated
    default OntStatement addDisjointWith(OntOPE other) {
        return addPropertyDisjointWithStatement(other);
    }

    /**
     * Represents a <a href="http://www.w3.org/TR/owl2-syntax/#Inverse_Object_Properties">ObjectInverseOf</a>.
     * Anonymous triple {@code _:x owl:inverseOf PN} which is also object property expression.
     */
    interface Inverse extends OntOPE {

        /**
         * Returns a named object property companion.
         * Every {@link Inverse} property has its own {@link OntNOP} property.
         * The triple pattern is {@code _:x owl:inverseOf PN}.
         *
         * @return {@link OntNDP}, not {@code null}
         */
        OntNOP getDirect();
    }
}
