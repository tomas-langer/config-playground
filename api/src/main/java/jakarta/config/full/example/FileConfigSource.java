package jakarta.config.full.example;

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

import jakarta.config.full.spi.ConfigContent;
import jakarta.config.full.spi.ConfigParser;
import jakarta.config.full.spi.ConfigSource;
import jakarta.config.full.spi.ParsableConfigSource;
import jakarta.config.full.spi.PollableConfigSource;
import jakarta.config.full.spi.WatchableConfigSource;

public class FileConfigSource implements ConfigSource,
                                         ParsableConfigSource,
                                         WatchableConfigSource<Path>,
                                         PollableConfigSource<byte[]> {

    private final Path sourcePath;

    public FileConfigSource(Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    public String getName() {
        return "File[" + sourcePath.toAbsolutePath() + "]";
    }

    @Override
    public Optional<ConfigParser> parser() {
        // this is a general file source, we want the parser to be guessed based on media type
        return Optional.empty();
    }

    @Override
    public Optional<String> mediaType() {
        // in need of a utility
        String suffix = getFileSuffix(sourcePath);
        switch (suffix) {
        case "properties":
            return Optional.of("text/x-java-properties");
        case "yaml":
            return Optional.of("application/x-yaml");
        case "json":
            return Optional.of("application/json");
        default:
            return Optional.empty();
        }
    }

    @Override
    public boolean isModified(byte[] stamp) {
        return !notModified(sourcePath, stamp);
    }


    @Override
    public Class<Path> targetType() {
        return Path.class;
    }

    @Override
    public Path target() {
        return sourcePath;
    }

    @Override
    public Optional<ConfigContent.ParsableContent> load() {
        // now we need to create all the necessary steps in one go, to make sure the digest matches the file
        Optional<DataAndDigest> dataAndDigest = FileSourceHelper.readDataAndDigest(filePath);
        if (dataAndDigest.isEmpty()) {
            return Optional.empty();
        }

        DataAndDigest dad = dataAndDigest.get();
        InputStream dataStream = new ByteArrayInputStream(dad.data());

        /*
         * Build the content
         */
        var builder = ConfigParser.Content.builder()
            .stamp(dad.digest())
            .data(dataStream);

        MediaTypes.detectType(filePath).ifPresent(builder::mediaType);

        return Optional.of(builder.build());
    }

    private String getFileSuffix(Path sourcePath) {
        Path fileName = sourcePath.getFileName();
        if (fileName == null) {
            return "";
        }
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        if (dot > -1) {
            return name.substring(dot + 1);
        }
        return "";
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
