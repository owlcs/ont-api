# ONT-API (ver. 3.x.x)

## Summary

ONT-API is an RDF-centric Java library to work with OWL.

For more info see [wiki](https://github.com/owlcs/ont-api/wiki).

## Dependencies

- **[Apache Jena](https://github.com/apache/jena)** (**4.x.x**)
- **[OWL-API](https://github.com/owlcs/owlapi)** (**5.x.x**)

## Requirements

- Java **11+**

## License

* Apache License Version 2.0
* GNU LGPL Version 3.0

## Example
```java
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.rdf.model.Model;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;

public class Examples {
    public static void main(String... args) {
        String iri = "http://example.com";
        OWLDataFactory df = OntManagers.getDataFactory();

        Ontology owl = OntManagers.createManager().createOntology(IRI.create(iri));
        owl.addAxiom(df.getOWLDeclarationAxiom(df.getOWLClass(iri + "#Class1")));

        Model jena = owl.asGraphModel();
        jena.createResource(iri + "#Class2", OWL.Class);

        owl.axioms().forEach(System.out::println);
        jena.write(System.out, "ttl");
    }
}
```

