package ru.avicomp.ontapi.config;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OntologyConfigurator;

import ru.avicomp.ontapi.OntApiException;

/**
 * The config builder.
 * see base class {@link org.semanticweb.owlapi.model.OntologyConfigurator}
 * TODO: complete it.
 * Created by szuev on 27.02.2017.
 */
public class OntConfig extends OntologyConfigurator {

    @Override
    public OntLoaderConfiguration buildLoaderConfiguration() {
        return new OntLoaderConfiguration(super.buildLoaderConfiguration());
    }

    @Override
    public OntWriterConfiguration buildWriterConfiguration() {
        return new OntWriterConfiguration(super.buildWriterConfiguration());
    }

    public static OntConfig copy(OntologyConfigurator from) {
        OntConfig res = new OntConfig();
        if (from == null) return res;
        try {
            Field ignoredImports = from.getClass().getDeclaredField("ignoredImports");
            ignoredImports.setAccessible(true);
            ignoredImports.set(res, ignoredImports.get(from));
            Field overrides = from.getClass().getDeclaredField("overrides");
            overrides.setAccessible(true);
            overrides.set(res, overrides.get(from));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new OntApiException("Can't copy configuration.", e);
        }
        return res;
    }


    public enum DefaultScheme implements Scheme {
        HTTP,
        HTTPS,
        FTP,
        FILE,;

        @Override
        public String key() {
            return name().toLowerCase();
        }

        @Override
        public boolean same(IRI iri) {
            return Objects.equals(key(), iri.getScheme());
        }

        public static Stream<DefaultScheme> all() {
            return Stream.of(values());
        }
    }

    public interface Scheme extends Serializable {
        String key();

        boolean same(IRI iri);
    }

    public interface OptionSetting {
        Serializable getDefaultValue();
    }
}
