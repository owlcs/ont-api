/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.transforms.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * A collection of deprecated IRIs, that are used in obsolete ontologies,
 * possible written in OWL1.1 specification or in some other propositional OWL1 dialects.
 * As shown by the OWL-API-contract-tests an OWL-API ontology (which is usually in RDF/XML format)
 * might contain these inappropriate IRIs (as {@link Property} or {@link Resource}).
 * <p>
 * Created by @ssz on 27.02.2019.
 *
 * @see com.github.owlcs.ontapi.jena.vocabulary.OWL the correct OWL2 vocabulary
 * @see org.apache.jena.vocabulary.OWL the correct OWL1 vocabulary
 * @see com.github.owlcs.ontapi.jena.vocabulary.XSD predicates that are used in facet restrictions
 * @see org.semanticweb.owlapi.vocab.OWLRDFVocabulary
 */
public class DEPRECATED {

    public static class OWL {

        public final static String NS = com.github.owlcs.ontapi.jena.vocabulary.OWL.NS;

        public static final Property propertyChain = property("propertyChain");
        public static final Property declaredAs = property("declaredAs");
        // used in annotations and negative assertions:
        public static final Property subject = property("subject");
        public static final Property object = property("object");
        public static final Property predicate = property("predicate");

        public static final Property objectPropertyDomain = property("objectPropertyDomain");
        public static final Property dataPropertyDomain = property("dataPropertyDomain");
        public static final Property objectPropertyRange = property("objectPropertyRange");
        public static final Property dataPropertyRange = property("dataPropertyRange");
        public static final Property subObjectPropertyOf = property("subObjectPropertyOf");
        public static final Property subDataPropertyOf = property("subDataPropertyOf");

        public static final Property disjointDataProperties = property("disjointDataProperties");
        public static final Property disjointObjectProperties = property("disjointObjectProperties");
        public static final Property equivalentDataProperty = property("equivalentDataProperty");
        public static final Property equivalentObjectProperty = property("equivalentObjectProperty");

        public static final Resource SelfRestriction = resource("SelfRestriction");
        public static final Resource DataRestriction = resource("DataRestriction");
        public static final Resource ObjectRestriction = resource("ObjectRestriction");

        public static final Resource NegativeObjectPropertyAssertion = resource("NegativeObjectPropertyAssertion");
        public static final Resource NegativeDataPropertyAssertion = resource("NegativeDataPropertyAssertion");

        public static final Resource DataProperty = resource("DataProperty");
        public static final Resource AntisymmetricProperty = resource("AntisymmetricProperty");
        public static final Resource FunctionalDataProperty = resource("FunctionalDataProperty");
        public static final Resource FunctionalObjectProperty = resource("FunctionalObjectProperty");

        // unknown things (don't know exactly where their from):
        public static final Property dataComplementOf = property("dataComplementOf");
        public static final Property minInclusive = property("minInclusive");
        public static final Property maxInclusive = property("maxInclusive");
        public static final Property maxExclusive = property("maxExclusive");
        public static final Property minExclusive = property("minExclusive");

        private static Resource resource(String local) {
            return ResourceFactory.createResource(NS + local);
        }

        private static Property property(String local) {
            return ResourceFactory.createProperty(NS + local);
        }
    }

    public static class RDF {
        public final static String NS = com.github.owlcs.ontapi.jena.vocabulary.RDF.getURI();

        // used in negative assertions:
        public static final Property subject = property("subject");
        public static final Property object = property("object");
        public static final Property predicate = property("predicate");

        private static Property property(String local) {
            return ResourceFactory.createProperty(NS + local);
        }
    }


}
