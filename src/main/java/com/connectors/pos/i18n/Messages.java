package com.connectors.pos.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Lightweight static accessor over the application's {@link MessageSource} so that
 * service-layer code can resolve user-facing (business rule / validation) error
 * messages in the request's current locale without having to inject a MessageSource
 * into every service. Backed by messages.properties / messages_ar.properties.
 * <p>
 * Falls back to reading the resource bundle directly when no Spring application
 * context is available (e.g. plain unit tests that construct services with
 * {@code new OrderService(...)} instead of booting Spring).
 */
@Component
public class Messages {

    private static final String BASENAME = "messages";

    private static volatile MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        Messages.messageSource = messageSource;
    }

    public static String get(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        MessageSource source = messageSource;
        if (source != null) {
            try {
                return source.getMessage(code, args, locale);
            } catch (NoSuchMessageException ignored) {
                // fall through to resource bundle lookup below
            }
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BASENAME, locale);
            String pattern = bundle.getString(code);
            return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
        } catch (MissingResourceException ignored) {
            return code;
        }
    }
}
