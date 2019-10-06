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

import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntSWRL;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * for "Rule" Axiom {@link org.semanticweb.owlapi.model.AxiomType#SWRL_RULE}
 * Specification: <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 */
public class SWRLRuleTranslator extends AxiomTranslator<SWRLRule> {

    @Override
    public void write(SWRLRule axiom, OntGraphModel model) {
        Stream<OntSWRL.Atom> head = axiom.head().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        Stream<OntSWRL.Atom> body = axiom.body().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        WriteHelper.addAnnotations(model.createSWRLImp(head.collect(Collectors.toList()),
                body.collect(Collectors.toList())), axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalObjects(model, OntSWRL.Imp.class).mapWith(OntObject::getRoot);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.getSubject().canAs(OntSWRL.Imp.class);
    }

    @Override
    public ONTObject<SWRLRule> toAxiom(OntStatement statement, InternalObjectFactory reader, InternalConfig config) {
        OntSWRL.Imp imp = statement.getSubject(OntSWRL.Imp.class);

        Collection<ONTObject<? extends SWRLAtom>> head = imp.head().map(reader::getSWRLAtom).collect(Collectors.toList());
        Collection<ONTObject<? extends SWRLAtom>> body = imp.body().map(reader::getSWRLAtom).collect(Collectors.toList());

        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        SWRLRule res = reader.getOWLDataFactory()
                .getSWRLRule(body.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()),
                        head.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, imp).append(annotations).appendWildcards(body).appendWildcards(head);
    }

}
