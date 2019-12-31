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

package com.github.owlcs.ontapi.transforms;

import com.github.owlcs.ontapi.jena.OntVocabulary;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.List;

/**
 * Class to perform ontology id transformation.
 * It merges several owl:Ontology sections to single one.
 * As primary chooses the one that has the largest number of triplets.
 * If there is no any owl:Ontology then new anonymous owl:Ontology will be added to the graph.
 */
public class OWLIDTransform extends TransformationModel {

    public OWLIDTransform(Graph graph) {
        super(graph, OntVocabulary.Factory.DUMMY);
    }

    @Override
    public void perform() {
        Model m = getWorkModel();
        // choose or create the new one:
        Resource ontology = Graphs.ontologyNode(getQueryModel().getGraph())
                .map(m::getRDFNode).map(RDFNode::asResource)
                .orElseGet(() -> m.createResource(OWL.Ontology));
        // move all content from other ontologies to the selected one
        ExtendedIterator<Resource> other = listStatements(null, RDF.type, OWL.Ontology)
                .mapWith(Statement::getSubject)
                .filterDrop(ontology::equals);
        List<Statement> rest = Iter.flatMap(other, o -> listStatements(o, null, null)).toList();
        rest.forEach(s -> ontology.addProperty(s.getPredicate(), s.getObject()));
        // remove all other ontologies
        m.remove(rest);
    }
}
