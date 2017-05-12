/*
 * Copyright (c) 2010-2020, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.struts2.fastjson.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.util.TextParseUtil;
import com.opensymphony.xwork2.util.WildcardUtil;

/**
 * Wrapper for JSONWriter with some utility methods.
 */
public class JSONOutputUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(JSONOutputUtils.class);

	public static void writeJSONToResponse(SerializationParams serializationParams) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(serializationParams.getSerialized())){
            stringBuilder.append(serializationParams.getSerialized());
        }
        if (StringUtils.isNotBlank(serializationParams.getWrapPrefix())){
            stringBuilder.insert(0, serializationParams.getWrapPrefix());
        } else if (serializationParams.isWrapWithComments()) {
            stringBuilder.insert(0, "/* ");
            stringBuilder.append(" */");
        } else if (serializationParams.isPrefix()){
            stringBuilder.insert(0, "{}&& ");
        }
        if (StringUtils.isNotBlank(serializationParams.getWrapSuffix())){
            stringBuilder.append(serializationParams.getWrapSuffix());
        }
        String json = stringBuilder.toString();

        LOG.debug("[JSON] {}", json);

        HttpServletResponse response = serializationParams.getResponse();

        // status or error code
        if (serializationParams.getStatusCode() > 0){
            response.setStatus(serializationParams.getStatusCode());
        } else if (serializationParams.getErrorCode() > 0){
            response.sendError(serializationParams.getErrorCode());
        }
        // content type
        response.setContentType(serializationParams.getContentType() + ";charset=" + serializationParams.getEncoding());

        if (serializationParams.isNoCache()) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", "0");
            response.setHeader("Pragma", "No-cache");
        }

        if (serializationParams.isGzip()) {
            response.addHeader("Content-Encoding", "gzip");
            GZIPOutputStream out = null;
            InputStream in = null;
            try {
                out = new GZIPOutputStream(response.getOutputStream());
                in = new ByteArrayInputStream(json.getBytes(serializationParams.getEncoding()));
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                if (in != null)
                    in.close();
                if (out != null) {
                    out.finish();
                    out.close();
                }
            }

        } else {
            response.setContentLength(json.getBytes(serializationParams.getEncoding()).length);
            PrintWriter out = response.getWriter();
            out.print(json);
        }
    }

    public static Set<String> asSet(String commaDelim) {
        if ((commaDelim == null) || (commaDelim.trim().length() == 0))
            return null;
        return TextParseUtil.commaDelimitedStringToSet(commaDelim);
    }

    public static boolean isGzipInRequest(HttpServletRequest request) {
        return StringUtils.contains(request.getHeader("Accept-Encoding"), "gzip");
    }

    public static final String REGEXP_PATTERN = "regexp";
    public static final String WILDCARD_PATTERN = "wildcard";
    /* package */ static final String SPLIT_PATTERN = "split";
    /* package */ static final String JOIN_STRING = "join";
    /* package */ static final String ARRAY_BEGIN_STRING = "array-begin";
    /* package */ static final String ARRAY_END_STRING = "array-end";

    /* package */ static Map<String, Map<String, String>> getIncludePatternData()
    {
        Map<String, Map<String, String>> includePatternData = new HashMap<String, Map<String, String>>();

        Map<String, String> data = new HashMap<String, String>();
        data.put(REGEXP_PATTERN, "\\\\\\.");
        data.put(WILDCARD_PATTERN, "\\.");
        includePatternData.put(SPLIT_PATTERN, data);

        data = new HashMap<String, String>();
        data.put(REGEXP_PATTERN, "\\.");
        data.put(WILDCARD_PATTERN, ".");
        includePatternData.put(JOIN_STRING, data);

        data = new HashMap<String, String>();
        data.put(REGEXP_PATTERN, "\\[");
        data.put(WILDCARD_PATTERN, "[");
        includePatternData.put(ARRAY_BEGIN_STRING, data);

        data = new HashMap<String, String>();
        data.put(REGEXP_PATTERN, "\\]");
        data.put(WILDCARD_PATTERN, "]");
        includePatternData.put(ARRAY_END_STRING, data);

        return includePatternData;
    }

    private static final Map<String, Map<String, String>> defaultIncludePatternData = getIncludePatternData();

    public static List<Pattern> processIncludePatterns(Set<String> includePatterns, String type) {
        return processIncludePatterns(includePatterns, type, defaultIncludePatternData);
    }

    /* package */ static List<Pattern> processIncludePatterns(Set<String> includePatterns, String type, Map<String, Map<String, String>> includePatternData) {
        if (includePatterns != null) {
            List<Pattern> results = new ArrayList<Pattern>(includePatterns.size());
            Map<String, String> existingPatterns = new HashMap<String, String>();
            for (String pattern : includePatterns) {
                processPattern(results, existingPatterns, pattern, type, includePatternData);
            }
            return results;
        } else {
            return null;
        }
    }

    private static void processPattern(List<Pattern> results, Map<String, String> existingPatterns, String pattern, String type, Map<String, Map<String, String>> includePatternData) {
        // Compile a pattern for each *unique* "level" of the object
        // hierarchy specified in the regex.
        String[] patternPieces = pattern.split(includePatternData.get(SPLIT_PATTERN).get(type));

        String patternExpr = "";
        for (String patternPiece : patternPieces) {
            patternExpr = processPatternPiece(results, existingPatterns, patternExpr, patternPiece, type, includePatternData);
        }
    }

    private static String processPatternPiece(List<Pattern> results, Map<String, String> existingPatterns, String patternExpr, String patternPiece, String type, Map<String, Map<String, String>> includePatternData) {
        if (patternExpr.length() > 0) {
            patternExpr += includePatternData.get(JOIN_STRING).get(type);
        }
        patternExpr += patternPiece;

        // Check for duplicate patterns so that there is no overlap.
        if (!existingPatterns.containsKey(patternExpr)) {
            existingPatterns.put(patternExpr, patternExpr);
            if (isIndexedProperty(patternPiece, type, includePatternData)) {
                addPattern(results, patternExpr.substring(0, patternExpr.lastIndexOf(includePatternData.get(ARRAY_BEGIN_STRING).get(type))), type);
            }
            addPattern(results, patternExpr, type);
        }
        return patternExpr;
    }

    /*
     * Add a pattern that does not have the indexed property matching (ie. list\[\d+\] becomes list).
     */
    private static boolean isIndexedProperty(String patternPiece, String type, Map<String, Map<String, String>> includePatternData) {
        return patternPiece.endsWith(includePatternData.get(ARRAY_END_STRING).get(type));
    }

    private static void addPattern(List<Pattern> results, String pattern, String type) {
        results.add(REGEXP_PATTERN.equals(type) ? Pattern.compile(pattern) : WildcardUtil.compileWildcardPattern(pattern));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding include {} expression: {}", (REGEXP_PATTERN.equals(type) ? "property" : "wildcard"), pattern);
        }
    }

}
