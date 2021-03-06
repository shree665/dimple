/*******************************************************************************
*   Copyright 2012-2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.sumproduct.customFactors;

import static java.util.Objects.*;

import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.factorfunctions.Normal;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariablePredicates;
import com.analog.lyric.dimple.solvers.core.SolverFactorCreationException;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.NormalParameters;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolverGraph;
import com.google.common.collect.Iterables;


public class CustomNormalConstantParameters extends GaussianFactorBase
{
	private static final int MEAN_PARAMETER_INDEX = 0;
	private static final int PRECISION_PARAMETER_INDEX = 1;

	
	public CustomNormalConstantParameters(Factor factor, SumProductSolverGraph parent)
	{
		super(factor, parent);
		assertUnboundedReal(factor);
		
		Normal factorFunction = (Normal)factor.getFactorFunction();

		if (!factorFunction.hasConstantParameters() &&
			!(factor.hasConstantAtIndex(MEAN_PARAMETER_INDEX) || !factor.hasConstantAtIndex(PRECISION_PARAMETER_INDEX)))
		{
			throw new SolverFactorCreationException("Normal factor must have constant parameters");
		}
	}

	
	@Override
	public void doUpdateEdge(int outPortNum)
	{
		// For constant parameters, output message will never change
	}


	@Override
	public void initialize()
	{
		super.initialize();
		
		// Pre-compute output message
		final Factor factor = _model;
		Normal factorFunction = (Normal)factor.getFactorFunction();

		NormalParameters outputMessage;
		if (factorFunction.hasConstantParameters())
			outputMessage = factorFunction.getParameters();
		else if (factor.hasConstantAtIndex(MEAN_PARAMETER_INDEX) && factor.hasConstantAtIndex(PRECISION_PARAMETER_INDEX))
		{
			double mean = requireNonNull(factor.getConstantValueByIndex(MEAN_PARAMETER_INDEX)).getDouble();
			double precision =
				requireNonNull(factor.getConstantValueByIndex(PRECISION_PARAMETER_INDEX)).getDouble();
			outputMessage = new NormalParameters(mean, precision);
		}
		else
			throw new DimpleException("Normal factor must have constant parameters");
		
		// Can set the output messages now since it will never change
		for (int edge = 0, n = getSiblingCount(); edge < n; edge++)
			getSiblingEdgeState(edge).factorToVarMsg.set(outputMessage);
	}
	
	
	/**
	 * Utility to indicate whether or not a factor is compatible with the requirements of this custom factor
	 * @deprecated as of release 0.08
	 */
	@Deprecated
	public static boolean isFactorCompatible(Factor factor)
	{
		FactorFunction factorFunction = factor.getFactorFunction();
		if (!(factorFunction instanceof Normal))
			return false;
		
		Normal specificFactorFunction = (Normal)factorFunction;
		
		boolean constantParameters = false;
		if (specificFactorFunction.hasConstantParameters())
			constantParameters = true;
		else if (factor.hasConstantAtIndex(MEAN_PARAMETER_INDEX) && factor.hasConstantAtIndex(PRECISION_PARAMETER_INDEX))
			constantParameters = true;

		if (!constantParameters)
			return false;
		
		return Iterables.all(factor.getSiblings(), VariablePredicates.isUnboundedReal());
	}


}
