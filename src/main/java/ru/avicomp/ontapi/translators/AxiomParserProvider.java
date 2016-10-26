package ru.avicomp.ontapi.translators;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.reflect.ClassPath;
import ru.avicomp.ontapi.OntException;

/**
 * Axiom Graph Translator loader.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {
    private static final Logger LOGGER = Logger.getLogger(AxiomParserProvider.class);

    public static Set<Class<? extends AxiomTranslator>> getParsers() {
        return ParserHolder.CLASS_SET;
    }

    @SuppressWarnings("unchecked")
    public static AxiomTranslator get(OWLAxiom axiom) {
        Class<? extends OWLAxiom> actualClass = axiom.getAxiomType().getActualClass();
        Class<? extends AxiomTranslator> parserClass = getParsers().stream().filter(c -> isRelatedToAxiom(c, actualClass)).
                findFirst().orElseThrow(() -> new OntException("Can't find parser for axiom: " + axiom));
        try {
            AxiomTranslator res = parserClass.newInstance();
            res.init(axiom);
            return res;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new OntException("Can't instance parser for axiom: " + axiom, e);
        }
    }

    private static boolean isRelatedToAxiom(Class<? extends AxiomTranslator> parserClass, Class<? extends OWLAxiom> actualClass) {
        ParameterizedType type = ((ParameterizedType) parserClass.getGenericSuperclass());
        if (type == null) return false;
        for (Type t : type.getActualTypeArguments()) {
            if (actualClass.getName().equals(t.getTypeName())) return true;
        }
        return false;
    }

    private static class ParserHolder {
        private static final Set<Class<? extends AxiomTranslator>> CLASS_SET = collect();

        static {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("There are following axiom-parsers (" + CLASS_SET.size() + "): ");
                CLASS_SET.forEach(LOGGER::debug);
            }
        }

        @SuppressWarnings("unchecked")
        private static Set<Class<? extends AxiomTranslator>> collect() {
            try {
                Set<ClassPath.ClassInfo> classes = ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses(AxiomTranslator.class.getPackage().getName());
                Stream<Class> res = classes.stream().map(ParserHolder::parseClass).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(AxiomTranslator.class::isAssignableFrom);
                return res.map((Function<Class, Class<? extends AxiomTranslator>>) c -> c).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new OntException("Can't collect parsers classes", e);
            }
        }

        private static Class parseClass(ClassPath.ClassInfo info) {
            try {
                return Class.forName(info.getName());
            } catch (ClassNotFoundException e) {
                throw new OntException("Can't find class " + info, e);
            }
        }
    }

}
