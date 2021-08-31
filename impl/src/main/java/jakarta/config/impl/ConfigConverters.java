/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.config.impl;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParsePosition;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.config.Config;
import jakarta.config.spi.Converter;

/**
 * Utility methods for converting configuration to Java types.
 * <p>
 * Note that this class defines many methods of the form {@code <type> to<type>(String)}
 * which are automatically registered with each builder.
 */
final class ConfigConverters {
    private static final List<OrdinalConverter> BUILT_INS = new LinkedList<>();

    static {
        // built-in converters - required by specification
        addBuiltIn(Boolean.class, ConfigConverters::toBoolean);
        addBuiltIn(Byte.class, ConfigConverters::toByte);
        addBuiltIn(Short.class, ConfigConverters::toShort);
        addBuiltIn(Integer.class, ConfigConverters::toInt);
        addBuiltIn(OptionalInt.class, new OptionalConverter<>(Integer.class, OptionalInt::of, OptionalInt::empty));
        addBuiltIn(Long.class, ConfigConverters::toLong);
        addBuiltIn(OptionalLong.class, new OptionalConverter<>(Long.class, OptionalLong::of, OptionalLong::empty));
        addBuiltIn(Float.class, ConfigConverters::toFloat);
        addBuiltIn(Double.class, ConfigConverters::toDouble);
        addBuiltIn(OptionalDouble.class, new OptionalConverter<>(Double.class, OptionalDouble::of, OptionalDouble::empty));
        addBuiltIn(Character.class, ConfigConverters::toChar);
        addBuiltIn(Class.class, ConfigConverters::toClass);

        // built-in converters - Helidon
        //javax.math
        addBuiltIn(BigDecimal.class, ConfigConverters::toBigDecimal);
        addBuiltIn(BigInteger.class, ConfigConverters::toBigInteger);
        //java.time
        addBuiltIn(Duration.class, ConfigConverters::toDuration);
        addBuiltIn(Period.class, ConfigConverters::toPeriod);
        addBuiltIn(LocalDate.class, ConfigConverters::toLocalDate);
        addBuiltIn(LocalDateTime.class, ConfigConverters::toLocalDateTime);
        addBuiltIn(LocalTime.class, ConfigConverters::toLocalTime);
        addBuiltIn(ZonedDateTime.class, ConfigConverters::toZonedDateTime);
        addBuiltIn(ZoneId.class, ConfigConverters::toZoneId);
        addBuiltIn(ZoneOffset.class, ConfigConverters::toZoneOffset);
        addBuiltIn(Instant.class, ConfigConverters::toInstant);
        addBuiltIn(OffsetTime.class, ConfigConverters::toOffsetTime);
        addBuiltIn(OffsetDateTime.class, ConfigConverters::toOffsetDateTime);
        addBuiltIn(YearMonth.class, YearMonth::parse);
        //java.io
        addBuiltIn(File.class, ConfigConverters::toFile);
        //java.nio
        addBuiltIn(Path.class, ConfigConverters::toPath);
        addBuiltIn(Charset.class, ConfigConverters::toCharset);
        //java.net
        addBuiltIn(URI.class, ConfigConverters::toUri);
        addBuiltIn(URL.class, ConfigConverters::toUrl);
        //java.util
        addBuiltIn(Pattern.class, ConfigConverters::toPattern);
        addBuiltIn(UUID.class, ConfigConverters::toUUID);

        // obsolete stuff
        // noinspection UseOfObsoleteDateTimeApi
        addBuiltIn(Date.class, ConfigConverters::toDate);
        // noinspection UseOfObsoleteDateTimeApi
        addBuiltIn(Calendar.class, ConfigConverters::toCalendar);
        // noinspection UseOfObsoleteDateTimeApi
        addBuiltIn(GregorianCalendar.class, ConfigConverters::toGregorianCalendar);
        // noinspection UseOfObsoleteDateTimeApi
        addBuiltIn(TimeZone.class, ConfigConverters::toTimeZone);
        // noinspection UseOfObsoleteDateTimeApi
        addBuiltIn(SimpleTimeZone.class, ConfigConverters::toSimpleTimeZone);
    }

