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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translator for DisjointUnion Axiom.
 * Example in turtle:
 * <pre>{@code
 * :MyClass1 owl:disjointUnionOf ( :MyClass2 [ a owl:Class ; owl:unionOf ( :MyClass3 :MyClass4  ) ] ) ;
 * }</pre>
 * <p>
 * Created by @szuev on 17.10.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Disjoint_Union_of_Class_Expressions'>9.1.4 Disjoint Union of Class Expressions</a>
 */
public class DisjointUnionTranslator extends AbstractListBasedTranslator<OWLDisjointUnionAxiom, OntClass, OWLClassExpression, OntCE, OWLClassExpression> {
    @Override
    public OWLObject getSubject(OWLDisjointUnionAxiom axiom) {
        return axiom.getOWLClass();
    }

    @Override
    public Property getPredicate() {
        return OWL.disjointUnionOf;
    }

    @Override
    public Stream<? extends OWLObject> getObjects(OWLDisjointUnionAxiom axiom) {
        return axiom.classExpressions();
    }

    @Override
    Class<OntClass> getView() {
        return OntClass.class;
    }

    @Override
    public ONTObject<OWLDisjointUnionAxiom> toAxiom(OntStatement statement, InternalObjectFactory reader, InternalConfig config) {
        return makeAxiom(statement, reader::get, OntClass::findDisjointUnion, reader::get, Collectors.toSet(),
                (s, m) -> reader.getOWLDataFactory().getOWLDisjointUnionAxiom(s.getOWLObject().asOWLClass(),
                        ONTObject.extractWildcards(m),
                        ONTObject.extract(reader.get(statement, config))));
    }
}
