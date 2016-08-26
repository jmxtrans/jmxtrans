/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.cluster.events;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
<<<<<<< HEAD

import static com.google.common.base.Preconditions.checkNotNull;
=======
import javax.annotation.concurrent.Immutable;
import java.util.Collection;

import static com.google.common.collect.ImmutableList.copyOf;
>>>>>>> 988d07c6c608d660654ed18a6232f2e7d68a38e8

/**
 * ConfigurationChangeEvent. This class is passed to all the registered ClusterStateChangeListener. It contains
 * the main events relating to the cluster connection.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
@ToString(exclude = "configuration")
@Immutable
public class ConfigurationChangeEvent {
<<<<<<< HEAD
    @Getter
    private final ConfigurationChangeEvent.Type type;

    @Getter
    private final String[] configuration;

    public ConfigurationChangeEvent(@Nonnull ConfigurationChangeEvent.Type type, @Nonnull String[] configuration) {
        this.type = type;
        this.configuration = configuration;
    }
    
    public static enum Type {
        JVM_CONFIGURATION_ADDED,
        JVM_CONFIGURATION_REMOVED,
        JVM_CONFIGURATION_CHANGED;

        private Type() {
        }
    }
=======
	@Getter @Nonnull
	private final ConfigurationChangeEvent.Type type;

	@Getter @Nonnull
	private final Collection<String> configuration;

	public ConfigurationChangeEvent(@Nonnull ConfigurationChangeEvent.Type type, @Nonnull Collection<String> configuration) {
		this.type = type;
		this.configuration = copyOf(configuration);
	}

	public enum Type {
		JVM_CONFIGURATION_ADDED,
		JVM_CONFIGURATION_REMOVED,
		JVM_CONFIGURATION_CHANGED;
	}
>>>>>>> 988d07c6c608d660654ed18a6232f2e7d68a38e8

}
