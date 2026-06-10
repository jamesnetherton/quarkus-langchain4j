package io.quarkiverse.langchain4j.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.runtime.aiservice.AiServiceContextProvider;
import io.quarkiverse.langchain4j.runtime.aiservice.QuarkusAiServiceContext;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@link AiServiceContextProvider} allows extensions to control
 * how the {@link QuarkusAiServiceContext} is created for specific AI service interfaces.
 * <p>
 * When a provider handles an interface, {@code AiServices.builder()} should return a fresh
 * context instead of the CDI-managed one, ensuring the builder-configured values are used.
 */
public class AiServiceContextProviderTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            CustomHandledService.class,
                            TestAiServiceContextProvider.class,
                            TestChatModel.class,
                            TestChatModelSupplier.class));

    @Test
    @ActivateRequestContext
    void providerShouldBeUsedForHandledInterface() {
        assertThat(TestAiServiceContextProvider.invoked)
                .as("Provider should not have been invoked yet")
                .isFalse();

        CustomHandledService service = AiServices.builder(CustomHandledService.class)
                .chatModel(new TestChatModel("from-builder"))
                .build();

        assertThat(TestAiServiceContextProvider.invoked)
                .as("Provider should have been invoked during builder creation")
                .isTrue();

        String result = service.chat("hello");
        assertThat(result).isEqualTo("from-builder");
    }

    @RegisterAiService(chatLanguageModelSupplier = TestChatModelSupplier.class, chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
    public interface CustomHandledService {
        @UserMessage("{msg}")
        String chat(String msg);
    }

    public static class TestChatModelSupplier implements Supplier<ChatModel> {
        @Override
        public ChatModel get() {
            return new TestChatModel("from-supplier");
        }
    }

    @ApplicationScoped
    public static class TestAiServiceContextProvider implements AiServiceContextProvider {
        static volatile boolean invoked = false;

        @Override
        public boolean handles(String aiServiceClassName) {
            return aiServiceClassName.endsWith("CustomHandledService");
        }

        @Override
        public AiServiceContext create(Class<?> aiServiceClass) {
            invoked = true;
            return new QuarkusAiServiceContext(aiServiceClass);
        }
    }

    public static class TestChatModel implements ChatModel {
        private final String prefix;

        public TestChatModel(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            return ChatResponse.builder()
                    .aiMessage(new AiMessage(prefix))
                    .build();
        }
    }
}
