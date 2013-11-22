/*******************************************************************************
 *   Copyright 2013 Analog Devices, Inc.
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

package com.analog.lyric.dimple.factorfunctions;

import java.util.Collection;

import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunctionUtilities;
import com.analog.lyric.dimple.model.values.IndexedValue;
import com.analog.lyric.dimple.model.values.Value;


/**
 * Deterministic vector inner product. This is a deterministic directed factor
 * (if smoothing is not enabled).
 * 
 * Optional smoothing may be applied, by providing a smoothing value in the
 * constructor. If smoothing is enabled, the distribution is smoothed by
 * exp(-difference^2/smoothing), where difference is the distance between the
 * output value and the deterministic output value for the corresponding inputs.
 * 
 * The variables are ordered as follows in the argument list:
 * 
 * 1) Output value
 * 2) First input vector (may be either a list of scalars or a RealJoint vector)
 * 3) Second input vector (may be either a list of scalars or a RealJoint vector)
 * 
 */
public class VectorInnerProduct extends FactorFunction
{
	protected double _beta = 0;
	protected boolean _smoothingSpecified = false;

	public VectorInnerProduct() {this(0);}
	public VectorInnerProduct(double smoothing)
	{
		super();

		if (smoothing > 0)
		{
			_beta = 1 / smoothing;
			_smoothingSpecified = true;
		}
	}

	@Override
	public double evalEnergy(Object ... arguments)
	{
		double expectedOutValue = 0;
		double[] firstInput = null;
		double[] secondInput = null;
		boolean firstInputIsArray = false;
		boolean secondInputIsArray = false;
		
		// Figure out the type of inputs (array or list of arguments)
		final int numArgs = arguments.length;
		Object firstInputArg = arguments[1];
		Object lastInputArg = arguments[numArgs-1];
		if (firstInputArg instanceof double[])
		{
			firstInput = (double[])firstInputArg;
			firstInputIsArray = true;
		}
		if (lastInputArg instanceof double[])
		{
			secondInput = (double[])lastInputArg;
			secondInputIsArray = true;
		}
		
		// Compute the output
		if (firstInputIsArray && secondInputIsArray)
		{
			int vectorLength = firstInput.length;
			for (int i = 0; i < vectorLength; i++)
				expectedOutValue += firstInput[i] * secondInput[i];
		}
		else if (firstInputIsArray)
		{
			int vectorLength = firstInput.length;
			int secondIndex = 2;
			for (int i = 0; i < vectorLength; i++)
				expectedOutValue += firstInput[i] * FactorFunctionUtilities.toDouble(arguments[secondIndex++]);
		}
		else if (secondInputIsArray)
		{
			int vectorLength = secondInput.length;
			int firstIndex = 1;
			for (int i = 0; i < vectorLength; i++)
				expectedOutValue += FactorFunctionUtilities.toDouble(arguments[firstIndex++]) * secondInput[i];
		}
		else	// Neither input is array
		{
			int vectorLength = (numArgs - 1) >> 1;
			int firstIndex = 1;
			int secondIndex = 1 + vectorLength;
			for (int i = 0; i < vectorLength; i++)
				expectedOutValue += FactorFunctionUtilities.toDouble(arguments[firstIndex++]) * FactorFunctionUtilities.toDouble(arguments[secondIndex++]);
		}

		// Get the output value
		double outValue = FactorFunctionUtilities.toDouble(arguments[0]);

		double diff = (outValue - expectedOutValue);
		double error = diff*diff;

		if (_smoothingSpecified)
			return error*_beta;
		else
			return (error == 0) ? 0 : Double.POSITIVE_INFINITY;
	}


