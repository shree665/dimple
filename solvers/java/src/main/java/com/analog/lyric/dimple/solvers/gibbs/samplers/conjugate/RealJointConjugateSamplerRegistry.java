/*******************************************************************************
*   Copyright 2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.factorfunctions.core.IUnaryFactorFunction;

public class RealJointConjugateSamplerRegistry
{
	// LIST	ALL RealConjugateSamplerFactory CLASSES HERE
	// TODO: Is there a way to do this with reflection?
	public static final IRealJointConjugateSamplerFactory[] availableConjugateSamplers =
	{
		DirichletSampler.factory,
	};
	

	// Find a sampler compatible with the specified factor function as an input
	public static @Nullable IRealJointConjugateSampler findCompatibleSampler(IUnaryFactorFunction ff)
	{

		for (IRealJointConjugateSamplerFactory sampler : availableConjugateSamplers)
			if (sampler.isCompatible(ff))
				return sampler.create();	// Create and return the sampler
		return null;
	}

	
	// Get a sampler by name; assumes it is located in this package
	public static @Nullable IRealJointConjugateSampler get(String samplerName)
	{
		String fullQualifiedName = RealJointConjugateSamplerRegistry.class.getPackage().getName() + "." + samplerName;
		try
		{
			IRealJointConjugateSampler sampler = (IRealJointConjugateSampler)(Class.forName(fullQualifiedName).getConstructor().newInstance());
			return sampler;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
