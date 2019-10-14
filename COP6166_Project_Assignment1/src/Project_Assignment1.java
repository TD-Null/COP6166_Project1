/*
 * Timothy Deligero, Viktor Orlorvic, Cody Dailidonis
 * Course: CO6616 (Multicore Programming)
 * Project Assignment #1
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
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
	
	// Contains the Value and Element for NotValue.
	int NotValue = Integer.MAX_VALUE;
	Node<Integer> NotValue_Elem = new Node<Integer>(NotValue);
	
	/*
	 * In the constructor, when given a capacity, have the initial and
	 * current capacity be the power of 2 of the given capacity, such that 
	 * the length of the first array segment is of 2^Y, where Y is the given
	 * capacity at the constructor. Add the first array segment with the
	 * current capacity.
	 */
	@SuppressWarnings("unchecked")
	Segmented(int capacity)
	{
		initialCapacity = currentCapacity = capacity;
		segments.add((Node<T> []) new Node[currentCapacity]);
		Arrays.fill(segments.get(0), NotValue_Elem);
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
		Node<T> [] array;
		
		/*
		 * Check if the array is within the size of the segments. If so,
		 * then get the array from the segmented storage at the segment
		 * index. If not, then the array is currently NULL.
		 */
		if(segmentIdx >= segments.size())
		{
			array = null;
		}
		
		else
		{
			array = this.segments.get(segmentIdx);
		}
		
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
	@SuppressWarnings("unchecked")
	Node<T> [] expand(int segIdx)
	{
		Node<T> [] array;
		
		// First, try to get the array at the given segment index.
		if(segIdx >= segments.size())
		{
			array = null;
		}
		
		else
		{
			array = this.segments.get(segIdx);
		}
		
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
			Node<T> [] newArray = (Node<T> []) new Node[newCapacity];
			Arrays.fill(newArray, NotValue_Elem);
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
		Node<T> [] array;
		
		/*
		 * Check if the array is within the size of the segments. If so,
		 * then get the array from the segmented storage at the segment
		 * index. If not, then the array is currently NULL.
		 */
		if(segmentIdx >= segments.size())
		{
			array = null;
		}
		
		else
		{
			array = this.segments.get(segmentIdx);
		}
		
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
		this.array = (Node<T> []) new Node [capacity];
		Arrays.fill(this.array, this.vec.NotValue_Elem);
	}
	
	/*
	 * Algorithm 3: When a thread attempts to resize the memory storage, it
	 * allocates a new Contiguous object with twice the capacity and a reference
	 * to the current Contiguous object. All elements up to the previous capacity
	 * are initialized as NotCopied, while the rest are initialized to NotValue.
	 * The elements from the old Contiguous object's array are copied onto the
	 * new Contiguous object's array.
	 */
	Contiguous<T> resize()
	{
		/*
		 * Create a new Contiguous object with twice the capacity. Have elements 
		 * up to the previous capacity initialized as NotCopied, while the rest 
		 * are initialized to NotValue.
		 */
		Contiguous<T> vnew = new Contiguous<T>(this.vec, this, this.capacity * 2);
		Arrays.fill(vnew.array, 0, this.capacity - 1, this.vec.NotCopied_Elem);
		Arrays.fill(vnew.array, this.capacity, vnew.array.length - 1, this.vec.NotValue_Elem);
		
		// Check if the current Contiguous object is the same as the old reference.
		if(this.vec.conStorage.equals(this))
		{
			// If so, copy all elements from the old reference into the new array.
			for(int i = this.capacity - 1; i >= 0; i--)
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
		if((int) this.array[pos].val == this.vec.NotCopied)
		{
			this.copyValue(pos);
		}
		
		// Return the element of the Contiguous object's array with the given position.
		return this.array[pos];
	}
	
	// Functions that stores an element within the contiguous memory storage at a given position.
	void store(int pos, Node<T> elem)
	{
		/*
		 * If the position given is greater then the current capacity, then
		 * use the resize() operation to allocate a new Contiguous object,
		 * then store the given element at the position of the new Contiguous
		 * object's array of elements.
		 */
		if(pos >= this.capacity)
		{
			this.resize();
			this.vec.conStorage.store(pos, elem);
		}
		
		else
		{
			/*
			 * Insert the element in the Contiguous object's array of 
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
	
	/*
	 * In the constructor, a boolean value is given to signify which type of
	 * internal storage to use for the vector class. Initialize the internal
	 * storage with the given capacity and set the current size of the Vector
	 * to be 0.
	 */
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
	 * Function that uses a pop operation on the Vectors' internal storage 
	 * given a position.
	 */
	@SuppressWarnings("unchecked")
	Node<T> popOp(int pos)
	{
		/*
		 * Get the popped element first at the tail of the Vector's 
		 * internal storage or array of elements.
		 */
		Node<T> pop_Elem = this.getSpot(pos);
		
		/*
		 * First, check which type of internal storage is being used for this 
		 * Vector. Then, store a NotValue element at the tail of the Vector's 
		 * internal storage or array of elements.
		 */
		if(!segmented_contiguous)
		{
			segStorage.store(pos, (Node<T>) NotValue_Elem);
		}
		
		else
		{
			conStorage.store(pos, (Node<T>) NotValue_Elem);
		}
		
		//System.out.println(pop_Elem.val);
		//System.out.println(this.getSpot(pos).val);
		
		// Return the popped element from the vector.
		return pop_Elem;
	}
	
	/* 
	 * Function that uses a push operation on the Vectors' internal storage 
	 * given a position a new Node element to push into the memory.
	 */
	void pushOp(int pos, Node<T> new_Node)
	{
		/*
		 * First, check which type of internal storage is being used for this 
		 * Vector. Then, store the new Node element at the tail of the Vector's 
		 * internal storage or array of elements.
		 */
		if(!segmented_contiguous)
		{
			segStorage.store(pos, new_Node);
		}
		
		else
		{
			conStorage.store(pos, new_Node);
		}
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
	 * Algorithm 6: A wait-free pop back operation that pops the
	 * element from the tail of the Vector's internal storage or
	 * array of elements.
	 */
	Return_Elem<T> WF_popBack()
	{
		// Get the position after the tail in the Vector's memory storage.
		int pos = this.size;
		
		/*
		 * If the position isn't within the bounds of Vector's memory
		 * storage, then return false and a NULL value.
		 */
		if(pos == 0)
		{
			return new Return_Elem<T>(false, null);
		}
			
		// Get the Node value at the given position.
		Node<T> spot = this.getSpot(pos - 1);
			
		/*
		 * If the current spot is a NotValue, then pop the element
		 * at the tail of the Vector's memory storage, decrement the
		 * size of the Vector, and return true and the value of the
		 * popped element. If not, then return false and a NULL value.
		 */
		if((int) spot.val != this.NotValue)
		{
			Node<T> pop_Elem = popOp(pos - 1);
			this.size -= 1;
			return new Return_Elem<T>(true, pop_Elem);
		}
		
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 9: A wait-free push back operation that pushes the
	 * given Node value onto the tail of the Vector's internal storage or
	 * array of elements.
	 */
	int WF_pushBack(Node<T> value)
	{
		// Get the position after the tail in the Vector's memory storage.
		int pos = this.size;
		
		// Get the Node value at the given position.
		Node<T> spot = this.getSpot(pos);
		//System.out.println(spot.val);
		/*
		 * If the current spot is a NotValue, then push the given Node
		 * value at the tail of the Vector's memory storage, increment the
		 * size of the Vector.
		 */
		if((int) spot.val == this.NotValue)
		{
			this.size += 1;
			
			/*
			 * If the position is 0, or the Vector is currently empty with
			 * a size of 0, then push the Node value at position 0. Else,
			 * push the value at the given position.
			 */
			if(pos == 0)
			{
				pushOp(0, value);
				return 0;
			}
			
			else
			{
				pushOp(pos, value);
				return pos - 1;
			}
			
		}
		
		return pos;
	}
	
	/*
	 * Algorithm 11: Compare and Set pop back operation that compares
	 * the value at the tail of the Vector's internal storage, and if
	 * valid, the size of the Vector is decremented and the Node value
	 * is popped from the Vector's memory.
	 */
	Return_Elem<T> CAS_popBack()
	{
		// Get the position of the tail in the Vector's memory storage.
		int pos = this.size - 1;
		
		/*
		 * If the position isn't within the bounds of Vector's memory
		 * storage, then return false and a NULL value.
		 */
		if(pos < 0)
		{
			return new Return_Elem<T>(false, null);
		}
		
		/*
		 * Get the Node value at the given position and if it is a 
		 * NotValue, then decrement the size, pop the element from
		 * the Vectors's internal storage, and return true and the
		 * value of the popped element. If not, then return false
		 * and a NULL value.
		 */
		else
		{
			Node<T> spot = this.getSpot(pos);
				
			if((int) spot.val != this.NotValue)
			{
				this.size -= 1;
				Node<T> value = popOp(pos);
				return new Return_Elem<T>(true, value);
			}
		}
			
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 12: Compare and Set push back operation that compares
	 * the value at the tail of the Vector's internal storage, and if
	 * valid, the size of the Vector is incremented and the given Node
	 * value is pushed onto the Vector's memory.
	 */
	int CAS_pushBack(Node<T> value)
	{
		// Get the position after the tail in the Vector's memory storage.
		int pos = this.size;
		
		// Get the Node value at the given position.
		Node<T> spot = this.getSpot(pos);
		
		/*
		 * If the Node value at the spot is a NotValue, then increment the
		 * size and push the given Node value onto the Vector's internal
		 * storage.
		 */
		if((int) spot.val == this.NotValue)
		{
			this.size += 1;
			pushOp(pos, value);
			return pos;
		}
		
		return pos;
	}
	
	/*
	 * Algorithm 13: Fetch-and-Add pop back operation that pops
	 * the Node element value in the Vector's internal storage at 
	 * the tail of the array of elements. This is done by fetching
	 * the position and popping the Node value at the position then
	 * decrementing the overall size of the Vector.
	 */
	Return_Elem<T> FAA_popBack()
	{
		/*
		 * Get the position of the tail of the array of elements
		 * and decrement the size afterwards.
		 */
		int pos = this.size - 1;
		this.size -= 1;
		
		/*
		 * If the given position is within the bounds of the Vector's
		 * internal storage of elements, then pop the element from the
		 * tail of the array of elements and returned true and popped
		 * element. If not, then increment the size to revert it back
		 * to its original value and return false and a NULL value.
		 */
		if(pos >= 0)
		{
			Node<T> value = this.getSpot(pos);
			popOp(pos);
			return new Return_Elem<T>(true, value);
		}
		
		this.size += 1;
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 14: Fetch-and-Add push back operation that pushes
	 * the given Node element value into the Vector's internal storage
	 * at the tail of the array of elements. This is done by fetching
	 * the position and pushing the Node value at the position then
	 * incrementing the overall size of the Vector.
	 */
	int FAA_pushBack(Node<T> value)
	{
		/*
		 * Get the position after the tail of the array of elements and 
		 * push the Node value onto the received position. Increment the
		 * size afterwards.
		 */
		int pos = this.size;
		pushOp(pos, value);
		this.size += 1;
		
		return pos;
	}
	
	/*
	 * Algorithm 15: Function that returns an element at the given position
	 * of the internal storage.
	 */
	Return_Elem<T> at(int pos)
	{
		/* 
		 * It is first checked if the position given isn't outside the capacity 
		 * of the internal storage.If so, then the thread cannot access that 
		 * position of the Vector, so return false and a NULL value.
		 */
		if(pos <= this.getCapacity())
		{
			// Get the Node element at the given position.
			Node<T> value = this.getSpot(pos);
			
			/* 
			 * If the value received is not equal to NotValue, then return true 
			 * and the value of the element. Else, return false and NULL.
			 */
			if((int) value.val != this.NotValue)
			{
				return new Return_Elem<T>(true, value);
			}
		}
		
		return new Return_Elem<T>(false, null);
	}
	
	/*
	 * Algorithm 16: A conditional write function that inserts a new
	 * Node element into the internal storage of the Vector, if the
	 * current Node element at the position is equal to the old Node
	 * element given in the function.
	 */
	Return_Elem<T> cwrite(int pos, Node<T> old_Elem, Node<T> new_Elem)
	{
		/* 
		 * It is first checked if the position given isn't outside the capacity 
		 * of the internal storage. If so, then the thread cannot access that 
		 * position of the Vector, so return false and a NULL value.
		 */
		if(pos <= this.getCapacity())
		{
			// Get the Node element at the given position.
			Node<T> value = this.getSpot(pos);
			
			/* 
			 * If the value received is equal to the old Node element given,
			 * then the store the new Node element at the given position,
			 * depending on what type of internal storage is used for this
			 * Vector and return true and the old Node element. If not, the
			 * return false and the new Node element.
			 */
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
	 * Algorithm 17: An insert function that inserts a given Node element 
	 * value at the given position. The elements must be shifted from the
	 * position to the tail of the Vector's internal storage or array of
	 * elements.
	 */
	boolean insertAt(int pos, Node<T> value)
	{
		/*
		 * First, check which type of memory storage is being used.
		 * Afterwards, insert the element into the Vector in the internal
		 * storage at the given position and shift the elements.
		 */
		if(!segmented_contiguous)
		{
			for(int i = size; i >= pos + 1; i--)
			{
				Node<T> shift = segStorage.getSpot(i - 1);
				segStorage.store(i, shift);
			}
			
			segStorage.store(pos, value);
		}
		
		else
		{
			for(int i = size; i >= pos + 1; i--)
			{
				Node<T> shift = conStorage.getSpot(i - 1);
				conStorage.store(i, shift);
			}
			
			conStorage.store(pos, value);
		}
		
		/*
		 * Increment the size of the Vector after inserting the element 
		 * into the memory storage.
		 */
		this.size += 1;
		
		return true;
	}
	
	/*
	 * Algorithm 18: An erase function that erase the Node element at the 
	 * given position. The elements must be shifted from the tail to the
	 * position of the Vector's internal storage or array of elements.
	 */
	boolean eraseAt(int pos)
	{
		/*
		 * First, check if the current size is 0. If so, then there is 
		 * nothing to erase from the Vector's internal storage.
		 */
		if(this.size == 0)
		{
			return false;
		}
		
		/*
		 * First, check which type of memory storage is being used.
		 * Afterwards, erase the element of the Vector in the internal
		 * storage at the given position and shift the elements.
		 */
		if(!segmented_contiguous)
		{
			for(int i = pos; i < this.size; i++)
			{
				Node<T> shift = segStorage.getSpot(i + 1);
				segStorage.store(i, shift);
			}
		}
		
		else
		{
			for(int i = pos; i < this.size; i++)
			{
				Node<T> shift = conStorage.getSpot(i + 1);
				conStorage.store(i, shift);
			}
		}
		
		/*
		 * Decrement the size of the Vector after erasing the element 
		 * from the memory storage.
		 */
		this.size -= 1;
		
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
		int random_pos = (int) (Math.random() * Project_Assignment1.vector.size);
		
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
		int random_pos = (int) (Math.random() * Project_Assignment1.vector.size);
		
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

class Cell
{
	AtomicLong seq;
	BitSet bits;
}

class MRLock
{
	Cell[] buffer;
	long mask;
	AtomicLong head;
	AtomicLong tail;
	
	MRLock(int size)
	{
		this.buffer = new Cell[size];
		this.mask = size - 1;
		this.head.set(0);
		this.tail.set(0);
		
		for(int i = 0; i < size; i++)
		{
			this.buffer[i].bits.set(0, size - 1);
			this.buffer[i].seq.set(i);
		}
	}
	
	long acquire(BitSet r)
	{
		Cell c;
		long pos = 0;
		
		while(true)
		{
			pos = this.tail.get();
			c = this.buffer[(int) (pos & this.mask)];
			long seq = c.seq.get();
			int dif = (int) seq - (int) pos;
			
			if(dif == 0)
			{
				if(this.tail.compareAndSet(pos, pos + 1))
				{
					break;
				}
			}
		}
		
		c.bits = r;
		c.seq.set(pos + 1);
		long spin = this.head.get();
		
		while(spin != pos)
		{
			BitSet result = this.buffer[(int) (spin & this.mask)].bits.get(0, 31);
			result.and(r);
			
			if((pos - this.buffer[(int) (spin & this.mask)].seq.get() > this.mask) 
					|| result.equals(0))
			{
				spin++;
			}
		}
		
		return pos;
	}
	
	void release(long h)
	{
		this.buffer[(int) (h & this.mask)].bits.set(0, (int) this.mask, false);
		long pos = this.head.get();
		
		while(this.buffer[(int) (pos & this.mask)].bits.equals(0))
		{
			Cell c = this.buffer[(int) (pos & this.mask)];
			long seq = c.seq.get();
			int dif = (int) seq - (int) (pos + 1);
			
			if(dif == 0)
			{
				if(this.head.compareAndSet(pos, pos + 1))
				{
					c.bits.set(0, 32);
					c.seq.set(pos + this.mask + 1);
				}
			}
			
			pos = this.head.get();
		}
	}
}

public class Project_Assignment1 
{
	// Contains the numbers of threads to use to test the wait-free vector.
	public static int num_threads = 1;
	
	// Contains a list of Nodes pre-allocated for each thread using during multithreading when accessing the stack.
	public static ArrayList<ArrayList<Node<Integer>>> threadNodes = new ArrayList<ArrayList<Node<Integer>>>(num_threads);
	
	// Contains the maximum number operations used for each thread when accessing the stack.
	public static int max_operations = 150000;
	
	// Contains the incremented number of operations for each test case.
	public static int inc_operations = 10000;
	
	// Contains the number of Nodes to insert into the stack before being accessed by multiple threads.
	public static int population = 500;
	
	// Contains a boolean value to signify either using segmented or contiguous memory in the Vector object.
	public static boolean segmented_contiguous = true;
	
	// Contains the initial capacity to be used when allocating a new Vector object.
	public static int capacity = 1024;
	
	// Contains the Vector object to be accessed by multiple threads.
	public static Vector<Integer> vector = new Vector<Integer>(segmented_contiguous, capacity);
	
	public static void main (String[] args)
    {
		/*
		 * Add a new list of Nodes for the new thread and populate the threads 
		 * with a list of Nodes. This is to allocate Nodes for each thread to
		 * use when accessing the Vector class object
		 */
		for(int i = 0; i < num_threads; i++)
		{
			threadNodes.add(new ArrayList<Node<Integer>>());
		}
		
		populateThreads(num_threads);
		
		System.out.println("# Operations:\tExecution time:");
		
		// For each test case, give different numbers of operations to use for each thread.
		for(int i = inc_operations; i <= max_operations; i += inc_operations)
		{
			// Declare a new Vector object for each test case.
			vector = new Vector<Integer>(segmented_contiguous, capacity);
			
			// Populate the vector with elements.
			populate(population);
			
			// Contains the threads that will be used for multithreading
			Thread threads[] = new Thread[num_threads];
			
			// Record the start of the execution time prior to spawning the threads.
			long start = System.nanoTime();
			
			// Spawn 4 concurrent threads accessing the stack.
			for(int j = 0; j < num_threads; j++)
			{
				threads[j] = new Thread(new VectorThread(j, i));
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
				
			// Convert the execution time to seconds.
			float execution_time = (float) duration / 1000000000;
			
			/*
			 * Print the number of operations used and the execution time 
			 * during multithreading.
			 */
			System.out.println(i + "\t\t" + execution_time + " sec");
		}
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
