package com.googlecode.jmxtrans.model.naming;

import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Result;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Naming strategy which uses an JEXL expression to format the name from the result, its class name, attribute name,
 * and more.  Note the resulting name can be used for any of a number of purposes.  For example, with OpenTSDB it could
 * be the metric name or a tag name.
 * <p/>
 * In the expression, all of the following may be used:
 * <b>Variables</b>
 * <dl>
 * <dt>alias</dt>
 * <dd>the classname alias of the MBean.</dd>
 * <dt>attribute</dt>
 * <dd>the name of the attribute of the MBean queried.</dd>
 * <dt>class</dt>
 * <dd>the effective classname of the MBean (uses #alias is not null, otherwise uses #realclass).</dd>
 * <dt>realclass</dt>
 * <dd>the class name of the MBean.</dd>
 * <dt>result</dt>
 * <dd>the full Result object.</dd>
 * </dl>
 */
public class JexlNamingStrategy implements NamingStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(JexlNamingStrategy.class);

	public static final String DEFAULT_EXPRESSION = "class + \".\" + attribute";
	public static final String VAR__CLASSNAME = "realclass";
	public static final String VAR__ATTRIBUTE_NAME = "attribute";
	public static final String VAR__CLASSNAME_ALIAS = "alias";
	public static final String VAR__EFFECTIVE_CLASSNAME = "class";
	public static final String VAR__TYPENAME = "typename";
	public static final String VAR__RESULT = "result";

	protected JexlEngine jexl;
	protected Expression parsedExpr;


	/**
	 * Create a new naming strategy using an JEXL expression and the default expression.
	 */
	public JexlNamingStrategy() throws JexlException {
		jexl = new JexlEngine();
		this.parsedExpr = jexl.createExpression(DEFAULT_EXPRESSION);
	}

	/**
	 * Create a new naming strategy using an JEXL expression and the given expression.
	 *
	 * @param expr - the JEXL expression to use to create names.
	 */
	public JexlNamingStrategy(String expr) throws JexlException {
		jexl = new JexlEngine();
		this.parsedExpr = jexl.createExpression(expr);
	}

	/**
	 * Format the name for the given result.
	 *
	 * @param result - the result of a JMX query.
	 * @return String - the formatted string resulting from the expression, or null if the formatting fails.
	 */
	@Override
	public String formatName(Result result) {
		String formatted;
		JexlContext context = new MapContext();

		this.populateContext(context, result);
		try {
			formatted = (String) this.parsedExpr.evaluate(context);
		} catch (JexlException jexlExc) {
			LOG.error("error applying JEXL expression to query results", jexlExc);
			formatted = null;
		}

		return formatted;
	}

	public void setExpression(String expr) throws JexlException {
		this.parsedExpr = this.jexl.createExpression(expr);
	}

	/**
	 * Populate the context with values from the result.
	 *
	 * @param context - the expression context used when evaluating JEXL expressions.
	 * @param result  - the result of a JMX query.
	 */
	protected void populateContext(JexlContext context, Result result) {
		context.set(VAR__CLASSNAME, result.getClassName());
		context.set(VAR__ATTRIBUTE_NAME, result.getAttributeName());
		context.set(VAR__CLASSNAME_ALIAS, result.getKeyAlias());

		Map<String, String> typeNameMap = KeyUtils.getTypeNameValueMap(result.getTypeName());
		context.set(VAR__TYPENAME, typeNameMap);

		String effectiveClassname = result.getKeyAlias();
		if (effectiveClassname == null) {
			effectiveClassname = result.getClassName();
		}
		context.set(VAR__EFFECTIVE_CLASSNAME, effectiveClassname);

		context.set(VAR__RESULT, result);
	}
}
