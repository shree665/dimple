/*******************************************************************************
*   Copyright 2012 Analog Devices Inc.
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

package com.analog.lyric.dimple.FactorFunctions;

import com.analog.lyric.dimple.FactorFunctions.core.FactorFunction;

/*
 * Function for testing kbestminsum works
 */
public class SumOfInputs extends FactorFunction 
{

	public SumOfInputs() {
		super("FactorFunctionForTesting");
	}

	@Override
	public double eval(Object... input) 
	{
		double sum = 0;
		for (int i = 0; i < input.length; i++)
		{
			sum += (Double)input[i];
		}
		return sum; 
	}
}