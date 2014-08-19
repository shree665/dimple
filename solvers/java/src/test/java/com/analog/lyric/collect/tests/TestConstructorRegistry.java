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

package com.analog.lyric.collect.tests;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.analog.lyric.collect.BinaryHeap;
import com.analog.lyric.collect.ConstructorRegistry;
import com.analog.lyric.collect.IHeap;

/**
 * Test for {@link ConstructorRegistry}
 * 
 * @since 0.07
 * @author Christopher Barber
 */
public class TestConstructorRegistry
{
	@Test
	public void test()
	{
		String lyricCollectPackage = IHeap.class.getPackage().getName();
		
		ConstructorRegistry<IHeap<?>> heapRegistry = new ConstructorRegistry<>(IHeap.class);
		
		assertTrue(heapRegistry.isEmpty());
		assertInvariants(heapRegistry);
		assertNull(heapRegistry.get("DoesNotExist"));
		assertNull(heapRegistry.getClass("DoesNotExist"));
		assertNull(heapRegistry.instantiate("DoesNotExist"));
		assertNull(heapRegistry.get("ArrayUtil"));
		
		assertArrayEquals(new String[] { lyricCollectPackage }, heapRegistry.getPackages());
		assertSame(BinaryHeap.class, heapRegistry.getClass("BinaryHeap"));
		assertInvariants(heapRegistry);
		assertEquals(1, heapRegistry.size());
		assertTrue(heapRegistry.containsKey("BinaryHeap"));
		
		ConstructorRegistry<Collection<?>> collectionRegistry =
			new ConstructorRegistry<>(Collection.class, lyricCollectPackage);
		assertTrue(collectionRegistry.isEmpty());
		assertInvariants(collectionRegistry);
		collectionRegistry.loadAll();
		assertFalse(collectionRegistry.isEmpty());
		assertTrue(collectionRegistry.containsKey("BinaryHeap"));
		assertTrue(collectionRegistry.containsKey("UniquePriorityQueue"));
		assertFalse(collectionRegistry.containsKey("Tuple")); // is abstract
		assertFalse(collectionRegistry.containsKey("Tuple2")); // doesn't have no-argument constructor
		
		collectionRegistry.addPackage("java.util");
		assertArrayEquals(new String[] { lyricCollectPackage, "java.util" }, collectionRegistry.getPackages());
		assertSame(ArrayDeque.class, collectionRegistry.getClass("ArrayDeque"));
		assertInvariants(collectionRegistry);
	}
	
	private <T> void assertInvariants(ConstructorRegistry<T> registry)
	{
		Class<? super T> superClass = registry.getSuperClass();
		
		int size = registry.size();
		Set<Map.Entry<String,Constructor<T>>> entries = registry.entrySet();
		Set<String> keys = registry.keySet();
		Collection<Constructor<T>> constructors = registry.values();
		assertEquals(size, entries.size());
		assertEquals(size, keys.size());
		assertEquals(size, constructors.size());
		
		for (Map.Entry<String, Constructor<T>> entry : entries)
		{
			String name = entry.getKey();
			Constructor<T> constructor  = entry.getValue();
			
			assertTrue(keys.contains(name));
			assertTrue(registry.containsKey(name));
			assertTrue(registry.containsValue(constructor));
			assertSame(constructor, registry.get(name));
			
			Class<? super T> c = constructor.getDeclaringClass();
			assertTrue(superClass.isAssignableFrom(c));
			assertSame(c, registry.getClass(name));
			assertEquals(name, c.getSimpleName());
			
			T instance = registry.instantiate(name);
			assertNotNull(instance);
			assertSame(c, instance.getClass());
		}
	}
}
