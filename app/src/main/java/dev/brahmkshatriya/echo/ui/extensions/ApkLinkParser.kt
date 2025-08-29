package dev.brahmkshatriya.echo.ui.extensions

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import java.util.Stack
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

object ApkLinkParser {
    fun getSupportedLinks(apkFile: File): List<String> {
        val zip = ZipFile(apkFile)
        val entry = zip.getEntry("AndroidManifest.xml")
        val manifest = parse(zip.getInputStream(entry)) ?: return listOf()
        return manifest.getElementsByTagName("intent-filter").run {
            (0 until length).flatMap { index ->
                val intentFilter = item(index)
                val schemes = mutableListOf<String>()
                val hosts = mutableListOf<String>()
                val paths = mutableListOf<String>()

                intentFilter.childNodes.run {
                    for (i in 0 until length) {
                        val node = item(i)
                        fun data(name: String) = node.attributes.getNamedItem(name)?.nodeValue
                        if (node.nodeName == "data") {
                            data("android:scheme")?.let { schemes.add(it) }
                            data("android:host")?.let { hosts.add(it) }
                            data("android:path")?.let {
                                paths.add(it)
                            }
                        }
                    }

                    schemes.flatMap { scheme ->
                        hosts.flatMap { host ->
                            if (paths.isEmpty()) listOf("$scheme://$host")
                            else paths.map { path -> "$scheme://$host$path" }
                        }
                    }
                }
            }
        }
    }

    private fun parse(input: InputStream) = runCatching {
        val xmlDom = XmlDom()
        runCatching {
            CompressedXmlParser(xmlDom).parse(input)
        }.getOrElse {
            NonCompressedXmlParser(xmlDom).parse(input)
        }
        xmlDom.document
    }.getOrNull()


    class Attribute {
        var name: String? = null
        var prefix: String? = null
        var namespace: String? = null
        var value: String? = null
    }

    class XmlDom {
        val document: Document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        private val mStack = Stack<Node?>()
        private fun isEmpty(text: String?) = text == null || "" == text
        fun startDocument() {
            mStack.push(document)
        }

        fun startElement(
            uri: String?, localName: String?, qName: String?, attrs: Array<Attribute>
        ) {
            val elt = if (isEmpty(uri)) document.createElement(localName)
            else document.createElementNS(uri, qName)

            for (attr in attrs) {
                if (isEmpty(attr.namespace)) elt.setAttribute(attr.name, attr.value)
                else elt.setAttributeNS(attr.namespace, attr.prefix + ':' + attr.name, attr.value)
            }
            mStack.peek()!!.appendChild(elt)
            mStack.push(elt)
        }

        fun endElement() {
            mStack.pop()
        }

        fun text(data: String?) {
            mStack.peek()!!.appendChild(document.createTextNode(data))
        }

        fun characterData(data: String?) {
            mStack.peek()!!.appendChild(document.createCDATASection(data))
        }
    }

    //NOT TESTED
    class NonCompressedXmlParser(private val mListener: XmlDom) {
        fun parse(input: InputStream) {
            val factory = SAXParserFactory.newInstance()
            val saxParser = factory.newSAXParser()
            val handler = object : DefaultHandler() {
                override fun startDocument() = mListener.startDocument()
                override fun startElement(
                    uri: String?, localName: String?, qName: String, attributes: Attributes
                ) {
                    val attrs = Array(attributes.length) { i ->
                        Attribute().apply {
                            name = attributes.getQName(i)
                            namespace = uri
                            value = attributes.getValue(i)
                        }
                    }
                    mListener.startElement(uri, localName, qName, attrs)
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    val data = String(ch, start, length)
                    mListener.characterData(data)
                }

                override fun endElement(uri: String?, localName: String?, qName: String) =
                    mListener.endElement()

                override fun endDocument() {}
            }

            saxParser.parse(input, handler)
        }
    }

    class CompressedXmlParser(private val mListener: XmlDom) {

        fun parse(input: InputStream) {
            mData = ByteArray(input.available())
            input.read(mData)
            input.close()
            parseCompressedXml()
        }

        private fun parseCompressedXml() {
            var word0: Int

            while (mParserOffset < mData.size) {
                word0 = getLEWord(mParserOffset)
                when (word0) {
                    WORD_START_DOCUMENT -> parseStartDocument()
                    WORD_STRING_TABLE -> parseStringTable()
                    WORD_RES_TABLE -> parseResourceTable()
                    WORD_START_NS -> parseNamespace(true)
                    WORD_END_NS -> parseNamespace(false)
                    WORD_START_TAG -> parseStartTag()
                    WORD_END_TAG -> parseEndTag()
                    WORD_TEXT -> parseText()
                    else -> mParserOffset += WORD_SIZE
                }
            }
        }

        private fun parseStartDocument() {
            mListener.startDocument()
            mParserOffset += 2 * WORD_SIZE
        }

        private fun parseStringTable() {
            val chunk = getLEWord(mParserOffset + (1 * WORD_SIZE))
            mStringsCount = getLEWord(mParserOffset + (2 * WORD_SIZE))
            mStylesCount = getLEWord(mParserOffset + (3 * WORD_SIZE))
            val strOffset = mParserOffset + getLEWord(mParserOffset + (5 * WORD_SIZE))
            mStringsTable = arrayOfNulls(mStringsCount)
            var offset: Int
            for (i in 0 until mStringsCount) {
                offset = strOffset + getLEWord(mParserOffset + ((i + 7) * WORD_SIZE))
                mStringsTable[i] = getStringFromStringTable(offset)
            }
            mParserOffset += chunk
        }

