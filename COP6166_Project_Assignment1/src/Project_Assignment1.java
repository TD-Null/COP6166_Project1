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
 * value and a representation of the resize bit.
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
	
	// Set the resize bit to 1.
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
		 * Use the given raw position from a thread to get the segment index 
		 * and item index locations within the list of segments.
		 */
		int pos = rawpos + this.initialCapacity;
		int itemIdx = pos ^ (1 << ((int) Math.floor(Math.log(pos) / Math.log(2))));
		int segmentIdx = ((int) Math.floor(Math.log(pos) / Math.log(2)) - ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))));
		
		// Obtain the array containing the requested element.
		Node<T> [] array = this.segments.get(segmentIdx);
		
		/*
		 * If the array is NULL, meaning that the memory storage has not
		 * been resized to given position, expand the list of segments
		 * with the given segment index.
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
	
	// Function that stores an element within the segmented memory storage at a given position.
	void store(int rawpos, Node<T> elem)
	{
		/*
		 * Use the given raw position from a thread to get the segment index 
		 * and item index locations within the list of segments.
		 */
		int pos = rawpos + this.initialCapacity;
		int itemIdx = pos ^ (1 << ((int) Math.floor(Math.log(pos) / Math.log(2))));
		int segmentIdx = ((int) Math.floor(Math.log(pos) / Math.log(2)) - ((int) Math.floor(Math.log(this.initialCapacity) / Math.log(2))));
		
		// Obtain the array containing the requested element.
		Node<T> [] array = this.segments.get(segmentIdx);
		
		/*
		 * If the array is NULL, meaning that the memory storage has not
		 * been resized to given position, expand the list of segments
		 * with the given segment index.
		 */
		if(array == null)
		{
			this.expand(segmentIdx);
		}
		
		/*
		 * Insert the given element into the memory with the given
		 * segment index and item index.
		 */
		this.segments.get(segmentIdx)[itemIdx] = elem;
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
	
	// Functions that stores an element within the contiguous memory storage at a given position.
	void store(int pos, Node<T> elem)
	{
		// Check first if the element at the position is a NotValue.
		if(this.getSpot(pos).equals(this.vec.NotValue_Elem))
		{
			/*
			 * Insert the element in the COntiguous object's array of 
			 * elements at the given position.
			 */
			this.array[pos] = elem;
		}
	}
}

