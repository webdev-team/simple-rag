package fr.m6.audio.factory.simplerag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentLoaderUtils
import dev.langchain4j.data.document.DocumentSource
import dev.langchain4j.data.document.DocumentType
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.service.AiServices
import fr.m6.audio.factory.simplerag.domain.FileContent
import fr.m6.audio.factory.simplerag.domain.Media
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

import java.time.Duration

import static java.time.temporal.ChronoUnit.SECONDS

@SpringBootTest
class ChatTest {
    @Value('${OPENAI_API_KEY}')
    String openAiApiKey

    File databaseFile = new File('./db.json')

    @Test
    void buildDatabase() {
        long time1 = System.currentTimeMillis()

        List<Document> documents = []

        new File(ChatTest.getResource("/transcripts-json-whisper").toURI()).eachFile {file ->
            FileContent content = loadFile(file)

            documents.add(toPlainDocument(content))
        }

        EmbeddingStoreWrapper store = new EmbeddingStoreWrapper()

        store.withIngestor {
            it.ingest(documents)
        }

        store.save(databaseFile)

        long time2 = System.currentTimeMillis()

        println "Ingestion in " + (time2 - time1) + 'ms\n\n'
    }

    @Test
    void chat() {
        EmbeddingStoreWrapper store = new EmbeddingStoreWrapper()
        store.load(databaseFile)

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(SimpleRagApplication.MODEL)
                .temperature(0)
                .timeout(Duration.of(60, SECONDS))
                .build()

        LibrarianAgent agent = AiServices.builder(LibrarianAgent)
                .chatLanguageModel(chatLanguageModel)
                .retriever(store.retriever)
                .build()

        doChat(store, agent, 'Bonjour, quelles sont les actualités insolites du jour ?')
        doChat(store, agent, 'Bonjour, quelles émissions parlent de musique ?')
    }

    void doChat(EmbeddingStoreWrapper store, LibrarianAgent agent, String question) {
        println "QUESTION"
        println question
        println ''

        println "REPONSE"
        println agent.chat(question) + '\n'

        println "SOURCES"
        store.retriever.findRelevant(question).each {
            println it.metadata().get('program') + ' à ' + it.metadata().get('time')
            println it.text()
            println ''
        }
    }

    FileContent loadFile(File jsonFile) {
        mapper().readValue(jsonFile, FileContent)
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper
    }

    private Document toPlainDocument(FileContent content) {
        Media media = content.media

        Map<String, Object> metadata = [mediaId: media.id, program: media.program, time: media.time, title: media.title, speakers: media.speakers?.join(', ')]

        String text = content.segments?.collect {
            it.items.content.join(' ')
        }?.join(' ') ?: ''

//        if (FormatUtils.removeHtmlElementsFromRichText(media.text)) {
//            documents.add(toDocument(FormatUtils.removeHtmlElementsFromRichText(media.text), metadata + [voice: media.speakers?.join(', ') ?: '', timecode: 0]))
//        }

        toDocument(text, metadata)
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
            documents.add(toDocument(segment.items.content.join(' '), metadata + [voice: segment.speaker, timecode: segment.startTime]))
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
