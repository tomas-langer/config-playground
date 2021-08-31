package jakarta.config.impl;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import jakarta.config.impl.FileSourceHelper.DataAndDigest;
import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceContext;
import jakarta.config.spi.ParsableConfigSource;
import jakarta.config.spi.PollableConfigSource;

public class FileConfigSource implements ConfigSource,
                                         ParsableConfigSource,
                                         PollableConfigSource<byte[]> {

    private final Path sourcePath;
    private final String name;
    private ConfigSourceContext context;

    FileConfigSource(String name, Path sourcePath) {
        this.name = name;
        this.sourcePath = sourcePath;
    }

    @Override
    public void init(ConfigSourceContext context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<ConfigParser> parser() {
        // this is a general file configSource, we want the parser to be guessed based on media type
        return Optional.empty();
    }

    @Override
    public Optional<String> mediaType() {
        return MediaTypes.detectType(sourcePath);
    }

    @Override
    public boolean isModified(byte[] stamp) {
        return !notModified(sourcePath, stamp);
    }

    @Override
    public Optional<ConfigContent.ParsableContent> load() {
        // now we need to create all the necessary steps in one go, to make sure the digest matches the file
        Optional<DataAndDigest> dataAndDigest = FileSourceHelper.readDataAndDigest(sourcePath);
        if (dataAndDigest.isEmpty()) {
            return Optional.empty();
        }

        DataAndDigest dad = dataAndDigest.get();
        InputStream dataStream = new ByteArrayInputStream(dad.data());

        /*
         * Build the content
         */
        return Optional.of(ParsableContentImpl.builder()
            .stamp(dad.digest())
            .data(dataStream)
            .update(it -> MediaTypes.detectType(sourcePath)
                .ifPresent(it::mediaType))
            .build());
    }

    @Override
    public String toString() {
        return getName();
    }

    private boolean notModified(Path sourcePath, byte[] stamp) {
        return digest(sourcePath)
            .map(newDigest -> Arrays.equals(newDigest, stamp))
            // if new stamp is not present, it means the file was deleted, so we consider it unchanged
            .orElse(true);
    }

    private static Optional<byte[]> digest(Path path) {
        MessageDigest digest = digest();

        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), digest)) {
            byte[] buffer = new byte[2048];
            while (dis.read(buffer) != -1) {
                // just discard - we are only interested in the digest information
            }
            return Optional.of(digest.digest());
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to calculate digest for file: " + path, e);
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get MD5 digest algorithm.", e);
        }
    }
}
