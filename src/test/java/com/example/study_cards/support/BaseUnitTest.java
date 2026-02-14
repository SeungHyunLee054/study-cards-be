package com.example.study_cards.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    protected static final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.create();
}