    private ConfigConverters() {
    }

    private static <T> void addBuiltIn(Class<T> clazz, Converter<T> converter) {
        BUILT_INS.add(new OrdinalConverter(converter, clazz, OrdinalConverter.BUILT_IN_PRIORITY));
    }

    /**
     * Maps {@code stringValue} to {@code byte}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code byte}
     */
    public static Byte toByte(String stringValue) {
        return Byte.parseByte(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code short}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code short}
     */
    public static Short toShort(String stringValue) {
        return Short.parseShort(stringValue);
    }

    //
    // Public mapping utility functions
    //

    /**
     * Maps {@code stringValue} to {@code int}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code int}
     */
    public static Integer toInt(String stringValue) {
        return Integer.parseInt(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code long}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code long}
     */
    public static Long toLong(String stringValue) {
        return Long.parseLong(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code float}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code float}
     */
    public static Float toFloat(String stringValue) {
        return Float.parseFloat(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code double}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code double}
     */
    public static Double toDouble(String stringValue) {
        return Double.parseDouble(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code boolean}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code boolean}
     */
    public static Boolean toBoolean(String stringValue) {
        final String lower = stringValue.toLowerCase();
        // according to microprofile config specification (section Built-in Converters)
        switch (lower) {
        case "true":
        case "1":
        case "yes":
        case "y":
        case "on":
            return true;
        default:
            return false;
        }
    }

    /**
     * Maps {@code stringValue} to {@code char}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code char}
     */
    public static Character toChar(String stringValue) {
        if (stringValue.length() != 1) {
            throw new IllegalArgumentException("Cannot convert to 'char'. The value must be just single character, "
                                                   + "but was '" + stringValue + "'.");
        }
        return stringValue.charAt(0);
    }

