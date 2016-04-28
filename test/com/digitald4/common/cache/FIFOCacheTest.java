package com.digitald4.common.cache;

import static org.junit.Assert.*;

import org.junit.Test;

public class FIFOCacheTest {

	@Test
	public void testWithChars() {
		FIFOCache<Character, Character> cache = new FIFOCache<Character, Character>(10);
		assertEquals(0, cache.queue.size());
		
		cache.put('1', '1');
		assertEquals(1, cache.queue.size());
		assertEquals("[1]", cache.queue.toString());
		
		cache.put('2', '2').put('3', '3').put('4', '4').put('5', '5').put('6', '6').put('7', '7');
		assertEquals(7, cache.queue.size());
		assertEquals("[1, 2, 3, 4, 5, 6, 7]", cache.queue.toString());
		
		cache.put('8', '8').put('9', '9').put('A', 'A');
		assertEquals(10, cache.queue.size());
		assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, A]", cache.queue.toString());
		
		cache.put('B', 'B');
		assertEquals(10, cache.queue.size());
		assertEquals("[2, 3, 4, 5, 6, 7, 8, 9, A, B]", cache.queue.toString());
		
		cache.put('C', 'C').put('D', 'D').put('E', 'E').put('F', 'F').put('1', '1').put('2', '2');
		assertEquals(10, cache.queue.size());
		assertEquals("[8, 9, A, B, C, D, E, F, 1, 2]", cache.queue.toString());
	}

	@Test
	public void testWithObjects() {
		BookCache cache = new BookCache(10);
		assertEquals(0, cache.queue.size());
		
		Book bible = new Book(1, "Bible");
		cache.put(bible.hashCode(), bible);
		assertEquals(1, cache.queue.size());
		assertEquals("[1]", cache.queue.toString());
		
		Book toCatch = new Book(85, "To Catch a Mocking Bird");
		cache.put(toCatch.hashCode(), toCatch);
		assertEquals(2, cache.queue.size());
		assertEquals("[1, 85]", cache.queue.toString());
		
		cache.put(new Book(67, "Soul Man"));
		assertEquals(3, cache.queue.size());
		assertEquals("[1, 85, 67]", cache.queue.toString());
		
		cache.evict(toCatch.hashCode());
		assertEquals(2, cache.queue.size());
		assertEquals("[1, 67]", cache.queue.toString());
		
		cache.evict(67);
		assertEquals(1, cache.queue.size());
		assertEquals("[1]", cache.queue.toString());
		
		cache.put(new Book(789, "Think Like a Man"))
				.put(new Book(42, "Hop on Pop"))
				.put(new Book(1000, "Money Ball"));
		assertEquals(4, cache.queue.size());
		assertEquals("[1, 789, 42, 1000]", cache.queue.toString());
		
		cache.put(new Book(19, "1 Fish, 2 Fish, Red Fish, Blue Fish"))
				.put(new Book(24, "Legendary"))
				.put(new Book(23, "Jordan"));
		assertEquals(7, cache.queue.size());
		assertEquals("[1, 789, 42, 1000, 19, 24, 23]", cache.queue.toString());
		assertTrue(cache.contains(1));
		assertEquals(bible, cache.get(1));
		
		cache.put(new Book(8, "The Dream"))
				.put(new Book(942, "Hoop Hype"))
				.put(new Book(32, "Magic"))
				.put(new Book(44, "The Cube"));
		assertEquals(10, cache.queue.size());
		assertEquals("[789, 42, 1000, 19, 24, 23, 8, 942, 32, 44]", cache.queue.toString());
		assertFalse(cache.contains(1));
		assertNull(cache.get(1));
	}
	
	private class Book {
		private final int id;
		private final String title;
		
		public Book(int id, String title) {
			this.id = id;
			this.title = title;
		}
		
		@Override
		public int hashCode() {
			return id;
		}
		
		@Override
		public String toString() {
			return title;
		}
	}
	
	private class BookCache extends FIFOCache<Integer, Book> {
		public BookCache(int limit) {
			super(limit);
		}
		
		public BookCache put(Book book) {
			put(book.hashCode(), book);
			return this;
		}
	}
}
