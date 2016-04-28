package com.digitald4.common.cache;

import com.digitald4.common.test.DD4TestCase;

public class EHCacheImplTest extends DD4TestCase{

	public void test_Bbanks() {
		

	}

//	public void test_Generic_Objects() {
//
//		ICache<Author, Set<Book>> booksByAuthorCache = EHCacheImpl.getInstance("booksByAuthor", 60, 60, 5, false);
//
//		Author author1 = new Author("Dan", "Brown");
//		Author author2 = new Author("Stephen", "King");      
//
//		Book book1 = new Book("The Davinci Code", Sets.newHashSet(author1, author2), new Date(2006,8,06));
//		Book book2 = new Book("Carrie", Sets.newHashSet(author2), new Date(2004,02,02));
//		Book book3 = new Book("Pet Cemetary", Sets.newHashSet(author2), new Date(2000,11,01));
//
//		booksByAuthorCache.put(author1, Sets.newHashSet(book1));
//		booksByAuthorCache.put(author2, Sets.newHashSet(book1, book2, book3));
//
//		Assert.assertEquals(Sets.newHashSet(book1), booksByAuthorCache.get(author1));
//		Assert.assertEquals(Sets.newHashSet(book1, book2, book3), booksByAuthorCache.get(author2));
//
//		System.out.println(Iterators.getOnlyElement(booksByAuthorCache.get(author1).iterator()));      
//	}

}
