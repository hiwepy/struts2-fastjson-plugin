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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.struts2.fastjson.annotation.ExcludeFilter;
import org.apache.struts2.fastjson.annotation.IncludeFilter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.NullPropertyFilter;
import com.alibaba.fastjson.serializer.PatternPropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.StringPropertyPreFilter;

public class JSONSerializerUtils {

	protected static  SerializerFeature[] GENERATE_FEATURES = { 
			
		SerializerFeature.QuoteFieldNames,
			
		// 输出空置字段
		SerializerFeature.WriteMapNullValue,

		//用枚举name()输出
		SerializerFeature.WriteEnumUsingName,
	    
		//list字段如果为null，输出为[]，而不是null
        SerializerFeature.WriteNullListAsEmpty,

        //字符类型字段如果为null，输出为""，而不是null
        SerializerFeature.WriteNullStringAsEmpty,
        
        //数值字段如果为null，输出为0，而不是null
        SerializerFeature.WriteNullNumberAsZero,

        //Boolean字段如果为null，输出为false，而不是null
        SerializerFeature.WriteNullBooleanAsFalse,

        SerializerFeature.WriteNonStringValueAsString,
        
        SerializerFeature.WriteBigDecimalAsPlain,
        
        
        //如果是true，类中的Get方法对应的Field是transient，序列化时将会被忽略。默认为true
        SerializerFeature.SkipTransientField,
        
        //消除对同一对象循环引用的问题，默认为false
        SerializerFeature.DisableCircularReferenceDetect,
        
        //使用指定的格式格式化日期类型对象
        SerializerFeature.WriteDateUseDateFormat,
        
        
        SerializerFeature.IgnoreNonFieldGetter,
        SerializerFeature.IgnoreErrorGetter
        
        
	};
	
	public final static String RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	static {
		JSON.DEFFAULT_DATE_FORMAT = RFC3339_FORMAT;
	}
	
	public static SerializeFilter[] buildFilters(Object object,  List<Pattern> excludeProperties,
			List<Pattern> includeProperties, boolean excludeNullProperties) {
		List<SerializeFilter> filters = new ArrayList<SerializeFilter>();
    	
		List<String> includes = new ArrayList<String>();
		List<String> excludes = new ArrayList<String>();
		
		IncludeFilter includeFilter = object.getClass().getAnnotation(IncludeFilter.class);
		if( null != includeFilter){
			
			for (String item : includeFilter.patterns()) {
                if (item != null) {
                	includeProperties.add(Pattern.compile(item));
                }
            }
			
			for (String item : includeFilter.properties()) {
                if (item != null) {
                	includes.add(item);
                }
            }
			
		}
		
		
		ExcludeFilter excludeFilter = object.getClass().getAnnotation(ExcludeFilter.class);
		if( null != excludeFilter){
			
			for (String item : excludeFilter.patterns()) {
                if (item != null) {
                	excludeProperties.add(Pattern.compile(item));
                }
            }
			
			for (String item : excludeFilter.properties()) {
                if (item != null) {
                	excludes.add(item);
                }
            }
			
		}
		
		filters.add(new StringPropertyPreFilter(object.getClass(), excludes, includes));
		filters.add(new PatternPropertyPreFilter(object.getClass(), excludeProperties, includeProperties));
		if(excludeNullProperties){
			filters.add(new NullPropertyFilter());
		}
		return filters.toArray( new SerializeFilter[filters.size()]);
	}
        
    /**
     * Serializes an object into JSON.
     * @param object to be serialized
     * @return JSON string
     * @throws JSONException in case of error during serialize
     */
    public static String serialize(Object object) throws JSONException {
    	
    	SerializeFilter[] filters = buildFilters(object, null, null, true) ;
        
        return JSONObject.toJSONString(object, filters, GENERATE_FEATURES);
    }

    /**
     * Serializes an object into JSON, excluding any properties matching any of
     * the regular expressions in the given Set.
     *
     * @param object to be serialized
     * @param excludeProperties Patterns matching properties to exclude
     * @param includeProperties Patterns matching properties to include
     * @param ignoreHierarchy whether to ignore properties defined on base classes of the root object
     * @param excludeNullProperties enable/disable excluding of null properties
     * @return JSON string
     * @throws JSONException in case of error during serialize
     */
    public static String serialize(Object object, List<Pattern> excludeProperties,
    		List<Pattern> includeProperties, boolean ignoreHierarchy, boolean excludeNullProperties)
            throws JSONException {
    	
    	SerializeFilter[] filters = buildFilters(object, excludeProperties, includeProperties, excludeNullProperties) ;
        
        return JSONObject.toJSONString(object, filters, GENERATE_FEATURES);
    }

    /**
     * Serializes an object into JSON, excluding any properties matching any of
     * the regular expressions in the given Set.
     *
     * @param object to be serialized
     * @param excludeProperties Patterns matching properties to exclude
     * @param includeProperties Patterns matching properties to include
     * @param ignoreHierarchy whether to ignore properties defined on base classes of the root object
     * @param excludeNullProperties enable/disable excluding of null properties
     * @param defaultDateFormat date format used to serialize dates
     * @return JSON string
     * @throws JSONException in case of error during serialize
     */
    public static String serialize(Object object, List<Pattern> excludeProperties, List<Pattern> includeProperties, 
    		boolean ignoreHierarchy,  boolean excludeNullProperties, String defaultDateFormat) throws JSONException {
    	
    	SerializeFilter[] filters = buildFilters(object, excludeProperties, includeProperties, excludeNullProperties) ;
    	JSON.DEFFAULT_DATE_FORMAT = defaultDateFormat;
        return JSONObject.toJSONString(object, filters, GENERATE_FEATURES);

    }

    
    /**
     * Serializes an object into JSON to the given writer.
     *
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @throws IOException  in case of IO errors
     * @throws JSONException in case of error during serialize
     */
    public static void serialize(Writer writer, Object object) throws IOException, JSONException {
        writer.write(serialize(object));
    }

    /**
     * Serializes an object into JSON to the given writer, excluding any
     * properties matching any of the regular expressions in the given
     * Set.
     *
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @param excludeProperties Patterns matching properties to ignore
     * @param includeProperties Patterns matching properties to include
     * @param excludeNullProperties enable/disable excluding of null properties
     * @throws IOException  in case of IO errors
     * @throws JSONException in case of error during serialize
     */
    public static void serialize(Writer writer, Object object, List<Pattern> excludeProperties,
    		List<Pattern> includeProperties, boolean excludeNullProperties) throws IOException,
            JSONException {
    	writer.write(serialize(object, excludeProperties, includeProperties, true, excludeNullProperties));
    }
    
    /**
     * Deserializes a object from JSON
     *
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONException in case of error during serialize
     */
    public static Object deserialize(String json) throws JSONException {
        return JSONObject.parse(json);
    }

    /**
     * Deserializes a object from JSON
     *
     * @param reader Reader to read a JSON string from
     * @return deserialized object
     * @throws JSONException when IOException happens
     */
    public static Object deserialize(Reader reader) throws JSONException {
        // read content
        BufferedReader bufferReader = new BufferedReader(reader);
        String line;
        StringBuilder buffer = new StringBuilder();

        try {
            while ((line = bufferReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }

        return deserialize(buffer.toString());
    }
    
}
