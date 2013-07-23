/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************/

package com.analog.lyric.dimple.model;

import java.lang.reflect.Array;
import java.util.Arrays;

public class RealJointDomain extends Domain
{
	/*-------
	 * State
	 */
	
	private RealDomain [] _domains;
	
	/*--------------
	 * Construction
	 */
	
	public RealJointDomain(int size)
	{
		_domains = new RealDomain[size];
		
		for (int i = 0; i < size; i++)
			_domains[i] = new RealDomain();
	}
	
	public RealJointDomain(RealDomain ... domains)
	{
		_domains = domains.clone();
	}
	
	/*----------------
	 * Domain methods
	 */
	
	/**
	 * @returns true if value is an array of length matching {@link #getNumVars()} and whose
	 * elements are contained in the correspond {@link RealDomain} in {@link #getRealDomains()}.
	 */
	@Override
	public boolean containsValue(Object value)
	{
		if (value.getClass().isArray() && Array.getLength(value) == _domains.length)
		{
			for (int i = 0, end = _domains.length; i < end; ++i)
			{
				if (! _domains[i].containsValue(Array.get(value, i)))
				{
					return false;
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean isDiscrete()
	{
		return false;
	}
	
	/*-------------------------
	 * RealJointDomain methods
	 */
	
	public RealDomain [] getRealDomains()
	{
		return _domains;
	}
	
	public RealDomain getRealDomain(int dimension)
	{
		return _domains[dimension];
	}
	
	public int getNumVars()
	{
		return _domains.length;
	}
	
	@Override
	public boolean isJoint()
	{
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this._domains);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof RealJointDomain))
			return false;

		RealJointDomain other = (RealJointDomain) obj;
		if (!Arrays.equals(this._domains, other._domains))
			return false;
		return true;
	}
	
	// Utility to check if a value is in the domain or not
	public boolean inDomain(double[] value)
	{
		int size = value.length;
		if (size != _domains.length)
			return false;
		
		for (int i = 0; i < size; i++)
			if (!_domains[i].inDomain(value[i]))
				return false;
		
		return true;
	}
	
}
