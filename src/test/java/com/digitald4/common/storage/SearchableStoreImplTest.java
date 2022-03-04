package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.mockito.Mockito.*;

import com.digitald4.common.model.BasicUser;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mock;

import java.util.stream.IntStream;

public class SearchableStoreImplTest {

  @Mock private final DAO dao = mock(DAO .class);
  @Mock private final Index index = mock(Index.class);

  private final SearchableStoreImpl<BasicUser> searchableStore =
      new SearchableStoreImpl<BasicUser>(BasicUser.class, () -> dao, index) {
        @Override
        public Document toDocument(BasicUser user) {
          return Document.newBuilder()
              .addField(Field.newBuilder().setName("firstName").setAtom(user.getFirstName()))
              .addField(Field.newBuilder().setName("lastName").setAtom(user.getLastName()))
              .addField(Field.newBuilder().setName("fullName").setAtom(user.fullName()))
              .build();
        }

        @Override
        public BasicUser fromDocument(Document document) {
          return new BasicUser()
              .setFirstName(document.getOnlyField("firstName").getAtom())
              .setLastName(document.getOnlyField("lastName").getAtom());
        }
  };

  @Test
  public void reindex() {
    searchableStore.reindex(ImmutableList.of(new BasicUser().setFirstName("First").setLastName("Last")));

    verify(index, times(1)).putAsync(anyListOf(Document.class));
  }

  @Test
  public void reindex_list() {
    searchableStore.reindex(
        ImmutableList.of(
            new BasicUser().setFirstName("First1").setLastName("Last1"),
            new BasicUser().setFirstName("First2").setLastName("Last2")));

    verify(index, times(1)).putAsync(anyListOf(Document.class));
  }

  @Test
  public void reindex_listPagination() {
    searchableStore.reindex(
        IntStream.range(1, 405)
            .mapToObj(i -> new BasicUser().setFirstName("First" + i).setLastName("Last" + i))
            .collect(toImmutableList()));

    verify(index, times(3)).putAsync(anyListOf(Document.class));
  }
}
