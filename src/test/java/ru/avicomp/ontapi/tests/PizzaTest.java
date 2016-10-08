package ru.avicomp.ontapi.tests;

/**
 * Pizza.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class PizzaTest extends LoadTestBase {
    @Override
    public String getFileName() {
        return "pizza.ttl";
    }

    @Override
    public long getTotalNumberOfAxioms() {
        return 945;
    }
}
