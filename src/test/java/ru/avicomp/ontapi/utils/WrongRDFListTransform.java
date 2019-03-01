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

package ru.avicomp.ontapi.utils;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.Transform;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Example of broken rdf:List from OWL-API-contract (e.g. see all.rdf),
 * instead 'rdf:nil' there is a resource with uri 'rdf:':
 * <pre>{@code
 * <rdf:Description rdf:about="a">
 *  <rdf:type>
 *      <owl:Restriction>
 *          <owl:onProperty rdf:resource="dp" />
 *          <owl:allValuesFrom>
 *              <rdfs:Datatype>
 *                  <owl:oneOf>
 *                      <rdf:Description>
 *                          <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#decimal">0.5</rdf:first>
 *                          <rdf:rest>
 *                              <rdf:Description>
 *                                  <rdf:first rdf:datatype="http://www.w3.org/2002/07/owl#rational">1/2</rdf:first>
 *                                  <rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#"/>
 *                              </rdf:Description>
 *                          </rdf:rest>
 *                      </rdf:Description>
 *                  </owl:oneOf>
 *              </rdfs:Datatype>
 *          </owl:allValuesFrom>
 *      </owl:Restriction>
 *  </rdf:type>
 *  </rdf:Description>
 * }</pre>
 * Created by szuev on 26.04.2017.
 */
public class WrongRDFListTransform extends Transform {

    public WrongRDFListTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        List<Statement> wrong = statements(null, RDF.rest, null)
                .filter(s -> s.getObject().isResource())
                .filter(s -> Objects.equals(RDF.uri, s.getObject().asResource().getURI()))
                .collect(Collectors.toList());
        Model m = getWorkModel();
        wrong.forEach(s -> m.remove(s).add(s.getSubject(), RDF.rest, RDF.nil));
    }
}
