/*
 * Timothy Deligero, Viktor Orlorvic, Cody Dailidonis
 * Course: CO6616 (Multicore Programming)
 * Project Assignment #1
 */

import java.io.*;
import java.util.*;
import java.lang.*;

/*
 * A Node class used in the Vector class to represent the
 * elements within the memory storage. Contains a Generic
 * value and a representation of the resize bit
 */
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
 * Contains an element to return a boolean value and a Node
 * elements from certain operations within the vector class.
 */
class Return_Elem<T>
{
	boolean check;
	Node<T> val;
	
	// Class constructor.
	Return_Elem(boolean check, Node<T> val)
	{
		this.check = check;
		this.val = val;
	}
}

/*
 * Class containing segmented memory for the storage of elements for 
 * the Vector class. This model has memory stored in a list of segments, 
 * allowing threads to append a new array segment to the list during 
 * resizing,  without having to copy elements over a new array. A thread 
 * will access the list in order to access an element with the given position.
 */
class Segmented<T>
{
	/*
	 * Fields containing the initial capacity when the class is
	 * first initialized and the current capacity to keep in check
	 * of the capacity after the memory storage has been expanded.
	 */
	int initialCapacity;
	int currentCapacity;
	
	// Contains the list of array segments containing the memory storage.
	ArrayList<Node<T> []> segments = new ArrayList<Node<T> []>();
	
	/*
	 * In the constructor, when given a capacity, have the initial and
	 * current be the power of 2 of the given capacity, such that the
	 * length of the first array segment is of 2^Y, where Y is the given
	 * capacity at the constructor. Add the first array segment with the
	 * current capacity.
	 */
	@SuppressWarnings("unchecked")
	Segmented(int capacity)
	{
		initialCapacity = currentCapacity = (int) Math.pow(2, capacity);
		segments.add((Node<T> []) new Node[currentCapacity]);
	}
	
	/*
	 * Algorithm 1: Using bitwise operations, the address of an elements
	 * can be obtained from the memory storage's list of segments.
	 */
	Node<T> getSpot(int rawpos)
	{
		/*
		 * Use the given raw position from a thread to get the segment ID and
		 * item ID locations within the list of segments.
		 */
		int pos = rawpos + this.initialCapacity;
		int itemIdx = pos ^ (1 << ((int) Math.floor(Math.log(pos) / Math.log(2))));
		int segmentIdx = ((int) Math.floor(Math.log(pos) / Math.log(2)) - ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))));
		
		// Obtain the array containing the requested element.
		Node<T> [] array = this.segments.get(segmentIdx);
		
		/*
		 * If the array is NULL, meaning that the memory storage has not
		 * been resized to given position, expand the list of segments
		 * with the given segment ID.
		 */
		if(array == null)
		{
			array = this.expand(segmentIdx);
		}
		
		/*
		 * Return the element of the memory storage with the given item ID
		 * of the current array segment.
		 */
		return array[itemIdx];
	}
	
	/*
	 * Algorithm 2: Expand the array up to the given segment ID using bitwise
	 * operations.
	 */
	Node<T> [] expand(int segIdx)
	{
		Node<T> [] array = this.segments.get(segIdx);
		
		// Check first if the given array segment is NULL before expanding.
		if(array == null)
		{
			// Get the new capacity using bitwise operations and being a power of 2.
			int newCapacity = (1 << ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))) + segIdx);
			
			/*
			 * With the new capacity, add the new array segment with the size of the 
			 * new capacity computed. Add the new capacity onto the current capacity
			 * of the memory storage.
			 */
			@SuppressWarnings("unchecked")
			Node<T> [] newArray = (Node<T> []) new Node[newCapacity];
			this.segments.add(newArray);
			this.currentCapacity += newCapacity;
			
			// Return the newly created array segment.
			return this.segments.get(segIdx);
		}
		
		// If the array segment is not NULL, then just return the segment itself.
		return array;
	}
}

