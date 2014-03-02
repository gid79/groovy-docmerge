package com.logicalpractice.groovy.docmerge

import groovy.json.JsonSlurper

import java.nio.file.FileSystems
import java.nio.file.Files

/**
 *
 */
def cli = new CliBuilder(usage: "blah")

cli.with {
    _ longOpt:'input', args: 1, argName:"file", 'path to input json'
    _ longOpt:'jdk7-docs', args: 1, argName:"path", 'path to jdk7 javadocs'
    _ longOpt:'output-dir', args: 1, argName:"path", 'output directory'

}
def options = cli.parse(args)

def inputPath = new File(options.input).toPath()
def jdk7Docs = new File(options.'jdk7-docs').toPath()
def outputDir = new File(options.'output-dir').toPath()

Files.createDirectories(outputDir)

assert Files.exists(jdk7Docs)
assert Files.exists(inputPath)

def groovyJdk7Info = new JsonSlurper().parse(inputPath.toFile())


new Jdk7DocMerge(outputDir, jdk7Docs, groovyJdk7Info).merge()