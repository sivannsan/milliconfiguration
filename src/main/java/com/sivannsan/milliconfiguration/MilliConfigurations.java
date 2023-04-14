package com.sivannsan.milliconfiguration;

import com.sivannsan.foundation.annotation.Nonnegative;
import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.common.Check;
import com.sivannsan.foundation.common.Validate;
import com.sivannsan.foundation.utility.FileUtility;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliDataParseException;
import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliNull;

import java.io.File;

@SuppressWarnings("unused")
public final class MilliConfigurations {
    private MilliConfigurations() {
    }

    /**
     * Load a MilliConfiguration from an existing file as provided
     * @param file  the existing file storing MilliData; if not, create one
     * @throws MilliDataParseException  if the loaded data can not be parsed; this can also occur in case the provided file does not store MilliData
     * @throws IllegalStateException    if the provided file does not exist
     * @see com.sivannsan.milliconfiguration.MilliConfigurations#create(File, boolean)
     */
    @Nonnull
    public static MilliConfiguration load(@Nonnull File file) throws MilliDataParseException, IllegalStateException {
        Validate.nonnull(file);
        if (file.isFile()) {
            StringBuilder builder = new StringBuilder();
            for (String line : FileUtility.readLines(file)) {
                builder.append(line.trim());
            }
            return new IMilliConfiguration(file, MilliData.Parser.parse(builder.toString()));
        }
        if (!file.exists()) {
            throw new IllegalStateException("Trying to load a MilliConfiguration yet the provided file does not exist");
        }
        if (file.isDirectory()) {
            throw new IllegalStateException("Trying to load a MilliConfiguration yet the provided file is a directory");
        }
        throw new IllegalStateException("Unexpected error while loading a MilliConfiguration!");
    }

    public static void create(@Nonnull File file, boolean force) {
        if (Validate.nonnull(file).exists() && !force) return;
        if (file.isFile()) return;
        if (file.isDirectory()) FileUtility.delete(file);
        FileUtility.createFile(file);
        FileUtility.writeLines(file, MilliNull.INSTANCE.toString());
    }

    private static final class IMilliConfiguration implements MilliConfiguration {
        @Nonnull
        private final File file;
        @Nonnull
        private MilliData content;

        private IMilliConfiguration(@Nonnull File file, @Nonnull MilliData content) {
            this.file = Validate.nonnull(file);
            this.content = Validate.nonnull(content);
        }

        @Override
        @Nonnull
        public File getFile() {
            return file;
        }

        @Override
        @Nonnull
        public MilliData get(@Nonnull String path) {
            if (Validate.nonnull(path).equals("")) return content;
            MilliData data = content;
            for (String key : path.split("\\.")) {
                if (data.isMilliMap()) {
                    data = data.asMilliMap().get(key);
                    continue;
                }
                if (data.isMilliList() && key.matches("[0-9]+")) {
                    data = data.asMilliList().get(Integer.parseInt(key));
                    continue;
                }
                return MilliNull.INSTANCE;
            }
            return data;
        }

        @Override
        public void set(@Nonnull String path, @Nonnull MilliData value) {
            Validate.nonnull(value);
            if (Validate.nonnull(path).equals("")) {
                content = value;
                return;
            }
            String[] keys = path.split("\\.");
            if (keys.length == 1) {
                if (content.isMilliList() && path.matches("[0-9]+")) {
                    content.asMilliList().update(Integer.parseInt(path), value);
                    return;
                }
                if (content.isMilliMap()) {
                    content.asMilliMap().put(path, value);
                    return;
                }
                content = new MilliMap(path, value);
                return;
            }
            MilliData grandparent;
            MilliData parent = content;
            if (parent.isMilliList() && keys[0].matches("[0-9]+")) {
                if (!Check.withinBounds(parent.asMilliList().asList(), Integer.parseInt(keys[0]))) return;
                grandparent = parent;
                parent = grandparent.asMilliList().get(Integer.parseInt(keys[0]));
            } else if (parent.isMilliMap()){
                grandparent = parent;
                parent = grandparent.asMilliMap().get(keys[0]);
            } else {
                content = new MilliMap(keys[0], MilliNull.INSTANCE);
                grandparent = content;
                parent = grandparent.asMilliMap().get(keys[0]);
            }
            for (int i = 1; i < keys.length - 1; i++) {
                String key = keys[i];
                String previousKey = keys[i - 1];
                if (parent.isMilliList() && key.matches("[0-9]+")) {
                    if (!Check.withinBounds(parent.asMilliList().asList(), Integer.parseInt(key))) return;
                    grandparent = parent;
                    parent = grandparent.asMilliList().get(Integer.parseInt(key));
                } else if (parent.isMilliMap()) {
                    grandparent = parent;
                    parent = grandparent.asMilliMap().get(key);
                } else {
                    if (grandparent.isMilliList()) {
                        grandparent.asMilliList().update(Integer.parseInt(previousKey), new MilliMap(key, MilliNull.INSTANCE));
                        grandparent = grandparent.asMilliList().get(Integer.parseInt(previousKey));
                        parent = grandparent.asMilliMap().get(key);
                    } else if (grandparent.isMilliMap()) {
                        grandparent.asMilliMap().put(previousKey, new MilliMap(key, MilliNull.INSTANCE));
                        grandparent = grandparent.asMilliMap().get(previousKey);
                        parent = grandparent.asMilliMap().get(key);
                    } else {
                        throw new RuntimeException("Reached an unexpected line of code while setting MilliData into a MilliConfiguration");
                    }
                }
            }
            String previousKey = keys[keys.length - 2];
            String key = keys[keys.length - 1];
            if (parent.isMilliList() && key.matches("[0-9]+")) {
                parent.asMilliList().update(Integer.parseInt(key), value);
            } else if (parent.isMilliMap()) {
                parent.asMilliMap().put(key, value);
            } else {
                if (grandparent.isMilliList()) {
                    grandparent.asMilliList().update(Integer.parseInt(previousKey), new MilliMap(key, value));
                } else if (grandparent.isMilliMap()) {
                    grandparent.asMilliMap().put(previousKey, new MilliMap(key, value));
                } else {
                    throw new RuntimeException("Reached an unexpected line of code while setting MilliData into a MilliConfiguration");
                }
            }
        }

        @Override
        @Nonnull
        public MilliData getContent() {
            return content;
        }

        @Override
        public void setContent(@Nonnull MilliData value) {
            content = Validate.nonnull(value);
        }

        @Override
        public void save(@Nonnegative int indent) {
            if (!file.exists()) throw new IllegalStateException("Unexpected file does not exist while saving a MilliData into that file");
            if (!file.isFile()) throw new IllegalStateException("Unexpected file is not a regular file while saving a MilliData into that file");
            FileUtility.writeLines(file, content.toString(Validate.nonnegative(indent)));
        }
    }
}
