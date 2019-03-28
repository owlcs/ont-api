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

package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary definition for the
 * <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 *
 * @see <a href='https://www.w3.org/Submission/SWRL/swrl.rdf'>SWRL schema</a>
 * @see org.semanticweb.owlapi.vocab.SWRLVocabulary
 * @see SWRLB
 */
public class SWRL {
    public final static String URI = "http://www.w3.org/2003/11/swrl";
    public final static String NS = URI + "#";

    public static final Resource Imp = resource("Imp");
    public static final Resource IndividualPropertyAtom = resource("IndividualPropertyAtom");
    public static final Resource DatavaluedPropertyAtom = resource("DatavaluedPropertyAtom");
    public static final Resource ClassAtom = resource("ClassAtom");
    public static final Resource DataRangeAtom = resource("DataRangeAtom");
    public static final Resource Variable = resource("Variable");
    public static final Resource AtomList = resource("AtomList");
    public static final Resource SameIndividualAtom = resource("SameIndividualAtom");
    public static final Resource DifferentIndividualsAtom = resource("DifferentIndividualsAtom");
    public static final Resource BuiltinAtom = resource("BuiltinAtom");
    public static final Resource Builtin = resource("Builtin");

    public static final Property head = property("head");
    public static final Property body = property("body");
    public static final Property classPredicate = property("classPredicate");
    public static final Property dataRange = property("dataRange");
    public static final Property propertyPredicate = property("propertyPredicate");
    public static final Property builtin = property("builtin");
    public static final Property arguments = property("arguments");
    public static final Property argument1 = property("argument1");
    public static final Property argument2 = property("argument2");

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }

}
