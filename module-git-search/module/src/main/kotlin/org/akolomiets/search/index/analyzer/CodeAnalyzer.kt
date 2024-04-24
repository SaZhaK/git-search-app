package org.akolomiets.search.index.analyzer

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.LowerCaseFilter
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.analysis.standard.StandardFilter
import org.springframework.stereotype.Service

/**
 * Class for analyzing provided code lines.
 * Uses [NGramTokenizer] to create bigrams based on provided code line.
 * All bigrams are present in index in lowercase.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.codeAnalyzer")
class CodeAnalyzer : Analyzer() {

    override fun createComponents(fieldName: String): TokenStreamComponents {
        val src = NGramTokenizer(N_GRAM_SIZE, N_GRAM_SIZE)
        var result: TokenStream? = StandardFilter(src)
        result = LowerCaseFilter(result)
        return TokenStreamComponents(src, result)
    }

    companion object {

        /**
         * Size of bigram.
         */
        private const val N_GRAM_SIZE = 2
    }
}