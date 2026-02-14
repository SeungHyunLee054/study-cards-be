package com.example.study_cards.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FailoverIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;

import java.util.List;

public final class FixtureMonkeyFactory {

    private FixtureMonkeyFactory() {
    }

    public static FixtureMonkey create() {
        return FixtureMonkey.builder()
                .objectIntrospector(new FailoverIntrospector(List.of(
                        BuilderArbitraryIntrospector.INSTANCE,
                        ConstructorPropertiesArbitraryIntrospector.INSTANCE,
                        FieldReflectionArbitraryIntrospector.INSTANCE
                )))
                .build();
    }
}
