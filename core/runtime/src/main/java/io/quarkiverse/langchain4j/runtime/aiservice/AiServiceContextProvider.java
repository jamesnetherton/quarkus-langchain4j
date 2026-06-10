package io.quarkiverse.langchain4j.runtime.aiservice;

import dev.langchain4j.service.AiServiceContext;

/**
 * Allows extensions to control how the {@link AiServiceContext} is created.
 */
public interface AiServiceContextProvider {

    /**
     * Returns whether this provider handles context creation for the given AI service class.
     *
     * @param aiServiceClassName the fully qualified name of the AI service interface
     * @return {@code true} if this provider should create the context, {@code false} to
     *         fall through to the default {@link QuarkusAiServiceContext} creation behavior
     */
    boolean handles(String aiServiceClassName);

    /**
     * Creates an {@link AiServiceContext} for the given AI service class.
     * Only called when {@link #handles(String)} returns {@code true}.
     *
     * @param aiServiceClass the AI service interface class
     * @return the created {@link AiServiceContext}
     */
    AiServiceContext create(Class<?> aiServiceClass);
}
