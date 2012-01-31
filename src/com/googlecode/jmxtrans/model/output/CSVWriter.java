package com.googlecode.jmxtrans.model.output;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.filter.Filter;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * Writes out data in CSV format, it's a simple Writer, creates
 * series of files on a dir, based on ObjectName.
 *
 * @author marcos.lois
 */
//@JsonPropertyOrder(value = { "settings", "filters" })
public class CSVWriter extends BaseOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(CSVWriter.class);
	
	private String filePath;
	private String filePrefix;

	/** The file writers. */
	private Map<String, Writer> writers = new HashMap<String, Writer>();
	
	/** The headers. */
	private Map<String, Map<String, List<String>>> headers = new HashMap<String,Map<String, List<String>>>();
	
	/** The date format. */
	private DateFormat fdate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	// TODO : Move to some upper layer, debate first
	private List<Filter> filters;

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.util.BaseOutputWriter#start()
	 */
	@Override
	public void start() throws LifecycleException {
		
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.util.BaseOutputWriter#stop()
	 */
	@Override
	public void stop() throws LifecycleException {
		if (this.writers != null) {
			try {
				for(Writer w : writers.values())
					if(w != null)
						w.close();
			} catch (IOException e) {
				throw new LifecycleException("Error closing file.", e);
			}
		}
	}

	/**
	 * Creates the logging.
	 *
	 * @param query the query
	 * @throws ValidationException the validation exception
	 */
	public void validateSetup(Query query) throws ValidationException {
		this.filePath = (String) this.getSettings().get("filePath");
		if (this.filePath == null) {
			throw new ValidationException("You must specify an filePath setting.", query);
		}
		
		String filePrefix = (String) this.getSettings().get("filePrefix");
		if(filePrefix != null) {
			this.filePrefix = filePrefix;
		}
		
		if(this.isDebugEnabled()) {
			log.debug("Settings :\n  filePath : '" + filePath + "'\n  filePrefix : '" + filePrefix +"'\n  Filters :");
			if(filters != null)
				for(Filter f: filters)
					log.debug("  '" + f.getClass().toString() + "'");
		}
	}

	/**
	 * The meat of the output. Very similar to GraphiteWriter.
	 *
	 * @param query the query
	 * @throws Exception the exception
	 */
	public void doWrite(Query query) throws Exception {
		// Required to not override files, on willcard queries
		List<String> typeNames = getTypeNames();
		
		String file = JmxUtils.getKeyString4(query, typeNames, filePrefix) + "csv";
		Writer writer = this.writers.get(file);
		if(writer == null) {
			writer = new FileWriter(filePath + "/" + file);
			this.writers.put(file, writer);
		}
		synchronized(CSVWriter.class) {
			// Create the headers for files
			if(this.headers.get(file) == null) {
					final Map<String, List<String>> header = new LinkedHashMap<String, List<String>>();
					StringBuilder sb = new StringBuilder();
					sb.append("TIME;");
					for (Result result : query.getResults()) {
//						log.debug("Query result : " + result);
						header.put(result.getAttributeName(), (List<String>) new ArrayList<String>(result.getValues().keySet()));
						if (result.getValues().keySet().size() == 1) {
							sb.append(result.getAttributeName() + ";");
						} else {
							for (String name : result.getValues().keySet()) {
								sb.append(result.getAttributeName() + "_" + name + ";");
							}
						}
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.append("\n");
					headers.put(file, header);
				
				
				writer.write(sb.toString());
				writer.flush();
			}
		}

		// Output the values
		Map<String, List<String>> header = headers.get(file);
		StringBuilder sb = new StringBuilder();
		sb.append(fdate.format(query.getResults().get(0).getEpoch()) + ";");
		for (Result result : query.getResults()) {
			for (String name : header.get(result.getAttributeName())) {
				if (result.getValues() != null) {
					Object val = doFilter(result.getValues().get(name));
					if (JmxUtils.isNumeric(val)) {
						sb.append(val.toString() + ";");
					} else {
						sb.append(";");
					}
				} else {
					sb.append(";");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("\n");
		writer.write(sb.toString());
		writer.flush();
	}
	
	/**
	 * Gets the filters.
	 *
	 * @return the filters
	 */
	public List<Filter> getFilters() {
		return filters;
	}

	/**
	 * Sets the filters.
	 *
	 * @param filters the new filters
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}
	
	public void addFilter(Filter filter) {
		if (this.filters == null) {
			this.filters = new ArrayList<Filter>();
		}
		this.filters.add(filter);
	}

	/**
	 * Do filter on all registered filters.
	 *
	 * @param obj the obj
	 * @return the object
	 */
	private Object doFilter(Object obj) {
		if(this.filters != null)
			for(Filter f : this.filters)
				obj = f.doFilter(obj);
		return obj;
	}
}
