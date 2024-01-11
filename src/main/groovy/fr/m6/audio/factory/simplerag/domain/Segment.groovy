package fr.m6.audio.factory.simplerag.domain

import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true)
class Segment {
    Integer startTime
    Integer endTime
    String speaker
    Item[] items
}
