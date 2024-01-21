# ONT-API (ver. 3.x.x)

<a href="https://maven-badges.herokuapp.com/maven-central/com.github.owlcs/ontapi"><img src="https://maven-badges.herokuapp.com/maven-central/com.github.owlcs/ontapi/badge.svg" height="25" alt="Maven Central"></a>
<a href="https://javadoc.io/doc/com.github.owlcs/ontapi/latest/index.html"><img src="https://javadoc.io/badge2/com.github.owlcs/ontapi/javadoc.svg" height="25" alt="Javadoc"></a>
<a href="https://jb.gg/OpenSourceSupport"><img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.svg" height="25" alt="JetBrains open source"></a>


## Summary

ONT-API is an [RDF](https://www.w3.org/TR/rdf11-concepts/)-centric Java library to work with [OWL2](https://www.w3.org/TR/owl2-syntax/).

For more info about the library see the project [wiki](https://github.com/owlcs/ont-api/wiki).

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
import vocabulary.com.github.sszuev.jena.ontapi.OWL;
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

