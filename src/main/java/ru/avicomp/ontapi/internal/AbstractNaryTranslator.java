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

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for following axioms:
 * <ul>
 * <li>EquivalentClasses ({@link EquivalentClassesTranslator})</li>
 * <li>EquivalentObjectProperties ({@link EquivalentObjectPropertiesTranslator})</li>
 * <li>EquivalentDataProperties ({@link EquivalentDataPropertiesTranslator})</li>
 * <li>SameIndividual ({@link SameIndividualTranslator})</li>
 * </ul>
 * Also for {@link AbstractTwoWayNaryTranslator} with following subclasses:
 * <ul>
 * <li>DisjointClasses ({@link DisjointClassesTranslator})</li>
 * <li>DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator})</li>
 * <li>DisjointDataProperties ({@link DisjointDataPropertiesTranslator})</li>
 * <li>DifferentIndividuals ({@link DifferentIndividualsTranslator})</li>
 * </ul>
 * <p>
 * Created by szuev on 13.10.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 * @param <OWL>   generic type of {@link OWLObject}
 * @param <ONT>   generic type of {@link OntObject}
 */
public abstract class AbstractNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>, OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AxiomTranslator<Axiom> {

    private final Comparator<OWL> uriFirstComparator = (a, b) -> a.isAnonymous() == b.isAnonymous() ? 0 : a.isAnonymous() ? -1 : 1;

    void write(OWLNaryAxiom<OWL> thisAxiom, Set<OWLAnnotation> annotations, OntGraphModel model) {
        List<OWL> operands = thisAxiom.operands().sorted(uriFirstComparator).distinct().collect(Collectors.toList());
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            return;
        }
        if (operands.size() != 2) {
            throw new OntApiException(getClass().getSimpleName() + ": expected two operands. Axiom: " + thisAxiom);
        }
        WriteHelper.writeTriple(model, operands.get(0), getPredicate(), operands.get(1), annotations.stream());
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        axiom.asPairwiseAxioms().forEach(a -> write(a, axiom.annotations().collect(Collectors.toSet()), model));
    }

    abstract Property getPredicate();

    abstract Class<ONT> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, ConfigProvider.Config config) {
        return Models.listStatements(model, null, getPredicate(), null)
                .filterKeep(s -> s.getSubject().canAs(getView()));
    }

    @Override
    public boolean testStatement(OntStatement statement, ConfigProvider.Config config) {
        return statement.getPredicate().equals(getPredicate()) && statement.getSubject().canAs(getView());
    }

    @Override
    public ExtendedIterator<ONTObject<Axiom>> listAxioms(OntGraphModel model,
                                                         InternalDataFactory factory,
                                                         ConfigProvider.Config config) {
        Map<Axiom, ONTObject<Axiom>> res = new HashMap<>(); // memory!
        super.listAxioms(model, factory, config)
                .forEachRemaining(c -> res.compute(c.getObject(), (a, w) -> w == null ? c : w.append(c)));
        return WrappedIterator.create(res.values().iterator());
    }
}
