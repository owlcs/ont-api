/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.owlapi.tests.util;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AnnotationWalkingControl;
import org.semanticweb.owlapi.util.OWLObjectWalker;

import java.util.*;

/**
 * Created by ses on 8/15/15.
 */
@SuppressWarnings("NullableProblems")
public class OWLObjectWalkerTest extends TestBase {

    private OWLAnnotation world;
    private OWLAnnotation cruelWorld;
    private OWLAnnotationProperty ap;
    private OWLAnnotation goodbye;
    private OWLAnnotation hello;

    @Before
    public void setUp() {
        ap = df.getOWLAnnotationProperty(iri("ap"));
        cruelWorld = df.getOWLAnnotation(ap, df.getOWLLiteral("cruel world"));
        goodbye = df.getOWLAnnotation(ap, df.getOWLLiteral("goodbye"), Collections.singleton(cruelWorld));
        world = df.getOWLAnnotation(ap, df.getOWLLiteral("world"));
        hello = df.getOWLAnnotation(ap, df.getOWLLiteral("hello"), Collections.singleton(world));
    }

    @Test
    public void testWalkAnnotations() {
        OWLOntology o = getOwlOntology();
        List<OWLAnnotation> emptyAnnotationList = Collections.emptyList();
        checkWalkWithFlags(o, AnnotationWalkingControl.DONT_WALK_ANNOTATIONS, emptyAnnotationList);
        checkWalkWithFlags(o, AnnotationWalkingControl.WALK_ONTOLOGY_ANNOTATIONS_ONLY, Collections.singletonList(hello));
        checkWalkWithFlags(o, AnnotationWalkingControl.WALK_ANNOTATIONS, Arrays.asList(hello, world, goodbye, cruelWorld));
    }

    private static void checkWalkWithFlags(OWLOntology o, AnnotationWalkingControl walkFlag,
                                           List<OWLAnnotation> expected) {
        final List<OWLAnnotation> visitedAnnotations = new ArrayList<>();
        OWLObjectVisitor visitor = new OWLObjectVisitor() {

            @Override
            public void visit(OWLAnnotation node) {
                visitedAnnotations.add(node);
            }
        };
        Set<? extends OWLObject> ontologySet = Collections.singleton(o);
        OWLObjectWalker<? extends OWLObject> walker;
        if (walkFlag == AnnotationWalkingControl.WALK_ONTOLOGY_ANNOTATIONS_ONLY) {
            walker = new OWLObjectWalker<>(ontologySet);
        } else {
            walker = new OWLObjectWalker<>(ontologySet, true, walkFlag);
        }
        walker.walkStructure(visitor);
        Assertions.assertEquals(expected, visitedAnnotations);
    }

    private OWLOntology getOwlOntology() {
        OWLOntology o = getOWLOntology();
        m.applyChange(new AddOntologyAnnotation(o, hello));
        o.addAxiom(df.getOWLDeclarationAxiom(ap, Collections.singleton(goodbye)));
        return o;
    }
}
