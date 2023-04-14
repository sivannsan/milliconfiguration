package com.sivannsan.milliconfiguration;

import com.sivannsan.foundation.annotation.Nonnegative;
import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.millidata.MilliData;

import java.io.File;

@SuppressWarnings("unused")
public interface MilliConfiguration {
    @Nonnull
    File getFile();

    /**
     * @param path  separated by dot (.)
     */
    @Nonnull
    MilliData get(@Nonnull String path);

    /**
     * @param path  separated by dot (.)
     */
    void set(@Nonnull String path, @Nonnull MilliData value);

    @Nonnull
    MilliData getContent();

    void setContent(@Nonnull MilliData value);

    /**
     * @param indent    the most frequently used values are 0 and 4
     */
    void save(@Nonnegative int indent);
}
