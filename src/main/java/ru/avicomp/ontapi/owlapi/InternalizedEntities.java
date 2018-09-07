/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLAnnotationPropertyImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLClassImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLDataPropertyImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLObjectPropertyImpl;

/**
 * Entities that are commonly used in implementations.
 * A copy-paste from uk.ac.manchester.cs.owl.owlapi.InternalizedEntities
 *
 * @author ignazio
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/InternalizedEntities.java'>uk.ac.manchester.cs.owl.owlapi.InternalizedEntities</a>
 */
@SuppressWarnings("WeakerAccess")
public class InternalizedEntities {

    public static final OWLClass OWL_THING = new OWLClassImpl(OWLRDFVocabulary.OWL_THING.getIRI());
    public static final OWLClass OWL_NOTHING = new OWLClassImpl(OWLRDFVocabulary.OWL_NOTHING.getIRI());

    public static final OWLObjectProperty OWL_TOP_OBJECT_PROPERTY = new OWLObjectPropertyImpl(OWLRDFVocabulary.OWL_TOP_OBJECT_PROPERTY.getIRI());
    public static final OWLObjectProperty OWL_BOTTOM_OBJECT_PROPERTY = new OWLObjectPropertyImpl(OWLRDFVocabulary.OWL_BOTTOM_OBJECT_PROPERTY.getIRI());

    public static final OWLDataProperty OWL_TOP_DATA_PROPERTY = new OWLDataPropertyImpl(OWLRDFVocabulary.OWL_TOP_DATA_PROPERTY.getIRI());
    public static final OWLDataProperty OWL_BOTTOM_DATA_PROPERTY = new OWLDataPropertyImpl(OWLRDFVocabulary.OWL_BOTTOM_DATA_PROPERTY.getIRI());

    public static final OWLAnnotationProperty RDFS_LABEL = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.RDFS_LABEL.getIRI());
    public static final OWLAnnotationProperty RDFS_COMMENT = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
    public static final OWLAnnotationProperty RDFS_SEE_ALSO = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.RDFS_SEE_ALSO.getIRI());
    public static final OWLAnnotationProperty RDFS_IS_DEFINED_BY = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.RDFS_IS_DEFINED_BY.getIRI());
    public static final OWLAnnotationProperty OWL_BACKWARD_COMPATIBLE_WITH = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.OWL_BACKWARD_COMPATIBLE_WITH.getIRI());
    public static final OWLAnnotationProperty OWL_INCOMPATIBLE_WITH = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.OWL_INCOMPATIBLE_WITH.getIRI());
    public static final OWLAnnotationProperty OWL_VERSION_INFO = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI());
    public static final OWLAnnotationProperty OWL_DEPRECATED = new OWLAnnotationPropertyImpl(OWLRDFVocabulary.OWL_DEPRECATED.getIRI());

    public static final OWLDatatype RDFS_LITERAL = new OWL2DatatypeImpl(OWL2Datatype.RDFS_LITERAL);
    public static final OWLDatatype RDF_PLAIN_LITERAL = new OWL2DatatypeImpl(OWL2Datatype.RDF_PLAIN_LITERAL);
    public static final OWLDatatype RDF_LANG_STRING = new OWL2DatatypeImpl(OWL2Datatype.RDF_LANG_STRING);
    public static final OWLDatatype XSD_STRING = new OWL2DatatypeImpl(OWL2Datatype.XSD_STRING);
    public static final OWLDatatype XSD_BOOLEAN = new OWL2DatatypeImpl(OWL2Datatype.XSD_BOOLEAN);
    public static final OWLDatatype XSD_DOUBLE = new OWL2DatatypeImpl(OWL2Datatype.XSD_DOUBLE);
    public static final OWLDatatype XSD_FLOAT = new OWL2DatatypeImpl(OWL2Datatype.XSD_FLOAT);
    public static final OWLDatatype XSD_INTEGER = new OWL2DatatypeImpl(OWL2Datatype.XSD_INTEGER);

    public static final OWLLiteral TRUE_LITERAL = OWLLiteralImpl.createLiteral(true);
    public static final OWLLiteral FALSE_LITERAL = OWLLiteralImpl.createLiteral(false);
}
