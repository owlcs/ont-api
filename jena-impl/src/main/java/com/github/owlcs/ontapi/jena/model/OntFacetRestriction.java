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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;

/**
 * Interface encapsulating an Ontology Facet Restriction abstraction.
 * <p>
 * Created by @szuev on 02.11.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/#Facets'>3.2 Facets</a>
 * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-facets'>4.3 Constraining Facets</a>
 * @see com.github.owlcs.ontapi.jena.vocabulary.XSD
 * @see OntModel#createFacetRestriction(Class, Literal)
 */
public interface OntFacetRestriction extends OntObject {
    /**
     * Answers the value of this Facet Restriction.
     *
     * @return {@link Literal}
     */
    Literal getValue();

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-pattern'>4.3.4 pattern</a>
     */
    interface Pattern extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-length'>4.3.1 length</a>
     */
    interface Length extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-minLength'>4.3.2 minLength</a>
     */
    interface MinLength extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-maxLength'>4.3.3 maxLength</a>
     */
    interface MaxLength extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-minInclusive'>4.3.10 minInclusive</a>
     */
    interface MinInclusive extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-maxInclusive'>4.3.7 maxInclusive</a>
     */
    interface MaxInclusive extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-minExclusive'>4.3.9 minExclusive</a>
     */
    interface MinExclusive extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-maxExclusive'>4.3.8 maxExclusive</a>
     */
    interface MaxExclusive extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-totalDigits'>4.3.11 totalDigits</a>
     */
    interface TotalDigits extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/xmlschema11-2/#rf-fractionDigits'>4.3.12 fractionDigits</a>
     */
    interface FractionDigits extends OntFacetRestriction {
    }

    /**
     * @see <a href='https://www.w3.org/TR/rdf-plain-literal/#langRange'>Table 1. The Facet Space of rdf:PlainLiteral</a>
     */
    interface LangRange extends OntFacetRestriction {
    }

}
