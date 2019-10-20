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

package ru.avicomp.ontapi.owlapi;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.*;

/**
 * Entities that are commonly used in implementations.
 * A modified copy-paste from {@code uk.ac.manchester.cs.owl.owlapi.InternalizedEntities}.
 *
 * @author ignazio
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/InternalizedEntities.java'>uk.ac.manchester.cs.owl.owlapi.InternalizedEntities</a>
 */
public class InternalizedEntities {

    public static final OWLClass OWL_THING = OWLClassImpl.fromResource(OWL.Thing);
    public static final OWLClass OWL_NOTHING = OWLClassImpl.fromResource(OWL.Nothing);

    public static final OWLObjectProperty OWL_TOP_OBJECT_PROPERTY = OWLObjectPropertyImpl.fromResource(OWL.topObjectProperty);
    public static final OWLObjectProperty OWL_BOTTOM_OBJECT_PROPERTY = OWLObjectPropertyImpl.fromResource(OWL.bottomObjectProperty);

    public static final OWLDataProperty OWL_TOP_DATA_PROPERTY = OWLDataPropertyImpl.fromResource(OWL.topDataProperty);
    public static final OWLDataProperty OWL_BOTTOM_DATA_PROPERTY = OWLDataPropertyImpl.fromResource(OWL.bottomDataProperty);

    public static final OWLAnnotationProperty RDFS_LABEL = OWLAnnotationPropertyImpl.fromResource(RDFS.label);
    public static final OWLAnnotationProperty RDFS_COMMENT = OWLAnnotationPropertyImpl.fromResource(RDFS.comment);
    public static final OWLAnnotationProperty RDFS_SEE_ALSO = OWLAnnotationPropertyImpl.fromResource(RDFS.seeAlso);
    public static final OWLAnnotationProperty RDFS_IS_DEFINED_BY = OWLAnnotationPropertyImpl.fromResource(RDFS.isDefinedBy);
    public static final OWLAnnotationProperty OWL_BACKWARD_COMPATIBLE_WITH = OWLAnnotationPropertyImpl.fromResource(OWL.backwardCompatibleWith);
    public static final OWLAnnotationProperty OWL_INCOMPATIBLE_WITH = OWLAnnotationPropertyImpl.fromResource(OWL.incompatibleWith);
    public static final OWLAnnotationProperty OWL_VERSION_INFO = OWLAnnotationPropertyImpl.fromResource(OWL.versionInfo);
    public static final OWLAnnotationProperty OWL_DEPRECATED = OWLAnnotationPropertyImpl.fromResource(OWL.deprecated);

    public static final OWLDatatype RDFS_LITERAL = OWLBuiltinDatatypeImpl.fromResource(RDFS.Literal);
    public static final OWLDatatype RDF_PLAIN_LITERAL = OWLBuiltinDatatypeImpl.fromResource(RDF.PlainLiteral);
    public static final OWLDatatype RDF_LANG_STRING = OWLBuiltinDatatypeImpl.fromResource(RDF.langString);
    public static final OWLDatatype XSD_STRING = OWLBuiltinDatatypeImpl.fromResource(XSD.xstring);
    public static final OWLDatatype XSD_BOOLEAN = OWLBuiltinDatatypeImpl.fromResource(XSD.xboolean);
    public static final OWLDatatype XSD_DOUBLE = OWLBuiltinDatatypeImpl.fromResource(XSD.xdouble);
    public static final OWLDatatype XSD_FLOAT = OWLBuiltinDatatypeImpl.fromResource(XSD.xfloat);
    public static final OWLDatatype XSD_INTEGER = OWLBuiltinDatatypeImpl.fromResource(XSD.integer);

    public static final OWLLiteral TRUE_LITERAL = OWLLiteralImpl.createLiteral(true);
    public static final OWLLiteral FALSE_LITERAL = OWLLiteralImpl.createLiteral(false);
}