/*
 * A wait-free vector class containing an internal storage, being either segmented
 * or contiguous, the current size, and utilizes tail operations, random access 
 * operations, and multi-position operations.
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
	
	@SuppressWarnings("unchecked")
	Node<T> popOp(int pos)
	{
		Node<T> pop_Elem = this.getSpot(pos);
		
		if(!segmented_contiguous)
		{
			segStorage.store(pos, (Node<T>) NotValue_Elem);
		}
		
		else
		{
			conStorage.store(pos, (Node<T>) NotValue_Elem);
		}
		
		return pop_Elem;
	}
	
	void pushOp(int pos, Node<T> new_Node)
	{
		if(!segmented_contiguous)
		{
			segStorage.store(pos, new_Node);
		}
		
		else
		{
			conStorage.store(pos, new_Node);
		}
	}
	
	void writeOp(int pos, Node<T> new_Node)
	{
		if(!segmented_contiguous)
		{
			segStorage.store(pos, new_Node);
		}
		
		else
		{
			conStorage.store(pos, new_Node);
		}
	}
	
	void shiftOp_insert(int pos, Node<T> new_Node)
	{
		
	}
	
	void shiftOp_erase(int pos)
	{
		
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
	 * Function within the Vector class to get the capacity of the Vector's
	 * internal storage. First, it is checked which type of storage is used 
	 * before getting the capacity.
	 */
	int getCapacity()
	{
		if(!segmented_contiguous)
		{
			return segStorage.currentCapacity;
		}

		return conStorage.capacity;
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
				return new Return_Elem<T>(false, null);
			}
			
			Node<T> spot = this.getSpot(pos);
			
			if((int) spot.val == NotValue)
			{
				Node<T> pop_Elem = popOp(pos - 1);
				this.size -= 1;
				return new Return_Elem<T>(true, pop_Elem);
			}
			
			else
			{
				pos++;
			}
		}
		
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 9:
	 */
	int WF_pushBack(Node<T> value)
	{
		int pos = this.size;
		
		for(int failures = 0; failures < limit; failures++)
		{
			Node<T> spot = this.getSpot(pos);
			
			if((int) spot.val == NotValue)
			{
				if(pos == 0)
				{
					if((int) spot.val == NotValue)
					{
						pushOp(0, value);
						this.size += 1;
						return 0;
					}
					
					else
					{
						pos++;
						spot = this.getSpot(pos);
					}
				}
				
				if((int) spot.val == NotValue)
				{
					pushOp(pos - 1, value);
					this.size += 1;
					return 0;
				}
				
				else
				{
					pos--;
				}
			}
		}
		
		return 0;
	}
	
	/*
	 * Algorithm 11:
	 */
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
				Node<T> spot = this.getSpot(pos);
				
				if((int) spot.val != NotValue)
				{
					this.size += 1;
					Node<T> value = popOp(pos);
					return new Return_Elem<T>(true, value);
				}
				
				pos--;
			}
		}
	}
	
	/*
	 * Algorithm 12:
	 */
	int CAS_pushBack()
	{
		int pos = this.size - 1;
		int failures = 0;
		
		while(true)
		{
			if(failures++ < limit)
			{
				
			}
			
			Node<T> spot = this.getSpot(pos);
			
			if((int) spot.val == NotValue)
			{
				size += 1;
				return pos;
			}
			
			pos++;
		}
	}
	
	/*
	 * Algorithm 13: 
	 */
	Return_Elem<T> FAA_popBack()
	{
		int pos = this.size - 1;
		this.size -= 1;
		
		if(pos >= 0)
		{
			Node<T> value = this.getSpot(pos);
			
			
			return new Return_Elem<T>(true, value);
		}
		
		this.size += 1;
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 14: 
	 */
	int FAA_pushBack(Node<T> value)
	{
		int pos = this.size;
		this.size += 1;
		
		
		return pos;
	}
	
	/*
	 * Algorithm 15: Function that return an element at the given position
	 * of the internal storage. It is first checked if the position given
	 * isn't outside the capacity of the internal storage. If so, then the
	 * thread cannot access that position of the Vector. If the value received
	 * is not equal to NotValue, then return true and the value of the element.
	 * Else, return false and NULL.
	 */
	Return_Elem<T> at(int pos)
	{
		if(pos <= this.getCapacity())
		{
			Node<T> value = this.getSpot(pos);
			
			if((int) value.val != this.NotValue)
			{
				return new Return_Elem<T>(true, value);
			}
		}
		
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 16: 
	 */
	Return_Elem<T> cwrite(int pos, Node<T> old_Elem, Node<T> new_Elem)
	{
		if(pos <= this.getCapacity())
		{
			Node<T> value = this.getSpot(pos);
			
			if(value.val == old_Elem.val)
			{
				if(!segmented_contiguous)
				{
					segStorage.store(pos, new_Elem);
				}
				
				else
				{
					conStorage.store(pos, new_Elem);
				}
				
				return new Return_Elem<T>(true, old_Elem);
			}
			
			else
			{
				return new Return_Elem<T>(false, value);
			}
		}
		
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 17: 
	 */
	boolean insertAt(int pos, Node<T> value)
	{
		shiftOp_insert(pos, value);
		return true;
	}
	
	/*
	 * Algorithm 18: 
	 */
	boolean eraseAt(int pos)
	{
		shiftOp_erase(pos);
		return true;
	}
}

/*
 * Thread class used to access the thread using tail operations,
 * random access operations, and multi-position operations at
 * random.
 */
class VectorThread extends Thread
{
	/*
	 * Contains an index value to identify each thread. 
	 * Used for accessing the current threads pre-allocated
	 * list of Nodes.
	 */
	int threadIndex;
	
	// Contains the number of operations to use for the current thread.
	int num_operations;
	
	// Counter used to access the thread's list of Nodes.
	int counter = 0;

	// In the constructor, initialize the thread ID and the number of operations.
	public VectorThread(int threadIndex, int num_operations)
	{
		this.threadIndex = threadIndex;
		this.num_operations = num_operations;
	}
	
	@Override
	public void run() 
	{
		// Contains the random number given.
		int random;
		
		// The thread will use up to the number of operations given to acccess the vector.
		for(int i = 0; i < num_operations; i++)
		{
			// Get a number of either 1 to 3 from the random number generator.
			random = (int) (Math.random() * 3) + 1;
			
			// If the number is 1, use a tail operation.
			if(random == 1)
			{
				tail_Operations();
			}
						
			// If the number is 2, use a random access operation.
			else if(random == 2)
			{
				randomAccess_Operations();
			}
			
			// If the number is 3, use a multi-position operation.
			else if(random == 3)
			{
				multiPosition_Operations();
			}
		}
	}
	
	// Use either a wait-free pop back or wait-free push back operation on the vector.
	private void tail_Operations()
	{
		// Random number generator.
		Random rand = new Random();
		
		// Get a number of either 0 or 1 from the random number generator.
		int random = rand.nextInt(2);
					
		// If the number is 0, use a wait-free pop back operation on the vector.
		if(random == 0)
		{
			// Pop the Node element at the tail of the vector.
			Project_Assignment1.vector.WF_popBack();
		}
					
		// If the number is 1, use a wait-free push back operation on the vector.
		else if(random == 1)
		{
			// Push a Node element from the thread's list of Nodes onto the tail of the vector.
			Node<Integer> n = Project_Assignment1.threadNodes.get(threadIndex).get(counter);
			Project_Assignment1.vector.WF_pushBack(n);
			counter++;
		}
	}
	
	// Use either an at() or conditional write operation on the vector. 
	private void randomAccess_Operations()
	{
		// Random number generator.
		Random rand = new Random();
		
		// Get a number of either 0 or 1 from the random number generator.
		int random = rand.nextInt(2);
		
		// Get a random position from the vector based on size.
		int random_pos = (int) (Math.random() * Project_Assignment1.vector.size) + 1;
		
		// If the number is 0, use a at() operation on the vector.
		if(random == 0)
		{
			// Get the element of the vector at the given position.
			Project_Assignment1.vector.at(random_pos);
		}
					
		// If the number is 1, use a conditional write operation on the vector.
		else if(random == 1)
		{
			/*
			 * Write a Node element at the given position of the vector using 
			 * a conditional write with a Node from thread's list of Nodes.
			 */
			Node<Integer> n = Project_Assignment1.threadNodes.get(threadIndex).get(counter);
			Project_Assignment1.vector.cwrite(random_pos, Project_Assignment1.vector.getSpot(random_pos), n);
			counter++;
		}
	}
	
	// Use either an insertAt() or eraseAt() operation on the vector.
	private void multiPosition_Operations()
	{
		// Random number generator.
		Random rand = new Random();
		
		// Get a number of either 0 or 1 from the random number generator.
		int random = rand.nextInt(2);
		
		// Get a random position from the vector based on size.
		int random_pos = (int) (Math.random() * Project_Assignment1.vector.size) + 1;
					
		// If the number is 0, use a insertAt() operation on the vector.
		if(random == 0)
		{
			/*
			 * Insert a Node element into the vector at the given position
			 * using a Node from thread's list of Nodes.
			 */
			Node<Integer> n = Project_Assignment1.threadNodes.get(threadIndex).get(counter);
			Project_Assignment1.vector.insertAt(random_pos, n);
			counter++;
		}
					
		// If the number is 1, use a eraseAt() pop back operation on the vector.
		else if(random == 1)
		{
			// Erase the Node element in the vector at the given position.
			Project_Assignment1.vector.eraseAt(random_pos);
		}
	}
}

public class Project_Assignment1 
{
	// Contains the numbers of threads to use to test the wait-free vector.
	public static int num_threads = 4;
	
	// Contains a list of Nodes pre-allocated for each thread using during multithreading when accessing the stack.
	public static ArrayList<ArrayList<Node<Integer>>> threadNodes = new ArrayList<ArrayList<Node<Integer>>>(num_threads);
	
	// Contains the maximum number operations used for each thread when accessing the stack.
	public static int max_operations = 150000;
	
	// Contains the number of Nodes to insert into the stack before being accessed by multiple threads.
	public static int population = 50000;
	
	// Contains a boolean value to signify either using segmented or contiguous memory in the Vector object.
	public static boolean segmented_contiguous = true;
	
	// Contains the initial capacity to be used when allocating a new Vector object.
	public static int capacity = 100;
	
	// Contains the Vector object to be accessed by multiple threads.
	public static Vector<Integer> vector = new Vector<Integer>(segmented_contiguous, capacity);
	
	public static void main (String[] args)
    {
		// Populate the vector with elements.
		populate(population);
		
		// Add a new list of Nodes for the new thread and populate the threads with a list of Nodes.
		for(int i = 0; i < num_threads; i++)
		{
			threadNodes.add(new ArrayList<Node<Integer>>());
		}
		
		populateThreads(num_threads);
		
		// Contains the threads that will be used for multithreading
		Thread threads[] = new Thread[num_threads];
		
		// Record the start of the execution time prior to spawning the threads.
		long start = System.nanoTime();
		
		// Spawn 4 concurrent threads accessing the stack.
		for(int i = 0; i < num_threads; i++)
		{
			threads[i] = new Thread(new VectorThread(i, max_operations));
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
	
	// Function used to populate the concurrent stack by pushing 'x' number of elements.
	public static void populate (int x)
	{
		for(int i = 0; i < x; i++)
		{
			Node<Integer> new_Node = new Node<Integer>(i);
			vector.WF_pushBack(new_Node);
		}
	}
	
	// Function used to populate each thread with its own list of Nodes.
	public static void populateThreads(int num_threads)
	{
		for(int i = 1; i <= num_threads; i++)
		{
			int start = (i * max_operations) + population;
			int end = ((i + 1) * max_operations) + population;
			
			for(int j = start; j < end; j++)
			{
				Node<Integer> new_Node = new Node<Integer>(j);
				threadNodes.get(i - 1).add(new_Node);
			}
		}
	}
}
