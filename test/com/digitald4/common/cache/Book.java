package com.digitald4.common.cache;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public final class Book implements Serializable{

	private static final long serialVersionUID = 1L;
	private final String title;
	private final Set<Author> authors;
	private final Date datePublished;

   public Book(final String title, final Set<Author> authors, final Date datePublished) {
	   this.title = title;
	   this.authors = authors;
	   this.datePublished = datePublished;
   }

   public String getTitle() {
      return this.title;
   }

   public Set<Author> getAuthors() {
      return this.authors;
   }

   public Date getDatePublished() {
      return this.datePublished;
   }

   @Override
   public String toString() {
      return "Book [title=" + this.title + ", authors=" + this.authors + ", datePublished=" + this.datePublished + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.authors == null) ? 0 : this.authors.hashCode());
      result = prime * result + ((this.datePublished == null) ? 0 : this.datePublished.hashCode());
      result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Book)) {
         return false;
      }
      Book other = (Book) obj;
      if (this.authors == null) {
         if (other.authors != null) {
            return false;
         }
      } else if (!this.authors.equals(other.authors)) {
         return false;
      }
      if (this.datePublished == null) {
         if (other.datePublished != null) {
            return false;
         }
      } else if (!this.datePublished.equals(other.datePublished)) {
         return false;
      }
      if (this.title == null) {
         if (other.title != null) {
            return false;
         }
      } else if (!this.title.equals(other.title)) {
         return false;
      }
      return true;
   }
}
