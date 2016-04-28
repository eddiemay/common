package com.digitald4.common.cache;

import java.io.Serializable;

public final class Author implements Serializable{

   /**
	 * 
	 */
	private static final long serialVersionUID = -4185356406486967647L;
private final String firstName;
   private final String lastName;

   public Author(final String firstName, final String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
   }

   public String getFirstName() {
      return this.firstName;
   }

   public String getLastName() {
      return this.lastName;
   }

   @Override
   public String toString() {
      return "Author [firstName=" + this.firstName + ", lastName=" + this.lastName + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.firstName == null) ? 0 : this.firstName.hashCode());
      result = prime * result + ((this.lastName == null) ? 0 : this.lastName.hashCode());
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
      if (!(obj instanceof Author)) {
         return false;
      }
      Author other = (Author) obj;
      if (this.firstName == null) {
         if (other.firstName != null) {
            return false;
         }
      } else if (!this.firstName.equals(other.firstName)) {
         return false;
      }
      if (this.lastName == null) {
         if (other.lastName != null) {
            return false;
         }
      } else if (!this.lastName.equals(other.lastName)) {
         return false;
      }
      return true;
   }
}
