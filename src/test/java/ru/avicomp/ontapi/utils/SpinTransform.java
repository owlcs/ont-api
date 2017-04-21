package ru.avicomp.ontapi.utils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.topbraid.spin.model.*;
import org.topbraid.spin.vocabulary.SP;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.Transform;

/**
 * To replace spin rdf:List with string.
 * For test purposes only.
 * <p>
 * Example of such list:
 * <pre> {@code
 * spin:body [
 *    rdf:type sp:Select ;
 *    sp:resultVariables (
 *        [
 *          sp:expression [
 *              rdf:type sp:Count ;
 *              sp:expression [
 *                  sp:varName \"subject\"^^xsd:string ;
 *                ] ;
 *            ] ;
 *          sp:varName \"result\"^^xsd:string ;
 *        ]
 *      ) ;
 *    sp:where (
 *        [
 *          sp:object spin:_arg2 ;
 *          sp:predicate spin:_arg1 ;
 *          sp:subject [
 *              sp:varName \"subject\"^^xsd:string ;
 *            ] ;
 *        ]
 *      ) ;
 *  ] ;
 * } </pre>
 * This will be replaced by
 * <pre> {@code
 * spin:body [ a        sp:Select ;
 *             sp:text  "SELECT ((COUNT(?subject)) AS ?result)\nWHERE {\n    ?subject spin:_arg1 spin:_arg2 .\n}"
 *           ] ;
 * }</pre>
 * <p>
 * Note: before processing add links to {@link org.apache.jena.util.FileManager} to avoid recourse to the internet.
 * <p>
 * Created by szuev on 21.04.2017.
 */
public class SpinTransform extends Transform {


    public SpinTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        List<Query> queries = queries().collect(Collectors.toList());
        String name = Graphs.getName(getBaseGraph());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}]queries count: {}", name, queries.size());
        }
        queries.forEach(query -> {
            Literal literal = ResourceFactory.createTypedLiteral(String.valueOf(query));
            Resource type = statements(query, RDF.type, null)
                    .map(Statement::getObject)
                    .filter(RDFNode::isURIResource)
                    .map(RDFNode::asResource)
                    .findFirst().orElseThrow(OntJenaException.supplier("No type for " + literal));
            Set<Statement> remove = Models.getAssociatedStatements(query);
            remove.stream()
                    .filter(s -> !(RDF.type.equals(s.getPredicate()) && type.equals(s.getObject())))
                    .forEach(statement -> getBaseModel().remove(statement));
            getBaseModel().add(query, SP.text, literal);
        });
    }

    public Stream<Query> queries() {
        return Stream.of(QueryType.values()).map(this::queries).flatMap(Function.identity());
    }

    protected Stream<Query> queries(QueryType type) {
        return statements(null, RDF.type, type.getType()).map(Statement::getSubject)
                .filter(s -> s.canAs(type.getView())).map(s -> s.as(type.getView()));
    }

    public enum QueryType {
        SELECT(SP.Select, Select.class),
        CONSTRUCT(SP.Construct, Construct.class),
        ASK(SP.Ask, Ask.class),
        DESCRIBE(SP.Describe, Describe.class);

        private final Resource type;
        private final Class<? extends Query> view;

        QueryType(Resource type, Class<? extends Query> view) {
            this.type = type;
            this.view = view;
        }

        public Resource getType() {
            return type;
        }

        public Class<? extends Query> getView() {
            return view;
        }
    }
}
