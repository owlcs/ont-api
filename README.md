### ONT-API (ver. 17/04/2017)

## Summary
ONT-API is OWL-API over Apache Jena.
In other words it is an attempt to befriend these two semantic-web technologies.
 
## Dependencies
- OWL-API (https://github.com/owlcs/owlapi), current version: 5.0.5
- Apache Jena (https://github.com/apache/jena), current version: 3.0.1

## Goal
Pure Jena does _NOT_ support OWL-2 specification and pure OWL-API does _NOT_ provide an adequate opportunity to work with an ontological graph.
ONT-API is an implementation of the OWL-API interfaces on Jena, thus in addition to the structural representation of ontology (axioms), 
it is possible also to work with the Graph directly.
This can be useful for Jena-users, since it allows to use API, that meets the specification, along with other products, 
written on Jena as well (such as topbraid-spin, d2rq, etc).
In additional, ONT-API may be useful for OWL-API users too, since the use of a single graph instead of storing disparate elements 
allows solving a number of problems and bugs in the original OWL-API. 
In particular, the graph can contain rdf:List, while in OWL-API (5.0.5) the attempt to load and reload 
the ontology with the rdf:List inside (which does not belong to any axiom) leads to the breakdown of this ontology.

## Concept
The principle of ONT-API is that all information remains in the graph, 
and if a set of triples from this graph corresponds to an axiom, then we can read them in the form of this axiom. 
Similarly, the writing of axioms goes in triplet form. 
In ONT-API axioms are not stored in separately if not to take into account the cache.
In the ONT-API reading and writing an ontology from a file or a stream happen through Jena,
however, the original OWL-API mechanisms to read/write also remain working, 
and even are used explicitly if the data format is not supported by Jena (for example Functional Syntax, Manchester Syntax, OWL/RDF, etc).
ONT-API supports all OWL-API features and options, but they are somewhat expanded. 
Instead of the original OWL-API interfaces in ONT-API there are overridden with several additional methods.
Also there are new configuration options and the policy with exceptions has been changed a little.
Nevertheless, it is always possible to use the original OWL-API or its parts in conjunction with ONT-API, 
for example, you can copy an ontology from the ONT-API manager to the OWL-API manager and vice versa.
The project contains tests from the OWL-API-contract, which show the working capacity of ONT-API.

## Structure (briefly)
* The root of project (package __ru.avicomp.ontapi__) contains the core classes. Some of them are:  
    * _OntManagers_ access point to the ONT-API and OWL-API
    * _OntologyManager_ is an extended __org.semanticweb.owlapi.model.OWLOntologyManager__ with access to overridden _OWLOntology_. 
    * _OntologyModel_ is an extended __org.semanticweb.owlapi.model.OWLOntology__ with methods to get Jena (Graph) Model shadow and method to manage axioms cache.
    * _OntFactoryImpl_ is an extended __org.semanticweb.owlapi.model.OWLOntologyFactory__, the point to create and load ontologies.
    * _OntFormat_ - the bridge between __org.apache.jena.riot.Lang__ and __org.semanticweb.owlapi.model.OWLDocumentFormat__.
* Package __ru.avicomp.ontapi.internal__ contains Axiom Translators (for reading and writing axioms to/from graph), technical interfaces and some helpers. Some important classes:
    * _AxiomTranslator_ - it is the base for any axiom translator. There are 39 axiom translators, each of them corresponds to the particular axiom-type.
    * _InternalModel_  - it is the buffer between triple and structural representation.
* Package __ru.avicomp.ontapi.config__ contains classes to work with project settings. Main classes:
    * _OntConfig_ it is overridden  __org.semanticweb.owlapi.model.OntologyConfigurator__, global manager config.
    * _OntLoaderConfiguration_ - overridden __org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration__.
    * _OntWriterConfiguration_ - overridden __org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration__.  
* Package __ru.avicomp.ontapi.jena__ is a separated subsystem with _ru.avicomp.ontapi.jena.model.OntGraphModel_ which 
is analogue of __org.apache.jena.ontology.OntModel__ but for OWL-2. This subsystem can be used autonomously.
* Package __ru.avicomp.ontapi.transforms__ is a separated subsystem also. It puts in order any graph (RDFS, OWL1) before using the main API. 
This little subsystem can be used autonomously.
