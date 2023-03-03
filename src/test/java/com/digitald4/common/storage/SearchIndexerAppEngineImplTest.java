package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Searchable;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.common.collect.ImmutableList;
import java.util.stream.IntStream;
import org.junit.Test;

public class SearchIndexerAppEngineImplTest {
  private static final Long ID = 123L;
  private static final String USERNAME = "user1";
  private static final int TYPE_ID = 10;
  private static final String EMAIL = "user@company.com";
  private static final String FIRST_NAME = "Ricky";
  private static final String LAST_NAME = "Bobby";
  private static final BasicUser BASIC_USER = new BasicUser().setId(ID).setUsername(USERNAME)
      .setTypeId(TYPE_ID).setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME);

  private final Index index = mock(Index.class);

  private final SearchIndexerAppEngineImpl searchIndexer = new SearchIndexerAppEngineImpl() {
    @Override
    protected Index computeIndex(Class<?> c) {
      return index;
    }
  };

  @Test
  public void fromDocument() {
    Document searchDocument = Document.newBuilder()
        .setId(String.valueOf(ID))
        .addField(Field.newBuilder().setName("username").setAtom(USERNAME))
        .addField(Field.newBuilder().setName("typeId").setNumber(TYPE_ID))
        .addField(Field.newBuilder().setName("funFact").setHTML("User was born in the Bahamas"))
        .addField(Field.newBuilder().setName("docOnlyData").setAtom("No field for this"))
        .addField(Field.newBuilder().setName("firstName").setAtom(FIRST_NAME))
        .addField(Field.newBuilder().setName("lastName").setAtom(LAST_NAME))
        .addField(Field.newBuilder().setName("docOnlyNumber").setNumber(52))
        .addField(Field.newBuilder().setName("email").setAtom(EMAIL))
        .build();

    assertThat(searchIndexer.fromDocument(BasicUser.class, searchDocument)).isEqualTo(BASIC_USER);
  }

  @Test
  public void reindex() {
    searchIndexer.index(
        ImmutableList.of(
            new SearchableUser().setId(123L).setFirstName("First").setLastName("Last")));

    verify(index, times(1)).putAsync(anyListOf(Document.class));
  }

  @Test
  public void reindex_list() {
    searchIndexer.index(
        ImmutableList.of(
            new SearchableUser().setId(123L).setFirstName("First1").setLastName("Last1"),
            new SearchableUser().setId(456L).setFirstName("First2").setLastName("Last2")));

    verify(index, times(1)).putAsync(anyListOf(Document.class));
  }

  @Test
  public void reindex_listPagination() {
    searchIndexer.index(
        IntStream.range(1, 405)
            .mapToObj(
                i -> new SearchableUser()
                    .setId((long) i).setFirstName("First" + i).setLastName("Last" + i))
            .collect(toImmutableList()));

    verify(index, times(3)).putAsync(anyListOf(Document.class));
  }

  public static class SearchableUser extends BasicUser implements Searchable {
    @Override
    public SearchableUser setId(Long id) {
      super.setId(id);
      return this;
    }

    @Override
    public SearchableUser setFirstName(String firstName) {
      super.setFirstName(firstName);
      return this;
    }

    @Override
    public SearchableUser setLastName(String lastName) {
      super.setLastName(lastName);
      return this;
    }
  }
}