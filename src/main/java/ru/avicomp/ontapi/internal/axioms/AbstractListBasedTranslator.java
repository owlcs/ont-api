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

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.AxiomTranslator;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.WriteHelper;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntList;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Base class for three following implementations:
 * <ul>
 * <li>{@link HasKeyTranslator}</li>
 * <li>{@link SubPropertyChainOfTranslator}</li>
 * <li>{@link DisjointUnionTranslator}</li>
 * </ul>
 * Created by @szuev on 18.10.2016.
 */
public abstract class AbstractListBasedTranslator<Axiom extends OWLLogicalAxiom,
        ONT_SUBJECT extends OntObject, OWL_SUBJECT extends OWLObject,
        ONT_MEMBER extends OntObject, OWL_MEMBER extends OWLObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<ONT_SUBJECT> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeList(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return Models.listLocalStatements(model, null, getPredicate(), null)
                .filterKeep(this::filter);
    }

    protected boolean filter(OntStatement statement) {
        return statement.getSubject().canAs(getView())
                && statement.getObject().canAs(RDFList.class);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return getPredicate().equals(statement.getPredicate()) && filter(statement);
    }

    ONTObject<Axiom> makeAxiom(OntStatement statement,
                               Function<ONT_SUBJECT, ONTObject<? extends OWL_SUBJECT>> subjectExtractor,
                               BiFunction<ONT_SUBJECT, RDFNode, Optional<OntList<ONT_MEMBER>>> listExtractor,
                               Function<ONT_MEMBER, ONTObject<? extends OWL_MEMBER>> memberExtractor,
                               Collector<ONTObject<? extends OWL_MEMBER>, ?, ? extends Collection<ONTObject<? extends OWL_MEMBER>>> collector,
                               BiFunction<ONTObject<? extends OWL_SUBJECT>, Collection<ONTObject<? extends OWL_MEMBER>>, Axiom> axiomMaker) {

        ONT_SUBJECT ontSubject = statement.getSubject(getView());
        ONTObject<? extends OWL_SUBJECT> subject = subjectExtractor.apply(ontSubject);
        OntList<ONT_MEMBER> list = listExtractor.apply(ontSubject, statement.getObject())
                .orElseThrow(() -> new OntApiException("Can't get OntList for statement " + Models.toString(statement)));
        Collection<ONTObject<? extends OWL_MEMBER>> members = list.members().map(memberExtractor).collect(collector);

        Axiom res = axiomMaker.apply(subject, members);
        return ONTObject.create(res, statement).append(() -> list.spec().map(FrontsTriple::asTriple));
    }

}
