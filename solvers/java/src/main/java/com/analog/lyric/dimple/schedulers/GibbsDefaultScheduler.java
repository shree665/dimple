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

package com.analog.lyric.dimple.schedulers;

/**
 * @author jeffb
 * 
 *         This class's purpose is simply to point to the scheduler that should
 *         be used if no other scheduler is specified *specifically* for the Gibbs solver.
 */
public class GibbsDefaultScheduler extends GibbsSequentialScanScheduler
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * {@inheritDoc}
	 * 
	 * @return true
	 */
	@Override
	public boolean isDefaultScheduler()
	{
		return true;
	}
}
