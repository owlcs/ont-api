/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.utils;

import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * A copy-paste from {@link org.topbraid.spin.vocabulary.SP}.
 * The difference: it does not modify {@link org.apache.jena.enhanced.BuiltinPersonalities#model the standard global jena personalities}.
 * <p>
 * Created by @szuev on 20.03.2018.
 */
@SuppressWarnings({"WeakerAccess", "unused", "deprecation"})
public class SP {
    public static final String SPIN_URI = "http://spinrdf.org";
    public static final String BASE_URI = SPIN_URI + "/sp";
    public static final String NS = BASE_URI + "#";
    public static final String PREFIX = "sp";

    public final static Resource Aggregation = resource("Aggregation");
    public final static Resource AltPath = resource("AltPath");
    public final static Resource Asc = resource("Asc");
    public final static Resource Ask = resource("Ask");
    public final static Resource Avg = resource("Avg");
    public final static Resource Bind = resource("Bind");
    public final static Resource Clear = resource("Clear");
    public final static Resource Command = resource("Command");
    public final static Resource Construct = resource("Construct");
    public final static Resource Count = resource("Count");
    public final static Resource Create = resource("Create");
    public final static Resource Delete = resource("Delete");
    public final static Resource DeleteData = resource("DeleteData");
    public final static Resource DeleteWhere = resource("DeleteWhere");
    public final static Resource Desc = resource("Desc");
    public final static Resource Describe = resource("Describe");
    public final static Resource Drop = resource("Drop");
    public final static Resource exists = resource("exists");
    public final static Resource Exists = resource("Exists");
    public final static Resource Expression = resource("Expression");
    public final static Resource Filter = resource("Filter");
    public final static Resource Insert = resource("Insert");
    public final static Resource InsertData = resource("InsertData");
    public final static Resource Let = resource("Let");
    public final static Resource Load = resource("Load");
    public final static Resource Max = resource("Max");
    public final static Resource Min = resource("Min");
    public final static Resource Modify = resource("Modify");
    public final static Resource ModPath = resource("ModPath");
    public final static Resource Minus = resource("Minus");
    public final static Resource NamedGraph = resource("NamedGraph");
    public final static Resource notExists = resource("notExists");
    public final static Resource NotExists = resource("NotExists");
    public final static Resource Optional = resource("Optional");
    public final static Resource Query = resource("Query");
    public final static Resource ReverseLinkPath = resource("ReverseLinkPath");
    public final static Resource ReversePath = resource("ReversePath");
    public final static Resource Select = resource("Select");
    public final static Resource Service = resource("Service");
    public final static Resource SeqPath = resource("SeqPath");
    public final static Resource SubQuery = resource("SubQuery");
    public final static Resource Sum = resource("Sum");
    public final static Resource Triple = resource("Triple");
    public final static Resource TriplePath = resource("TriplePath");
    public final static Resource TriplePattern = resource("TriplePattern");
    public final static Resource TripleTemplate = resource("TripleTemplate");
    public final static Resource undef = resource("undef");
    public final static Resource Union = resource("Union");
    public final static Resource Update = resource("Update");
    public final static Resource Values = resource("Values");
    public final static Resource Variable = resource("Variable");

    public final static Property all = property("all");
    public final static Property arg = property("arg");
    public final static Property arg1 = property("arg1");
    public final static Property arg2 = property("arg2");
    public final static Property arg3 = property("arg3");
    public final static Property arg4 = property("arg4");
    public final static Property arg5 = property("arg5");
    public final static Property as = property("as");
    public final static Property bindings = property("bindings");
    public final static Property data = property("data");
    public final static Property default_ = property("default");
    public final static Property deletePattern = property("deletePattern");
    public final static Property distinct = property("distinct");
    public final static Property document = property("document");
    public final static Property elements = property("elements");
    public final static Property expression = property("expression");
    public final static Property from = property("from");
    public final static Property fromNamed = property("fromNamed");
    public final static Property graphIRI = property("graphIRI");
    public final static Property graphNameNode = property("graphNameNode");
    public final static Property groupBy = property("groupBy");
    public final static Property having = property("having");
    public final static Property insertPattern = property("insertPattern");
    public final static Property into = property("into");
    public final static Property limit = property("limit");
    public final static Property modMax = property("modMax");
    public final static Property modMin = property("modMin");
    public final static Property named = property("named");
    public final static Property node = property("node");
    public final static Property object = property("object");
    public final static Property offset = property("offset");
    public final static Property orderBy = property("orderBy");
    public final static Property path = property("path");
    public final static Property path1 = property("path1");
    public final static Property path2 = property("path2");
    public final static Property predicate = property("predicate");
    public final static Property query = property("query");
    public final static Property reduced = property("reduced");
    public final static Property resultNodes = property("resultNodes");
    public final static Property resultVariables = property("resultVariables");
    public final static Property separator = property("separator");
    public final static Property serviceURI = property("serviceURI");
    public final static Property silent = property("silent");
    public final static Property str = property("str");
    public final static Property strlang = property("strlang");
    public final static Property subject = property("subject");
    public final static Property subPath = property("subPath");
    public final static Property templates = property("templates");
    public final static Property text = property("text");
    public final static Property using = property("using");
    public final static Property usingNamed = property("usingNamed");
    public final static Property values = property("values");
    public final static Property variable = property("variable");
    public final static Property varName = property("varName");
    public final static Property varNames = property("varNames");
    public final static Property where = property("where");
    public final static Property with = property("with");

    public final static Resource bound = resource("bound");
    public final static Resource eq = resource("eq");
    public final static Resource not = resource("not");
    public final static Resource regex = resource("regex");
    public final static Resource sub = resource("sub");
    public final static Resource unaryMinus = resource("unaryMinus");

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }

    public static Personality<RDFNode> SPIN_PERSONALITY = init(OntModelConfig.getStandardPersonality());

    public static Model createModel(Graph graph) {
        return new ModelCom(graph, SPIN_PERSONALITY);
    }

    /**
     * @param p {@link Personality} to modify
     * @return {@link Personality} the same instance
     * @see org.topbraid.spin.vocabulary.SP#init(Personality)
     */
    @SuppressWarnings("JavadocReference")
    public static Personality<RDFNode> init(Personality<RDFNode> p) {
        p.add(org.topbraid.spin.model.Aggregation.class, newSimpleImplementation(SPL.Argument.asNode(), org.topbraid.spin.model.impl.AggregationImpl.class));
        p.add(org.topbraid.spin.model.Argument.class, newSimpleImplementation(SPL.Argument.asNode(), org.topbraid.spin.model.impl.ArgumentImpl.class));
        p.add(org.topbraid.spin.model.Attribute.class, newSimpleImplementation(SPL.Attribute.asNode(), org.topbraid.spin.model.impl.AttributeImpl.class));
        p.add(org.topbraid.spin.model.Ask.class, newSimpleImplementation(Ask.asNode(), org.topbraid.spin.model.impl.AskImpl.class));
        p.add(org.topbraid.spin.model.Bind.class, new org.topbraid.spin.util.SimpleImplementation2(Bind.asNode(), Let.asNode(), org.topbraid.spin.model.impl.BindImpl.class));
        p.add(org.topbraid.spin.model.update.Clear.class, newSimpleImplementation(Clear.asNode(), org.topbraid.spin.model.update.impl.ClearImpl.class));
        p.add(org.topbraid.spin.model.Construct.class, newSimpleImplementation(Construct.asNode(), org.topbraid.spin.model.impl.ConstructImpl.class));
        p.add(org.topbraid.spin.model.update.Create.class, newSimpleImplementation(Create.asNode(), org.topbraid.spin.model.update.impl.CreateImpl.class));
        p.add(org.topbraid.spin.model.update.Delete.class, newSimpleImplementation(Delete.asNode(), org.topbraid.spin.model.update.impl.DeleteImpl.class));
        p.add(org.topbraid.spin.model.update.DeleteData.class, newSimpleImplementation(DeleteData.asNode(), org.topbraid.spin.model.update.impl.DeleteDataImpl.class));
        p.add(org.topbraid.spin.model.update.DeleteWhere.class, newSimpleImplementation(DeleteWhere.asNode(), org.topbraid.spin.model.update.impl.DeleteWhereImpl.class));
        p.add(org.topbraid.spin.model.Describe.class, newSimpleImplementation(Describe.asNode(), org.topbraid.spin.model.impl.DescribeImpl.class));
        p.add(org.topbraid.spin.model.update.Drop.class, newSimpleImplementation(Drop.asNode(), org.topbraid.spin.model.update.impl.DropImpl.class));
        p.add(org.topbraid.spin.model.ElementList.class, newSimpleImplementation(RDF.List.asNode(), org.topbraid.spin.model.impl.ElementListImpl.class));
        p.add(org.topbraid.spin.model.Exists.class, newSimpleImplementation(Exists.asNode(), org.topbraid.spin.model.impl.ExistsImpl.class));
        p.add(org.topbraid.spin.model.Function.class, newSimpleImplementation(SPIN.Function.asNode(), org.topbraid.spin.model.impl.FunctionImpl.class));
        p.add(org.topbraid.spin.model.FunctionCall.class, newSimpleImplementation(SPIN.Function.asNode(), org.topbraid.spin.model.impl.FunctionCallImpl.class));
        p.add(org.topbraid.spin.model.Filter.class, newSimpleImplementation(Filter.asNode(), org.topbraid.spin.model.impl.FilterImpl.class));
        p.add(org.topbraid.spin.model.update.Insert.class, newSimpleImplementation(Insert.asNode(), org.topbraid.spin.model.update.impl.InsertImpl.class));
        p.add(org.topbraid.spin.model.update.InsertData.class, newSimpleImplementation(InsertData.asNode(), org.topbraid.spin.model.update.impl.InsertDataImpl.class));
        p.add(org.topbraid.spin.model.update.Load.class, newSimpleImplementation(Load.asNode(), org.topbraid.spin.model.update.impl.LoadImpl.class));
        p.add(org.topbraid.spin.model.Minus.class, newSimpleImplementation(Minus.asNode(), org.topbraid.spin.model.impl.MinusImpl.class));
        p.add(org.topbraid.spin.model.update.Modify.class, newSimpleImplementation(Modify.asNode(), org.topbraid.spin.model.update.impl.ModifyImpl.class));
        p.add(org.topbraid.spin.model.Module.class, newSimpleImplementation(SPIN.Module.asNode(), org.topbraid.spin.model.impl.ModuleImpl.class));
        p.add(org.topbraid.spin.model.NamedGraph.class, newSimpleImplementation(NamedGraph.asNode(), org.topbraid.spin.model.impl.NamedGraphImpl.class));
        p.add(org.topbraid.spin.model.NotExists.class, newSimpleImplementation(NotExists.asNode(), org.topbraid.spin.model.impl.NotExistsImpl.class));
        p.add(org.topbraid.spin.model.Optional.class, newSimpleImplementation(Optional.asNode(), org.topbraid.spin.model.impl.OptionalImpl.class));
        p.add(org.topbraid.spin.model.Service.class, newSimpleImplementation(Service.asNode(), org.topbraid.spin.model.impl.ServiceImpl.class));
        p.add(org.topbraid.spin.model.Select.class, newSimpleImplementation(Select.asNode(), org.topbraid.spin.model.impl.SelectImpl.class));
        p.add(org.topbraid.spin.model.SubQuery.class, newSimpleImplementation(SubQuery.asNode(), org.topbraid.spin.model.impl.SubQueryImpl.class));
        p.add(org.topbraid.spin.model.SPINInstance.class, newSimpleImplementation(RDFS.Resource.asNode(), org.topbraid.spin.model.impl.SPINInstanceImpl.class));
        p.add(org.topbraid.spin.model.Template.class, newSimpleImplementation(SPIN.Template.asNode(), org.topbraid.spin.model.impl.TemplateImpl.class));
        p.add(org.topbraid.spin.model.TemplateCall.class, newSimpleImplementation(RDFS.Resource.asNode(), org.topbraid.spin.model.impl.TemplateCallImpl.class));
        p.add(org.topbraid.spin.model.TriplePath.class, newSimpleImplementation(TriplePath.asNode(), org.topbraid.spin.model.impl.TriplePathImpl.class));
        p.add(org.topbraid.spin.model.TriplePattern.class, newSimpleImplementation(TriplePattern.asNode(), org.topbraid.spin.model.impl.TriplePatternImpl.class));
        p.add(org.topbraid.spin.model.TripleTemplate.class, newSimpleImplementation(TripleTemplate.asNode(), org.topbraid.spin.model.impl.TripleTemplateImpl.class));
        p.add(org.topbraid.spin.model.Union.class, newSimpleImplementation(Union.asNode(), org.topbraid.spin.model.impl.UnionImpl.class));
        p.add(org.topbraid.spin.model.Values.class, newSimpleImplementation(Values.asNode(), org.topbraid.spin.model.impl.ValuesImpl.class));
        p.add(org.topbraid.spin.model.Variable.class, newSimpleImplementation(Variable.asNode(), org.topbraid.spin.model.impl.VariableImpl.class));
        return p;
    }

    private static org.topbraid.spin.util.SimpleImplementation newSimpleImplementation(Node n, Class<?> t) {
        return new org.topbraid.spin.util.SimpleImplementation(n, t);
    }
}
