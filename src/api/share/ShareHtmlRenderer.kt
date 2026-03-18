package info.vlassiev.serg.api.share

import io.ktor.http.ContentType
import io.ktor.http.withCharset

val shareContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)

fun renderShareHtml(title: String, imageUrl: String, description: String, redirectUrl: String): String {
    return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta property="og:title" content="${escapeHtml(title)}">
<meta property="og:image" content="${escapeHtml(imageUrl)}">
<meta property="og:description" content="${escapeHtml(description)}">
<meta property="og:type" content="website">
<meta property="og:url" content="https://serg.vlassiev.info${escapeHtml(redirectUrl)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${escapeHtml(title)}">
<meta name="twitter:image" content="${escapeHtml(imageUrl)}">
<title>${escapeHtml(title)}</title>
</head>
<body>
<script>window.location.replace('${escapeJs(redirectUrl)}')</script>
<noscript><a href="${escapeHtml(redirectUrl)}">Open gallery</a></noscript>
</body>
</html>"""
}

private fun escapeHtml(s: String) = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun escapeJs(s: String) = s
    .replace("\\", "\\\\")
    .replace("'", "\\'")
