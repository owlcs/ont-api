/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.config;

/**
 * A common interface to access axioms settings.
 * <p>
 * Created by @ssz on 15.03.2019.
 *
 * @since 1.4.0
 */
public interface AxiomsSettings {

    /**
     * Answers whether annotation axioms (instances of {@link org.semanticweb.owlapi.model.OWLAnnotationAxiom})
     * should retrieve from the {@code Graph}.
     * If {@code false} then {@code Annotation Property Domain}, {@code Annotation Property Range},
     * {@code Annotation Property Assertion} and {@code SubAnnotationPropertyOf} OWL Axioms are ignored.
     * It is native OWL-API option, in OWL-API-impl it controls axioms loading from a source,
     * while in ONT-API it is a graph reading option,
     * which can be turned on or off without any actual changing underlying data.
     * By default, the reading (loading) of annotation axioms is enabled (i.e. the method returns {@code true}).
     * <p>
     * Note: the behaviour is slightly different from OWL-API (checked version: 5.1.4):
     * if read annotation axioms is disabled, all {@code Annotation Property Assertion} axioms
     * turn into OWL Annotations in the composition of the nearest declaration axioms.
     * For example, consider the snippet:
     * <pre>{@code
     * <C>  rdf:type      owl:Class ;
     *      rdfs:comment  "comment1"@ex .
     * }</pre>
     * If this option is off, there above RDF corresponds to the axiom
     * {@code Declaration(Annotation(rdfs:comment "comment1"@es) Class(<C>))} in ONT-API structural view,
     * while in OWL-API it would be just naked declaration (i.e. {@code Declaration(Class(<C>))}).
     *
     * @return boolean
     * @see OntSettings#OWL_API_LOAD_CONF_LOAD_ANNOTATIONS
     * @see AxiomsControl#setLoadAnnotationAxioms(boolean)
     */
    boolean isLoadAnnotationAxioms();

    /**
     * Answers whether the bulk annotations is allowed in declaration axioms or
     * annotations should go separately as annotation assertion axioms.
     * More specifically, this optional setting stands to manage
     * behaviour of annotation assertions in conjunction with declarations:
     * depending on this parameter bulk annotations fall either into declaration or annotation assertion.
     * Consider the following RDF example:
     * <pre>{@code
     * <C>   a                       owl:Class ;
     *       rdfs:comment            "plain assertion" ;
     *       rdfs:label              "bulk assertion" .
     * [     a                       owl:Axiom ;
     *       rdfs:comment            "the child" ;
     *       owl:annotatedProperty   rdfs:label ;
     *       owl:annotatedSource     <C> ;
     *       owl:annotatedTarget     "bulk assertion"
     * ] .
     * }</pre>
     * In case this option is turned on, the RDF slice above corresponds to the following list of axioms:
     * <ul>
     * <li>{@code AnnotationAssertion(rdfs:comment <C> "plain assertion"^^xsd:string)}</li>
     * <li>{@code AnnotationAssertion(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label <C> "bulk assertion"^^xsd:string)}</li>
     * <li>{@code Declaration(Class(<C>))}</li>
     * </ul>
     * In case it is turned off there would be the following axioms:
     * <ul>
     * <li>{@code Declaration(Annotation(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label "bulk assertion"^^xsd:string) Class(<C>))}</li>
     * <li>{@code AnnotationAssertion(rdfs:comment <C> "plain assertion"^^xsd:string)}</li>
     * </ul>
     * Note: the {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat Manchester OWL-API Parser}
     * does NOT seem to work correctly in the second case (checked version: 5.1.4) -
     * the loss of annotations is expected in case of reload ontology in manchester syntax.
     * By default, annotated annotation assertions are allowed (the method answers {@code true}).
     *
     * @return boolean
     * @see OntSettings#ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS
     * @see AxiomsControl#setAllowBulkAnnotationAssertions(boolean)
     */
    boolean isAllowBulkAnnotationAssertions();

