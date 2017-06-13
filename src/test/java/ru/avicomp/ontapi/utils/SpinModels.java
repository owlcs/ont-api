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

package ru.avicomp.ontapi.utils;

import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.IRI;
import org.topbraid.spin.model.*;
import org.topbraid.spin.model.impl.*;
import org.topbraid.spin.model.update.*;
import org.topbraid.spin.model.update.impl.*;
import org.topbraid.spin.util.SimpleImplementation;
import org.topbraid.spin.util.SimpleImplementation2;

import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;

/**
 * Collection of all spin models (located in resources)
 * <p>
 * Created by szuev on 21.04.2017.
 */
public enum SpinModels {
    SP("/spin/sp.ttl", "http://spinrdf.org/sp"),
    SPIN("/spin/spin.ttl", "http://spinrdf.org/spin"),
    SPL("/spin/spl.spin.ttl", "http://spinrdf.org/spl"),
    SPIF("/spin/spif.ttl", "http://spinrdf.org/spif"),
    SPINMAP("/spin/spinmap.spin.ttl", "http://spinrdf.org/spinmap"),
    SMF("/spin/functions-smf.ttl", "http://topbraid.org/functions-smf"),
    FN("/spin/functions-fn.ttl", "http://topbraid.org/functions-fn"),
    AFN("/spin/functions-afn.ttl", "http://topbraid.org/functions-afn"),
    SMF_BASE("/spin/sparqlmotionfunctions.ttl", "http://topbraid.org/sparqlmotionfunctions"),
    SPINMAPL("/spin/spinmapl.spin.ttl", "http://topbraid.org/spin/spinmapl");

    /**
     * see {@link org.topbraid.spin.vocabulary.SP#init(Personality)}
     */
    public static final Personality<RDFNode> SPIN_PERSONALITY = new Personality<>(OntModelConfig.STANDARD_PERSONALITY)
            .add(Aggregation.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPL.Argument.asNode(), AggregationImpl.class))
            .add(Argument.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPL.Argument.asNode(), ArgumentImpl.class))
            .add(Attribute.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPL.Attribute.asNode(), AttributeImpl.class))
            .add(Ask.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Ask.asNode(), AskImpl.class))
            .add(Bind.class, new SimpleImplementation2(org.topbraid.spin.vocabulary.SP.Bind.asNode(), org.topbraid.spin.vocabulary.SP.Let.asNode(), BindImpl.class))
            .add(Clear.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Clear.asNode(), ClearImpl.class))
            .add(Construct.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Construct.asNode(), ConstructImpl.class))
            .add(Create.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Create.asNode(), CreateImpl.class))
            .add(org.topbraid.spin.model.update.Delete.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Delete.asNode(), org.topbraid.spin.model.update.impl.DeleteImpl.class))
            .add(DeleteData.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.DeleteData.asNode(), DeleteDataImpl.class))
            .add(DeleteWhere.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.DeleteWhere.asNode(), DeleteWhereImpl.class))
            .add(Describe.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Describe.asNode(), DescribeImpl.class))
            .add(Drop.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Drop.asNode(), DropImpl.class))
            .add(ElementList.class, new SimpleImplementation(RDF.List.asNode(), ElementListImpl.class))
            .add(Exists.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Exists.asNode(), ExistsImpl.class))
            .add(Function.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPIN.Function.asNode(), FunctionImpl.class))
            .add(FunctionCall.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPIN.Function.asNode(), FunctionCallImpl.class))
            .add(Filter.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Filter.asNode(), FilterImpl.class))
            .add(org.topbraid.spin.model.update.Insert.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Insert.asNode(), org.topbraid.spin.model.update.impl.InsertImpl.class))
            .add(InsertData.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.InsertData.asNode(), InsertDataImpl.class))
            .add(Load.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Load.asNode(), LoadImpl.class))
            .add(Minus.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Minus.asNode(), MinusImpl.class))
            .add(Modify.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Modify.asNode(), ModifyImpl.class))
            .add(Module.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPIN.Module.asNode(), ModuleImpl.class))
            .add(NamedGraph.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.NamedGraph.asNode(), NamedGraphImpl.class))
            .add(NotExists.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.NotExists.asNode(), NotExistsImpl.class))
            .add(Optional.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Optional.asNode(), OptionalImpl.class))
            .add(Service.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Service.asNode(), ServiceImpl.class))
            .add(Select.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Select.asNode(), SelectImpl.class))
            .add(SubQuery.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.SubQuery.asNode(), SubQueryImpl.class))
            .add(SPINInstance.class, new SimpleImplementation(RDFS.Resource.asNode(), SPINInstanceImpl.class))
            .add(Template.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SPIN.Template.asNode(), TemplateImpl.class))
            .add(TemplateCall.class, new SimpleImplementation(RDFS.Resource.asNode(), TemplateCallImpl.class))
            .add(TriplePath.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.TriplePath.asNode(), TriplePathImpl.class))
            .add(TriplePattern.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.TriplePattern.asNode(), TriplePatternImpl.class))
            .add(TripleTemplate.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.TripleTemplate.asNode(), TripleTemplateImpl.class))
            .add(Union.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Union.asNode(), UnionImpl.class))
            .add(Values.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Values.asNode(), ValuesImpl.class))
            .add(Variable.class, new SimpleImplementation(org.topbraid.spin.vocabulary.SP.Variable.asNode(), VariableImpl.class));

    public static final OntPersonality ONT_SPIN_PERSONALITY = OntModelConfig.ONT_PERSONALITY_BUILDER.build(SPIN_PERSONALITY, Configurable.Mode.LAX);

    private final String file, uri;

    SpinModels(String file, String uri) {
        this.file = file;
        this.uri = uri;
    }

    public static void addMappings(OntologyManager m) {
        for (SpinModels spin : values()) {
            m.getIRIMappers().add(FileMap.create(spin.getIRI(), spin.getFile()));
        }
    }

    public static void addMappings(FileManager fileManager) {
        for (SpinModels spin : values()) {
            fileManager.getLocationMapper().addAltEntry(spin.getIRI().getIRIString(), spin.getFile().toURI().toString());
        }
    }

    public IRI getIRI() {
        return IRI.create(uri);
    }

    public IRI getFile() {
        return IRI.create(ReadWriteUtils.getResourceURI(file));
    }
}
