package com.logicalpractice.groovy.docmerge

import groovy.text.SimpleTemplateEngine
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.Files.copy
import static java.nio.file.Files.createDirectories
import static java.nio.file.Files.notExists

/**
 *
 */
class Jdk7DocMerge {

    Path output
    Path jdkDocs
    def groovyJdkInfo

    def enhancedPaths

    Jdk7DocMerge(Path output, Path jdkDocs, groovyJdkInfo) {
        this.output = output
        this.jdkDocs = jdkDocs
        this.groovyJdkInfo = groovyJdkInfo

        enhancedPaths = groovyJdkInfo.classes.collectEntries { k, v -> [k.replaceAll(/\./, '/') + ".html", k] }
    }


    def merge() {
        Files.walkFileTree(jdkDocs, new SimpleFileVisitor<Path>(){
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                def stripped = file.subpath(1,file.nameCount)
                def target = output.resolve(stripped)
                if( notExists(target.parent) ) {
                    createDirectories(target.parent)
                }
                def className = enhancedPaths[stripped.toString()]
                if( className ) {
                    println "merging $stripped -> $target"
                    def classInfo = groovyJdkInfo.classes[className]
                    assert classInfo != null
                    Document doc = Jsoup.parse(file.toFile().text)
                    mergeMethods(doc, classInfo)

                    target.toFile().text = doc.outerHtml()

                } else {
                    copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                }

                FileVisitResult.CONTINUE
            }
        })
    }

    private def extractMethodName(Element elm) {
        elm.select("td.colLast a").first().text()
    }

    private def mergeMethods(Document doc, info) {
        doc.getElementsByTag('head').first().append('''
            <style type='text/css'>
            .groovy_tag {
                color: white;
                background-color: green;
                font-size: smaller;
                padding: 2px;
                border-radius: 2px;
            }
            </style>
        ''')
        info.methods.each { method ->
            def name = method.name
            def methodRows = doc.select('a').find {it.attr("name") == "method_summary"}.parent()
                .select("table.overviewSummary tr.altColor, table.overviewSummary tr.rowColor")

            def referenceRow = methodRows.find { extractMethodName(it) > name } ?: methodRows.last()
            referenceRow.before(
                """
                <tr class="altColor">
                 <td class="colFirst"><code>${method.isStatic ? 'static ' : ''}${method.returnTypeDocUrl}</code></td>
                 <td class="colLast"><code><B><A HREF="#${method.name}(${method.parametersSignature})">${method.name}</A></B>(${method.parametersDocUrl})</code>
                   <span class='groovy_tag' >groovy</span>
                      <div class="block">
                        ${method.shortComment}
                      </div>
                 </td>
                </tr>
                """
            )
        }
        // must fix the alternating stripping of table rows
        doc.select('a').find {it.attr("name") == "method_summary"}.parent()
            .select("table.overviewSummary tr")
            .eachWithIndex { Element entry, int i ->
            if( entry.className() in ['altColor','rowColor'] ) {
                entry.classNames([i % 2 == 0 ? 'altColor' : 'rowColor'] as Set)
            }
        }
        // now the method detail blocks
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate('''
            <A name="${method.name}(${method.parametersSignature})"><!-- --></A>
            <UL class="blockList">
                <li class="blockList">
                    <h4>${method.name}</h4>
                    <PRE>public ${method.isStatic ? 'static ' : ''}${method.returnTypeDocUrl} <B>${method.name}</B>(${
                            method.parametersDocUrl
                    })</PRE>
                    <div class="block">
                        ${method.comment}
                    </div>
                    <% if (method.parametersSignature) { %>
                    <dl><DT><B>Parameters:</B></DT>
                    \t<% method.parameters.each { param -> %>
                    \t\t<DD><CODE>${param.name}</CODE> - ${param.comment}.</DD>
                    <% } %>
                        </dl>
                        <%
                        } %>

                    <% if (method.returnComment) { %>
                    <dl><DT><B>Returns:</B></DT><DD>${method.returnComment}</DD></dl>
                    <% } %>

                    <%if (method.since) { %>
                    <dl>
                    <DT><B>Since:</B></DT>
                    <DD>${method.since}</DD>
                    </dl>
                    <%}%>

                    <% if (method.seeComments) { %>
                    <dl>
                    <DT><B>See:</B></DT>
                    <% method.seeComments.each { param -> %>
                        <DD>${param.target}.</DD>
                    <% }%>
                    </dl>
                    <%    } %>
                </li>
            </UL>

        ''')
        info.methods.each { method ->
            def methodAnchor = "${method.name}(${method.parametersSignature})"
            def detailContainer = doc.select('a').find {it.attr("name") == "method_detail"}.parent()

            def referenceAnchor =  detailContainer.getElementsByTag('a')
                .findAll { it.attr('name') }
                .find { it.attr('name') > methodAnchor }

            def detailFragment = template.make(method:method) as String

            if( referenceAnchor ){
                referenceAnchor.before(detailFragment)
            } else {
                detailContainer.append(detailFragment)
            }

        }
    }
}
