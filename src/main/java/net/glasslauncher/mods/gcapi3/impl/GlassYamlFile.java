package net.glasslauncher.mods.gcapi3.impl;

import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.api.QuoteStyle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlassYamlFile extends YamlFile {

    public GlassYamlFile() {
        super();
    }

    public GlassYamlFile(String override) throws IOException {
        super();
        loadFromString(override);
    }

    public GlassYamlFile(File file) throws IllegalArgumentException {
        super(file);
        options().useComments(true);
        setCommentFormat(YamlCommentFormat.PRETTY);
        options().quoteStyleDefaults().setQuoteStyle(List.class, QuoteStyle.DOUBLE);
        options().quoteStyleDefaults().setQuoteStyle(Map.class, QuoteStyle.DOUBLE);
        options().quoteStyleDefaults().setQuoteStyle(String.class, QuoteStyle.DOUBLE);
        options().quoteStyleDefaults().setQuoteStyle(String[].class, QuoteStyle.DOUBLE);
        options().headerFormatter()
                .prefixFirst("#####################################################")
                .commentPrefix("##  ")
                .commentSuffix("  ##")
                .suffixLast("#####################################################");

        //   ###################################################
        setHeader("""
                CONFIG GENERATED BY GLASS CONFIG API (GCAPI3).
                VERIFY YOU HAVE TYPED YOUR CONFIG CORRECTLY\040\040
                BEFORE SUBMITTING BUG OR CRASH REPORTS.\040\040\040\040\040\040
                USE QUOTES (") WHEN TYPING TEXT VALUES.\040\040\040\040\040\040
                USE THE IN-GAME EDITOR WHERE POSSIBLE.\040\040\040\040\040\040\040""");
    }

    public Float getFloat(String key, Float defaultValue) {
        return (Float) get(key, defaultValue);
    }

    public Float getFloat(String key) {
        return (Float) get(key);
    }

    public <T extends Enum<?>> T getEnum(String key, Class<T> targetEnum, T defaultValue) {
        return targetEnum.getEnumConstants()[getInt(key, defaultValue.ordinal())];
    }

    public <T extends Enum<?>> T getEnum(String key, Class<T> targetEnum) {
        int value = getInt(key, -1);
        if (value < 0) {
            return null;
        }
        return targetEnum.getEnumConstants()[value];
    }

    public <T extends Enum<?>> void setEnum(String key, T value) {
        set(key, value.ordinal());
    }

    // This should be safe enough if the map's already pre-filtered... right?
    // Fuck, this is so hacky.
    public void merge(GlassYamlFile other) {
        merge(map, other.map);
    }

    private void merge(Map<String, Object> self, Map<String, Object> other) {
        other.forEach((key, value) -> {
            if (value.getClass() == HashMap.class && self.get(key) != null) {
                //noinspection unchecked
                merge((HashMap<String, Object>) self.get(key), (HashMap<String, Object>) value);
            }
            else {
                self.put(key, value);
            }
        });
    }

    // I hope you like me fucking with internals
    @Override
    public GlassYamlWrapper path(String path) {
        return new GlassYamlWrapper(this, path);
    }

    public GlassYamlWrapper path() {
        return new GlassYamlWrapper(this, "");
    }
}
