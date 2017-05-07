package org.apache.struts2.fastjson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.StrutsConstants;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.fastjson.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONException;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.WildcardUtil;

/**
 * <!-- START SNIPPET: description --> <p/> This result serializes an action
 * into JSON. <p/> <!-- END SNIPPET: description --> <p/> <p/> <u>Result
 * parameters:</u> <p/> <!-- START SNIPPET: parameters --> <p/>
 * <ul>
 * <p/>
 * <li>excludeProperties - list of regular expressions matching the properties
 * to be excluded. The regular expressions are evaluated against the OGNL
 * expression representation of the properties. </li>
 * <p/>
 * </ul>
 * <p/> <!-- END SNIPPET: parameters --> <p/> <b>Example:</b> <p/>
 * <p/>
 * <pre>
 * &lt;!-- START SNIPPET: example --&gt;
 * &lt;result name=&quot;success&quot; type=&quot;json&quot; /&gt;
 * &lt;!-- END SNIPPET: example --&gt;
 * </pre>
 */
@SuppressWarnings("serial")
public class FastJSONResult implements Result {

	protected static final Logger LOG = LoggerFactory.getLogger(FastJSONResult.class);
	
    /**
     * This result type doesn't have a default param, null is ok to reduce noise in logs
     */
    public static final String DEFAULT_PARAM = null;
    protected String encoding;
    protected String defaultEncoding = "ISO-8859-1";
    protected List<Pattern> includeProperties;
    protected List<Pattern> excludeProperties;
    protected String root;
    protected boolean wrapWithComments;
    protected boolean prefix;
    protected boolean enableGZIP = false;
    protected boolean ignoreHierarchy = true;
    protected boolean ignoreInterfaces = true;
    protected boolean noCache = false;
    protected boolean excludeNullProperties = false;
    protected String defaultDateFormat = null;
    protected int statusCode;
    protected int errorCode;
    protected String callbackParameter;
    protected String contentType;
    protected String wrapPrefix;
    protected String wrapSuffix;
    
    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String val) {
        this.defaultEncoding = val;
    }

    /**
     * Gets a list of regular expressions of properties to exclude from the JSON
     * output.
     *
     * @return A list of compiled regular expression patterns
     */
    public List<Pattern> getExcludePropertiesList() {
        return this.excludeProperties;
    }

    /**
     * Sets a comma-delimited list of regular expressions to match properties
     * that should be excluded from the JSON output.
     *
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setExcludeProperties(String commaDelim) {
        Set<String> excludePatterns = JSONUtils.asSet(commaDelim);
        if (excludePatterns != null) {
            this.excludeProperties = new ArrayList<Pattern>(excludePatterns.size());
            for (String pattern : excludePatterns) {
                this.excludeProperties.add(Pattern.compile(pattern));
            }
        }
    }

    /**
     * Sets a comma-delimited list of wildcard expressions to match properties
     * that should be excluded from the JSON output.
     *
     * @param commaDelim A comma-delimited list of wildcard patterns
     */
    public void setExcludeWildcards(String commaDelim) {
        Set<String> excludePatterns = JSONUtils.asSet(commaDelim);
        if (excludePatterns != null) {
            this.excludeProperties = new ArrayList<Pattern>(excludePatterns.size());
            for (String pattern : excludePatterns) {
                this.excludeProperties.add(WildcardUtil.compileWildcardPattern(pattern));
            }
        }
    }

    /**
     * @return the includeProperties
     */
    public List<Pattern> getIncludePropertiesList() {
        return includeProperties;
    }

    /**
     * Sets a comma-delimited list of regular expressions to match properties
     * that should be included in the JSON output.
     *
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setIncludeProperties(String commaDelim) {
        includeProperties = JSONUtils.processIncludePatterns(JSONUtils.asSet(commaDelim), JSONUtils.REGEXP_PATTERN);
    }

    /**
     * Sets a comma-delimited list of wildcard expressions to match properties
     * that should be included in the JSON output.
     *
     * @param commaDelim A comma-delimited list of wildcard patterns
     */
    public void setIncludeWildcards(String commaDelim) {
        includeProperties = JSONUtils.processIncludePatterns(JSONUtils.asSet(commaDelim), JSONUtils.WILDCARD_PATTERN);
    }

    public void execute(ActionInvocation invocation) throws Exception {
        ActionContext actionContext = invocation.getInvocationContext();
        HttpServletRequest request = (HttpServletRequest) actionContext.get(StrutsStatics.HTTP_REQUEST);
        HttpServletResponse response = (HttpServletResponse) actionContext.get(StrutsStatics.HTTP_RESPONSE);
        try {
        	Object rootObject = findRootObject(invocation);
            writeToResponse(response, createJSONString(request, rootObject), enableGzip(request));
        } catch (IOException exception) {
            LOG.error(exception.getMessage(), exception);
            throw exception;
        }
    }

    protected Object findRootObject(ActionInvocation invocation) {
        Object rootObject;
        if (this.root != null) {
            ValueStack stack = invocation.getStack();
            rootObject = stack.findValue(root);
        } else {
            rootObject = invocation.getStack().peek(); // model overrides action
        }
        return rootObject;
    }

    protected String createJSONString(HttpServletRequest request, Object rootObject) throws JSONException {
        String json = JSONUtils.serialize(rootObject, excludeProperties, includeProperties, ignoreHierarchy,
                                         excludeNullProperties, defaultDateFormat);
        json = addCallbackIfApplicable(request, json);
        return json;
    }

    protected boolean enableGzip(HttpServletRequest request) {
        return enableGZIP && JSONUtils.isGzipInRequest(request);
    }

    protected void writeToResponse(HttpServletResponse response, String json, boolean gzip) throws IOException {
        JSONUtils.writeJSONToResponse(new SerializationParams(response, getEncoding(), isWrapWithComments(),
                json, false, gzip, noCache, statusCode, errorCode, prefix, contentType, wrapPrefix,
                wrapSuffix));
    }
    
    /**
     * Retrieve the encoding <p/>
     *
     * @return The encoding associated with this template (defaults to the value
     *         of param 'encoding', if empty default to 'struts.i18n.encoding' property)
     */
    protected String getEncoding() {
        String encoding = this.encoding;

        if (encoding == null) {
            encoding = this.defaultEncoding;
        }

        if (encoding == null) {
            encoding = System.getProperty("file.encoding");
        }

        if (encoding == null) {
            encoding = "UTF-8";
        }

        return encoding;
    }

    protected String addCallbackIfApplicable(HttpServletRequest request, String json) {
        if ((callbackParameter != null) && (callbackParameter.length() > 0)) {
            String callbackName = request.getParameter(callbackParameter);
            if ((callbackName != null) && (callbackName.length() > 0))
                json = callbackName + "(" + json + ")";
        }
        return json;
    }

    /**
     * @return OGNL expression of root object to be serialized
     */
    public String getRoot() {
        return this.root;
    }

    /**
     * Sets the root object to be serialized, defaults to the Action
     *
     * @param root OGNL expression of root object to be serialized
     */
    public void setRoot(String root) {
        this.root = root;
    }

    /**
     * @return Generated JSON must be enclosed in comments
     */
    public boolean isWrapWithComments() {
        return this.wrapWithComments;
    }

    /**
     * Wrap generated JSON with comments
     *
     * @param wrapWithComments
     */
    public void setWrapWithComments(boolean wrapWithComments) {
        this.wrapWithComments = wrapWithComments;
    }

    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }

    /**
     * Controls whether interfaces should be inspected for method annotations
     * You may need to set to this true if your action is a proxy as annotations
     * on methods are not inherited
     */
    public void setIgnoreInterfaces(boolean ignoreInterfaces) {
        this.ignoreInterfaces = ignoreInterfaces;
    }

    public boolean isEnableGZIP() {
        return enableGZIP;
    }

    public void setEnableGZIP(boolean enableGZIP) {
        this.enableGZIP = enableGZIP;
    }

    public boolean isNoCache() {
        return noCache;
    }

    /**
     * Add headers to response to prevent the browser from caching the response
     *
     * @param noCache
     */
    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public boolean isIgnoreHierarchy() {
        return ignoreHierarchy;
    }

    public boolean isExcludeNullProperties() {
        return excludeNullProperties;
    }

    /**
     * Do not serialize properties with a null value
     *
     * @param excludeNullProperties
     */
    public void setExcludeNullProperties(boolean excludeNullProperties) {
        this.excludeNullProperties = excludeNullProperties;
    }

    /**
     * Status code to be set in the response
     *
     * @param statusCode
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Error code to be set in the response
     *
     * @param errorCode
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setCallbackParameter(String callbackParameter) {
        this.callbackParameter = callbackParameter;
    }

    public String getCallbackParameter() {
        return callbackParameter;
    }

    /**
     * Prefix JSON with "{} &&"
     *
     * @param prefix
     */
    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }

    /**
     * Content type to be set in the response
     *
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getWrapPrefix() {
        return wrapPrefix;
    }

    /**
     * Text to be inserted at the begining of the response
     */
    public void setWrapPrefix(String wrapPrefix) {
        this.wrapPrefix = wrapPrefix;
    }

    public String getWrapSuffix() {
        return wrapSuffix;
    }

    /**
     * Text to be inserted at the end of the response
     */
    public void setWrapSuffix(String wrapSuffix) {
        this.wrapSuffix = wrapSuffix;
    }

    /**
     * If defined will be used instead of {@link #defaultEncoding}, you can define it with result
     * &lt;result name=&quot;success&quot; type=&quot;json&quot;&gt;
     *     &lt;param name=&quot;encoding&quot;&gt;UTF-8&lt;/param&gt;
     * &lt;/result&gt;
     *
     * @param encoding valid encoding string
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getDefaultDateFormat() {
        return defaultDateFormat;
    }

    @Inject(required=false,value="struts.json.dateformat")
    public void setDefaultDateFormat(String defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
    }
}
