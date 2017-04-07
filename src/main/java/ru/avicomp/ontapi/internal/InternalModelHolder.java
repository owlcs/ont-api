package ru.avicomp.ontapi.internal;

/**
 * This is a technical interface to provide access to the {@link InternalModel}.
 * <p>
 * Created by @szuev on 07.04.2017.
 */
public interface InternalModelHolder {

    InternalModel getBase();

    void setBase(InternalModel m);
}
