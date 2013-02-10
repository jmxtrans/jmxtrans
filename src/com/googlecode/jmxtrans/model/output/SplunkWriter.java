package com.googlecode.jmxtrans.model.output;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * Writes out data in a Key=Value format to a file.
 * automatically handle rolling the files after they reach a certain size.
 * 
 * Extends jon's KeyOutWriter
 * 
 * @author oded
 */
public class SplunkWriter extends KeyOutWriter {
	
	/**
	 * The meat of the output.
	 */
	@Override
	public void doWrite(Query query) throws Exception {
		List<Result> results = query.getResults();
		
		if (results.size() > 0) {
			StringBuilder sb = new StringBuilder();
			Result r = results.get(0);
			DateFormat fdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			sb.append(fdate.format(r.getEpoch()));
			sb.append(" jmxhost=");
			sb.append(query.getServer().getHost());
			sb.append(" ");
			sb.append(r.getTypeName().replace(",", " "));
			for (Result result : results ) {
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						if (JmxUtils.isNumeric(values.getValue())) {
							sb.append(" ");
							sb.append(result.getAttributeName());
							sb.append("=");
							sb.append(values.getValue().toString());
	
						}
					}
				}
			}
			logger.info(sb.toString());
		}
	}

}
