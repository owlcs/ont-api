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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.vocabulary.AVC;

import java.util.List;
import java.util.stream.Stream;

/**
 * Class to perform the final tuning of the OWL-2 ontology: mostly for fixing missed owl-declarations where it is possible.
 * It have to be running after {@link RDFSTransform} and {@link OWLCommonTransform}.
 * <p>
 * This transformer is designed to put in order any external (mainly none-OWL2) ontologies.
 * Also there are lots examples of incomplete or wrong ontologies provided by the tests from OWL-API contract pack,
 * which are not necessarily RDFS or OWL1.
 * And it seems such situations have to be relative rare in the real world, since
 * any API which meets specification would not produce ontologies, when there is some true parts of OWL2,
 * but no explicit declarations or some other components from which they consist.
 * At least one can be sure that ONT-API does not provide anything that only partially complies with the specification;
 * but for correct output the input should also be correct.
 * <p>
 * Consists of two inner transforms:
 * <ul>
 * <li>The first, {@link ManifestDeclarator}, works with the obvious cases
 * when the type of the left or the right statements part is defined by the predicate or from some other clear hints.
 * E.g. if we have triple "A rdfs:subClassOf B" then we know exactly - both "A" and "B" are owl-class expressions.
 * </li>
 * <li>The second, {@link ReasonerDeclarator}, performs iterative analyzing of whole graph to choose the correct entities type.
 * E.g. we can have owl-restriction (existential/universal quantification)
 * "_:x rdf:type owl:Restriction; owl:onProperty A; owl:allValuesFrom B",
 * where "A" and "B" could be either object property and class expressions or data property and data-range,
 * and therefore we need to find other entries of these two entities in the graph;
 * for this example the only one declaration either of "A" or "B" is enough.
 * </li>
 * </ul>
 *
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Short Guide</a>
 */
@SuppressWarnings("WeakerAccess")
public class OWLDeclarationTransform extends Transform {

    private static final List<Resource> PROPERTY_TYPES = Stream.of(OWL.DatatypeProperty, OWL.ObjectProperty, OWL.AnnotationProperty)
            .collect(Iter.toUnmodifiableList());
    private static final List<Resource> CLASS_TYPES = Stream.of(OWL.Class, RDFS.Datatype)
            .collect(Iter.toUnmodifiableList());


    protected final Transform manifestDeclarator;
    protected final Transform reasonerDeclarator;

    public OWLDeclarationTransform(Graph graph) {
        super(graph);
        this.manifestDeclarator = new ManifestDeclarator(graph);
        this.reasonerDeclarator = new ReasonerDeclarator(graph);
    }

    @Override
    public void perform() {
        try {
            manifestDeclarator.perform();
            reasonerDeclarator.perform();
        } finally {
            finalActions();
        }
    }

    @Override
    public Stream<Triple> uncertainTriples() {
        return reasonerDeclarator.uncertainTriples();
    }

    protected void finalActions() {
        getWorkModel().removeAll(null, RDF.type, AVC.AnonymousIndividual);
        // at times the ontology could contain some rdfs garbage,
        // even if other transformers (OWLTransformer, RDFSTransformer) have been used.
        listStatements(null, RDF.type, RDF.Property)
                .mapWith(Statement::getSubject)
                .filterKeep(RDFNode::isURIResource)
                .filterKeep(s -> hasAnyType(s, PROPERTY_TYPES))
                .toList()
                .forEach(p -> undeclare(p, RDF.Property));
        listStatements(null, RDF.type, RDFS.Class)
                .mapWith(Statement::getSubject)
                .filterKeep(RDFNode::isURIResource)
                .filterKeep(s -> hasAnyType(s, CLASS_TYPES))
                .toList()
                .forEach(c -> undeclare(c, RDFS.Class));
    }

}
