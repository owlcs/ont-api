# ONT-API (ver. 4.x.x)

<a href="https://maven-badges.herokuapp.com/maven-central/com.github.owlcs/ontapi"><img src="https://maven-badges.herokuapp.com/maven-central/com.github.owlcs/ontapi/badge.svg" height="25" alt="Maven Central"></a>
<a href="https://javadoc.io/doc/com.github.owlcs/ontapi/latest/index.html"><img src="https://javadoc.io/badge2/com.github.owlcs/ontapi/javadoc.svg" height="25" alt="Javadoc"></a>
<a href="https://jb.gg/OpenSourceSupport"><img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.svg" height="25" alt="JetBrains open source"></a>


## Summary

ONT-API is an [RDF](https://www.w3.org/TR/rdf11-concepts/)-centric Java library to work with [OWL2](https://www.w3.org/TR/owl2-syntax/).

For more info about the library see the project [wiki](https://github.com/owlcs/ont-api/wiki).

## Dependencies

- **[Apache Jena](https://github.com/apache/jena)** (**5.x.x**)
- **[OWL-API](https://github.com/owlcs/owlapi)** (**5.x.x**)
- **[concurrent-rdf-graph](https://github.com/sszuev/concurrent-rdf-graph)** (jitpack)

## Requirements

- Java **17+**

## License

* Apache License Version 2.0
* GNU LGPL Version 3.0

## Example

```java
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;

public class Examples {
    public static void main(String... args) {
        String iri = "https://github.com/owlcs/ont-api";

        OWLDataFactory df = OntManagers.getDataFactory();
        OntologyManager om = OntManagers.createManager();

        // set ontology specification, see jena-owl2 for more details
        om.getOntologyConfigurator().setSpecification(OntSpecification.OWL2_EL_MEM_RULES_INF);

        // create OWLAPI impl
        Ontology owlapi = om.createOntology(IRI.create(iri));
        // add OWL class declaration
        owlapi.addAxiom(df.getOWLDeclarationAxiom(df.getOWLClass(iri + "#Class1")));

        // view as Jena impl
        OntModel jena = owlapi.asGraphModel();
        // add OWL class declaration
        jena.createResource(iri + "#Class2", OWL.Class);

        // lists axioms (finds axioms from base graph, inferred by Jena Reasoner are not included)
        owlapi.axioms().forEach(System.out::println);

        // and print to stdout in turtle format
        jena.write(System.out, "ttl");

        // play with InfModel representation
        System.out.println(jena.asInferenceModel().validate().isValid());

        // SPARQL (finds all statements including inferred ones)
        try (QueryExecution exec = QueryExecutionFactory.create(
                QueryFactory.create(
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                                "SELECT ?s ?o WHERE { ?s a ?o }"
                ), jena)) {
            ResultSet res = exec.execSelect();
            while (res.hasNext()) {
                System.out.println(res.next());
            }
        }
    }
}
```

