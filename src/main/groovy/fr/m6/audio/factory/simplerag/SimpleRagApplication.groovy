package fr.m6.audio.factory.simplerag

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiModelName
import dev.langchain4j.retriever.EmbeddingStoreRetriever
import dev.langchain4j.retriever.Retriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader

import java.time.Duration
import java.time.temporal.ChronoUnit

import static java.time.temporal.ChronoUnit.SECONDS

@SpringBootApplication
class SimpleRagApplication {
    static final String MODEL = OpenAiModelName.GPT_4

    static void main(String[] args) {
        SpringApplication.run(SimpleRagApplication, args)
    }

    @Bean
    LibrarianAgent customerSupportAgent(ChatLanguageModel chatLanguageModel, Retriever<TextSegment> retriever) {
        return AiServices.builder(LibrarianAgent)
                .chatLanguageModel(chatLanguageModel)
                .retriever(retriever)
                .build();
    }

    @Bean
    ChatLanguageModel chatLanguageModel() {
        OpenAiChatModel.builder()
                .apiKey('xxx')
                .modelName(MODEL)
                .temperature(0)
//                .topP(1.0)
//                .maxTokens(100)
//                .presencePenalty(0)
//                .frequencyPenalty(0)
                .timeout(Duration.of(60, SECONDS))
//                .maxRetries(3)
//                .logRequests(true)
//                .logResponses(true)
                .build()
    }

    @Bean
    Retriever<TextSegment> retriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using
        int maxResultsRetrieved = 20
        double minScore = 0.8

        return EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, maxResultsRetrieved, minScore);
    }


    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, ResourceLoader resourceLoader) throws IOException {

        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        // 1. Create an in-memory embedding store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

//		// 2. Load an example document ("Miles of Smiles" terms of use)
//		Resource resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt");
//		Document document = loadDocument(resource.getFile().toPath());
//
//		// 3. Split the document into segments 100 tokens each
//		// 4. Convert segments into embeddings
//		// 5. Store embeddings into embedding store
//		// All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
//		DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, new OpenAiTokenizer(GPT_3_5_TURBO));
//		EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//				.documentSplitter(documentSplitter)
//				.embeddingModel(embeddingModel)
//				.embeddingStore(embeddingStore)
//				.build();
//		ingestor.ingest(document);

        return embeddingStore;
    }
}
