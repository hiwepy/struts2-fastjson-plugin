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
package com.alibaba.fastjson.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringPropertyPreFilter implements PropertyPreFilter {

    private final Class<?>    clazz;
    private final List<String> includes = new ArrayList<String>();
    private final List<String> excludes = new ArrayList<String>();
    private int               maxLevel = 0;

    public StringPropertyPreFilter(String... includeProperties){
        this(null, includeProperties);
    }

    public StringPropertyPreFilter(Class<?> clazz, String... includeProperties){
        this(clazz, null, Arrays.asList(includeProperties));
    }
    
    public StringPropertyPreFilter(Class<?> clazz, 
    		List<String>  excludeProperties,
    		List<String> includeProperties){
        super();
        this.clazz = clazz;
        
        if( null != excludeProperties ){
        	for (String item : excludeProperties) {
                if (item != null) {
                    this.excludes.add(item);
                }
            }
        }
        
        if( null != includeProperties ){
        	for (String item : includeProperties) {
                if (item != null) {
                    this.includes.add(item);
                }
            }
        }
        
    }
    
    /**
     * @since 1.2.9
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * @since 1.2.9
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public boolean apply(JSONSerializer serializer, Object source, String name) {
        if (source == null) {
            return true;
        }

        if (clazz != null && !clazz.isInstance(source)) {
            return true;
        }

        if (this.excludes.contains(name)) {
            return false;
        }
        
        if (maxLevel > 0) {
            int level = 0;
            SerialContext context = serializer.context;
            while (context != null) {
                level++;
                if (level > maxLevel) {
                    return false;
                }
                context = context.parent;
            }
        }

        if (includes.size() == 0 || includes.contains(name)) {
            return true;
        }
        
        return false;
    }

}
