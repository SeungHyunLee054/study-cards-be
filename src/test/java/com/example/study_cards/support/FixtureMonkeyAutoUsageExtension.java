package com.example.study_cards.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class FixtureMonkeyAutoUsageExtension implements BeforeEachCallback {

    private static final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.create();

    @Override
    public void beforeEach(ExtensionContext context) {
        fixtureMonkey.giveMeOne(String.class);
    }
}
