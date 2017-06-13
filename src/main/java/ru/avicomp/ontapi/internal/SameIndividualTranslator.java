/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;

import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLSameIndividualAxiomImpl;

/**
 * base class {@link AbstractNaryTranslator}
 * example:
 * :indi1 owl:sameAs :indi2, :indi3 .
 * <p>
 * Created by szuev on 13.10.2016.
 */
public class SameIndividualTranslator extends AbstractNaryTranslator<OWLSameIndividualAxiom, OWLIndividual, OntIndividual> {
    @Override
    public Property getPredicate() {
        return OWL.sameAs;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    OWLSameIndividualAxiom create(Stream<OWLIndividual> components, Set<OWLAnnotation> annotations) {
        return new OWLSameIndividualAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    private Set<InternalObject<OWLSameIndividualAxiom>> extractStandaloneAxioms(Set<InternalObject<OWLSameIndividualAxiom>> set) {
        return set.stream().filter(a -> set.stream().filter(b -> !a.equals(b))
                .noneMatch(b -> ReadHelper.isIntersect(a.getObject(), b.getObject()))).collect(Collectors.toSet());
    }

    /**
     * todo: at the moment it is not used. see description of super method.
     * Compresses nary axioms to more compact form.
     * The difference with parent method is in fact that if individual 'A' is same as individual 'B', and
     * 'B' is same as 'C' then 'A' is same as 'C'.
     *
     * @param init initial Map with Axioms as keys
     * @return shrunken map of axioms
     */
    @Override
    Set<InternalObject<OWLSameIndividualAxiom>> shrink(Set<InternalObject<OWLSameIndividualAxiom>> init) {
        if (init.size() < 2) {
            return new HashSet<>(init);
        }
        Set<InternalObject<OWLSameIndividualAxiom>> unique = extractStandaloneAxioms(init);
        Set<InternalObject<OWLSameIndividualAxiom>> res = new HashSet<>();
        res.addAll(unique);
        if (res.size() == init.size()) return res;
        Set<InternalObject<OWLSameIndividualAxiom>> tmp = new HashSet<>(init);
        tmp.removeAll(unique);
        // assemble a single axiom of the remaining pairwise axioms
        Stream<OWLAnnotation> annotations = tmp.stream().map(InternalObject::getObject).map(HasAnnotations::annotations).findAny().orElse(Stream.empty());
        // do operands(stream)->set->stream to avoid BootstrapMethodError
        Stream<OWLIndividual> components = tmp.stream().map(InternalObject::getObject).map(axiom -> axiom.operands().collect(Collectors.toSet()).stream()).flatMap(Function.identity()).distinct();
        Set<Triple> triples = tmp.stream().map(InternalObject::triples).flatMap(Function.identity()).collect(Collectors.toSet());
        OWLSameIndividualAxiom multi = create(components, annotations.collect(Collectors.toSet()));
        res.add(new InternalObject<>(multi, triples));
        return res;
    }

    @Override
    public InternalObject<OWLSameIndividualAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLIndividual> a = ReadHelper.fetchIndividual(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<? extends OWLIndividual> b = ReadHelper.fetchIndividual(statement.getObject().as(getView()), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLSameIndividualAxiom res = conf.dataFactory().getOWLSameIndividualAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
