import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 *
 */
Document doc = Jsoup.connect("http://docs.oracle.com/javase/7/docs/api/java/lang/String.html").get();

def extractMethodName(Element elm) {
//    println elm
    elm.select("td.colLast a").first().text()
}

def fragment = """
<tr class="altColor">
 <td class="colFirst"><code>int</code></td>
 <td class="colLast"><code><strong><a href="../../java/lang/String.html#count(java.lang.String)">count</a></strong>(<a href="../../java/lang/String.html" title="class in java.lang">String</a>&nbsp;text)</code>
  <div class="block">
    wibble.
  </div> </td>
</tr>
"""

Element methodAfter = doc.select('a').find {it.attr("name") == "method_summary"}.parent()
    .select("table.overviewSummary tr.altColor, table.overviewSummary tr.rowColor")
    .find { extractMethodName(it) > "count" }


methodAfter.before(fragment)

new File("out.html").withWriter { it << doc.outerHtml() }
println "done"

