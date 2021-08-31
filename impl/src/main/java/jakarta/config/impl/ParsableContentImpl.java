package jakarta.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.config.spi.ConfigContent;

class ParsableContentImpl implements ConfigContent.ParsableContent {
    private static final Logger LOGGER = Logger.getLogger(ParsableContentImpl.class.getName());

    private final String mediaType;
    private final InputStream data;
    private final Charset charset;
    private final Object stamp;

    private ParsableContentImpl(Builder builder) {
        this.mediaType = builder.mediaType;
        this.data = builder.data;
        this.charset = builder.charset;
        this.stamp = builder.stamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        try {
            data.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to close input stream", e);
        }
    }

    @Override
    public Optional<Object> stamp() {
        return Optional.ofNullable(stamp);
    }

    @Override
    public Optional<String> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    @Override
    public InputStream data() {
        return data;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    static class Builder implements jakarta.config.impl.Builder<Builder, ParsableContent> {
        private Object stamp;
        private InputStream data;
        private String mediaType;
        private Charset charset = StandardCharsets.UTF_8;

        private Builder() {
        }

        @Override
        public ParsableContent build() {
            Objects.requireNonNull(data, "Parsable content exists, yet input stream was not configured.");

            return new ParsableContentImpl(this);
        }

        /**
         * Content stamp.
         *
         * @param stamp stamp of the content
         * @return updated builder instance
         */
        Builder stamp(Object stamp) {
            this.stamp = stamp;
            return this;
        }

        /**
         * Data of the config source as loaded from underlying storage.
         *
         * @param data to be parsed
         * @return updated builder instance
         */
        public Builder data(InputStream data) {
            Objects.requireNonNull(data, "Parsable input stream must be provided");
            this.data = data;
            return this;
        }

        /**
         * Media type of the content if known by the config source.
         * Media type is configured on content, as sometimes you need the actual file to exist to be able to
         * "guess" its media type, and this is the place we are sure it exists.
         *
         * @param mediaType media type of the content as understood by the config source
         * @return updated builder instance
         */
        public Builder mediaType(String mediaType) {
            Objects.requireNonNull(mediaType, "Media type must be provided, or this method should not be called");
            this.mediaType = mediaType;
            return this;
        }

        /**
         * Configure charset if known by the config source.
         *
         * @param charset charset to use if the content should be read using a reader
         * @return updated builder instance
         */
        public Builder charset(Charset charset) {
            Objects.requireNonNull(charset, "Charset must be provided, or this method should not be called");
            this.charset = charset;
            return this;
        }
    }
}
