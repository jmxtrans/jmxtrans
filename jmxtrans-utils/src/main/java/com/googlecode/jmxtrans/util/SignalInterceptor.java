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
package com.googlecode.jmxtrans.util;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public abstract class SignalInterceptor {

	protected SignalInterceptor() {
	}

	/**
	 * <p>
	 * Register for the given signal. Note that the signal name should
	 * <b>not</b> begin with <b>SIG</b>. For example, if you are interested in
	 * <b>SIGTERM</b>, you should call <code>register("TERM")</code>.
	 * </p>
	 * <p>
	 * If the registration fails for any reason, a
	 * <code>SignalInterceptorException</code> will be thrown. This is usually
	 * caused by one of the following conditions:
	 * </p>
	 * <ul>
	 * <li>The <code>sun.misc.Signal*</code> classes are not available (e.g. you
	 * are not using Sun's JVM).</li>
	 * <li><code>signame</code> is not a valid trappable signal name on this OS
	 * (e.g. <b>KILL</b> can't be trapped, <b>HUP</b> does not exist on Windows)
	 * </li>
	 * <li>The JVM refuses to let you trap <code>signame</code> because it is
	 * already being used for some other important purpose (e.g. <b>QUIT</b>
	 * and/or <b>BREAK</b> cause the JVM to print diagnostic output).</li>
	 * </ul>
	 */
	protected void register(String signame) throws SignalInterceptorException {
		try {
			new SignalInterceptorHelper(signame, this);
		} catch (Throwable e) {
			throw new SignalInterceptorException(signame, e);
		}
	}

	/**
	 * A wrapper around <code>register(String)</code> which never throws an
	 * exception. Instead, it returns <code>true</code> if registration
	 * succeeded, and <code>false</code> if it failed.
	 */
	protected boolean registerQuietly(String signame) {
		try {
			register(signame);
		} catch (Throwable e) {
			return false;
		}
		return true;
	}

	/**
	 * Handle the given signal (which you had previously registered for). If
	 * this method return false, or throws an exception, subsequent handlers in
	 * the chain will <b>not</b> be called.
	 */
	protected abstract boolean handle(String signame);

	/**
	 * <p>
	 * Private helper class for <code>SignalInterceptor</code>.
	 * </p>
	 * <p>
	 * This class exists separately from <code>SignalInterceptor</code> to
	 * permit graceful handling of LinkageErrors when the
	 * <code>sun.misc.Signal*</code> classes don't exist.
	 * </p>
	 */
	@IgnoreJRERequirement
	private static class SignalInterceptorHelper implements SignalHandler {

		private final SignalHandler oldHandler;

		private final SignalInterceptor interceptor;

		@IgnoreJRERequirement
		SignalInterceptorHelper(String signame, SignalInterceptor interceptor) {
			this.interceptor = interceptor;
			Signal signal = new Signal(signame);
			oldHandler = Signal.handle(signal, this);
		}

		@IgnoreJRERequirement
		public void handle(Signal sig) {
			if (interceptor.handle(sig.getName()) && (oldHandler != null)) {
				oldHandler.handle(sig);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class SignalInterceptorException extends Exception {

		SignalInterceptorException(String signal, Throwable cause) {
			super("Unable to register for SIG" + signal, cause);
		}

	}
}