    /**
     * Maps {@code stringValue} to {@code Class<?>}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Class<?>}
     */
    public static Class<?> toClass(String stringValue) {
        try {
            return Class.forName(stringValue);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Maps {@code stringValue} to {@code UUID}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code UUID}
     */
    public static UUID toUUID(String stringValue) {
        return UUID.fromString(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code BigDecimal}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code BigDecimal}
     */
    public static BigDecimal toBigDecimal(String stringValue) {
        return new BigDecimal(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code BigInteger}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code BigInteger}
     */
    public static BigInteger toBigInteger(String stringValue) {
        return new BigInteger(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code File}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code File}
     */
    public static File toFile(String stringValue) {
        return new File(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Path}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Path}
     */
    public static Path toPath(String stringValue) {
        return Paths.get(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Charset}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Charset}
     */
    public static Charset toCharset(String stringValue) {
        return Charset.forName(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Pattern}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Pattern}
     */
    public static Pattern toPattern(String stringValue) {
        return Pattern.compile(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code URI}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code URI}
     */
    public static URI toUri(String stringValue) {
        return URI.create(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code URL}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code URL}
     */
    public static URL toUrl(String stringValue) {
        try {
            return new URL(stringValue);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Maps {@code stringValue} to {@code Date}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Date}
     * @see java.time.format.DateTimeFormatter#ISO_DATE_TIME
     * @deprecated Use one of the time API classes, such as {@link java.time.Instant} or {@link java.time.ZonedDateTime}
     */
    @SuppressWarnings({"UseOfObsoleteDateTimeApi", "DeprecatedIsStillUsed"})
    @Deprecated
    public static Date toDate(String stringValue) {
        try {
            return new Date(
                Instant.from(buildDateTimeFormatter(stringValue).parse(stringValue)).toEpochMilli());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static DateTimeFormatter buildDateTimeFormatter(String stringValue) {
        /*
        A Java 8 bug causes DateTimeFormatter.withZone to override an explicit
        time zone in the parsed string, contrary to the documented behavior. So
        if the string includes a zone do NOT use withZone in building the formatter.
         */
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor accessor = formatter.parseUnresolved(stringValue, pp);
        if (!accessor.isSupported(ChronoField.OFFSET_SECONDS)) {
            formatter = formatter.withZone(ZoneId.of("UTC"));
        }
        return formatter;
    }

    /**
     * Maps {@code stringValue} to {@code Calendar}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Calendar}
     * @see java.time.format.DateTimeFormatter#ISO_DATE_TIME
     * @deprecated use new time API, such as {@link java.time.ZonedDateTime}
     */
    @SuppressWarnings({"UseOfObsoleteDateTimeApi", "DeprecatedIsStillUsed"})
    @Deprecated
    public static Calendar toCalendar(String stringValue) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(toDate(stringValue));
        return calendar;
    }

    /**
     * Maps {@code stringValue} to {@code GregorianCalendar}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code GregorianCalendar}
     * @see java.time.format.DateTimeFormatter#ISO_DATE_TIME
     * @deprecated use new time API, such as {@link java.time.ZonedDateTime}
     */
    @SuppressWarnings({"UseOfObsoleteDateTimeApi", "DeprecatedIsStillUsed"})
    @Deprecated
    public static GregorianCalendar toGregorianCalendar(String stringValue) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(toDate(stringValue));
        return calendar;
    }

    /**
     * Maps {@code stringValue} to {@code LocalDate}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code LocalDate}
     * @see java.time.LocalDate#parse(CharSequence)
     */
    public static LocalDate toLocalDate(String stringValue) {
        return LocalDate.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code LocalTime}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code LocalTime}
     * @see java.time.LocalTime#parse(CharSequence)
     */
    public static LocalTime toLocalTime(String stringValue) {
        return LocalTime.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code LocalDateTime}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code LocalDateTime}
     * @see java.time.LocalDateTime#parse(CharSequence)
     */
    public static LocalDateTime toLocalDateTime(String stringValue) {
        return LocalDateTime.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code ZonedDateTime}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code ZonedDateTime}
     * @see java.time.ZonedDateTime#parse(CharSequence)
     */
    public static ZonedDateTime toZonedDateTime(String stringValue) {
        return ZonedDateTime.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code ZoneId}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code ZoneId}
     * @see java.time.ZoneId#of(String)
     */
    public static ZoneId toZoneId(String stringValue) {
        return ZoneId.of(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code ZoneOffset}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code ZoneOffset}
     * @see java.time.ZoneOffset#of(String)
     */
    public static ZoneOffset toZoneOffset(String stringValue) {
        return ZoneOffset.of(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code TimeZone}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code TimeZone}
     * @see java.time.ZoneId#of(String)
     * @deprecated use new time API, such as {@link java.time.ZoneId}
     */
    @SuppressWarnings({"UseOfObsoleteDateTimeApi", "DeprecatedIsStillUsed"})
    @Deprecated
    public static TimeZone toTimeZone(String stringValue) {
        ZoneId zoneId = toZoneId(stringValue);
        return TimeZone.getTimeZone(zoneId);
    }

    /**
     * Maps {@code stringValue} to {@code SimpleTimeZone}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code SimpleTimeZone}
     * @see java.time.ZoneId#of(String)
     * @deprecated use new time API, such as {@link java.time.ZoneId}
     */
    @SuppressWarnings({"UseOfObsoleteDateTimeApi", "DeprecatedIsStillUsed"})
    @Deprecated
    public static SimpleTimeZone toSimpleTimeZone(String stringValue) {
        return new SimpleTimeZone(toTimeZone(stringValue).getRawOffset(), stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Instant}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Instant}
     * @see java.time.Instant#parse(CharSequence)
     */
    public static Instant toInstant(String stringValue) {
        return Instant.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code OffsetDateTime}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code OffsetDateTime}
     * @see java.time.OffsetDateTime#parse(CharSequence)
     */
    public static OffsetDateTime toOffsetDateTime(String stringValue) {
        return OffsetDateTime.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code OffsetTime}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code OffsetTime}
     * @see java.time.OffsetTime#parse(CharSequence)
     */
    public static OffsetTime toOffsetTime(String stringValue) {
        return OffsetTime.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Duration}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Duration}
     * @see java.time.Duration#parse(CharSequence)
     */
    public static Duration toDuration(String stringValue) {
        return Duration.parse(stringValue);
    }

    /**
     * Maps {@code stringValue} to {@code Period}.
     *
     * @param stringValue source value as a {@code String}
     * @return mapped {@code stringValue} to {@code Period}
     * @see java.time.Period#parse(CharSequence)
     */
    public static Period toPeriod(String stringValue) {
        return Period.parse(stringValue);
    }

    /**
     * Transform all leaf nodes (values) into Map instance.
     * <p>
     * Fully qualified key of config node is used as a key in returned Map.
     * {@link Config#detach() Detach} config node before transforming to Map in case you want to cut
     * current Config node key prefix.
     * <p>
     * Let's say we work with following configuration:
     * <pre>
     * app:
     *      name: Example 1
     *      page-size: 20
     * logging:
     *      app.level = INFO
     *      level = WARNING
     * </pre>
     * Map {@code app1} contains two keys: {@code app.name}, {@code app.page-size}.
     * <pre>{@code
     * Map<String, String> app1 = ConfigMappers.toMap(config.get("app"));
     * }</pre>
     * {@link Config#detach() Detaching} {@code app} config node returns new Config instance with "reset" local root.
     * <pre>{@code
     * Map<String, String> app2 = ConfigMappers.toMap(config.get("app").detach());
     * }</pre>
     * Map {@code app2} contains two keys without {@code app} prefix: {@code name}, {@code page-size}.
     *
     * @param config config node used to transform into Properties
     * @return new Map instance that contains all config leaf node values
     * @see Config#detach()
     */
    public static Map<String, String> toMap(Config config) {
        switch (config.type()) {
        case VALUE:
            return Map.of(config.key(), config.asString().get());
        case MISSING:
            return Map.of();
        default:
            return config.asMap().orElseGet(Map::of);
        }
    }

    /**
     * Transform all leaf nodes (values) into Properties instance.
     * <p>
     * Fully qualified key of config node is used as a key in returned Properties.
     * {@link Config#detach() Detach} config node before transforming to Properties in case you want to cut
     * current Config node key prefix.
     * <p>
     * Let's say we work with following configuration:
     * <pre>
     * app:
     *      name: Example 1
     *      page-size: 20
     * logging:
     *      app.level = INFO
     *      level = WARNING
     * </pre>
     * Properties {@code app1} contains two keys: {@code app.name}, {@code app.page-size}.
     * <pre>
     * Properties app1 = ConfigMappers.toProperties(config.get("app"));
     * </pre>
     * {@link Config#detach() Detaching} {@code app} config node returns new Config instance with "reset" local root.
     * <pre>
     * Properties app2 = ConfigMappers.toProperties(config.get("app").detach());
     * </pre>
     * Properties {@code app2} contains two keys without {@code app} prefix: {@code name}, {@code page-size}.
     *
     * @param config config node used to transform into Properties
     * @return Properties instance that contains all config leaf node values.
     * @see Config#detach()
     */
    public static Properties toProperties(Config config) {
        Properties properties = new Properties();
        toMap(config).forEach(properties::setProperty);
        return properties;
    }

    public static void addBuiltInConverters(List<OrdinalConverter> ordinalConverters) {
        ordinalConverters.addAll(BUILT_INS);
    }

    private static class OptionalConverter<T, U> implements Converter<T> {
        private final Class<U> type;
        private final Function<U, T> withValue;
        private final Supplier<T> empty;

        private OptionalConverter(Class<U> type, Function<U, T> withValue, Supplier<T> empty) {
            this.type = type;
            this.withValue = withValue;
            this.empty = empty;
        }

        @Override
        public T convert(String value) throws IllegalArgumentException, NullPointerException {
            throw new IllegalArgumentException("Cannot convert from String, requires Config node");
        }

        @Override
        public boolean useConfigNode() {
            return true;
        }

        @Override
        public T convert(Config config) throws IllegalArgumentException, NullPointerException {
            return config.as(type)
                .map(withValue)
                .orElseGet(empty);
        }
    }
}