        private fun parseResourceTable() {
            val chunk = getLEWord(mParserOffset + (1 * WORD_SIZE))
            mResCount = (chunk / 4) - 2
            mResourcesIds = IntArray(mResCount)
            for (i in 0 until mResCount) {
                mResourcesIds[i] = getLEWord(mParserOffset + ((i + 2) * WORD_SIZE))
            }
            mParserOffset += chunk
        }

        private fun parseNamespace(start: Boolean) {
            val prefixIdx = getLEWord(mParserOffset + (4 * WORD_SIZE))
            val uriIdx = getLEWord(mParserOffset + (5 * WORD_SIZE))
            val uri = getString(uriIdx)
            val prefix = getString(prefixIdx)
            if (start) mNamespaces[uri] = prefix
            else mNamespaces.remove(uri)
            mParserOffset += 6 * WORD_SIZE
        }

        private fun parseStartTag() {
            val uriIdx = getLEWord(mParserOffset + (4 * WORD_SIZE))
            val nameIdx = getLEWord(mParserOffset + (5 * WORD_SIZE))
            val attrCount = getLEShort(mParserOffset + (7 * WORD_SIZE))
            val name = getString(nameIdx)
            val (uri, qName) = if (uriIdx == -0x1) "" to name else {
                val uri = getString(uriIdx)
                uri to if (mNamespaces.containsKey(uri)) mNamespaces[uri] + ':' + name
                else name
            }
            mParserOffset += 9 * WORD_SIZE
            val attrs = Array(attrCount) {
                parseAttribute().also { mParserOffset += 5 * 4 }
            }
            mListener.startElement(uri, name, qName, attrs)
        }

        private fun parseAttribute(): Attribute {
            val attrNSIdx = getLEWord(mParserOffset)
            val attrNameIdx = getLEWord(mParserOffset + (1 * WORD_SIZE))
            val attrValueIdx = getLEWord(mParserOffset + (2 * WORD_SIZE))
            val attrType = getLEWord(mParserOffset + (3 * WORD_SIZE))
            val attrData = getLEWord(mParserOffset + (4 * WORD_SIZE))

            val attr = Attribute()
            attr.name = getString(attrNameIdx)

            if (attrNSIdx == -0x1) {
                attr.namespace = null
                attr.prefix = null
            } else {
                val uri = getString(attrNSIdx)
                if (mNamespaces.containsKey(uri)) {
                    attr.namespace = uri
                    attr.prefix = mNamespaces[uri]
                }
            }
            attr.value = if (attrValueIdx == -0x1) getAttributeValue(attrType, attrData)
            else getString(attrValueIdx)

            return attr
        }

        private fun parseText() {
            val strIndex = getLEWord(mParserOffset + (4 * WORD_SIZE))
            val data = getString(strIndex)
            mListener.characterData(data)
            mParserOffset += 7 * WORD_SIZE
        }

        private fun parseEndTag() {
            mListener.endElement()
            mParserOffset += 6 * WORD_SIZE
        }

        private fun getString(index: Int): String? {
            val res = if (index in 0..<mStringsCount) mStringsTable[index] else null
            return res
        }

        private fun getStringFromStringTable(offset: Int): String {
            val strLength: Int
            val chars: ByteArray
            if (mData[offset + 1] == mData[offset]) {
                strLength = mData[offset].toInt()
                chars = ByteArray(strLength)
                for (i in 0 until strLength) {
                    chars[i] = mData[offset + 2 + i]
                }
            } else {
                strLength =
                    ((mData[offset + 1].toInt() shl 8) and 0xFF00) or (mData[offset].toInt() and 0xFF)
                chars = ByteArray(strLength)
                for (i in 0 until strLength) {
                    chars[i] = mData[offset + 2 + (i * 2)]
                }
            }
            return String(chars)
        }

        private fun getLEWord(off: Int) = (((mData[off + 3].toInt() shl 24) and -0x1000000)
                or ((mData[off + 2].toInt() shl 16) and 0x00ff0000)
                or ((mData[off + 1].toInt() shl 8) and 0x0000ff00)
                or ((mData[off + 0].toInt() shl 0) and 0x000000ff))

        private fun getLEShort(off: Int) =
            ((mData[off + 1].toInt() shl 8) and 0xff00) or ((mData[off + 0].toInt() shl 0) and 0x00ff)

        private fun getAttributeValue(type: Int, data: Int) = when (type) {
            TYPE_STRING -> getString(data)
            TYPE_ID_REF -> String.format("@id/0x%08X", data)
            TYPE_ATTR_REF -> String.format("?id/0x%08X", data)
            else -> String.format("%08X/0x%08X", type, data)
        }

        private val mNamespaces: MutableMap<String?, String?> = HashMap()
        private lateinit var mData: ByteArray

        private lateinit var mStringsTable: Array<String?>
        private lateinit var mResourcesIds: IntArray
        private var mStringsCount = 0
        private var mStylesCount = 0
        private var mResCount = 0
        private var mParserOffset = 0

        companion object {
            const val WORD_START_DOCUMENT: Int = 0x00080003

            const val WORD_STRING_TABLE: Int = 0x001C0001
            const val WORD_RES_TABLE: Int = 0x00080180

            const val WORD_START_NS: Int = 0x00100100
            const val WORD_END_NS: Int = 0x00100101
            const val WORD_START_TAG: Int = 0x00100102
            const val WORD_END_TAG: Int = 0x00100103
            const val WORD_TEXT: Int = 0x00100104
            const val WORD_SIZE: Int = 4

            private const val TYPE_ID_REF = 0x01000008
            private const val TYPE_ATTR_REF = 0x02000008
            private const val TYPE_STRING = 0x03000008
        }
    }
}