    /**
     * Answers whether the Range, Domain and SubClassOf axioms should be separated
     * in case there is a 'punning' in annotation property with the other property (data or object).
     * More specifically, the option determines the behavior while reading annotation axioms
     * in case of a punning entity as subject in the root statement.
     * There are three types of annotation axioms with following defining statements:
     * <ul>
     * <li>{@code Ai rdfs:subPropertyOf Aj}</li>
     * <li>{@code A rdfs:domain U}</li>
     * <li>{@code A rdfs:range U}</li>
     * </ul>
     * If the annotation property {@code A}
     * is also an object property ({@code P}) or data property ({@code R})
     * then, in case this option is turned on,
     * the statements above define also corresponded object or data property axioms.
     * Otherwise, (if the option is turned off), the annotation axioms of above type
     * are ignored in favour of object or data property axioms.
     * <p>
     * Please note: {@link com.github.owlcs.ontapi.jena.impl.conf.OntPersonality.Punnings}
     * is the general mechanism to control punnings.
     * It is accessible through the config methods
     * {@link OntConfig#getPersonality()} and {@link OntLoaderConfiguration#getPersonality()}.
     * <p>
     * The default value of this option is {@code true}.
     *
     * @return boolean
     * @see OntSettings#ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS
     * @see AxiomsControl#setIgnoreAnnotationAxiomOverlaps(boolean)
     */
    boolean isIgnoreAnnotationAxiomOverlaps();

    /**
     * Answers whether the declaration axioms should be listed.
     * This method is invited to match OWL-API behaviour.
     * Some native OWL-API parsers skips declarations on loading.
     * Using this option you can uniform global behaviour by discarding declarations in live ontology.
     * The default value of this option is {@code true}.
     *
     * @return boolean
     * @see OntSettings#ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS
     * @see AxiomsControl#setAllowReadDeclarations(boolean)
     */
    boolean isAllowReadDeclarations();

    /**
     * Answers whether the different bulk annotations for the same axiom should go as different axioms.
     * If this optional parameter is set to {@code true},
     * each bulk annotation RDF resource will generate a separated axiom.
     * Otherwise, all bulk-annotations RDF resources with the same main statement in axiomatic view
     * go together as a part of a single axiom that corresponds the main triple.
     * Consider the following ontology snippet:
     * <pre>{@code
     * <A>     a                owl:Class ;
     *         rdfs:subClassOf  owl:Thing .
     * [ a                      owl:Axiom ;
     *   rdfs:comment           "X" ;
     *   rdfs:label             "Z" ;
     *   owl:annotatedProperty  rdfs:subClassOf ;
     *   owl:annotatedSource    <A> ;
     *   owl:annotatedTarget    owl:Thing
     * ] .
     * [ a                      owl:Axiom ;
     *   rdfs:comment           "W" ;
     *   owl:annotatedProperty  rdfs:subClassOf ;
     *   owl:annotatedSource    <A> ;
     *   owl:annotatedTarget    owl:Thing
     * ] .
     * }</pre>
     * If {@code isSplitAxiomAnnotations()} equals {@code true}
     * then the ontology above gives the following two axioms:
     * <pre>{@code
     * SubClassOf(Annotation(rdfs:comment "W"^^xsd:string) <A> owl:Thing)
     * SubClassOf(Annotation(rdfs:comment "X"^^xsd:string) Annotation(rdfs:label "Z"^^xsd:string) <A> owl:Thing)
     * }</pre>
     * If {@code isSplitAxiomAnnotations()} equals {@code false},
     * then there is only single {@code SubClassOf} axiom:
     * <pre>{@code
     * SubClassOf(Annotation(rdfs:comment "W"^^xsd:string) Annotation(rdfs:comment "X"^^xsd:string) Annotation(rdfs:label "Z"^^xsd:string) <string:A> owl:Thing)
     * }</pre>
     * The default value of this option is {@code false}.
     *
     * @return boolean
     * @see OntSettings#ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS
     * @see AxiomsControl#setSplitAxiomAnnotations(boolean)
     * @since 1.3.0
     */
    boolean isSplitAxiomAnnotations();

    /**
     * Answers whether errors that arise when parsing axioms from a graph should be ignored.
     * More specifically, it manages handling exceptions in case unable to read axioms of some type from a graph.
     * If specified all errors would be silently ignored.
     * AN RDF recursion (i.e. {@code <A> rdfs:subClassOf <A>}) is an example of such buggy situation.
     * The default value of this option is {@code false}.
     *
     * @return boolean
     * @see OntSettings#ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS
     * @see AxiomsControl#setIgnoreAxiomsReadErrors(boolean)
     * @since 1.1.0
     */
    boolean isIgnoreAxiomsReadErrors();

}
