package ru.avicomp.ontapi.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.transforms.Transform;

/**
 * Example of broken rdf:List from OWL-API-contract (e.g. see all.rdf),
 * instead 'rdf:nil' there is a resource with uri 'rdf:':
 * <pre>
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
 * </pre>
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
        Model m = getBaseModel();
        wrong.forEach(s -> {
            m.remove(s);
            m.add(s.getSubject(), RDF.rest, RDF.nil);
        });
    }
}