	@Override
	public final boolean isDirected() {return true;}
	@Override
	public final int[] getDirectedToIndices() {return new int[]{0};}
	@Override
	public final boolean isDeterministicDirected() {return !_smoothingSpecified;}
	@Override
	public final void evalDeterministic(Object[] arguments)
	{
		double outValue = 0;
		double[] firstInput = null;
		double[] secondInput = null;
		boolean firstInputIsArray = false;
		boolean secondInputIsArray = false;
		
		// Figure out the type of inputs (array or list of arguments)
		final int numArgs = arguments.length;
		Object firstInputArg = arguments[1];
		Object lastInputArg = arguments[numArgs-1];
		if (firstInputArg instanceof double[])
		{
			firstInput = (double[])firstInputArg;
			firstInputIsArray = true;
		}
		if (lastInputArg instanceof double[])
		{
			secondInput = (double[])lastInputArg;
			secondInputIsArray = true;
		}
		
		// Compute the output
		if (firstInputIsArray && secondInputIsArray)
		{
			int vectorLength = firstInput.length;
			for (int i = 0; i < vectorLength; i++)
				outValue += firstInput[i] * secondInput[i];
		}
		else if (firstInputIsArray)
		{
			int vectorLength = firstInput.length;
			int secondIndex = 2;
			for (int i = 0; i < vectorLength; i++)
				outValue += firstInput[i] * FactorFunctionUtilities.toDouble(arguments[secondIndex++]);
		}
		else if (secondInputIsArray)
		{
			int vectorLength = secondInput.length;
			int firstIndex = 1;
			for (int i = 0; i < vectorLength; i++)
				outValue += FactorFunctionUtilities.toDouble(arguments[firstIndex++]) * secondInput[i];
		}
		else	// Neither input is array
		{
			int vectorLength = (numArgs - 1) >> 1;
			int firstIndex = 1;
			int secondIndex = 1 + vectorLength;
			for (int i = 0; i < vectorLength; i++)
				outValue += FactorFunctionUtilities.toDouble(arguments[firstIndex++]) * FactorFunctionUtilities.toDouble(arguments[secondIndex++]);
		}

		// Replace the output values
		arguments[0] = outValue;
	}
	
	@Override
	public final int updateDeterministicLimit(int numEdges)
	{
		// Each incremental update uses 2 multiply/adds versus <vector-length> for a full update so
		// the limit should be <vector-length>/2. The number should be lower if one of the vectors
		// is a constant passed in one arg, but we don't know that here.
		return _smoothingSpecified ? 0 : numEdges / 2;
	}
	
	@Override
	public final boolean updateDeterministic(Value[] values, Collection<IndexedValue> oldValues)
	{
		final int nValues = values.length;
		final int outputOffset = 0;
		final int offset1 = 1;
		
		final Object obj1 = values[offset1].getObject();
		final double[] v1 = obj1 instanceof double[] ? (double[])obj1 : null;
		
		final Object obj2 = values[nValues - 1].getObject();
		final double[] v2 = obj2 instanceof double[] ? (double[])obj2 : null;
		
		boolean incremental = false;
		
		doIncremental:
		{
			if (v1 != null && v2 != null)
			{
				// If both vectors are passed in single variables, then
				// we don't currently have a way to determine which values changed, so we can't
				// do an incremental update.
				break doIncremental;
			}
			
			final int vectorLength = v1 != null ? v1.length : (v2 != null ? v2.length : ((nValues - 1) / 2));

			if (vectorLength / 2 < oldValues.size())
			{
				// If oldValues is more than half the size of vectors, then it will cost more to
				// do an incremental update.
				break doIncremental;
			}
			
			final int offset2 = v1 == null ? offset1 + vectorLength : offset1 + 1;

			final int minSupportedIndex = v1 == null ? offset1 : offset1 + 1;
			final int maxSupportedIndex = v2 == null ? nValues: nValues - 1;

			final Value outputValue = values[outputOffset];
			double output = outputValue.getDouble();

			for (IndexedValue old : oldValues)
			{
				final int changedIndex = old.getIndex();
				
				if (changedIndex < offset1 || nValues <= changedIndex)
				{
					throw new IndexOutOfBoundsException();
				}

				if (changedIndex < minSupportedIndex || changedIndex >= maxSupportedIndex)
				{
					// Must be referring to an array variable. Since we don't know how many or which of the
					// elements of the array have changed, there is no point in trying to do an incremental
					// update.
					break doIncremental;
				}
				
				final double newInput = values[changedIndex].getDouble();
				final double oldInput = old.getValue().getDouble();

				if (newInput != oldInput)
				{
					double otherInput;
					if (changedIndex < offset2)
					{
						final int i = changedIndex - offset1;
						otherInput = v2 != null ? v2[i] : values[offset2 + i].getDouble();
					}
					else
					{
						final int i = changedIndex - offset2;
						otherInput = v1 != null ? v1[i] : values[offset1 + i].getDouble();
					}
					output = output - otherInput * oldInput + otherInput * newInput;
				}
			}

			outputValue.setDouble(output);
			incremental = true;
		}
		
		return incremental || super.updateDeterministic(values, oldValues);
	}
}
