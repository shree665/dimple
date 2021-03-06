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

package com.analog.lyric.dimple.schedulers.schedule;

import net.jcip.annotations.NotThreadSafe;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.schedulers.IScheduler;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;

@NotThreadSafe
public abstract class ScheduleBase implements ISchedule
{
	private static final long serialVersionUID = 1L;

	/*-------
	 * State
	 */
	
	protected @Nullable IScheduler _scheduler;
	protected @Nullable FactorGraph _factorGraph;
	protected long _structureVersion;
	protected long _version;
	
	/*--------------
	 * Construction
	 */
	
	protected ScheduleBase(@Nullable IScheduler scheduler, @Nullable FactorGraph factorGraph)
	{
		_scheduler = scheduler;
		_factorGraph = factorGraph;
		_structureVersion = factorGraph != null ? factorGraph.structureVersion() : -1L;
	}
	
	/*----------------
	 * Object methods
	 */
	
	@Override
	public String toString()
	{
		return super.toString();
	}

	/*--------------------
	 * IOptionKey methods
	 */
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Most schedules are mutable, so this returns true.
	 */
	@Override
	public boolean isMutable()
	{
		return true;
	}
	
	/*-------------------
	 * ISchedule methods
	 */
	
	/*
	 * This method is called when setSchedule is called on the FactorGraph.
	 */
	@Override
	public void attach(FactorGraph factorGraph)
	{
		if (factorGraph != _factorGraph)
		{
			++_version;
			_factorGraph = factorGraph;
		}
		_structureVersion = factorGraph.structureVersion();
	}
	
	@Override
	public @Nullable FactorGraph getFactorGraph()
	{
		return _factorGraph;
	}
	
	@Override
	public @Nullable IScheduler getScheduler()
	{
		return _scheduler;
	}

	public
	void setScheduler(IScheduler scheduler)
	{
		_scheduler = scheduler;
	}
	
	@Override
	public boolean isCustom()
	{
		return false;
	}
	
	@Override
	public boolean isUpToDateForSolver(ISolverFactorGraph sgraph)
	{
		final FactorGraph fg = sgraph.getModelObject();
		final IScheduler scheduler = _scheduler;
		return (scheduler == null || scheduler.equals(sgraph.getScheduler())) &&
			_factorGraph == fg && _structureVersion == fg.structureVersion();
	}
	
	@Override
	public long scheduleVersion()
	{
		return _version;
	}
	
	@Override
	public long structureVersion()
	{
		return _structureVersion;
	}
}
