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

package com.analog.lyric.dimple.events;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

import com.analog.lyric.dimple.model.core.FactorGraph;

/**
 * 
 * @since 0.06
 * @author Christopher Barber
 */
@ThreadSafe
public class DimpleEventLogger implements Closeable
{
	/*-------
	 * State
	 */
	
	/**
	 * Events will be printed on this stream, if non-null.
	 */
	private volatile PrintStream _out;
	
	/**
	 * The file used for {@link #_out}, if opened by {@link #open(File)}.
	 */
	private volatile File _file = null;
	
	private volatile int _verbosity;
	
	private final Set<DimpleEventListener> _listeners = new HashSet<DimpleEventListener>();
	private final Set<FactorGraph> _graphs = new HashSet<FactorGraph>();

	private final DimpleEventHandler<DimpleEvent> _handler = new EventPrinter();
	
	private class EventPrinter extends DimpleEventHandler<DimpleEvent>
	{
		@Override
		public void handleEvent(DimpleEvent event)
		{
			PrintStream out = _out;
			if (out != null)
			{
				event.println(out, _verbosity);
				out.flush();
			}
		}
	}
	
	/*--------------
	 * Construction
	 */
	
	/**
	 * Constructs logger with output going to {@link System#err} and
	 * {@link #verbosity()} set to zero.
	 * 
	 * @since 0.06
	 */
	public DimpleEventLogger()
	{
		_out = System.err;
		_verbosity = 0;
	}
	
	/*-------------------
	 * Closeable methods
	 */
	
	/**
	 * Closes underlying stream.
	 * <p>
	 * If {@link #out()} is non-null, closes it if not one of {@link System#out} or {@link System#err},
	 * and then sets to null.
	 */
	@Override
	public synchronized void close()
	{
		if (_out != null)
		{
			if (_out != System.out && _out != System.err)
			{
				_out.close();
			}
			_out = null;
		}
	}
	
	/*---------
	 * Methods
	 */

	/**
	 * File last used by {@link #open(File)} or null if last opened by {@link #open(PrintStream)}.
	 * 
	 * @since 0.06
	 */
	public File file()
	{
		return _file;
	}
	
	/**
	 * True if {@link #out()} is non-null.
	 * 
	 * @since 0.06
	 */
	public boolean isOpen()
	{
		return _out != null;
	}
	
	/**
	 * Enable logging of given event type on specified targets.
	 * <p>
	 * Enables logging by registering an event handler with the {@link FactorGraph#getEventListener()}
	 * for the root graph containing each source. If a root graph does not currently have a listener,
	 * its listener will automatically be set to {@link DimpleEventListener#getDefault()}.
	 * <p>
	 * @param eventType is the superclass of the type of events that will be logged. If {@code eventType}
	 * is abstract then all subtypes will be logged, otherwise only that specific type will be logged.
	 * @param sources lists the objects that should log events. This will affect both those objects and their
	 * children unless blocked.
	 * @since 0.06
	 */
	public synchronized void log(Class<? extends DimpleEvent> eventType, IDimpleEventSource ... sources)
	{
		for (IDimpleEventSource source : sources)
		{
			DimpleEventListener listener = listenerForSource(source);
			listener.register(_handler, eventType, Modifier.isAbstract(eventType.getModifiers()), source);
		}
	}
	
	/**
	 * Directs log output to append to file.
	 * <p>
	 * Invokes {@link #close()} before opening new file.
	 * <p>
	 * @param file is non-null file that will be opened in append mode.
	 * @throws FileNotFoundException
	 * @since 0.06
	 * @see #open(File, boolean)
	 */
	public void open(File file) throws FileNotFoundException
	{
		open(file, true);
	}
	
	/**
	 * Directs log output to a file.
	 * <p>
	 * Invokes {@link #close()} before opening new file.
	 * <p>
	 * @param file is non-null file that will be opened in append mode.
	 * @param append indicates whether to append to the file or overwrite the existing contents.
	 * @throws FileNotFoundException
	 * @since 0.06
	 * @see #open(File)
	 * @see #open(PrintStream)
	 */
	public synchronized void open(File file, boolean append) throws FileNotFoundException
	{
		close();
		_out = new PrintStream(new FileOutputStream(file, append));
		_file = file;
	}
	
	/**
	 * Directs log output to given stream.
	 * 
	 * @param out
	 * @since 0.06
	 * @see #open(File)
	 * @see #open(File, boolean)
	 */
	public synchronized void open(PrintStream out)
	{
		close();
		_out = out;
		_file = null;
	}
	
	/**
	 * Clears all logging handlers controlled by this object.
	 * 
	 * @since 0.06
	 */
	public synchronized void clear()
	{
		DimpleEventListener defaultListener = null;
		for (DimpleEventListener listener : _listeners)
		{
			listener.unregisterAll(_handler);
			if (listener.isDefault())
			{
				defaultListener = listener;
			}
		}
		_listeners.clear();
		if (defaultListener != null && defaultListener.isEmpty())
		{
			for (FactorGraph graph : _graphs)
			{
				if (graph.getEventListener() == defaultListener)
				{
					graph.setEventListener(null);
				}
			}
		}
		_graphs.clear();
	}
	
	/**
	 * The current stream used for logging. May be null.
	 * @since 0.06
	 */
	public PrintStream out()
	{
		return _out;
	}
	
	public int verbosity()
	{
		return _verbosity;
	}
	
	public void verbosity(int verbosity)
	{
		_verbosity = verbosity;
	}
	
	/*-----------------
	 * Private methods
	 */
	
	private DimpleEventListener listenerForSource(IDimpleEventSource source)
	{
		final FactorGraph rootGraph = source.getContainingGraph().getRootGraph();

		DimpleEventListener listener = rootGraph.getEventListener();
		if (listener == null)
		{
			listener = DimpleEventListener.getDefault();
			rootGraph.setEventListener(listener);
		}

		_graphs.add(rootGraph);
		_listeners.add(listener);

		return listener;
	}
}
