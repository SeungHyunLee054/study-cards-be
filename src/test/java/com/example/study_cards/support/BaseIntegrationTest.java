package com.example.study_cards.support;

import com.example.study_cards.TestcontainersConfiguration;
import com.navercorp.fixturemonkey.FixtureMonkey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.create();
}
