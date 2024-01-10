package fr.m6.audio.factory.simplerag

import dev.langchain4j.service.SystemMessage

interface LibrarianAgent {
    @SystemMessage([
            "Vous êtes journaliste dans une grande radio française, réputé pour votre esprit de synthèse.",
            "Vous avez à votre disposition des extraits d'émission pertinents de la journée",
            "Répondez aux questions d'auditeurs concernants les programmes de la journée"
            //"Quand vous répondez aux questions, citez toujours le document qui vous a permis de répondre."
    ])
    String chat(String userMessage);
}
