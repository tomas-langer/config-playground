package jakarta.config.full.example;

import java.nio.file.Path;
import java.util.function.Consumer;

import jakarta.config.full.spi.ChangeWatcher;

public class FileChangeWatcher implements ChangeWatcher<Path> {
    @Override
    public void start(Path target,
                      Consumer<ChangeEvent<Path>> listener) {

    }

    @Override
    public Class<Path> type() {
        return Path.class;
    }
}
