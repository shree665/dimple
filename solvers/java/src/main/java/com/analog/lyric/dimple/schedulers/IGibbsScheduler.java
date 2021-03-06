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

package com.analog.lyric.dimple.schedulers;

import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.variables.VariableBlock;
import com.analog.lyric.dimple.schedulers.schedule.IGibbsSchedule;
import com.analog.lyric.dimple.schedulers.scheduleEntry.BlockScheduleEntry;
import com.analog.lyric.dimple.schedulers.scheduleEntry.IBlockUpdater;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;


/**
 * 
 * @since 0.06
 * @author jeffb
 */
public interface IGibbsScheduler extends IScheduler
{
	@Override
	public IGibbsSchedule createSchedule(FactorGraph graph);
	
	@Override
	public IGibbsSchedule createSchedule(ISolverFactorGraph graph);

	/**
	 * @deprecated use {@link #addBlockWithReplacement(IBlockUpdater, VariableBlock)} instead.
	 */
	@Deprecated
	public void addBlockScheduleEntry(BlockScheduleEntry blockScheduleEntry);
	
	/**
	 * Adds a block schedule entry and replacing any existing node and edge entries.
	 * <p>
	 * This will remove any node or edge schedule entries for variables contained in the block.
	 * <p>
	 * @param blockUpdater
	 * @param block
	 * @since 0.08
	 */
	public void addBlockWithReplacement(IBlockUpdater blockUpdater, VariableBlock block);
}
