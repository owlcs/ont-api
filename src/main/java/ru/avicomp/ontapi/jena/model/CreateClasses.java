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

import java.util.Collection;

/**
 * A technical interface to generate {@link OntCE Class Expression}s.
 * Created by @ssz on 13.05.2019.
 *
 * @since 1.4.0
 */
interface CreateClasses {

    /**
     * Creates an Existential Quantification Object Property Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:someValuesFrom C .
     * }</pre>
     *
     * @param property {@link OntOPE object property expression}, not {@code null}
     * @param ce       {@link OntCE class expression}, not {@code null}
     * @return {@link OntCE.ObjectSomeValuesFrom}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Existential_Quantification'>8.2.1 Existential Quantification</a>
     */
    OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE property, OntCE ce);

    /**
     * Creates an Existential Quantification Data Property Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:someValuesFrom D .
     * }</pre>
     *
     * @param property {@link OntNDP data property}, not {@code null}
     * @param dr       {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.DataSomeValuesFrom}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Existential_Quantification_2'>8.4.1 Existential Quantification</a>
     */
    OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP property, OntDR dr);

    /**
     * Creates an Universal Quantification Object Property Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:allValuesFrom C .
     * }</pre>
     *
     * @param property {@link OntOPE object property expression}, not {@code null}
     * @param ce       {@link OntCE class expression}, not {@code null}
     * @return {@link OntCE.ObjectAllValuesFrom}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Universal_Quantification'>8.2.2 Universal Quantification</a>
     */
    OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE property, OntCE ce);

    /**
     * Creates an Universal Quantification Data Property Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:allValuesFrom D .
     * }</pre>
     *
     * @param property {@link OntNDP data property}, not {@code null}
     * @param dr       {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.DataAllValuesFrom}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Universal_Quantification_2'>8.4.2 Universal Quantification</a>
     */
    OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP property, OntDR dr);

    /**
     * Creates an Individual Value Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:hasValue a .
     * }</pre>
     *
     * @param property   {@link OntOPE object property expression}, not {@code null}
     * @param individual {@link OntIndividual}, not {@code null}
     * @return {@link OntCE.ObjectHasValue}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Individual_Value_Restriction'>8.2.3 Individual Value Restriction</a>
     */
    OntCE.ObjectHasValue createObjectHasValue(OntOPE property, OntIndividual individual);

    /**
     * Creates a Literal Value Restriction.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:hasValue v .
     * }</pre>
     *
     * @param property {@link OntNDP data property}, not {@code null}
     * @param literal  {@link Literal}, not {@code null}
     * @return {@link OntCE.DataHasValue}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Literal_Value_Restriction'>8.4.3 Literal Value Restriction</a>
     */
    OntCE.DataHasValue createDataHasValue(OntNDP property, Literal literal);

    /**
     * Creates an Object Minimum Cardinality Restriction, possible Qualified.
     * If {@code ce} is {@code null}, it is taken to be {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Thing owl:Thing}.
     * In that case the return restriction is unqualified.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:minCardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:minQualifiedCardinality n .
     * _:x owl:onClass C .
     * }</pre>
     *
     * @param property    {@link OntOPE object property expression}, not {@code null}
     * @param cardinality int, non-negative number
     * @param ce          {@link OntCE class expression} or {@code null}
     * @return {@link OntCE.ObjectMinCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Minimum_Cardinality'>8.3.1 Minimum Cardinality</a>
     */
    OntCE.ObjectMinCardinality createObjectMinCardinality(OntOPE property, int cardinality, OntCE ce);

    /**
     * Creates a Data Minimum Cardinality Restriction, possible Qualified.
     * If {@code dr} is {@code null}, it is taken to be {@link org.apache.jena.vocabulary.RDFS#Literal rdfs:Literal}.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:minCardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:minQualifiedCardinality n .
     * _:x owl:onDataRange D .
     * }</pre>
     *
     * @param property    {@link OntNDP data property}, not {@code null}
     * @param cardinality int, non-negative number
     * @param dr          {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.DataMinCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Minimum_Cardinality_2'>8.5.1 Minimum Cardinality</a>
     */
    OntCE.DataMinCardinality createDataMinCardinality(OntNDP property, int cardinality, OntDR dr);

    /**
     * Creates an Object Maximum Cardinality Restriction, possible Qualified.
     * If {@code ce} is {@code null}, it is taken to be {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Thing owl:Thing}.
     * In that case the return restriction is unqualified.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:maxCardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:maxQualifiedCardinality n .
     * _:x owl:onClass C .
     * }</pre>
     *
     * @param property     {@link OntOPE object property expression}, not {@code null}
     * @param cardinality, int, non-negative number
     * @param ce           {@link OntCE class expression} or {@code null}
     * @return {@link OntCE.ObjectMaxCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Maximum_Cardinality'>8.3.2 Maximum Cardinality</a>
     */
    OntCE.ObjectMaxCardinality createObjectMaxCardinality(OntOPE property, int cardinality, OntCE ce);

    /**
     * Creates a Data Maximum Cardinality Restriction, possible Qualified.
     * If {@code dr} is {@code null}, it is taken to be {@link org.apache.jena.vocabulary.RDFS#Literal rdfs:Literal}.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:maxCardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:maxQualifiedCardinality n .
     * _:x owl:onDataRange D .
     * }</pre>
     *
     * @param property    {@link OntNDP data property}, not {@code null}
     * @param cardinality int, non-negative number
     * @param dr          {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.DataMaxCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Maximum_Cardinality_2'>8.5.2 Maximum Cardinality</a>
     */
    OntCE.DataMaxCardinality createDataMaxCardinality(OntNDP property, int cardinality, OntDR dr);

    /**
     * Creates an Object Exact Cardinality Restriction, possible Qualified.
     * If {@code ce} is {@code null}, it is taken to be {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Thing owl:Thing}.
     * In that case the return restriction is unqualified.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:cardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:qualifiedCardinality n .
     * _:x owl:onClass C .
     * }</pre>
     *
     * @param property     {@link OntOPE object property expression}, not {@code null}
     * @param cardinality, int, non-negative number
     * @param ce           {@link OntCE class expression} or {@code null}
     * @return {@link OntCE.ObjectCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Exact_Cardinality'>8.3.3 Exact Cardinality</a>
     */
    OntCE.ObjectCardinality createObjectCardinality(OntOPE property, int cardinality, OntCE ce);

    /**
     * Creates a Data Exact Cardinality Restriction, possible Qualified.
     * If {@code dr} is {@code null}, it is taken to be {@link org.apache.jena.vocabulary.RDFS#Literal rdfs:Literal}.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:cardinality n .
     * } or {@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty R .
     * _:x owl:qualifiedCardinality n .
     * _:x owl:onDataRange D .
     * }</pre>
     *
     * @param property    {@link OntNDP data property}, not {@code null}
     * @param cardinality int, non-negative number
     * @param dr          {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.DataCardinality}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Exact_Cardinality_2'>8.5.3 Exact Cardinality</a>
     */
    OntCE.DataCardinality createDataCardinality(OntNDP property, int cardinality, OntDR dr);

    /**
     * Creates a Local Reflexivity Class Expression (Self-Restriction).
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperty P .
     * _:x owl:hasSelf "true"^^xsd:boolean .
     * }</pre>
     *
     * @param property {@link OntOPE object property expression}, not {@code null}
     * @return {@link OntCE.HasSelf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Self-Restriction'>8.2.4 Self-Restriction</a>
     */
    OntCE.HasSelf createHasSelf(OntOPE property);

    /**
     * Creates an Union of Class Expressions.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Class .
     * _:x owl:unionOf ( C1 ... Cn ) .
     * }</pre>
     *
     * @param classes {@code Collection} of {@link OntCE class expression}s without {@code null}s
     * @return {@link OntCE.UnionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Union_of_Class_Expressions'>8.1.2 Union of Class Expressions</a>
     */
    OntCE.UnionOf createUnionOf(Collection<OntCE> classes);

    /**
     * Creates an Intersection of Class Expressions.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Class .
     * _:x owl:intersectionOf ( C1 ... Cn ) .
     * }</pre>
     *
     * @param classes {@code Collection} of {@link OntCE class expression}s without {@code null}s
     * @return {@link OntCE.IntersectionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Intersection_of_Class_Expressions'>8.1.1 Intersection of Class Expressions</a>
     */
    OntCE.IntersectionOf createIntersectionOf(Collection<OntCE> classes);

    /**
     * Creates an Enumeration of Individuals.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Class .
     * _:x owl:oneOf ( a1 ... an ).
     * }</pre>
     *
     * @param individuals {@code Collection} of {@link OntIndividual individual}s without {@code null}s
     * @return {@link OntCE.OneOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Enumeration_of_Individuals'>8.1.4 Enumeration of Individuals</a>
     */
    OntCE.OneOf createOneOf(Collection<OntIndividual> individuals);

    /**
     * Create a Complement of Class Expressions.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Class .
     * _:x owl:complementOf C .
     * }</pre>
     *
     * @param ce {@link OntCE class expression} or {@code null}
     * @return {@link OntCE.ComplementOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Complement_of_Class_Expressions'>8.1.3 Complement of Class Expressions</a>
     */
    OntCE.ComplementOf createComplementOf(OntCE ce);

    /**
     * Creates a N-Ary Data Universal Quantification N-Ary Restriction.
     * Note: currently an Unary Restriction is preferable since in OWL2 data-range arity is always {@code 1}.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperties ( R1 ... Rn ) .
     * _:x owl:allValuesFrom Dn .
     * }</pre>
     *
     * @param properties {@code Collection} of {@link OntDR data range}s without {@code null}s
     * @param dr         {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.NaryDataAllValuesFrom}
     * @see OntDR#arity()
     * @see #createDataAllValuesFrom(OntNDP, OntDR)
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Universal_Quantification_2'>8.4.2 Universal Quantification</a>
     */
    OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Collection<OntNDP> properties, OntDR dr);

    /**
     * Creates a N-Ary Data Existential Quantification N-Ary Restriction.
     * Note: currently an Unary Restriction is preferable since in OWL2 data-range arity is always {@code 1}.
     * The RDF structure:
     * <pre>{@code
     * _:x rdf:type owl:Restriction .
     * _:x owl:onProperties ( R1 ... Rn ) .
     * _:x owl:someValuesFrom Dn .
     * }</pre>
     *
     * @param properties {@code Collection} of {@link OntDR data range}s without {@code null}s
     * @param dr         {@link OntDR data range}, not {@code null}
     * @return {@link OntCE.NaryDataAllValuesFrom}
     * @see OntDR#arity()
     * @see #createDataSomeValuesFrom(OntNDP, OntDR)
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Existential_Quantification_2'>8.4.1 Existential Quantification</a>
     */
    OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Collection<OntNDP> properties, OntDR dr);

}
