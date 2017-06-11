/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017. Diorite (by Bartłomiej Mazur (aka GotoFinal))
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.engio.mbassy.dispatch.groovy;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import groovy.lang.GroovyClassLoader;
import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.subscription.SubscriptionContext;

public final class GroovyFilter<M> implements IMessageFilter<M> {
    private static final String BASE_SCRIPT = "@groovy.transform.CompileStatic\n" +
                                              "static net.engio.mbassy.listener.IMessageFilter provide()\n" +
                                              "{\n" +
                                              "    net.engio.mbassy.listener.IMessageFilter result = { <type> msg, net.engio.mbassy.subscription" +
                                              ".SubscriptionContext context ->\n" +
                                              "        return <script>;\n" +
                                              "    }\n" +
                                              "    return result;\n" +
                                              "}\n" +
                                              "return provide();";

    private static final Pattern TYPE_REGEX   = Pattern.compile("<type>", Pattern.LITERAL);
    private static final Pattern SCRIPT_REGEX = Pattern.compile("<script>", Pattern.LITERAL);

    private final IMessageFilter<M> groovyFilter;

    private static boolean isGroovyAvailable = new ScriptEngineManager().getEngineByExtension("groovy") != null;

    public static boolean isGroovyAvailable()
    {
        return isGroovyAvailable;
    }

    private GroovyFilter(IMessageFilter<M> groovyFilter) {this.groovyFilter = groovyFilter;}

    @Override
    public boolean accepts(M message, SubscriptionContext context)
    {
        return this.groovyFilter.accepts(message, context);
    }

    private static Map<ClassLoader, EngineContext> engines = Collections.synchronizedMap(new HashMap<>(5));

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> GroovyFilter<T> create(String source, Class<?> type) {
        ClassLoader classLoader = type.getClassLoader();
        EngineContext engineContext = engines.computeIfAbsent(classLoader, EngineContext::new);
        try {
            String script = TYPE_REGEX.matcher(BASE_SCRIPT).replaceFirst(Matcher.quoteReplacement(type.getCanonicalName()));
            script = SCRIPT_REGEX.matcher(script).replaceFirst(Matcher.quoteReplacement(source));

            return new GroovyFilter((IMessageFilter) engineContext.engine.eval(script));
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error while compiling groovy expression: `" + source + "`", e);
        }
    }

    static class EngineContext {
        private final ClassLoader  loader;
        private final ScriptEngine engine;

        EngineContext(ClassLoader loader) {
            this.loader = loader;
            this.engine = new GroovyScriptEngineImpl(new GroovyClassLoader(loader));
        }
    }
}
