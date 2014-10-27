package uk.ac.manchester.cs.owl.owlapi;

import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * Created by ses on 10/7/14.
 */
public interface HasIncrementalSignatureGenerationSupport {

    /**
     * @param entities
     *        entity set where entities will be added
     * @return the modified input entities
     */
    @Nonnull
    default Set<OWLEntity> addSignatureEntitiesToSet(
            @Nonnull Set<OWLEntity> entities) {
        return entities;
    }

    /**
     * @param anons
     *        anonymous individuals set where individuals will be added
     * @return the modified input individuals
     */
    @Nonnull
    default Set<OWLAnonymousIndividual> addAnonymousIndividualsToSet(
            @Nonnull Set<OWLAnonymousIndividual> anons) {
        return anons;
    }
}
