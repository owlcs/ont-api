package ru.avicomp.ontapi.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.RDFRendererBase;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.EscapeUtils;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import ru.avicomp.ontapi.OntException;

/**
 * ONT-API Turtle Renderer
 * it is mostly copy-past from {@link org.semanticweb.owlapi.rdf.turtle.renderer.TurtleRenderer} (5.0.2) because it is almost private.
 * <p>
 * Created by szuev on 12.05.2016.
 */
public class OntTurtleRenderer extends RDFRendererBase {

    private final PrintWriter writer;
    private final PrefixManager pm;
    private final Set<RDFNode> pending = new HashSet<>();
    private final Deque<RDFResourceBlankNode> nodesToRenderSeparately = new LinkedList<>();
    private final OWLDocumentFormat format;
    private final Deque<Integer> tabs = new LinkedList<>();
    private int bufferLength = 0;
    private int lastNewLineIndex = 0;
    private int level = 0;

    /**
     * @param ontology ontology
     * @param writer   writer
     */
    OntTurtleRenderer(OWLOntology ontology, Writer writer) {
        super(ontology, OntFormat.TTL_RDF.getOwlFormat(), ontology.getOWLOntologyManager().getOntologyWriterConfiguration());
        this.format = OntFormat.TTL_RDF.getOwlFormat();
        this.writer = new PrintWriter(writer);
        pm = new DefaultPrefixManager();
        if (PrefixManager.class.isInstance(ontology.getFormat())) {
            PrefixManager _pm = (PrefixManager) ontology.getFormat();
            pm.copyPrefixesFrom(_pm);
            pm.setPrefixComparator(_pm.getPrefixComparator());
        }
    }

    private void writeNamespaces() {
        pm.getPrefixName2PrefixMap().forEach((k, v) -> {
            write("@prefix ");
            write(k);
            writeSpace();
            writeAsURI(v);
            write(" .");
            writeNewLine();
        });
    }

    private void pushTab() {
        tabs.push(getIndent());
    }

    private void popTab() {
        if (!tabs.isEmpty()) {
            tabs.pop();
        }
    }

    private void write(@Nonnull String s) {
        int newLineIndex = s.indexOf('\n');
        if (newLineIndex != -1) {
            lastNewLineIndex = bufferLength + newLineIndex;
        }
        writer.write(s);
        bufferLength += s.length();
    }

    private int getCurrentPos() {
        return bufferLength;
    }

    private int getIndent() {
        return getCurrentPos() - lastNewLineIndex;
    }

    private void writeAsURI(@Nonnull String s) {
        write("<");
        write(s);
        write(">");
    }

    private void write(@Nonnull IRI iri) {
        if (NodeID.isAnonymousNodeIRI(iri)) {
            write(iri.toString());
        /*} else if (iri.equals(ontology.getOntologyID().getOntologyIRI().orElse(null))) {
            writeAsURI(iri.toString());*/
        } else {
            String name = pm.getPrefixIRI(iri);
            if (name == null) {
                // No QName!
                // As this is not an XML output, qnames are not necessary; other
                // splits are allowed.
                name = forceSplitIfPrefixExists(iri);
            }
            if (name == null) {
                // no qname and no matching prefix
                writeAsURI(iri.toString());
            } else {
                if (name.indexOf(':') != -1) {
                    write(name);
                } else {
                    write(":");
                    write(name);
                }
            }
        }
    }

