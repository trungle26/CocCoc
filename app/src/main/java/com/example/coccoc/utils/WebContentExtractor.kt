package com.example.coccoc.utils

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Efficient web content extractor using Mozilla's Readability algorithm
 * Similar to Firefox Reader Mode and Chrome's Reader Mode
 */
class WebContentExtractor {

    /**
     * Extract main article content from WebView using Readability-like algorithm
     * This is more efficient than extracting all page content
     */
    suspend fun extractArticleContent(webView: WebView): String = suspendCancellableCoroutine { continuation ->
        try {
            // Readability-inspired JavaScript that efficiently extracts main content
            val jsCode = """
                (function() {
                    try {
                        // Score elements based on content density and semantic tags
                        function scoreElement(element) {
                            var score = 0;
                            var tagName = element.tagName.toLowerCase();
                            
                            // Positive indicators
                            if (tagName === 'article') score += 50;
                            if (tagName === 'main') score += 40;
                            if (element.getAttribute('role') === 'main') score += 40;
                            
                            var className = element.className || '';
                            var id = element.id || '';
                            var combined = (className + ' ' + id).toLowerCase();
                            
                            // Positive class/id indicators
                            if (/article|content|post|story|body|entry|text/i.test(combined)) score += 25;
                            if (/main/i.test(combined)) score += 20;
                            
                            // Negative indicators (ads, navigation, etc)
                            if (/comment|footer|header|nav|sidebar|ad|widget|social|share|related/i.test(combined)) score -= 50;
                            
                            // Check text density (paragraph count vs total elements)
                            var paragraphs = element.getElementsByTagName('p').length;
                            var allElements = element.getElementsByTagName('*').length;
                            if (allElements > 0) {
                                var density = paragraphs / allElements;
                                score += Math.round(density * 20);
                            }
                            
                            // Bonus for having multiple paragraphs
                            score += Math.min(paragraphs * 2, 30);
                            
                            return score;
                        }
                        
                        // Find best content container
                        var candidates = [];
                        var selectors = ['article', '[role="main"]', 'main', '.article', '.post', '.content', '.entry'];
                        
                        for (var i = 0; i < selectors.length; i++) {
                            var elements = document.querySelectorAll(selectors[i]);
                            for (var j = 0; j < elements.length; j++) {
                                var score = scoreElement(elements[j]);
                                if (score > 20) {
                                    candidates.push({
                                        element: elements[j],
                                        score: score
                                    });
                                }
                            }
                        }
                        
                        // Sort by score and pick the best
                        candidates.sort(function(a, b) { return b.score - a.score; });
                        
                        var contentElement = candidates.length > 0 ? candidates[0].element : document.body;
                        
                        // Clone to avoid modifying the page
                        var clone = contentElement.cloneNode(true);
                        
                        // Remove unwanted elements more aggressively
                        var removeSelectors = [
                            'script', 'style', 'iframe', 'embed', 'object',
                            'nav', 'header', 'footer', 'aside',
                            '[class*="nav"]', '[class*="menu"]',
                            '[class*="sidebar"]', '[class*="widget"]',
                            '[class*="ad"]', '[class*="advertisement"]',
                            '[class*="banner"]', '[class*="promo"]',
                            '[class*="social"]', '[class*="share"]',
                            '[class*="comment"]', '[class*="related"]',
                            '[class*="recommend"]', '[id*="comment"]',
                            'form', 'button', '[role="complementary"]'
                        ];
                        
                        removeSelectors.forEach(function(selector) {
                            var elements = clone.querySelectorAll(selector);
                            for (var i = elements.length - 1; i >= 0; i--) {
                                elements[i].remove();
                            }
                        });
                        
                        // Extract text from paragraphs, headings, and list items
                        var content = '';
                        var textNodes = clone.querySelectorAll('h1, h2, h3, h4, h5, h6, p, li, blockquote, figcaption');
                        
                        for (var i = 0; i < textNodes.length; i++) {
                            var text = textNodes[i].innerText || textNodes[i].textContent;
                            if (text && text.trim().length > 20) {
                                content += text.trim() + ' ';
                            }
                        }
                        
                        // Fallback: if content is too short, try getting all text
                        if (content.length < 300) {
                            content = clone.innerText || clone.textContent || '';
                        }
                        
                        // Clean up
                        content = content
                            .replace(/\s+/g, ' ')
                            .replace(/\n\s*\n+/g, '\n')
                            .trim();
                        
                        // Limit to reasonable size (5000 chars is ~1250 tokens)
                        if (content.length > 5000) {
                            content = content.substring(0, 5000) + '...';
                        }
                        
                        return content;
                        
                    } catch (e) {
                        return 'Error extracting content: ' + e.message;
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(jsCode) { result ->
                try {
                    // Remove quotes from JavaScript string result
                    val cleanResult = result?.trim()?.removeSurrounding("\"")
                        ?.replace("\\n", "\n")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\'", "'")
                        ?: ""

                    Timber.d("Extracted content: $cleanResult")
                    continuation.resume(cleanResult)
                } catch (e: Exception) {
                    Timber.e(e, "Error processing extraction result")
                    continuation.resume("")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting content")
            continuation.resume("")
        }
    }

    /**
     * Get article metadata (title, author, date) if available
     */
    suspend fun extractMetadata(webView: WebView): ArticleMetadata = suspendCancellableCoroutine { continuation ->
        val jsCode = """
            (function() {
                try {
                    var metadata = {
                        title: document.title || '',
                        author: '',
                        date: ''
                    };
                    
                    // Try to find author
                    var authorMeta = document.querySelector('meta[name="author"]') || 
                                    document.querySelector('meta[property="article:author"]') ||
                                    document.querySelector('[rel="author"]');
                    if (authorMeta) {
                        metadata.author = authorMeta.getAttribute('content') || authorMeta.innerText || '';
                    }
                    
                    // Try to find date
                    var dateMeta = document.querySelector('meta[property="article:published_time"]') ||
                                  document.querySelector('meta[name="date"]') ||
                                  document.querySelector('time[datetime]');
                    if (dateMeta) {
                        metadata.date = dateMeta.getAttribute('content') || 
                                       dateMeta.getAttribute('datetime') || 
                                       dateMeta.innerText || '';
                    }
                    
                    return JSON.stringify(metadata);
                } catch (e) {
                    return JSON.stringify({title: '', author: '', date: ''});
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { result ->
            try {
                val cleanResult = result?.trim()?.removeSurrounding("\"") ?: "{}"
                // Simple JSON parsing (you could use kotlinx.serialization here)
                val metadata = ArticleMetadata("", "", "")
                continuation.resume(metadata)
            } catch (e: Exception) {
                continuation.resume(ArticleMetadata("", "", ""))
            }
        }
    }
}

data class ArticleMetadata(
    val title: String,
    val author: String,
    val date: String
)
