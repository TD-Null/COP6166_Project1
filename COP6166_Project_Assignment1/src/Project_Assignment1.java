/*
 * Timothy Deligero, Viktor Orlorvic, Cody Dailidonis
 * Course: CO6616 (Multicore Programming)
 * Project Assignment #1
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.lang.*;

class Node<T>
{
	T val;
	int resizeBit = 0;
	
	// Class constructor.
	Node(T val)
	{
		this.val = val;
	}
	
	void setResizeBit()
	{
		this.resizeBit = 1;
	}
}

/*
 * Segmented memory.
 */
class Segmented<T>
{
	int initialCapacity;
	int currentCapacity;
	ArrayList<Node<T> []> segments = new ArrayList<Node<T> []>();
	
	@SuppressWarnings("unchecked")
	Segmented(int capacity)
	{
		initialCapacity = capacity;
		currentCapacity = capacity;
		segments.add((Node<T> []) new Node[capacity]);
	}
	
	Node<T> getSpot(int rawpos)
	{
		int pos = rawpos + this.initialCapacity;
		int itemIdx = pos ^ (1 << ((int) Math.floor(Math.log(pos) / Math.log(2))));
		int segmentIdx = ((int) Math.floor(Math.log(pos) / Math.log(2)) - ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))));
		Node<T> [] array = this.segments.get(segmentIdx);
		
		if(array == null)
		{
			array = this.expand(segmentIdx);
		}
		
		return array[itemIdx];
	}
	
	Node<T> [] expand(int segIdx)
	{
		Node<T> [] array = this.segments.get(segIdx);
		
		if(array == null)
		{
			int newCapacity = (1 << ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))) + segIdx);
			@SuppressWarnings("unchecked")
			Node<T> [] newArray = (Node<T> []) new Node[newCapacity];
			this.segments.add(newArray);
			this.currentCapacity += newCapacity;
			return this.segments.get(segIdx);
		}
		
		return array;
	}
}

/*
 * Contiguous memory
 */
class Contiguous<T>
{
	int capacity;
	Contiguous<T> old;
	Vector<T> vec;
	Node<T> [] array;
	int NotCopied = -1;
	
	@SuppressWarnings("unchecked")
	Contiguous(Vector<T> vec, Contiguous<T> old, int capacity)
	{
		this.capacity = capacity;
		this.old = old;
		this.vec = vec;
		array = (Node<T> []) new Node [capacity];
	}
	
	Contiguous<T> resize()
	{
		Contiguous<T> vnew = new Contiguous<T>(this.vec, this, this.capacity * 2);
		Arrays.fill(vnew.array, new Node<Integer>(NotCopied));
		
		for(int i = this.capacity; i >= 0; i--)
		{
			vnew.copyValue(i);
		}
		
		return this.vec.conStorage;
	}
	
	void copyValue(int pos)
	{
		if((int) this.old.array[pos].val == NotCopied)
		{
			this.old.copyValue(pos);
		}
		
		this.old.array[pos].setResizeBit();
		
		Node<T> v = this.old.array[pos];
		
		this.array[pos] = v;
	}
	
	Node<T> getSpot(int pos)
	{
		if(pos >= this.capacity)
		{
			Contiguous<T> newArray = this.resize();
			return newArray.getSpot(pos);
		}
		
		if((int) this.old.array[pos].val == NotCopied)
		{
			this.copyValue(pos);
		}
		
		return this.array[pos];
	}
}

class Vector<T>
{
	Segmented<T> segStorage;
	Contiguous<T> conStorage;
	int size;
	boolean segmented_contiguous;
	
	@SuppressWarnings("unchecked")
	Vector(boolean segmented_contiguous, int capacity)
	{
		this.segmented_contiguous = segmented_contiguous;
		
		if(!segmented_contiguous)
		{
			segStorage = new Segmented<T>(capacity);
		}
		
		else
		{
			conStorage = new Contiguous<T>(this, null, capacity);
		}
		
		size = 0;
	}
	
	boolean WFpopBack()
	{
		int pos = this.size;
		
	}
	
	
	T WFpushBack()
	{
		
	}
}

class VectorThread extends Thread
{

	public VectorThread()
	{
		
	}
	
	@Override
	public void run() 
	{
		
	}
}

public class Project_Assignment1 
{
	// Contains the maximum numbers of threads to use to test the lock-free stack.
	public static int max_threads = 4;
		
	public static void main (String[] args)
    {
		// Contains the number of threads to be generated.
		int num_threads = 1;
		
		for(int i = 1; i <= max_threads; i++)
		{
			// Contains the threads that will be used for multithreading.
			Thread threads[] = new Thread[num_threads];
			
			// Record the start of the execution time prior to spawning the threads.
			long start = System.nanoTime();
			
			// Spawn 'i' number of concurrent threads to access the Stack.
			for(int j = 0; j < num_threads; j++)
			{
				threads[j] = new Thread(new VectorThread());
				threads[j].start();
			}
			
			// Join the threads.
			for(int j = 0; j < num_threads; j++)
			{
				try
				{
					threads[j].join();
				}
				
				catch (Exception ex)
				{
					System.out.println("Failed to join thread.");
				}
			}
			
			// Record the end of the execution time after all threads are complete.
			long end = System.nanoTime();
			
			// Record the total execution time.
			long duration = end - start;
			
			// First convert the execution time to milliseconds then to seconds.
			float execution_time = (float) duration / 1000000000;
		}
    }
}
