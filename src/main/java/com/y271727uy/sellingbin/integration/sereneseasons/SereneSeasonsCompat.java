package com.y271727uy.sellingbin.integration.sereneseasons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class SereneSeasonsCompat {
    private static final String[] HELPER_CLASS_NAMES = {
            "sereneseasons.api.season.SeasonHelper",
            "sereneseasons.api.season.SeasonsHelper"
    };

    private static final String[] STATE_METHOD_NAMES = {
            "getSeasonState",
            "getSeasonStateForLevel",
            "getSeasonStateAt",
            "getSeasonStateAtPos",
            "getSeasonStateAtLevel",
            "getSeasonStateFromLevel"
    };

    private static final String[] SEASON_METHOD_NAMES = {
            "getSeason",
            "getSubSeason",
            "season",
            "subSeason",
            "getName",
            "name"
    };

    private SereneSeasonsCompat() {
    }

    public static Optional<String> getCurrentSeasonId(Level level) {
        return getCurrentSeasonId(level, null);
    }

    public static Optional<String> getCurrentSeasonId(Level level, BlockPos pos) {
        if (level == null) {
            return Optional.empty();
        }

        for (String helperClassName : HELPER_CLASS_NAMES) {
            Class<?> helperClass = findClass(helperClassName);
            if (helperClass == null) {
                continue;
            }

            Object seasonState = invokeSeasonState(helperClass, level, pos);
            if (seasonState == null) {
                continue;
            }

            Optional<String> currentSeason = extractSeasonId(seasonState);
            if (currentSeason.isPresent()) {
                return currentSeason;
            }
        }

        return Optional.empty();
    }

    private static Optional<String> extractSeasonId(Object seasonState) {
        for (String methodName : SEASON_METHOD_NAMES) {
            Object season = invokeNoArg(seasonState, methodName);
            if (season == null) {
                continue;
            }

            Optional<String> normalized = normalizeSeasonId(season.toString());
            if (normalized.isPresent()) {
                return normalized;
            }
        }

        return normalizeSeasonId(seasonState.toString());
    }

    private static Object invokeSeasonState(Class<?> type, Level level, BlockPos pos) {
        for (String methodName : STATE_METHOD_NAMES) {
            for (Method method : type.getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }

                if (method.getParameterCount() == 1) {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    if (!parameterType.isInstance(level) && !parameterType.isAssignableFrom(level.getClass())) {
                        continue;
                    }

                    Object result = invokeStatic(method, level);
                    if (result != null) {
                        return result;
                    }
                    continue;
                }

                if (method.getParameterCount() == 2 && pos != null) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    boolean firstMatches = parameterTypes[0].isInstance(level) || parameterTypes[0].isAssignableFrom(level.getClass());
                    boolean secondMatches = parameterTypes[1].isInstance(pos) || parameterTypes[1].isAssignableFrom(pos.getClass());
                    if (!firstMatches || !secondMatches) {
                        continue;
                    }

                    Object result = invokeStatic(method, level, pos);
                    if (result != null) {
                        return result;
                    }
                    continue;
                }

                if (method.getParameterCount() == 0) {
                    Object result = invokeStatic(method);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    private static Object invokeStatic(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }

            try {
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Optional<String> normalizeSeasonId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("spring")) {
            return Optional.of("spring");
        }
        if (normalized.contains("summer")) {
            return Optional.of("summer");
        }
        if (normalized.contains("autumn") || normalized.contains("fall")) {
            return Optional.of("autumn");
        }
        if (normalized.contains("winter")) {
            return Optional.of("winter");
        }
        if (normalized.contains("unknown") || normalized.contains("null")) {
            return Optional.empty();
        }

        return Optional.of(normalized);
    }
}

