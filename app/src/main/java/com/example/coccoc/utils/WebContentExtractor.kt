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
            // Improved JavaScript that focuses on actual article content, not metadata
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
                            
                            // Positive class/id indicators for Vietnamese newspapers
                            if (/article|content|post|story|body|entry|text|baiviet|noidung/i.test(combined)) score += 25;
                            if (/main|detail|full/i.test(combined)) score += 20;
                            
                            // Strong negative indicators (metadata, media controls, etc)
                            if (/comment|footer|header|nav|sidebar|menu|toolbar/i.test(combined)) score -= 100;
                            if (/ad|advertisement|banner|promo/i.test(combined)) score -= 100;
                            if (/widget|social|share|related|recommend/i.test(combined)) score -= 80;
                            if (/audio|video|media|player|source|track/i.test(combined)) score -= 60;
                            if (/metadata|info|author|date|tag|category/i.test(combined)) score -= 40;
                            
                            // Check text content quality
                            var text = element.innerText || element.textContent || '';
                            var textLength = text.trim().length;
                            
                            // Penalize if text is too short (likely not article)
                            if (textLength < 500) score -= 30;
                            else if (textLength > 1000) score += 40; // Bonus for substantial content
                            
                            // Check paragraph density
                            var paragraphs = element.getElementsByTagName('p').length;
                            var allElements = element.getElementsByTagName('*').length;
                            
                            if (allElements > 0) {
                                var density = paragraphs / allElements;
                                score += Math.round(density * 30);
                            }
                            
                            // Strong bonus for having many paragraphs (actual articles have many)
                            if (paragraphs > 5) score += 50;
                            else if (paragraphs < 3) score -= 20;
                            
                            return score;
                        }
                        
                        // Find best content container
                        var candidates = [];
                        
                        // Try semantic selectors first
                        var selectors = [
                            'article', 
                            '[role="main"]', 
                            'main', 
                            '.article-content',
                            '.article-body',
                            '.post-content',
                            '.entry-content',
                            '.story-content',
                            '.detail-content',
                            '[class*="content"]',
                            '[class*="article"]',
                            '[id*="content"]',
                            '[id*="article"]'
                        ];
                        
                        for (var i = 0; i < selectors.length; i++) {
                            var elements = document.querySelectorAll(selectors[i]);
                            for (var j = 0; j < elements.length; j++) {
                                var score = scoreElement(elements[j]);
                                if (score > 30) { // Raised threshold
                                    candidates.push({
                                        element: elements[j],
                                        score: score
                                    });
                                }
                            }
                        }
                        
                        // Sort by score and pick the best
                        candidates.sort(function(a, b) { return b.score - a.score; });
                        
                        var contentElement = null;
                        
                        // Pick the best candidate if score is good enough
                        if (candidates.length > 0 && candidates[0].score > 50) {
                            contentElement = candidates[0].element;
                        }
                        
                        // Fallback: Find container with most paragraphs
                        if (!contentElement) {
                            var maxParagraphs = 0;
                            var allDivs = document.querySelectorAll('div, section, article');
                            
                            for (var i = 0; i < allDivs.length; i++) {
                                var div = allDivs[i];
                                var paragraphs = div.querySelectorAll('p');
                                var className = (div.className || '').toLowerCase();
                                
                                // Skip if it's obviously not content
                                if (/comment|footer|header|nav|sidebar|ad|widget/i.test(className)) {
                                    continue;
                                }
                                
                                if (paragraphs.length > maxParagraphs && paragraphs.length > 3) {
                                    maxParagraphs = paragraphs.length;
                                    contentElement = div;
                                }
                            }
                        }
                        
                        // Last resort: use body
                        if (!contentElement) {
                            contentElement = document.body;
                        }
                        
                        // Clone to avoid modifying the page
                        var clone = contentElement.cloneNode(true);
                        
                        // Remove unwanted elements very aggressively
                        var removeSelectors = [
                            'script', 'style', 'iframe', 'embed', 'object', 'noscript',
                            'nav', 'header', 'footer', 'aside',
                            // Remove media elements and controls
                            'audio', 'video', 'source', 'track',
                            '[class*="audio"]', '[class*="video"]', '[class*="media"]',
                            '[class*="player"]', '[id*="player"]',
                            // Remove navigation and menus
                            '[class*="nav"]', '[class*="menu"]', '[class*="toolbar"]',
                            '[id*="nav"]', '[id*="menu"]',
                            // Remove ads and promotions
                            '[class*="sidebar"]', '[class*="widget"]',
                            '[class*="ad"]', '[class*="advertisement"]',
                            '[class*="banner"]', '[class*="promo"]',
                            // Remove social and sharing
                            '[class*="social"]', '[class*="share"]',
                            '[class*="comment"]', '[class*="related"]',
                            '[class*="recommend"]', '[id*="comment"]',
                            // Remove metadata sections
                            '[class*="author"]', '[class*="meta"]', '[class*="tags"]',
                            'form', 'button', 'input', 'select', 'textarea',
                            '[role="complementary"]', '[role="navigation"]'
                        ];
                        
                        removeSelectors.forEach(function(selector) {
                            try {
                                var elements = clone.querySelectorAll(selector);
                                for (var i = elements.length - 1; i >= 0; i--) {
                                    elements[i].remove();
                                }
                            } catch(e) {}
                        });
                        
                        // Extract text from paragraphs, headings, and list items only
                        var content = '';
                        var textNodes = clone.querySelectorAll('h1, h2, h3, h4, h5, h6, p, li, blockquote, figcaption');
                        
                        for (var i = 0; i < textNodes.length; i++) {
                            var text = textNodes[i].innerText || textNodes[i].textContent;
                            if (text && text.trim().length > 20) {
                                // Skip if it looks like metadata or controls
                                var lowerText = text.toLowerCase();
                                if (/^(play|pause|mute|volume|audio|video|source)/i.test(lowerText)) {
                                    continue;
                                }
                                content += text.trim() + '\n';
                            }
                        }
                        
                        // Fallback: if content is too short, try getting all paragraph text directly
                        if (content.length < 500) {
                            content = '';
                            var allParagraphs = document.querySelectorAll('p');
                            for (var i = 0; i < allParagraphs.length; i++) {
                                var p = allParagraphs[i];
                                var pText = p.innerText || p.textContent;
                                if (pText && pText.trim().length > 30) {
                                    content += pText.trim() + '\n';
                                }
                            }
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
}
