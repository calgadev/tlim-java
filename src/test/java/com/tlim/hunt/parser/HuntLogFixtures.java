package com.tlim.hunt.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Real Hunt Analyser exports copied from the game client into src/test/resources/hunts. */
public final class HuntLogFixtures {

    private HuntLogFixtures() {}

    public static String read(String name) {
        try (InputStream in = HuntLogFixtures.class.getResourceAsStream("/hunts/" + name)) {
            if (in == null) throw new IllegalStateException("Missing fixture: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