    private String forceSplitIfPrefixExists(IRI iri) {
        List<Map.Entry<String, String>> prefixName2PrefixMap = new ArrayList<>(pm.getPrefixName2PrefixMap().entrySet());
        // sort the entries in reverse lexicographic order by value (longest
        // prefix first)
        Collections.sort(prefixName2PrefixMap, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        String actualIRI = iri.toString();
        for (Map.Entry<String, String> e : prefixName2PrefixMap) {
            if (actualIRI.startsWith(e.getValue())) {
                return e.getKey() + actualIRI.substring(e.getValue().length());
            }
        }
        return null;
    }

    private void writeNewLine() {
        write("\n");
        int tabIndent = 0;
        if (!tabs.isEmpty()) {
            tabIndent = tabs.peek();
        }
        for (int i = 1; i < tabIndent; i++) {
            write(" ");
        }
    }

    private void writeAt() {
        write("@");
    }

    private void writeSpace() {
        write(" ");
    }

    private void write(@Nonnull RDFNode node) {
        if (node.isLiteral()) {
            write((RDFLiteral) node);
        } else {
            write((RDFResource) node);
        }
    }

    private void write(RDFLiteral node) {
        if (!node.isPlainLiteral()) {
            if (node.getDatatype().equals(XSDVocabulary.INTEGER.getIRI())) {
                write(node.getLexicalValue());
            } else if (node.getDatatype().equals(XSDVocabulary.DECIMAL.getIRI())) {
                write(node.getLexicalValue());
            } else {
                writeStringLiteral(node.getLexicalValue());
                if (node.hasLang()) {
                    writeAt();
                    write(node.getLang());
                } else {
                    write("^^");
                    write(node.getDatatype());
                }
            }
        } else {
            writeStringLiteral(node.getLexicalValue());
            if (node.hasLang()) {
                writeAt();
                write(node.getLang());
            }
        }
    }

    private void writeStringLiteral(@Nonnull String literal) {
        String escapedLiteral = EscapeUtils.escapeString(literal);
        if (escapedLiteral.indexOf('\n') != -1) {
            write("\"\"\"");
            write(escapedLiteral);
            write("\"\"\"");
        } else {
            write("\"");
            write(escapedLiteral);
            write("\"");
        }
    }

    private void write(@Nonnull RDFResource node) {
        if (!node.isAnonymous()) {
            write(node.getIRI());
        } else {
            pushTab();
            if (!isObjectList(node)) {
                render(node);
            } else {
                // List - special syntax
                List<RDFNode> list = new ArrayList<>();
                toJavaList(node, list);
                pushTab();
                write("(");
                write(" ");
                pushTab();
                for (Iterator<RDFNode> it = list.iterator(); it.hasNext(); ) {
                    write(OntException.notNull(it.next(), "Null iteration element."));
                    if (it.hasNext()) {
                        writeNewLine();
                    }
                }
                popTab();
                writeNewLine();
                write(")");
                popTab();
            }
            popTab();
        }
    }

    @Override
    protected void beginDocument() {
        writeNamespaces();
        writeNewLine();
    }

    @Override
    protected void endDocument() {
        writer.flush();
        writeComment(format.getKey());
        if (!format.isAddMissingTypes()) {
            // missing type declarations could have been omitted, adding a
            // comment to document it
            writeComment("Warning: type declarations were not added automatically.");
        }
        writer.flush();
    }

    @Override
    protected void writeClassComment(@Nonnull OWLClass cls) {
    }

    @Override
    protected void writeObjectPropertyComment(@Nonnull OWLObjectProperty prop) {
    }

    @Override
    protected void writeDataPropertyComment(@Nonnull OWLDataProperty prop) {
    }

    @Override
    protected void writeIndividualComments(@Nonnull OWLNamedIndividual ind) {
    }

    @Override
    protected void writeAnnotationPropertyComment(@Nonnull OWLAnnotationProperty prop) {
    }

    @Override
    protected void writeDatatypeComment(@Nonnull OWLDatatype datatype) {
    }

    private void writeComment(@Nonnull String comment) {
        write("###  ");
        write(comment);
        writeNewLine();
    }

    @Override
    protected void endObject() {
        //writeNewLine();
    }

    @Override
    protected void writeBanner(@Nullable String name) {
    }

    @Override
    public void render(@Nonnull RDFResource node) {
        level++;
        Collection<RDFTriple> triples;
        if (pending.contains(node)) {
            // We essentially remove all structure sharing during parsing - any
            // cycles therefore indicate a bug!
            triples = Collections.emptyList();
        } else {
            triples = graph.getTriplesForSubject(node);
        }
        pending.add(node);
        RDFResource lastSubject = null;
        RDFResourceIRI lastPredicate = null;
        boolean first = true;
        for (RDFTriple triple : triples) {
            RDFResource subj = triple.getSubject();
            RDFResourceIRI pred = triple.getPredicate();
            RDFNode object = triple.getObject();
            if (lastSubject != null && (subj.equals(lastSubject) || subj.isAnonymous())) {
                if (lastPredicate != null && pred.equals(lastPredicate)) {
                    // Only the object differs from previous triple
                    // Just write the object
                    write(" ,");
                    writeNewLine();
                    if (object.isAnonymous() && object.isIndividual() && object.shouldOutputId()) {
                        if (!pending.contains(object)) {
                            nodesToRenderSeparately.add((RDFResourceBlankNode) object);
                        }
                        write(object.getIRI());
                    } else {
                        write(object);
                    }
                } else {
                    // The predicate, object differ from previous triple
                    // Just write the predicate and object
                    write(" ;");
                    popTab();
                    writeNewLine();
                    write(triple.getPredicate());
                    writeSpace();
                    pushTab();
                    if (object.isAnonymous() && object.isIndividual() && object.shouldOutputId()) {
                        if (!pending.contains(object)) {
                            nodesToRenderSeparately.add((RDFResourceBlankNode) object);
                        }
                        write(object.getIRI());
                    } else {
                        write(object);
                    }
                }
            } else {
                if (!first) {
                    popTab();
                    popTab();
                    writeNewLine();
                }
                // Subject, predicate and object are different from last triple
                if (!node.isAnonymous()) {
                    write(subj);
                    writeSpace();
                } else if (node.isIndividual() && node.shouldOutputId()) {
                    write(subj.getIRI());
                    writeSpace();
                } else {
                    pushTab();
                    write("[");
                    writeSpace();
                }
                pushTab();
                write(triple.getPredicate());
                writeSpace();
                pushTab();
                if (object.isAnonymous() && object.isIndividual() && object.shouldOutputId()) {
                    if (!pending.contains(object)) {
                        nodesToRenderSeparately.add((RDFResourceBlankNode) object);
                    }
                    write(object.getIRI());
                } else {
                    write(object);
                }
            }
            lastSubject = subj;
            lastPredicate = pred;
            first = false;
        }
        if (node.isAnonymous()) {
            popTab();
            popTab();
            if (!node.isIndividual() || !node.shouldOutputId()) {
                if (triples.isEmpty()) {
                    write("[ ");
                } else {
                    writeNewLine();
                }
                write("]");
            }
            popTab();
        } else {
            popTab();
            popTab();
        }
        if (level == 1 && !triples.isEmpty()) {
            write(" .\n\n");
        }
        writer.flush();
        level--;
        while (!nodesToRenderSeparately.isEmpty()) {
            render(nodesToRenderSeparately.poll());
        }
        pending.remove(node);
    }
}