/*
 * Class containing contiguous memory for the storage of elements for the
 * Vector class. This model has memory stored into an array of elements as
 * well as into the arrays of referenced old Contiguous objects. When resized,
 * the elements are copied over to a new array with twice the current capacity.
 */
class Contiguous<T>
{
	/*
	 * Fields containing the current capacity, reference to the old Contiguous
	 * object, reference to the vector class containing the Contiguous storage,
	 * and the array of elements in memory.
	 */
	int capacity;
	Contiguous<T> old;
	Vector<T> vec;
	Node<T> [] array;
	
	// In the constructor, initialize the field within the Contiguous object.
	@SuppressWarnings("unchecked")
	Contiguous(Vector<T> vec, Contiguous<T> old, int capacity)
	{
		this.capacity = capacity;
		this.old = old;
		this.vec = vec;
		array = (Node<T> []) new Node [capacity];
	}
	
	/*
	 * Algorithm 3: When a thread attempts to resize the memory storage, it
	 * allocates a new Contiguous object with twice the capacity and a reference
	 * to the current Contiguous object. All elements up to the previous capacity
	 * are initialized as NotCopied, while the rest are initialized to NotValue.
	 * The elements from the old Contiguous object's array are copied onto the
	 * new Contiguous object's array.
	 */
	@SuppressWarnings("unchecked")
	Contiguous<T> resize()
	{
		/*
		 * Create a new Contiguous object with twice the capacity. Have elements 
		 * up to the previous capacity initialized as NotCopied, while the rest 
		 * are initialized to NotValue.
		 */
		Contiguous<T> vnew = new Contiguous<T>(this.vec, this, this.capacity * 2);
		Arrays.fill(vnew.array, 0, this.capacity, this.vec.NotCopied_Elem);
		Arrays.fill(vnew.array, this.capacity + 1, vnew.array.length - 1, this.vec.NotValue_Elem);
		
		// Check if the current Contiguous object is the same as the old reference.
		if(this.vec.conStorage.equals(this))
		{
			// If so, copy all elements from the old reference into the new array.
			for(int i = this.capacity; i >= 0; i--)
			{
				vnew.copyValue(i);
			}
			
			// Initialize the Vector class's Contiguous object with the new one.
			this.vec.conStorage = vnew;
		}
		
		// Return the current Contiguous object of the vector.
		return this.vec.conStorage;
	}
	
	/*
	 * Algorithm 4: Copies the values from the old Contiguous object's array
	 * into the current Contiguous object;s array of elements.
	 */
	@SuppressWarnings("unchecked")
	void copyValue(int pos)
	{
		/*
		 * Check first if the value of the element is NotCopied, signifying
		 * that the position hasn't had the element copied over the position.
		 */
		if((int) this.old.array[pos].val == this.vec.NotCopied)
		{
			// Copy the element into the position of the array.
			this.old.copyValue(pos);
		}
		
		// Set the resize bit of the Node element to 1.
		this.old.array[pos].setResizeBit();
		
		/*
		 * Get the element of the Node from the old Contiguous object 
		 * with the given position and mark the value's two least significant 
		 * bits. Initialize the given position of the current Contiguous 
		 * object's array with the obtained element.
		 */
		Node<Integer> v = (Node<Integer>) this.old.array[pos];
		v.val = v.val & ~0x2;
		this.array[pos] = (Node<T>) v;
	}
	
	/*
	 * Algorithm 5: Get the element from the current Contiguous object's
	 * array at the given position.
	 */
	Node<T> getSpot(int pos)
	{
		/*
		 * If the position given is greater then the current capacity, then
		 * use the resize() operation to allocate a new Contiguous object,
		 * then get the element within the given position of the new array.
		 */
		if(pos >= this.capacity)
		{
			Contiguous<T> newArray = this.resize();
			return newArray.getSpot(pos);
		}
		
		/*
		 * If a thread sees that the position of the array is NotCopied, then
		 * copy the value from the old referenced Contiguous object.
		 */
		if((int) this.old.array[pos].val == this.vec.NotCopied)
		{
			this.copyValue(pos);
		}
		
		// Return the element of the Contiguous object's array with the given position.
		return this.array[pos];
	}
}

