package fr.m6.audio.factory.simplerag

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.FileSystemDocumentLoader
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.retriever.Retriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static fr.m6.audio.factory.simplerag.SimpleRagApplication.MODEL

@SpringBootTest
class ChatTest {
    @Autowired
    LibrarianAgent agent

    @Autowired
    Retriever<TextSegment> retriever

    @Autowired
    EmbeddingModel embeddingModel

    @Autowired
    EmbeddingStore<TextSegment> embeddingStore

    @BeforeEach
    void init() {
        // 2. Load an example document ("Miles of Smiles" terms of use)
//        Document document = FileSystemDocumentLoader.loadDocument(new File(ChatTest.getResource("/miles-of-smiles-terms-of-use.txt").toURI()).toPath());
        long time1 = System.currentTimeMillis()

        List<Document> documents = []

        new File(ChatTest.getResource("/transcripts").toURI()).eachFile {file ->
            Document doc = FileSystemDocumentLoader.loadDocument(file.toPath())

            doc.metadata().add('source', file.name)

            documents.add(doc)
        }

//        List<Document> documents = FileSystemDocumentLoader.loadDocuments(new File(ChatTest.getResource("/transcripts").toURI()).toPath());


        // 3. Split the document into segments 100 tokens each
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
//        DocumentSplitter documentSplitter = DocumentSplitters.recursive(50, 10, new OpenAiTokenizer(OpenAiModelName.GPT_3_5_TURBO));

        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(60, 0, new OpenAiTokenizer(MODEL))

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()


        ingestor.ingest(documents)

        long time2 = System.currentTimeMillis()

        println "ingestion " + (time2 - time1) + '\n\n'
    }

    @Test
    void chat() {
//        println agent.chat('Bonjour, quelles émissions parlent de musique ?') + '\n\n'

        println agent.chat('Bonjour, quelles sont les actualités insolites du jour ?') + '\n\n'

        retriever.findRelevant('Bonjour, quelles émissions parlent de musique ?').each {
            println it.metadata().get('source') + ' at index ' + it.metadata().get('index')
            println it.text()
            println ''
        }

        // println agent.chat('Hi, I forgot when my booking is.')
    }
}
