package com.alchemedia.applock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Guards the translations: every language has every string, with the same placeholders and valid escaping. */
class StringResourcesTest {
    private val res = File("src/main/res")
    private val languages = listOf("es", "de", "fr", "it", "pt", "pl")

    private class Strings(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(dir: String): Strings {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val strings = linkedMapOf<String, String>()
        val plurals = linkedMapOf<String, Map<String, String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            when (node.tagName) {
                "string" -> if (node.getAttribute("translatable") != "false") strings[node.getAttribute("name")] = node.textContent
                "plurals" -> {
                    val items = node.getElementsByTagName("item")
                    plurals[node.getAttribute("name")] = (0 until items.length).associate {
                        val item = items.item(it) as Element
                        item.getAttribute("quantity") to item.textContent
                    }
                }
            }
        }
        return Strings(strings, plurals)
    }

    private val english = load("values")

    private fun placeholders(text: String) = Regex("%\\d\\$[sd]|%[sd]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyLanguageHasEveryString() {
        languages.forEach { lang ->
            val t = load("values-$lang")
            assertEquals("$lang is missing strings", emptySet<String>(), english.strings.keys - t.strings.keys)
            assertEquals("$lang has unknown strings", emptySet<String>(), t.strings.keys - english.strings.keys)
            assertEquals("$lang plurals", english.plurals.keys, t.plurals.keys)
        }
    }

    @Test
    fun placeholdersMatchEnglish() {
        languages.forEach { lang ->
            val t = load("values-$lang")
            english.strings.forEach { (name, en) ->
                assertEquals("$lang/$name placeholders", placeholders(en), placeholders(t.strings.getValue(name)))
            }
            english.plurals.forEach { (name, en) ->
                val translated = t.plurals.getValue(name)
                translated.forEach { (q, text) -> assertEquals("$lang/$name/$q", placeholders(en.getValue("other")), placeholders(text)) }
            }
        }
    }

    @Test
    fun noTranslationIsBlank() {
        languages.forEach { lang ->
            load("values-$lang").strings.forEach { (name, text) -> assertTrue("$lang/$name is blank", text.isNotBlank()) }
        }
    }

    @Test
    fun polishHasAllFourPluralForms() {
        val pl = load("values-pl")
        pl.plurals.forEach { (name, forms) ->
            assertEquals("pl/$name", setOf("one", "few", "many", "other"), forms.keys)
        }
    }

    @Test
    fun rawFilesEscapeApostrophesAndQuotes() {
        // aapt2 strips an unescaped double quote and rejects an unescaped apostrophe.
        listOf("values") .plus(languages.map { "values-$it" }).forEach { dir ->
            File(res, "$dir/strings.xml").readLines().forEachIndexed { index, line ->
                val body = line.substringAfter('>', "").substringBeforeLast("</")
                if (line.contains("<string ") || line.contains("<item ")) {
                    assertTrue("$dir:${index + 1} has an unescaped apostrophe", !Regex("(?<!\\\\)'").containsMatchIn(body))
                    assertTrue("$dir:${index + 1} has an unescaped quote", !Regex("(?<!\\\\)\"").containsMatchIn(body))
                }
            }
        }
    }

    @Test
    fun manifestLocaleConfigListsEveryTranslatedLanguage() {
        val config = File(res, "xml/locales_config.xml").readText()
        (languages + "en").forEach { assertTrue("locales_config missing $it", config.contains("android:name=\"$it\"")) }
    }
}
