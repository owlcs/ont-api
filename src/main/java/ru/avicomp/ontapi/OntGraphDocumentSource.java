package ru.avicomp.ontapi;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.PrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import ru.avicomp.ontapi.jena.utils.Graphs;

/**
 * Base class for {@link OWLOntologyDocumentSource} to have direct access to any encapsulated graph.
 * <p>
 * Created by szuev on 22.02.2017.
 */
public abstract class OntGraphDocumentSource implements OWLOntologyDocumentSource {
    protected static final Logger LOGGER = LoggerFactory.getLogger(OntGraphDocumentSource.class);

    private List<Exception> exceptions = new ArrayList<>();

    public abstract Graph getGraph();

    @Override
    public Optional<Reader> getReader() {
        return getInputStream().map(is -> new InputStreamReader(is, Charsets.UTF_8));
    }

    @Override
    public Optional<InputStream> getInputStream() {
        return format().map(OntFormat::getLang).map(this::toInputStream).filter(Objects::nonNull);
    }

    private InputStream toInputStream(Lang lang) {
        try {
            return toInputStream(getGraph(), lang);
        } catch (Exception e) {
            LOGGER.error("Can't get InputStream.", e);
            exceptions.add(e);
            return null;
        }
    }

    public static InputStream toInputStream(Graph graph, Lang lang) { // move to graph utils?
        PipedInputStream res = new PipedInputStream();
        new Thread(() -> {
            IOException ex = null;
            try (PipedOutputStream out = new PipedOutputStream(res)) {
                RDFDataMgr.write(out, graph, lang);
            } catch (IOException e) {
                LOGGER.error("Can't write.", e);
                ex = e;
            }
            if (ex != null) throw new OntApiException("Exception while converting output->input", ex);
        }).start();
        return res;
    }

    protected IRI getGraphIRI() {
        String uri = Graphs.getURI(getGraph());
        return uri != null ? IRI.create(uri) : null;
    }

    @Override
    public Optional<OWLDocumentFormat> getFormat() {
        PrefixMapping pm = getGraph().getPrefixMapping();
        return format().map(OntFormat::createOwlFormat)
                .map(f -> {
                    PrefixManager res = PrefixManager.class.cast(f);
                    pm.getNsPrefixMap().forEach(res::setPrefix);
                    return f;
                });
    }

    public OntFormat getOntFormat() {
        return OntFormat.TURTLE;
    }

    private Optional<OntFormat> format() {
        return Optional.of(getOntFormat());
    }

    @Override
    public Optional<String> getMIMEType() {
        return format().map(OntFormat::getLang).map(Lang::getContentType).map(ContentType::getContentType);
    }

    @Override
    public boolean hasAlredyFailedOnStreams() {
        return !exceptions.isEmpty();
    }

    @Override
    public boolean hasAlredyFailedOnIRIResolution() {
        return false;
    }

    @Override
    public void setIRIResolutionFailed(boolean value) {
        throw new OntApiException.Unsupported(getClass(), "Unsupported");
    }
}
