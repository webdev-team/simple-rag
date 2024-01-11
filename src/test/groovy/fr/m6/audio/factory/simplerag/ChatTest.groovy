package fr.m6.audio.factory.simplerag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentLoaderUtils
import dev.langchain4j.data.document.DocumentSource
import dev.langchain4j.data.document.DocumentType
import dev.langchain4j.data.document.FileSystemDocumentLoader
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.retriever.Retriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import fr.m6.audio.factory.simplerag.domain.FileContent
import fr.m6.audio.factory.simplerag.domain.Media
import groovy.json.JsonSlurper
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

    File databaseFile = new File('./db.json')

    @Test
    void buildDatabase() {
        long time1 = System.currentTimeMillis()

        List<Document> documents = []

        new File(ChatTest.getResource("/transcripts-json").toURI()).eachFile {file ->
            FileContent content = loadFile(file)

            documents.addAll(toDocuments(content))
        }

        EmbeddingStoreWrapper store = new EmbeddingStoreWrapper()

        store.withIngestor {
            it.ingest(documents)
        }

        store.save(databaseFile)

        long time2 = System.currentTimeMillis()

        println "ingestion " + (time2 - time1) + '\n\n'
    }

    @Test
    void chat() {
        EmbeddingStoreWrapper store = new EmbeddingStoreWrapper()

        store.load(databaseFile)

//        println agent.chat('Bonjour, quelles émissions parlent de musique ?') + '\n\n'

        println agent.chat('Bonjour, quelles sont les actualités insolites du jour ?') + '\n\n'

        retriever.findRelevant('Bonjour, quelles émissions parlent de musique ?').each {
            println it.metadata().get('source') + ' at index ' + it.metadata().get('index')
            println it.text()
            println ''
        }

        // println agent.chat('Hi, I forgot when my booking is.')
    }



    FileContent loadFile(File jsonFile) {
        mapper().readValue(jsonFile, FileContent)
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper
    }

    private List<Document> toDocuments(FileContent content) {
        Media media = content.media

        Map<String, Object> metadata = [mediaId: media.id, title: media.title]
        List<Document> documents = []

        documents.add(toDocument(media.title, metadata + [voice: media.speakers?.join(', ') ?: '', timecode: 0]))

//        if (FormatUtils.removeHtmlElementsFromRichText(media.text)) {
//            documents.add(toDocument(FormatUtils.removeHtmlElementsFromRichText(media.text), metadata + [voice: media.speakers?.join(', ') ?: '', timecode: 0]))
//        }

        content.segments?.each { segment ->
            documents.add(toDocument(segment.items.content.join(''), metadata + [voice: segment.speaker, timecode: segment.startTime]))
        }

        documents
    }

    private Document toDocument(String text, Map<String, Object> metadata) {
        // protected API but there is no loader to load from memory
        DocumentLoaderUtils.load(new DocumentSource() {
            @Override
            InputStream inputStream() throws IOException {
                return new ByteArrayInputStream(text.getBytes('UTF-8'))
            }

            @Override
            Metadata metadata() {
                Metadata result = new Metadata()

                metadata.each { it ->
                    if (it.value != null) {
                        result.add(it.key, it.value)
                    }
                }

                result
            }
        }, DocumentLoaderUtils.parserFor(DocumentType.TXT))
    }
}
