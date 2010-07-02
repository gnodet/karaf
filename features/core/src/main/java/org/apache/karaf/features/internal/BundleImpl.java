/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal;

import org.apache.karaf.features.BundleInfo;

/**
 * A holder of bundle info
 */
public class BundleImpl implements BundleInfo {

    private int startLevel;
    private String location;
    private boolean start;
    

    public BundleImpl() {
    }

    public BundleImpl(String location) {
    	this.location = location;
    }
    
    
    public BundleImpl(String location, boolean start) {
    	this.location = location;
        this.setStart(start);
    }
    
    public BundleImpl(String location, Integer startLevel) {
    	this.location = location;
        this.startLevel = startLevel;
    }
    
    public BundleImpl(String location, Integer startLevel, boolean start) {
    	this.location = location;
        this.startLevel = startLevel;
    }
    
    public void setStartLevel(Integer startLevel) {
    	this.startLevel = startLevel;
    }
	
    public int getStartLevel() {
		return this.startLevel;
	}
	
	public void setName(String location) {
		this.location = location;
	}
	
	public String getLocation() {
		return this.location;
	}

	public void setStart(boolean start) {
		this.start = start;
	}

	public boolean isStart() {
		return start;
	}
}
