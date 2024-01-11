package fr.m6.audio.factory.simplerag.domain

import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true)
class FileContent {
    Media media
    Source source
    Segment[] segments
}
