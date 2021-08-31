package jakarta.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.config.spi.ConfigContent.ParsableContent;
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ParsableConfigSource;
import jakarta.config.spi.PollableConfigSource;

class UrlConfigSource implements ParsableConfigSource, PollableConfigSource<Instant> {
    private static final Logger LOGGER = Logger.getLogger(UrlConfigSource.class.getName());

    private static final String GET_METHOD = "GET";
    private static final String HEAD_METHOD = "HEAD";
    private static final int STATUS_NOT_FOUND = 404;

    private final String name;
    private final URL url;
    private final Optional<String> mediaType;

    UrlConfigSource(String name, URL url) {
        this.name = name;
        this.url = url;
        String path = url.getPath();
        Optional<String> mediaType = Optional.empty();
        if (path != null) {
            int i = path.lastIndexOf('.');
            if (i > -1) {
                mediaType = MediaTypes.detectSuffix(path.substring(i + 1));
            }
        }
        this.mediaType = mediaType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<ParsableContent> load() {
        try {
            URLConnection urlConnection = url.openConnection();

            if (urlConnection instanceof HttpURLConnection) {
                return httpContent((HttpURLConnection) urlConnection);
            } else {
                return genericContent(urlConnection);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config configSource for URL " + url);
        }
    }

    @Override
    public Optional<ConfigParser> parser() {
        return Optional.empty();
    }

    @Override
    public Optional<String> mediaType() {
        return mediaType;
    }

    @Override
    public boolean isModified(Instant stamp) {
        return isModified(url, stamp);
    }

    @Override
    public String toString() {
        return getName();
    }

    private Optional<ParsableContent> httpContent(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod(GET_METHOD);

        try {
            connection.connect();
        } catch (IOException e) {
            // considering this to be unavailable
            LOGGER.log(Level.FINEST, "Failed to connect to " + url + ", considering this configSource to be missing", e);
            return Optional.empty();
        }

        if (STATUS_NOT_FOUND == connection.getResponseCode()) {
            return Optional.empty();
        }

        Optional<String> mediaType = mediaType(connection.getContentType());
        final Instant timestamp;
        if (connection.getLastModified() == 0) {
            timestamp = Instant.now();
            LOGGER.fine("Missing GET '" + url + "' response header 'Last-Modified'. Used current time '"
                            + timestamp + "' as a content timestamp.");
        } else {
            timestamp = Instant.ofEpochMilli(connection.getLastModified());
        }

        InputStream inputStream = connection.getInputStream();
        Charset charset = getContentCharset(connection.getContentEncoding());

        return Optional.of(ParsableContentImpl.builder()
                               .data(inputStream)
                               .charset(charset)
                               .stamp(timestamp)
                               .update(it -> mediaType.ifPresent(it::mediaType))
                               .build());
    }

    private Charset getContentCharset(String contentEncoding) {
        if (contentEncoding == null) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(contentEncoding);
    }

    private Optional<String> mediaType(String responseMediaType) {
        return Optional.ofNullable(responseMediaType)
            .or(this::mediaType);
    }

    private Optional<ParsableContent> genericContent(URLConnection urlConnection) throws IOException {
        InputStream is = urlConnection.getInputStream();

        return Optional.of(ParsableContentImpl.builder()
                               .data(is)
                               .stamp(Instant.now())
                               .update(it -> mediaType.ifPresent(it::mediaType))
                               .build());
    }

    boolean isModified(URL url, Instant stamp) {
        return dataStamp(url)
            .map(newStamp -> newStamp.isAfter(stamp) || newStamp.equals(Instant.MIN))
            .orElse(true);
    }

    Optional<Instant> dataStamp(URL url) {
        // the URL may not be an HTTP URL
        try {
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection connection = (HttpURLConnection) urlConnection;
                try {
                    connection.setRequestMethod(HEAD_METHOD);
                    if (STATUS_NOT_FOUND == connection.getResponseCode()) {
                        return Optional.empty();
                    }
                    if (connection.getLastModified() != 0) {
                        return Optional.of(Instant.ofEpochMilli(connection.getLastModified()));
                    }
                } finally {
                    connection.disconnect();
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, ex, () -> "Configuration at url '" + url + "' HEAD is not accessible.");
            return Optional.empty();
        }

        Instant timestamp = Instant.MIN;
        LOGGER.finer("Missing HEAD '" + url + "' response header 'Last-Modified'. Used time '"
                         + timestamp + "' as a content timestamp.");
        return Optional.of(timestamp);
    }
}
