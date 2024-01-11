package fr.m6.audio.factory.simplerag.domain

import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true)
class Media {
    Long id
    Long programId
    String program
    String date
    String time
    String title
    String summary
    String[] speakers
}
