/*
 * Copyright (c) 2018 (https://github.com/vindell).
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

import com.alibaba.fastjson.serializer.PropertyFilter;

public class NullPropertyFilter implements PropertyFilter {

	/**
	 * 过滤不需要被序列化的属性
	 * @param source 属性所在的对象
	 * @param name 属性名
	 * @param value 属性值
	 * @return 返回false属性将被忽略，ture属性将被保留
	 */
	@Override
	public boolean apply(Object source, String name, Object value) {
		//所属对象和属性值均不能为空
		return (source != null && value != null  && !"null".equalsIgnoreCase(String.valueOf(value))) ?  true : false;
		
	}

}