/*
 * A wait-free vector class containing an internal storage, being either segmented
 * or contiguous, the current size, and utilizes tail operations, a condition write
 * operation, and multi-position operations.
 */
class Vector<T>
{
	/*
	 * Fields containing a Segmented or Contiguous internal storage, the size,
	 * and the boolean value to represent which type of internal storage is
	 * used for the current Vector class.
	 */
	Segmented<T> segStorage;
	Contiguous<T> conStorage;
	int size;
	boolean segmented_contiguous;
	
	/*
	 * Contains the values and elements for the NotCopied and NotValue
	 * to be used for the operations of the Vector class.
	 */
	int NotCopied = Integer.MIN_VALUE;
	int NotValue = Integer.MAX_VALUE;
	Node<Integer> NotCopied_Elem = new Node<Integer>(NotCopied);
	Node<Integer> NotValue_Elem = new Node<Integer>(NotValue);
	
	// Contains the limit of failures when a thread attempts to do an operation.
	int limit = 100;
	
	/*
	 * In the constructor, a boolean value is given to signify which type of
	 * internal storage to use for the vector class. Initialize the internal
	 * storage with the given capacity and set the current size of the Vector
	 * to be 0.
	 */
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
	
	/*
	 * Function within the Vector class to get the element of the given
	 * position within the internal storage. First, it is checked which
	 * type of storage is used before getting the element from the spot.
	 */
	Node<T> getSpot(int pos)
	{
		if(!segmented_contiguous)
		{
			return segStorage.getSpot(pos);
		}

		return conStorage.getSpot(pos);
	}
	
	/*
	 * Algorithm 6: 
	 */
	Return_Elem<T> WF_popBack()
	{
		int pos = this.size;
		
		for(int failures = 0; failures < limit; failures++)
		{
			if(pos == 0)
			{
				return new Return_Elem(false, null);
			}
			
			Node<T> spot = this.getSpot(pos);
			
			if((int) spot.val == NotValue)
			{
				
			}
			
			else
			{
				pos++;
			}
		}
		
	}
	
	
	Node<T> WF_pushBack()
	{
		int pos = this.size;
		
		for(int failures = 0; failures < limit; failures++)
		{
			Node<T> spot = this.getSpot(pos);
			
			
		}
	}
	
	Return_Elem<T> CAS_popBack()
	{
		int pos = this.size - 1;
		int failures = 0;
		
		while(true)
		{
			if(failures++ < limit)
			{
				
			}
			
			else if(pos < 0)
			{
				return new Return_Elem<T>(false, null);
			}
			
			else
			{
				
			}
		}
	}
	
	T CAS_pushBack()
	{
		
	}
	
	Return_Elem<T> FAA_popBack()
	{
		
	}
	
	T FAA_pushBack()
	{
		
	}
	
	Return_Elem<T> cwrite(int pos, Node<T> old_Elem, Node<T> new_Elem)
	{
		
	}
	
	boolean insertAt(int pos, Node<T> value)
	{
		return true;
	}
	
	boolean eraseAt(int pos)
	{
		return true;
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
	// Contains the numbers of threads to use to test the wait-free vector.
	public static int num_threads = 4;
		
	public static void main (String[] args)
    {
		Thread threads[] = new Thread[num_threads];
		
		// Record the start of the execution time prior to spawning the threads.
		long start = System.nanoTime();
		
		// Spawn 4 concurrent threads accessing the stack.
		for(int i = 0; i < num_threads; i++)
		{
			threads[i] = new Thread(new VectorThread());
			threads[i].start();
		}
		
		// Join the threads.
		for(int i = 0; i < num_threads; i++)
		{
			try
			{
				threads[i].join();
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
