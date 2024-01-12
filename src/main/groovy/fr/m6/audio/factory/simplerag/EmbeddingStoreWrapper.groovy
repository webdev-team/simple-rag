package fr.m6.audio.factory.simplerag

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.data.segment.TextSegmentTransformer
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.retriever.EmbeddingStoreRetriever
import dev.langchain4j.retriever.Retriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

class EmbeddingStoreWrapper {
    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>()
    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel()
    DocumentSplitter splitter = new DocumentBySentenceSplitter(120, 20, new OpenAiTokenizer(SimpleRagApplication.MODEL))

    /**
     * Process segments (to add metadata for exemple) before they go in the store
     */
    TextSegmentTransformer textSegmentTransformer

    void withIngestor(@ClosureParams(value = SimpleType, options = "dev.langchain4j.store.embedding.EmbeddingStoreIngestor") Closure<Void> closure) {
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .textSegmentTransformer(textSegmentTransformer)
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build()

        closure.call(ingestor)
    }

    Retriever<TextSegment> getRetriever() {
        int maxResults = 16
        double minScore = 0.7
        EmbeddingStoreRetriever.from(store, embeddingModel, maxResults, minScore)
    }

    void load(File path) {
        store = InMemoryEmbeddingStore.fromFile(path.absolutePath)
    }

    void save(File path) {
        store.serializeToFile(path.absolutePath)
    }
}
